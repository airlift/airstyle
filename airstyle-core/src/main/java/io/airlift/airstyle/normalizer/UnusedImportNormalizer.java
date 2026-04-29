/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.airlift.airstyle.normalizer;

import io.airlift.airstyle.model.SourceModel;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Removes single-type imports and static member imports that are not
/// referenced anywhere in the compilation unit (including Javadoc tags).
/// Wildcard imports are left alone — deciding whether each member is used
/// requires full classpath resolution that this normalizer does not attempt.
///
/// ### Example
///
/// Before:
/// ```java
/// import java.util.List;
/// import java.util.Map;
/// import java.util.Optional;
///
/// class Test
/// {
///     List<String> names = new ArrayList<>();
/// }
/// ```
///
/// After:
/// ```java
/// import java.util.List;
///
/// class Test
/// {
///     List<String> names = new ArrayList<>();
/// }
/// ```
public final class UnusedImportNormalizer
{
    private UnusedImportNormalizer() {}

    public static String normalize(String source)
    {
        SourceModel sourceModel = SourceModel.create(source);
        CompilationUnit compilationUnit = sourceModel.compilationUnit();
        ASTRewrite rewrite = ASTRewrite.create(compilationUnit.getAST());
        List<ImportDeclaration> unusedImports = collectUnusedImports(sourceModel);
        if (unusedImports.isEmpty()) {
            return source;
        }

        for (ImportDeclaration importDeclaration : unusedImports) {
            // Keep the import when the user attached an explanatory comment
            // to it — removing the import would take the comment with it
            // and lose the author's rationale.
            if (hasAttachedComment(sourceModel, importDeclaration)) {
                continue;
            }
            rewrite.remove(importDeclaration, null);
        }

        String rewritten = AstRewrites.apply(source, rewrite);
        return removeSyntheticLeadingBlankLines(sourceModel, rewritten);
    }

    private static boolean hasAttachedComment(SourceModel sourceModel, ImportDeclaration importDeclaration)
    {
        int start = importDeclaration.getStartPosition();
        int end = start + importDeclaration.getLength();
        int lineStart = sourceModel.lineStart(start);
        int nextLineStart = sourceModel.lineEnd(end) + 1;
        String source = sourceModel.source();
        // Leading comment on the preceding line.
        if (lineStart > 0) {
            int prevLineEnd = lineStart - 1;
            int prevLineStart = sourceModel.lineStart(prevLineEnd);
            String prev = source.substring(prevLineStart, prevLineEnd).trim();
            if (prev.startsWith("//") || prev.startsWith("/*") || prev.startsWith("*")) {
                return true;
            }
        }
        // Trailing line comment on the same line as the import.
        String importLine = source.substring(lineStart, Math.min(nextLineStart, source.length()));
        int slashSlash = importLine.indexOf("//");
        int slashStar = importLine.indexOf("/*");
        int commentStart = (slashSlash >= 0 && (slashStar < 0 || slashSlash < slashStar)) ? slashSlash : slashStar;
        return commentStart > importDeclaration.getLength();
    }

    private static List<ImportDeclaration> collectUnusedImports(SourceModel sourceModel)
    {
        CompilationUnit compilationUnit = sourceModel.compilationUnit();
        Set<String> usedIdentifiers = collectUsedIdentifiers(compilationUnit);
        List<ImportDeclaration> unusedImports = new ArrayList<>();

        for (Object importObject : compilationUnit.imports()) {
            ImportDeclaration importDeclaration = (ImportDeclaration) importObject;
            if (isModuleImport(importDeclaration)) {
                continue;
            }
            if (importDeclaration.isOnDemand()) {
                continue;
            }

            String importName = importDeclaration.getName().getFullyQualifiedName();
            String simpleName = simpleName(importName);
            if (!usedIdentifiers.contains(simpleName)) {
                unusedImports.add(importDeclaration);
            }
        }

        return unusedImports;
    }

    private static String removeSyntheticLeadingBlankLines(SourceModel sourceModel, String rewritten)
    {
        CompilationUnit compilationUnit = sourceModel.compilationUnit();
        if (compilationUnit.getPackage() != null || compilationUnit.imports().isEmpty()) {
            return rewritten;
        }
        ImportDeclaration firstImport = (ImportDeclaration) compilationUnit.imports().getFirst();
        if (firstImport.getStartPosition() != 0 || !rewritten.startsWith("\n")) {
            return rewritten;
        }

        int index = 0;
        while (index < rewritten.length()) {
            int lineEnd = rewritten.indexOf('\n', index);
            if (lineEnd < 0) {
                return "";
            }
            if (!rewritten.substring(index, lineEnd).isBlank()) {
                return rewritten.substring(index);
            }
            index = lineEnd + 1;
        }
        return "";
    }

    private static Set<String> collectUsedIdentifiers(CompilationUnit compilationUnit)
    {
        Set<String> identifiers = new HashSet<>();
        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(Javadoc node)
            {
                return true;
            }

            @Override
            public boolean visit(SimpleName node)
            {
                if (isInImportOrPackageDeclaration(node)) {
                    return true;
                }
                if (isQualifiedNameSegmentThatDoesNotUseAnImport(node)) {
                    return true;
                }
                identifiers.add(node.getIdentifier());
                return true;
            }
        });
        return identifiers;
    }

    private static boolean isInImportOrPackageDeclaration(SimpleName node)
    {
        for (ASTNode current = node; current != null; current = current.getParent()) {
            if (current instanceof ImportDeclaration || current instanceof PackageDeclaration) {
                return true;
            }
        }
        return false;
    }

    private static boolean isQualifiedNameSegmentThatDoesNotUseAnImport(SimpleName node)
    {
        if (!(node.getParent() instanceof QualifiedName qualifiedName)) {
            return false;
        }
        if (qualifiedName.getQualifier() == node) {
            return false;
        }
        return qualifiedName.getQualifier() instanceof QualifiedName;
    }

    private static String simpleName(String fullyQualifiedName)
    {
        int lastDot = fullyQualifiedName.lastIndexOf('.');
        if (lastDot < 0) {
            return fullyQualifiedName;
        }
        return fullyQualifiedName.substring(lastDot + 1);
    }

    private static boolean isModuleImport(ImportDeclaration importDeclaration)
    {
        return Modifier.isModule(importDeclaration.getModifiers());
    }
}
