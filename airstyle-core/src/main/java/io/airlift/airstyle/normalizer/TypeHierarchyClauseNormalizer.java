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

import io.airlift.airstyle.JavaLanguageSupport;
import io.airlift.airstyle.model.SourceModel;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.airlift.airstyle.AirstyleFormatter.CONTINUATION_INDENT;

/// Normalizes `extends`, `implements`, and `permits` clauses: when a clause
/// has three or more items or already spans multiple lines, each item lands
/// on its own line at continuation indent; short two-item clauses stay
/// inline.
///
/// ### Example
///
/// Before:
/// ```java
/// class Handler implements Runnable, AutoCloseable, Comparable<Handler>, Serializable
/// {
/// }
/// ```
///
/// After:
/// ```java
/// class Handler
///         implements Runnable,
///                 AutoCloseable,
///                 Comparable<Handler>,
///                 Serializable
/// {
/// }
/// ```
public final class TypeHierarchyClauseNormalizer
{
    private TypeHierarchyClauseNormalizer() {}

    public static String normalize(String source, String referenceSource)
    {
        SourceModel sourceModel = SourceModel.create(source);
        Set<String> preserveOrderClauseKeys = collectPreserveOrderClauseKeys(referenceSource);
        CompilationUnit compilationUnit = sourceModel.compilationUnit();
        List<ClauseRewrite> rewrites = new ArrayList<>();

        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(TypeDeclaration node)
            {
                if (node.getSuperclassType() != null) {
                    addClauseRewrite(sourceModel, rewrites, preserveOrderClauseKeys, node, "extends", List.of(node.getSuperclassType()), false);
                }
                if (!node.superInterfaceTypes().isEmpty()) {
                    String keyword = node.isInterface() ? "extends" : "implements";
                    addClauseRewrite(sourceModel, rewrites, preserveOrderClauseKeys, node, keyword, types(node.superInterfaceTypes()), true);
                }
                if (!node.permittedTypes().isEmpty()) {
                    addClauseRewrite(sourceModel, rewrites, preserveOrderClauseKeys, node, "permits", types(node.permittedTypes()), true);
                }
                return true;
            }

            @Override
            public boolean visit(RecordDeclaration node)
            {
                if (!node.superInterfaceTypes().isEmpty()) {
                    addClauseRewrite(sourceModel, rewrites, preserveOrderClauseKeys, node, "implements", types(node.superInterfaceTypes()), true);
                }
                return true;
            }

            @Override
            public boolean visit(EnumDeclaration node)
            {
                if (!node.superInterfaceTypes().isEmpty()) {
                    addClauseRewrite(sourceModel, rewrites, preserveOrderClauseKeys, node, "implements", types(node.superInterfaceTypes()), true);
                }
                return true;
            }
        });

        if (rewrites.isEmpty()) {
            return source;
        }

        rewrites.sort(Comparator.comparingInt(ClauseRewrite::start).reversed());

        StringBuilder result = new StringBuilder(source);
        for (ClauseRewrite rewrite : rewrites) {
            result.replace(rewrite.start(), rewrite.end(), rewrite.replacement());
        }
        return result.toString();
    }

    private static void addClauseRewrite(
            SourceModel sourceModel,
            List<ClauseRewrite> rewrites,
            Set<String> preserveOrderClauseKeys,
            ASTNode declaration,
            String keyword,
            List<? extends Type> types,
            boolean sortItems)
    {
        String source = sourceModel.source();
        if (types.isEmpty()) {
            return;
        }

        int firstTypeStart = types.getFirst().getStartPosition();
        int lastTypeEnd = endPosition(types.getLast());
        int declarationStart = declaration.getStartPosition();

        int keywordStart = findClauseKeywordStart(sourceModel, declarationStart, firstTypeStart, keyword);
        if (keywordStart < declarationStart) {
            return;
        }
        if (sourceModel.commentOverlaps(firstTypeStart, lastTypeEnd)) {
            return;
        }

        int declarationLineStart = sourceModel.lineStart(declarationStart);
        String declarationIndent = leadingWhitespace(sourceModel, declarationLineStart);
        String clauseIndent = declarationIndent + CONTINUATION_INDENT;
        List<String> leadingComments = extractLeadingClauseComments(sourceModel, keywordStart + keyword.length(), firstTypeStart);

        int keywordLineStart = sourceModel.lineStart(keywordStart);
        boolean keywordStartsLine = sourceModel.containsOnlyWhitespace(keywordLineStart, keywordStart);

        int replaceStart;
        boolean leadingNewline;
        if (keywordStartsLine) {
            replaceStart = adjustReplaceStartForLeadingBlankLines(sourceModel, declarationLineStart, keywordLineStart);
            leadingNewline = false;
        }
        else {
            replaceStart = keywordStart;
            while (replaceStart > declarationStart && source.charAt(replaceStart - 1) == ' ') {
                replaceStart--;
            }
            leadingNewline = true;
        }

        List<String> items = extractItems(source, types);
        if (items.isEmpty()) {
            return;
        }

        boolean wrappedItems = isWrappedItemsClause(sourceModel, types);
        boolean keepInlineTwoItems = items.size() == 2 && !wrappedItems && leadingComments.isEmpty();
        boolean preserveExistingOrder = keepInlineTwoItems && preserveOrderClauseKeys.contains(clauseKey(declaration, keyword));
        if (sortItems && items.size() > 1 && !preserveExistingOrder) {
            items.sort(Comparator.naturalOrder());
        }
        String replacement = keepInlineTwoItems
                ? buildInlineClause(keyword, items, clauseIndent, keywordStartsLine, leadingNewline)
                : buildClause(keyword, items, clauseIndent, leadingNewline, leadingComments);
        rewrites.add(new ClauseRewrite(replaceStart, lastTypeEnd, replacement));
    }

    private static int adjustReplaceStartForLeadingBlankLines(SourceModel sourceModel, int minimumLineStart, int keywordLineStart)
    {
        int replaceStart = keywordLineStart;
        int line = sourceModel.lineNumber(keywordLineStart) - 1;
        while (line >= 0) {
            int previousLineStart = sourceModel.lineStartForLine(line);
            if (previousLineStart < minimumLineStart) {
                break;
            }

            int previousLineEnd = sourceModel.lineEnd(previousLineStart);
            if (!sourceModel.containsOnlyWhitespace(previousLineStart, previousLineEnd)) {
                break;
            }

            replaceStart = previousLineStart;
            line--;
        }
        return replaceStart;
    }

    private static Set<String> collectPreserveOrderClauseKeys(String source)
    {
        Set<String> keys = new HashSet<>();
        SourceModel sourceModel = SourceModel.create(source);
        CompilationUnit compilationUnit = sourceModel.compilationUnit();
        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(TypeDeclaration node)
            {
                if (!node.superInterfaceTypes().isEmpty()) {
                    String keyword = node.isInterface() ? "extends" : "implements";
                    recordPreserveOrderClause(sourceModel, keys, node, keyword, types(node.superInterfaceTypes()));
                }
                if (!node.permittedTypes().isEmpty()) {
                    recordPreserveOrderClause(sourceModel, keys, node, "permits", types(node.permittedTypes()));
                }
                return true;
            }

            @Override
            public boolean visit(RecordDeclaration node)
            {
                if (!node.superInterfaceTypes().isEmpty()) {
                    recordPreserveOrderClause(sourceModel, keys, node, "implements", types(node.superInterfaceTypes()));
                }
                return true;
            }

            @Override
            public boolean visit(EnumDeclaration node)
            {
                if (!node.superInterfaceTypes().isEmpty()) {
                    recordPreserveOrderClause(sourceModel, keys, node, "implements", types(node.superInterfaceTypes()));
                }
                return true;
            }
        });

        return keys;
    }

    private static void recordPreserveOrderClause(SourceModel sourceModel, Set<String> keys, ASTNode declaration, String keyword, List<? extends Type> types)
    {
        if (types.isEmpty()) {
            return;
        }

        int firstTypeStart = types.getFirst().getStartPosition();
        int declarationStart = declaration.getStartPosition();
        int keywordStart = findClauseKeywordStart(sourceModel, declarationStart, firstTypeStart, keyword);
        if (keywordStart < declarationStart) {
            return;
        }

        String source = sourceModel.source();
        int keywordLineStart = sourceModel.lineStart(keywordStart);
        boolean keywordStartsLine = sourceModel.containsOnlyWhitespace(keywordLineStart, keywordStart);
        List<String> items = extractItems(source, types);
        boolean wrappedItems = isWrappedItemsClause(sourceModel, types);
        boolean keepInlineTwoItems = items.size() == 2 && !wrappedItems;

        if (keywordStartsLine && keepInlineTwoItems) {
            keys.add(clauseKey(declaration, keyword));
        }
    }

    private static int findClauseKeywordStart(SourceModel sourceModel, int declarationStart, int firstTypeStart, String keyword)
    {
        int keywordStart = sourceModel.findLastKeywordBetween(declarationStart, firstTypeStart, keyword);
        if (keywordStart < declarationStart) {
            return keywordStart;
        }

        int tokenAfterKeyword = sourceModel.firstTokenTypeBetween(keywordStart + keyword.length(), firstTypeStart);
        if (tokenAfterKeyword != ITerminalSymbols.TokenNameEOF) {
            return -1;
        }
        return keywordStart;
    }

    private static String clauseKey(ASTNode declaration, String keyword)
    {
        return declarationIdentity(declaration) + "|" + keyword;
    }

    private static String declarationIdentity(ASTNode declaration)
    {
        List<String> parts = new ArrayList<>();
        ASTNode current = declaration;
        while (current != null) {
            switch (current) {
                case TypeDeclaration node -> parts.add("T:" + node.getName().getIdentifier());
                case EnumDeclaration node -> parts.add("E:" + node.getName().getIdentifier());
                case RecordDeclaration node -> parts.add("R:" + node.getName().getIdentifier());
                case MethodDeclaration node -> parts.add("M:" + node.getName().getIdentifier());
                case Initializer ignored -> parts.add("I");
                default -> {}
            }
            current = current.getParent();
        }

        StringBuilder key = new StringBuilder();
        for (int index = parts.size() - 1; index >= 0; index--) {
            if (!key.isEmpty()) {
                key.append('/');
            }
            key.append(parts.get(index));
        }
        return key.toString();
    }

    private static List<String> extractItems(String source, List<? extends Type> types)
    {
        List<String> items = new ArrayList<>();
        for (Type type : types) {
            int start = type.getStartPosition();
            int end = start + type.getLength();
            if (start < 0 || end > source.length() || start >= end) {
                continue;
            }
            String text = source.substring(start, end);
            text = normalizeItemWhitespace(text);
            if (!text.isEmpty()) {
                items.add(text);
            }
        }
        return items;
    }

    private static String normalizeItemWhitespace(String text)
    {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }

        IScanner scanner = newScanner();
        scanner.setSource(trimmed.toCharArray());

        StringBuilder result = new StringBuilder(trimmed.length());
        int previousTokenEnd = -1;
        try {
            while (true) {
                int token = scanner.getNextToken();
                if (token == ITerminalSymbols.TokenNameEOF) {
                    return result.toString();
                }

                int tokenStart = scanner.getCurrentTokenStartPosition();
                int tokenEnd = scanner.getCurrentTokenEndPosition() + 1;
                if (previousTokenEnd >= 0 && containsWhitespace(trimmed, previousTokenEnd, tokenStart)) {
                    result.append(' ');
                }
                result.append(trimmed, tokenStart, tokenEnd);
                previousTokenEnd = tokenEnd;
            }
        }
        catch (InvalidInputException _) {
            return trimmed;
        }
    }

    private static boolean containsWhitespace(String text, int start, int end)
    {
        for (int index = start; index < end; index++) {
            if (Character.isWhitespace(text.charAt(index))) {
                return true;
            }
        }
        return false;
    }

    private static IScanner newScanner()
    {
        return ToolFactory.createScanner(
                false,
                false,
                false,
                JavaLanguageSupport.latestJavaVersion(),
                JavaLanguageSupport.latestJavaVersion(),
                true);
    }

    private static boolean isWrappedItemsClause(SourceModel sourceModel, List<? extends Type> types)
    {
        if (types.size() < 2) {
            return false;
        }

        for (int index = 0; index < types.size() - 1; index++) {
            int leftEnd = endPosition(types.get(index));
            int rightStart = types.get(index + 1).getStartPosition();
            if (sourceModel.containsLineBreak(leftEnd, rightStart)) {
                return true;
            }
        }

        return false;
    }

    private static List<String> extractLeadingClauseComments(SourceModel sourceModel, int start, int end)
    {
        List<String> comments = new ArrayList<>();
        for (SourceModel.CommentRange commentRange : sourceModel.commentsContainedIn(start, end)) {
            comments.add(sourceModel.text(commentRange).trim());
        }
        return comments;
    }

    private static String buildClause(String keyword, List<String> items, String clauseIndent, boolean leadingNewline, List<String> leadingComments)
    {
        String alignIndent = clauseIndent + " ".repeat(keyword.length() + 1);

        StringBuilder builder = new StringBuilder();
        if (leadingNewline) {
            builder.append('\n');
        }
        appendLeadingComments(builder, clauseIndent, leadingComments);
        builder.append(clauseIndent)
                .append(keyword)
                .append(' ')
                .append(items.getFirst());

        if (items.size() > 1) {
            builder.append(',');
            for (int index = 1; index < items.size(); index++) {
                builder.append('\n')
                        .append(alignIndent)
                        .append(items.get(index));
                if (index < items.size() - 1) {
                    builder.append(',');
                }
            }
        }

        return builder.toString();
    }

    private static void appendLeadingComments(StringBuilder builder, String clauseIndent, List<String> leadingComments)
    {
        for (String comment : leadingComments) {
            if (!builder.isEmpty() && builder.charAt(builder.length() - 1) != '\n') {
                builder.append('\n');
            }
            String[] lines = comment.split("\n", -1);
            for (int index = 0; index < lines.length; index++) {
                if (index > 0) {
                    builder.append('\n');
                }
                builder.append(clauseIndent)
                        .append(lines[index].stripTrailing());
            }
            builder.append('\n');
        }
    }

    private static String buildInlineClause(String keyword, List<String> items, String clauseIndent, boolean keywordStartsLine, boolean leadingNewline)
    {
        StringBuilder builder = new StringBuilder();
        if (keywordStartsLine) {
            builder.append(clauseIndent);
        }
        else if (leadingNewline) {
            builder.append('\n')
                    .append(clauseIndent);
        }
        builder.append(keyword)
                .append(' ')
                .append(items.getFirst())
                .append(", ")
                .append(items.get(1));
        return builder.toString();
    }

    private static int endPosition(ASTNode node)
    {
        return node.getStartPosition() + node.getLength();
    }

    private static List<Type> types(List<?> rawTypes)
    {
        List<Type> result = new ArrayList<>(rawTypes.size());
        for (Object rawType : rawTypes) {
            result.add((Type) rawType);
        }
        return List.copyOf(result);
    }

    private static String leadingWhitespace(SourceModel sourceModel, int lineStart)
    {
        return sourceModel.lineIndent(lineStart);
    }

    private record ClauseRewrite(int start, int end, String replacement) {}
}
