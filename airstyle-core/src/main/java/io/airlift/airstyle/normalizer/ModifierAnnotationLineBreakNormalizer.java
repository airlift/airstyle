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
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.ArrayList;
import java.util.List;

/// Forces each declaration-level modifier annotation on a
/// class/method/field/enum/record to end with a newline so every
/// declaration-level annotation sits on its own line — `@VisibleForTesting final int x;`
/// becomes `@VisibleForTesting\nfinal int x;`. Only annotations
/// that appear before the first keyword modifier (public, private, final,
/// static, etc.) are treated as declaration-level; an annotation that
/// follows a keyword modifier is a type-use annotation (e.g.
/// `private final @Nullable String x`) and stays inline with the type. A trailing `//` or
/// `/* */` comment on the same line as the annotation is commentary about
/// the annotation itself and stays adjacent; only subsequent code tokens
/// trigger the split.
public final class ModifierAnnotationLineBreakNormalizer
{
    private ModifierAnnotationLineBreakNormalizer() {}

    public static String normalize(String source)
    {
        SourceModel sourceModel = SourceModel.create(source);
        CompilationUnit compilationUnit = sourceModel.compilationUnit();
        List<Replacement> replacements = new ArrayList<>();
        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(TypeDeclaration node)
            {
                collect(sourceModel, node, replacements);
                return true;
            }

            @Override
            public boolean visit(EnumDeclaration node)
            {
                collect(sourceModel, node, replacements);
                return true;
            }

            @Override
            public boolean visit(RecordDeclaration node)
            {
                collect(sourceModel, node, replacements);
                return true;
            }

            @Override
            public boolean visit(FieldDeclaration node)
            {
                collect(sourceModel, node, replacements);
                return true;
            }

            @Override
            public boolean visit(MethodDeclaration node)
            {
                collect(sourceModel, node, replacements);
                return true;
            }
        });
        return Replacement.applyAll(source, replacements);
    }

    private static void collect(SourceModel sourceModel, BodyDeclaration node, List<Replacement> replacements)
    {
        String source = sourceModel.source();
        int declStart = node.getStartPosition();
        int lineStart = sourceModel.lineStart(declStart);
        String indent = sourceModel.lineIndent(lineStart);

        for (Object modifier : node.modifiers()) {
            if (!(modifier instanceof Annotation annotation)) {
                // Any annotation after a keyword modifier is a type-use
                // annotation attached to the following type — leave it inline.
                break;
            }
            int annEnd = annotation.getStartPosition() + annotation.getLength();
            if (annEnd >= source.length()) {
                continue;
            }
            // Is there non-whitespace content between annEnd and end-of-line?
            // A `//` or `/*` on the same line is a trailing comment about the
            // annotation — leave it in place.
            int scan = annEnd;
            while (scan < source.length() && source.charAt(scan) != '\n') {
                char c = source.charAt(scan);
                if (c != ' ' && c != '\t' && c != '\r') {
                    boolean trailingComment = c == '/'
                            && scan + 1 < source.length()
                            && (source.charAt(scan + 1) == '/' || source.charAt(scan + 1) == '*');
                    if (!trailingComment) {
                        // Insert a line break right after the annotation.
                        replacements.add(new Replacement(annEnd, scan, "\n" + indent));
                    }
                    break;
                }
                scan++;
            }
        }
    }
}
