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

import io.airlift.airstyle.model.ImportBlockModel;
import io.airlift.airstyle.model.SourceModel;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Normalizes static-import usage for configured rules: some methods must be
/// called through a static import (so the qualified call is rewritten to the
/// unqualified form and the import is added), while other static imports are
/// banned (the import is removed and calls are requalified with the type).
///
/// ### Example
///
/// Given a rule that requires `assertThat` to be used as a static import and
/// another rule that bans a static import of `Collectors.toList`:
///
/// Before:
/// ```java
/// import static java.util.stream.Collectors.toList;
/// import org.assertj.core.api.Assertions;
///
/// class Test
/// {
///     void check()
///     {
///         Assertions.assertThat(list).isNotEmpty();
///         list.stream().collect(toList());
///     }
/// }
/// ```
///
/// After:
/// ```java
/// import java.util.stream.Collectors;
///
/// import static org.assertj.core.api.Assertions.assertThat;
///
/// class Test
/// {
///     void check()
///     {
///         assertThat(list).isNotEmpty();
///         list.stream().collect(Collectors.toList());
///     }
/// }
/// ```
public final class StaticImportRuleNormalizer
{
    private static final Set<String> BANNED_STATIC_METHOD_NAMES = Set.of("of", "copyOf", "valueOf", "builder");
    private static final String STRING_CLASS = "java.lang.String";
    private static final String OPTIONAL_CLASS = "java.util.Optional";
    private static final Set<String> BANNED_PLAIN_IMPORT_CLASSES = Set.of(
            "org.testng.Assert",
            "com.google.common.base.MoreObjects",
            "com.google.common.base.Preconditions",
            "com.google.common.base.Verify");

    private static final Map<String, Set<String>> REQUIRE_STATIC_IMPORTS = Map.of(
            "java.util.Objects", Set.of("requireNonNull"),
            "java.lang.Math", Set.of("toIntExact"),
            "com.google.common.collect.ImmutableMap", Set.of("toImmutableMap"),
            "com.google.common.collect.ImmutableList", Set.of("toImmutableList"),
            "com.google.common.collect.ImmutableSet", Set.of("toImmutableSet"));

    private StaticImportRuleNormalizer() {}

