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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PackageDeclaration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Removes redundant imports: exact duplicates, imports of types in
/// `java.lang` (always available), and imports of types in the current
/// compilation unit's own package.
///
/// ### Example
///
/// Before:
/// ```java
/// package com.example;
///
/// import java.lang.String;
/// import java.util.List;
/// import java.util.List;
/// import com.example.Helper;
/// ```
///
/// After:
/// ```java
/// package com.example;
///
/// import java.util.List;
/// ```
public final class RedundantImportNormalizer
{
    private RedundantImportNormalizer() {}

    public static String normalize(String source)
    {
        SourceModel sourceModel = SourceModel.create(source);
        CompilationUnit compilationUnit = sourceModel.compilationUnit();
        List<ImportRemoval> redundantImports = collectRedundantImports(sourceModel, compilationUnit);
        if (redundantImports.isEmpty()) {
            return source;
        }

        List<Replacement> replacements = new ArrayList<>(redundantImports.size());
        for (ImportRemoval importRemoval : redundantImports) {
            replacements.add(removalReplacement(
                    sourceModel,
                    importRemoval.importDeclaration(),
                    importRemoval.preserveTrailingComment()));
        }

        return Replacement.applyAll(source, replacements);
    }

    private static List<ImportRemoval> collectRedundantImports(SourceModel sourceModel, CompilationUnit compilationUnit)
    {
        List<ImportDeclaration> imports = new ArrayList<>();
        for (Object importObject : compilationUnit.imports()) {
            ImportDeclaration importDeclaration = (ImportDeclaration) importObject;
            if (!isModuleImport(importDeclaration)) {
                imports.add(importDeclaration);
            }
        }

        List<ImportRemoval> redundantImports = new ArrayList<>();
        Set<ImportDeclaration> alreadyMarked = new HashSet<>();
        Map<String, List<ImportDeclaration>> importsByKey = new LinkedHashMap<>();
        for (ImportDeclaration importDeclaration : imports) {
            importsByKey.computeIfAbsent(dedupeKey(importDeclaration), _ -> new ArrayList<>())
                    .add(importDeclaration);
        }

        PackageDeclaration packageDeclaration = compilationUnit.getPackage();
        String packageName = packageDeclaration == null ? "" : packageDeclaration.getName().getFullyQualifiedName();

        for (List<ImportDeclaration> duplicateGroup : importsByKey.values()) {
            if (duplicateGroup.size() <= 1) {
                continue;
            }

            ImportDeclaration duplicateWinner = chooseDuplicateWinner(sourceModel, duplicateGroup);
            for (ImportDeclaration duplicateImport : duplicateGroup) {
                if (duplicateImport == duplicateWinner) {
                    continue;
                }
                if (alreadyMarked.add(duplicateImport)) {
                    redundantImports.add(new ImportRemoval(duplicateImport, false));
                }
            }
        }

        for (ImportDeclaration importDeclaration : imports) {
            if (importDeclaration.isStatic()) {
                continue;
            }

            String importName = importDeclaration.getName().getFullyQualifiedName();
            if (isJavaLangImport(importDeclaration, importName) || isSamePackageImport(importDeclaration, importName, packageName)) {
                if (alreadyMarked.add(importDeclaration)) {
                    redundantImports.add(new ImportRemoval(importDeclaration, true));
                }
            }
        }

        return redundantImports;
    }

    private static boolean isJavaLangImport(ImportDeclaration importDeclaration, String importName)
    {
        if (importDeclaration.isOnDemand()) {
            return "java.lang".equals(importName);
        }
        if (!importName.startsWith("java.lang.")) {
            return false;
        }

        // Only top-level java.lang types are implicitly imported.
        // Subpackages (e.g., java.lang.annotation.*) are not implicit and must be preserved.
        String suffix = importName.substring("java.lang.".length());
        return !suffix.contains(".");
    }

    private static boolean isSamePackageImport(ImportDeclaration importDeclaration, String importName, String packageName)
    {
        if (packageName.isEmpty()) {
            return false;
        }

        if (importDeclaration.isOnDemand()) {
            return packageName.equals(importName);
        }

        int lastDot = importName.lastIndexOf('.');
        if (lastDot < 0) {
            return false;
        }
        String importPackage = importName.substring(0, lastDot);
        return packageName.equals(importPackage);
    }

    private static Replacement removalReplacement(SourceModel sourceModel, ImportDeclaration importDeclaration, boolean preserveTrailingComment)
    {
        String source = sourceModel.source();
        int importStart = importDeclaration.getStartPosition();
        int importEnd = importStart + importDeclaration.getLength();
        int lineStart = sourceModel.lineStart(importStart);
        int lineEnd = sourceModel.lineEnd(lineStart);
        int lineEndExclusive = Math.min(source.length(), lineEnd + 1);

        int trailingCommentStart = preserveTrailingComment
                ? findTrailingCommentStart(source, importEnd, lineEnd)
                : -1;
        if (trailingCommentStart >= 0) {
            String preservedComment = source.substring(trailingCommentStart, lineEnd).stripLeading();
            if (lineEndExclusive > lineEnd) {
                preservedComment += source.substring(lineEnd, lineEndExclusive);
            }
            return new Replacement(lineStart, lineEndExclusive, preservedComment);
        }

        return new Replacement(lineStart, lineEndExclusive, "");
    }

    private static int findTrailingCommentStart(String source, int searchStart, int lineEndExclusive)
    {
        int lineCommentStart = source.indexOf("//", searchStart);
        if (lineCommentStart >= lineEndExclusive) {
            lineCommentStart = -1;
        }

        int blockCommentStart = source.indexOf("/*", searchStart);
        if (blockCommentStart >= lineEndExclusive) {
            blockCommentStart = -1;
        }

        if (lineCommentStart < 0) {
            return blockCommentStart;
        }
        if (blockCommentStart < 0) {
            return lineCommentStart;
        }
        return Math.min(lineCommentStart, blockCommentStart);
    }

    private static ImportDeclaration chooseDuplicateWinner(SourceModel sourceModel, List<ImportDeclaration> imports)
    {
        ImportDeclaration winner = null;
        for (ImportDeclaration importDeclaration : imports) {
            if (winner == null) {
                winner = importDeclaration;
            }
            if (hasTrailingInlineComment(sourceModel, importDeclaration)) {
                winner = importDeclaration;
            }
        }
        return winner;
    }

    private static boolean hasTrailingInlineComment(SourceModel sourceModel, ImportDeclaration importDeclaration)
    {
        String source = sourceModel.source();
        int importStart = importDeclaration.getStartPosition();
        int importEnd = importStart + importDeclaration.getLength();
        int lineStart = sourceModel.lineStart(importStart);
        int lineEnd = sourceModel.lineEnd(lineStart);
        return findTrailingCommentStart(source, importEnd, lineEnd) >= 0;
    }

    private static String dedupeKey(ImportDeclaration importDeclaration)
    {
        String importName = importDeclaration.getName().getFullyQualifiedName();
        return (importDeclaration.isStatic() ? "static:" : "normal:")
                + importName
                + (importDeclaration.isOnDemand() ? ".*" : "");
    }

    private static boolean isModuleImport(ImportDeclaration importDeclaration)
    {
        return Modifier.isModule(importDeclaration.getModifiers());
    }

    private record ImportRemoval(ImportDeclaration importDeclaration, boolean preserveTrailingComment) {}
}
