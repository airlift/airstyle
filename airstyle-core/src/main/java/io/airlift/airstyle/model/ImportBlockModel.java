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
package io.airlift.airstyle.model;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.jdt.core.dom.Modifier.isModule;

public final class ImportBlockModel
{
    private final String source;
    private final String[] lines;
    private final SourceModel sourceModel;
    private final List<ImportLine> imports;
    private final int startLine;
    private final int endLine;
    private final boolean hasUnsupportedContent;

    private ImportBlockModel(String source)
    {
        this.source = source;
        this.lines = source.split("\n", -1);
        this.sourceModel = SourceModel.create(source);
        this.imports = collectImports(sourceModel);
        this.startLine = imports.isEmpty() ? -1 : imports.getFirst().line();
        this.endLine = imports.isEmpty() ? -1 : imports.getLast().line();
        this.hasUnsupportedContent = computeHasUnsupportedContent();
    }

    public static ImportBlockModel create(String source)
    {
        return new ImportBlockModel(source);
    }

    public boolean hasImports()
    {
        return !imports.isEmpty();
    }

    public List<ImportLine> imports()
    {
        return imports;
    }

    public boolean hasUnsupportedContent()
    {
        return hasUnsupportedContent;
    }

    public String replaceImportsText(String importText)
    {
        if (!hasImports()) {
            return source;
        }

        StringBuilder result = new StringBuilder();
        for (int index = 0; index < startLine; index++) {
            if (index > 0) {
                result.append('\n');
            }
            result.append(lines[index]);
        }

        if (!importText.isEmpty()) {
            if (!result.isEmpty()) {
                result.append('\n');
            }
            result.append(importText);
        }

        for (int index = endLine + 1; index < lines.length; index++) {
            if (!result.isEmpty()) {
                result.append('\n');
            }
            result.append(lines[index]);
        }
        return result.toString();
    }

    public String insertImportsText(String importText)
    {
        if (importText.isEmpty()) {
            return source;
        }

        int insertionLine = determineInsertionLineWithoutImports();
        String prefix = joinLines(0, insertionLine);
        String suffix = joinLines(insertionLine, lines.length);

        prefix = trimTrailingBlankLines(prefix);
        suffix = trimLeadingBlankLines(suffix);

        StringBuilder result = new StringBuilder();
        if (!prefix.isEmpty()) {
            result.append(prefix).append("\n\n");
        }
        result.append(importText);
        if (!suffix.isEmpty()) {
            result.append("\n\n").append(suffix);
        }
        return result.toString();
    }

    private boolean computeHasUnsupportedContent()
    {
        if (!hasImports()) {
            return false;
        }

        Map<Integer, ImportLine> importsByLine = new HashMap<>();
        for (ImportLine importLine : imports) {
            importsByLine.put(importLine.line(), importLine);
        }

        for (int line = startLine; line <= endLine; line++) {
            String trimmed = lines[line].trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            ImportLine importLine = importsByLine.get(line);
            if (importLine != null && trimmed.equals(importLine.text())) {
                continue;
            }
            return true;
        }
        return false;
    }

    private List<ImportLine> collectImports(SourceModel sourceModel)
    {
        CompilationUnit compilationUnit = sourceModel.compilationUnit();
        @SuppressWarnings("unchecked")
        List<ImportDeclaration> importDeclarations = compilationUnit.imports();

        if (importDeclarations.isEmpty()) {
            return List.of();
        }

        List<ImportLine> results = new ArrayList<>(importDeclarations.size());
        for (ImportDeclaration importDeclaration : importDeclarations) {
            int line = sourceModel.lineNumber(importDeclaration.getStartPosition());
            int lineStart = sourceModel.lineStart(importDeclaration.getStartPosition());
            int lineEnd = sourceModel.lineEnd(lineStart);
            String text = source.substring(lineStart, lineEnd).trim();
            results.add(new ImportLine(
                    line,
                    text,
                    importDeclaration.getName().getFullyQualifiedName(),
                    importDeclaration.isStatic(),
                    importDeclaration.isOnDemand(),
                    isModule(importDeclaration.getModifiers())));
        }
        return List.copyOf(results);
    }

    private int determineInsertionLineWithoutImports()
    {
        PackageDeclaration packageDeclaration = sourceModel.compilationUnit().getPackage();
        if (packageDeclaration != null) {
            int packageEnd = packageDeclaration.getStartPosition() + packageDeclaration.getLength() - 1;
            return sourceModel.lineNumber(packageEnd) + 1;
        }

        ASTNode firstType = firstTopLevelType();
        if (firstType == null) {
            return 0;
        }

        return sourceModel.attachedLeadingCommentStartLine(firstType);
    }

    private ASTNode firstTopLevelType()
    {
        @SuppressWarnings("unchecked")
        List<ASTNode> types = sourceModel.compilationUnit().types();
        if (types.isEmpty()) {
            return null;
        }
        return types.getFirst();
    }

    private String joinLines(int startLineInclusive, int endLineExclusive)
    {
        if (startLineInclusive >= endLineExclusive) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (int index = startLineInclusive; index < endLineExclusive; index++) {
            if (index > startLineInclusive) {
                result.append('\n');
            }
            result.append(lines[index]);
        }
        return result.toString();
    }

    private static String trimTrailingBlankLines(String value)
    {
        String[] parts = value.split("\n", -1);
        int end = parts.length;
        while (end > 0 && parts[end - 1].trim().isEmpty()) {
            end--;
        }
        return joinLines(parts, 0, end);
    }

    private static String trimLeadingBlankLines(String value)
    {
        String[] parts = value.split("\n", -1);
        int start = 0;
        while (start < parts.length && parts[start].trim().isEmpty()) {
            start++;
        }
        return joinLines(parts, start, parts.length);
    }

    private static String joinLines(String[] parts, int start, int end)
    {
        if (start >= end) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (int index = start; index < end; index++) {
            if (index > start) {
                result.append('\n');
            }
            result.append(parts[index]);
        }
        return result.toString();
    }

    public record ImportLine(int line, String text, String importName, boolean isStatic, boolean isOnDemand, boolean isModuleImport) {}
}