    public static String normalize(String source)
    {
        SourceModel sourceModel = SourceModel.create(source);
        CompilationUnit compilationUnit = sourceModel.compilationUnit();
        if (!mayNeedStaticImportNormalization(compilationUnit)) {
            return source;
        }
        ASTRewrite rewrite = ASTRewrite.create(compilationUnit.getAST());
        boolean[] sourceRewritten = {false};

        Map<String, Set<String>> bannedStaticImportsByMethod = new HashMap<>();
        Set<String> staticImportsToRemove = new HashSet<>();
        Set<String> importsToRemove = new HashSet<>();
        Set<String> staticImportsToAdd = new HashSet<>();
        Set<String> importsToAdd = new HashSet<>();
        Map<String, String> importedClassesBySimpleName = importedClassesBySimpleName(compilationUnit);

        collectImportRules(compilationUnit, bannedStaticImportsByMethod, staticImportsToRemove, importsToRemove);

        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(MethodInvocation node)
            {
                String methodName = node.getName().getIdentifier();
                Expression expression = node.getExpression();

                if (expression == null) {
                    Set<String> classes = bannedStaticImportsByMethod.get(methodName);
                    if (classes != null && classes.size() == 1) {
                        String classFqn = classes.iterator().next();
                        rewrite.set(
                                node,
                                MethodInvocation.EXPRESSION_PROPERTY,
                                compilationUnit.getAST().newSimpleName(simpleName(classFqn)),
                                null);
                        sourceRewritten[0] = true;
                        importsToAdd.add(classFqn);
                    }
                    return true;
                }

                if (!(expression instanceof Name expressionName)) {
                    return true;
                }

                String expressionFqn = resolveClassName(expressionName, importedClassesBySimpleName);
                Set<String> methods = REQUIRE_STATIC_IMPORTS.get(expressionFqn);
                if (methods != null && methods.contains(methodName)) {
                    rewrite.set(node, MethodInvocation.EXPRESSION_PROPERTY, null, null);
                    sourceRewritten[0] = true;
                    staticImportsToAdd.add(expressionFqn + "." + methodName);
                }
                else if (BANNED_PLAIN_IMPORT_CLASSES.contains(expressionFqn)) {
                    rewrite.set(node, MethodInvocation.EXPRESSION_PROPERTY, null, null);
                    sourceRewritten[0] = true;
                    staticImportsToAdd.add(expressionFqn + "." + methodName);
                    importsToRemove.add(expressionFqn);
                }

                return true;
            }
        });

        boolean importChanges = hasImportChanges(staticImportsToRemove, importsToRemove, staticImportsToAdd, importsToAdd);
        if (!sourceRewritten[0] && !importChanges) {
            return source;
        }

        ImportBlockModel importBlock = ImportBlockModel.create(source);
        if (importChanges && importBlock.hasImports() && importBlock.hasUnsupportedContent()) {
            return source;
        }

        String rewritten = sourceRewritten[0] ? AstRewrites.apply(source, rewrite) : source;
        return rewriteImports(rewritten, staticImportsToRemove, importsToRemove, staticImportsToAdd, importsToAdd);
    }

    private static boolean mayNeedStaticImportNormalization(CompilationUnit compilationUnit)
    {
        @SuppressWarnings("unchecked")
        List<ImportDeclaration> imports = compilationUnit.imports();
        for (ImportDeclaration importDeclaration : imports) {
            String importName = importDeclaration.getName().getFullyQualifiedName();
            if (importDeclaration.isStatic()) {
                if (importDeclaration.isOnDemand()) {
                    continue;
                }
                int lastDot = importName.lastIndexOf('.');
                if (lastDot < 0) {
                    continue;
                }
                if (isBannedStaticImport(importName.substring(0, lastDot), importName.substring(lastDot + 1))) {
                    return true;
                }
                continue;
            }
            if (BANNED_PLAIN_IMPORT_CLASSES.contains(importName)) {
                return true;
            }
        }

        Map<String, String> importedClassesBySimpleName = importedClassesBySimpleName(compilationUnit);
        boolean[] relevantInvocationFound = {false};
        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(MethodInvocation node)
            {
                if (relevantInvocationFound[0]) {
                    return false;
                }
                if (!(node.getExpression() instanceof Name expressionName)) {
                    return true;
                }

                String expressionFqn = resolveClassName(expressionName, importedClassesBySimpleName);
                String methodName = node.getName().getIdentifier();
                Set<String> methods = REQUIRE_STATIC_IMPORTS.get(expressionFqn);
                if ((methods != null && methods.contains(methodName)) || BANNED_PLAIN_IMPORT_CLASSES.contains(expressionFqn)) {
                    relevantInvocationFound[0] = true;
                    return false;
                }
                return true;
            }
        });
        return relevantInvocationFound[0];
    }

    private static void collectImportRules(
            CompilationUnit compilationUnit,
            Map<String, Set<String>> bannedStaticImportsByMethod,
            Set<String> staticImportsToRemove,
            Set<String> importsToRemove)
    {
        @SuppressWarnings("unchecked")
        List<ImportDeclaration> imports = compilationUnit.imports();
        for (ImportDeclaration importDeclaration : imports) {
            String importName = importDeclaration.getName().getFullyQualifiedName();
            if (!importDeclaration.isStatic()) {
                if (BANNED_PLAIN_IMPORT_CLASSES.contains(importName)) {
                    importsToRemove.add(importName);
                }
                continue;
            }
            if (importDeclaration.isOnDemand()) {
                continue;
            }

            int lastDot = importName.lastIndexOf('.');
            if (lastDot < 0) {
                continue;
            }
            String classFqn = importName.substring(0, lastDot);
            String member = importName.substring(lastDot + 1);

            if (isBannedStaticImport(classFqn, member)) {
                staticImportsToRemove.add(importName);
                bannedStaticImportsByMethod.computeIfAbsent(member, _ -> new HashSet<>()).add(classFqn);
            }
        }
    }

    private static boolean isBannedStaticImport(String classFqn, String member)
    {
        if (BANNED_STATIC_METHOD_NAMES.contains(member)) {
            return true;
        }
        if (member.equals("format") && !classFqn.equals(STRING_CLASS)) {
            return true;
        }
        return classFqn.equals(OPTIONAL_CLASS);
    }

    private static Map<String, String> importedClassesBySimpleName(CompilationUnit compilationUnit)
    {
        Map<String, String> importedClassesBySimpleName = new HashMap<>();

        @SuppressWarnings("unchecked")
        List<ImportDeclaration> imports = compilationUnit.imports();
        for (ImportDeclaration importDeclaration : imports) {
            if (importDeclaration.isStatic()) {
                continue;
            }

            String imported = importDeclaration.getName().getFullyQualifiedName();
            importedClassesBySimpleName.putIfAbsent(simpleName(imported), imported);
        }
        return importedClassesBySimpleName;
    }

    private static String resolveClassName(Name expressionName, Map<String, String> importedClassesBySimpleName)
    {
        if (expressionName.isQualifiedName()) {
            return expressionName.getFullyQualifiedName();
        }

        String expressionText = expressionName.getFullyQualifiedName();
        String imported = importedClassesBySimpleName.get(expressionText);
        if (imported != null) {
            return imported;
        }

        if (expressionText.equals("Objects")) {
            return "java.util.Objects";
        }
        if (expressionText.equals("Math")) {
            return "java.lang.Math";
        }
        return expressionText;
    }

    private static String rewriteImports(
            String source,
            Set<String> staticImportsToRemove,
            Set<String> importsToRemove,
            Set<String> staticImportsToAdd,
            Set<String> importsToAdd)
    {
        if (staticImportsToRemove.isEmpty() && importsToRemove.isEmpty() && staticImportsToAdd.isEmpty() && importsToAdd.isEmpty()) {
            return source;
        }

        ImportBlockModel importBlock = ImportBlockModel.create(source);
        List<String> remainingImports = new ArrayList<>();

        for (ImportBlockModel.ImportLine importLine : importBlock.imports()) {
            if (importLine.isStatic()) {
                if (!staticImportsToRemove.contains(importLine.importName())) {
                    remainingImports.add(importLine.text());
                }
            }
            else if (!importsToRemove.contains(importLine.importName())) {
                remainingImports.add(importLine.text());
            }
        }

        for (String importName : importsToAdd) {
            remainingImports.add("import " + importName + ";");
        }
        for (String importName : staticImportsToAdd) {
            remainingImports.add("import static " + importName + ";");
        }

        LinkedHashSet<String> deduplicated = new LinkedHashSet<>(remainingImports);
        List<String> updatedImports = new ArrayList<>(deduplicated);

        if (importBlock.hasImports()) {
            return importBlock.replaceImportsText(String.join("\n", updatedImports));
        }
        return importBlock.insertImportsText(String.join("\n", updatedImports));
    }

    private static boolean hasImportChanges(
            Set<String> staticImportsToRemove,
            Set<String> importsToRemove,
            Set<String> staticImportsToAdd,
            Set<String> importsToAdd)
    {
        return !staticImportsToRemove.isEmpty()
                || !importsToRemove.isEmpty()
                || !staticImportsToAdd.isEmpty()
                || !importsToAdd.isEmpty();
    }

    private static String simpleName(String className)
    {
        int lastDot = className.lastIndexOf('.');
        if (lastDot < 0 || lastDot == className.length() - 1) {
            return className;
        }
        return className.substring(lastDot + 1);
    }
}
