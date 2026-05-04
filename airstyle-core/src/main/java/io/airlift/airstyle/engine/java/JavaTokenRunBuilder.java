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
package io.airlift.airstyle.engine.java;

import io.airlift.airstyle.engine.Block;
import io.airlift.airstyle.engine.Indent;
import io.airlift.airstyle.engine.Spacing;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;

import java.util.ArrayList;
import java.util.List;

final class JavaTokenRunBuilder
{
    private final JavaSourceContext sourceContext;
    private final JavaSpacingPolicy spacingPolicy;

    JavaTokenRunBuilder(JavaSourceContext sourceContext, JavaSpacingPolicy spacingPolicy)
    {
        this.sourceContext = sourceContext;
        this.spacingPolicy = spacingPolicy;
    }

    Block buildTokensRange(int start, int end, String debugName)
    {
        return buildTokensRange(start, end, debugName, true);
    }

    Block buildTokensRange(int start, int end, String debugName, boolean canUseFirstChildIndent)
    {
        return buildTokensRange(start, end, debugName, canUseFirstChildIndent, TextBlockMarginPolicy.CANONICAL);
    }

    Block buildTokensRangePreservingTextBlockMargin(int start, int end, String debugName, boolean canUseFirstChildIndent)
    {
        return buildTokensRange(start, end, debugName, canUseFirstChildIndent, TextBlockMarginPolicy.PRESERVE_POSITIVE);
    }

    Block buildTokensRangePreservingFullTextBlockMargin(int start, int end, String debugName, boolean canUseFirstChildIndent)
    {
        return buildTokensRange(start, end, debugName, canUseFirstChildIndent, TextBlockMarginPolicy.PRESERVE_FULL);
    }

    Block buildTokensRangePreservingNegativeTextBlockMargin(int start, int end, String debugName, boolean canUseFirstChildIndent)
    {
        return buildTokensRange(start, end, debugName, canUseFirstChildIndent, TextBlockMarginPolicy.PRESERVE_NEGATIVE);
    }

    private Block buildTokensRange(int start, int end, String debugName, boolean canUseFirstChildIndent, TextBlockMarginPolicy textBlockMarginPolicy)
    {
        return buildTokenRun(JavaSourceRange.leaf(debugName, start, end), debugName, canUseFirstChildIndent, textBlockMarginPolicy);
    }

    Block buildTokensRangeWithLineStartIndent(int start, int end, String debugName, Indent lineStartIndent)
    {
        return buildTokenRun(JavaSourceRange.leaf(debugName, start, end), debugName, true, lineStartIndent);
    }

    Block buildLeafTokenRun(JavaSourceRange range, String debugName)
    {
        return buildTokenRun(range, debugName, true);
    }

    Block buildLeafTokenRun(JavaSourceRange range, String debugName, boolean canUseFirstChildIndent)
    {
        return buildTokenRun(range, debugName, canUseFirstChildIndent);
    }

    private Block buildTokenRun(JavaSourceRange range, String debugName, boolean canUseFirstChildIndent)
    {
        return buildTokenRun(range, debugName, canUseFirstChildIndent, TextBlockMarginPolicy.CANONICAL);
    }

    private Block buildTokenRun(JavaSourceRange range, String debugName, boolean canUseFirstChildIndent, TextBlockMarginPolicy textBlockMarginPolicy)
    {
        return buildTokenRun(range, debugName, canUseFirstChildIndent, null, textBlockMarginPolicy);
    }

    private Block buildTokenRun(JavaSourceRange range, String debugName, boolean canUseFirstChildIndent, Indent lineStartIndent)
    {
        return buildTokenRun(range, debugName, canUseFirstChildIndent, lineStartIndent, TextBlockMarginPolicy.CANONICAL);
    }

    private Block buildTokenRun(JavaSourceRange range, String debugName, boolean canUseFirstChildIndent, Indent lineStartIndent, TextBlockMarginPolicy textBlockMarginPolicy)
    {
        JavaBlock.Builder composite = JavaBlock.builder(range.start(), range.end(), debugName);
        if (!canUseFirstChildIndent) {
            composite.canUseFirstChildIndent(false);
        }
        List<JavaTokens.Token> tokens = sourceContext.tokensIn(range.start(), range.end());
        Block prev = null;
        JavaTokens.Token prevToken = null;
        int prevType = -1;
        int prevStart = -1;
        int prevEnd = -1;
        int prevNonCommentType = -1;
        int prevPrevNonCommentType = -1;
        int prevNonCommentStart = -1;
        int prevPrevNonCommentStart = -1;
        for (JavaTokens.Token token : tokens) {
            boolean startsLine = prevToken == null || sourceContext.lineBreakBetween(prevToken, token);
            Indent leafIndent = startsLine ? lineStartIndent : null;
            Block leaf = leafFor(token, leafIndent, textBlockMarginPolicy);
            if (prev != null) {
                Spacing spacing = spacingPolicy.between(
                        prevType,
                        prevStart,
                        prevEnd,
                        token,
                        prevPrevNonCommentType,
                        prevPrevNonCommentStart);
                composite.spacing(prev, leaf, spacing);
            }
            composite.child(leaf);
            prevToken = token;
            prev = leaf;
            prevType = token.type();
            prevStart = token.start();
            prevEnd = token.end();
            if (!token.isComment()) {
                prevPrevNonCommentType = prevNonCommentType;
                prevPrevNonCommentStart = prevNonCommentStart;
                prevNonCommentType = token.type();
                prevNonCommentStart = token.start();
            }
        }
        return composite.build();
    }

    private Block leafFor(JavaTokens.Token token, Indent indent, TextBlockMarginPolicy textBlockMarginPolicy)
    {
        if (token.text().startsWith("\"\"\"") && token.text().contains("\n")) {
            return textBlock(token, indent, textBlockMarginPolicy);
        }
        boolean trailingNewlineComment = token.isComment()
                && (token.type() == ITerminalSymbols.TokenNameCOMMENT_LINE
                || token.type() == ITerminalSymbols.TokenNameCOMMENT_MARKDOWN);
        if (trailingNewlineComment && token.text().endsWith("\n")) {
            return leaf(token.start(), token.end() - 1, token.text().substring(0, token.text().length() - 1), indent);
        }
        return leaf(token.start(), token.end(), token.text(), indent);
    }

    private Block textBlock(JavaTokens.Token token, Indent indent, TextBlockMarginPolicy textBlockMarginPolicy)
    {
        int openingColumn = columnOf(token.start());
        boolean startsAtLineIndent = startsAtLineIndent(token.start());
        List<LineRange> ranges = textBlockLineRanges(token.text(), openingColumn, startsAtLineIndent, textBlockMarginPolicy);
        if (ranges.isEmpty()) {
            return leaf(token.start(), token.end(), token.text(), indent);
        }

        JavaBlock.Builder textBlock = JavaBlock.builder(token.start(), token.end(), token.text())
                .canUseFirstChildIndent(false);
        if (indent != null) {
            textBlock.indent(indent);
        }

        Block previous = null;
        for (int index = 0; index < ranges.size(); index++) {
            LineRange range = ranges.get(index);
            Indent lineIndent = index == 0 ? Indent.noneIndent() : Indent.relativeSpaceIndent(range.indentOffset());
            Block line = leaf(token.start() + range.start(), token.start() + range.end(), token.text(), lineIndent);
            if (previous != null) {
                textBlock.spacing(previous, line, Spacing.createSpacing(0, 0, 1, true, 99));
            }
            textBlock.child(line);
            previous = line;
        }
        return textBlock.build();
    }

    private static List<LineRange> textBlockLineRanges(String text, int openingColumn, boolean startsAtLineIndent, TextBlockMarginPolicy textBlockMarginPolicy)
    {
        int firstNewline = text.indexOf('\n', 3);
        if (firstNewline < 0) {
            return List.of();
        }

        int jlsIndent = textBlockIndent(text);
        if (jlsIndent < 0) {
            return List.of();
        }
        int contentIndent = startsAtLineIndent ? jlsIndent : textBlockMinimumContentIndent(text);
        if (contentIndent < 0) {
            contentIndent = jlsIndent;
        }
        int rawBaseIndentOffset = jlsIndent - openingColumn;
        int baseIndentOffset = startsAtLineIndent ? textBlockMarginPolicy.baseIndentOffset(rawBaseIndentOffset) : 0;
        int closingIndentOffset = canonicalClosingIndentOffset(text, contentIndent, baseIndentOffset);

        List<LineRange> ranges = new ArrayList<>();
        ranges.add(new LineRange(0, firstNewline, 0));

        int lineStart = firstNewline + 1;
        while (lineStart < text.length()) {
            int lineEnd = text.indexOf('\n', lineStart);
            boolean lastLine = lineEnd < 0;
            if (lastLine) {
                lineEnd = text.length();
            }

            int quoteStart = lastLine && text.endsWith("\"\"\"") ? lineEnd - 3 : -1;
            boolean standaloneClosingLine = quoteStart >= lineStart
                    && containsOnlyWhitespace(text, lineStart, quoteStart);
            int contentStart = Math.min(lineStart + contentIndent, lineEnd);
            if (!lastLine && containsOnlyWhitespace(text, contentStart, lineEnd)) {
                contentStart = lineEnd;
            }
            else if (standaloneClosingLine) {
                contentStart = quoteStart;
            }

            int indentOffset = baseIndentOffset;
            if (standaloneClosingLine) {
                indentOffset = closingIndentOffset;
            }

            ranges.add(new LineRange(contentStart, lineEnd, indentOffset));
            lineStart = lineEnd + 1;
        }
        return List.copyOf(ranges);
    }

    private static int textBlockIndent(String text)
    {
        int firstNewline = text.indexOf('\n', 3);
        if (firstNewline < 0) {
            return -1;
        }

        int closingDelimiterStart = text.length() - 3;
        int minimum = Integer.MAX_VALUE;
        int lineStart = firstNewline + 1;
        while (lineStart < text.length()) {
            int lineEnd = text.indexOf('\n', lineStart);
            if (lineEnd < 0) {
                lineEnd = text.length();
            }

            int contentEnd = lineStart <= closingDelimiterStart && closingDelimiterStart <= lineEnd
                    ? closingDelimiterStart
                    : lineEnd;
            int firstNonWhitespace = firstNonWhitespace(text, lineStart, contentEnd);
            boolean closingLine = lineStart <= closingDelimiterStart && closingDelimiterStart <= lineEnd;
            if (closingLine || firstNonWhitespace < contentEnd) {
                minimum = Math.min(minimum, firstNonWhitespace - lineStart);
            }
            lineStart = lineEnd + 1;
        }
        return minimum == Integer.MAX_VALUE ? -1 : minimum;
    }

    private static int textBlockMinimumContentIndent(String text)
    {
        int firstNewline = text.indexOf('\n', 3);
        if (firstNewline < 0) {
            return -1;
        }

        int closingDelimiterStart = text.length() - 3;
        int minimum = Integer.MAX_VALUE;
        int lineStart = firstNewline + 1;
        while (lineStart < text.length()) {
            int lineEnd = text.indexOf('\n', lineStart);
            if (lineEnd < 0) {
                lineEnd = text.length();
            }

            if (!(lineStart <= closingDelimiterStart && closingDelimiterStart <= lineEnd)) {
                int firstNonWhitespace = firstNonWhitespace(text, lineStart, lineEnd);
                if (firstNonWhitespace < lineEnd) {
                    minimum = Math.min(minimum, firstNonWhitespace - lineStart);
                }
            }
            lineStart = lineEnd + 1;
        }
        return minimum == Integer.MAX_VALUE ? -1 : minimum;
    }

    private static int canonicalClosingIndentOffset(String text, int contentIndent, int baseIndentOffset)
    {
        List<Integer> nonBlankContentIndentOffsets = new ArrayList<>();
        boolean allEndWithContinuationEscape = true;
        int closingDelimiterStart = text.length() - 3;
        int lineStart = text.indexOf('\n', 3) + 1;
        while (lineStart > 0 && lineStart < text.length()) {
            int lineEnd = text.indexOf('\n', lineStart);
            if (lineEnd < 0) {
                lineEnd = text.length();
            }
            if (!(lineStart <= closingDelimiterStart && closingDelimiterStart <= lineEnd)) {
                int firstNonWhitespace = firstNonWhitespace(text, lineStart, lineEnd);
                if (firstNonWhitespace < lineEnd) {
                    nonBlankContentIndentOffsets.add(Math.max(0, firstNonWhitespace - lineStart - contentIndent));
                    if (text.charAt(lineEnd - 1) != '\\') {
                        allEndWithContinuationEscape = false;
                    }
                }
            }
            lineStart = lineEnd + 1;
        }
        if (nonBlankContentIndentOffsets.size() <= 1 || !allEndWithContinuationEscape) {
            return baseIndentOffset;
        }

        int firstOffset = nonBlankContentIndentOffsets.getFirst();
        int laterMinimumOffset = nonBlankContentIndentOffsets.stream()
                .skip(1)
                .mapToInt(Integer::intValue)
                .min()
                .orElse(0);
        if (firstOffset == 0 && laterMinimumOffset > 0) {
            return baseIndentOffset + laterMinimumOffset;
        }
        return baseIndentOffset;
    }

    private int columnOf(int offset)
    {
        int column = 0;
        for (int index = offset - 1; index >= 0; index--) {
            if (sourceContext.source().charAt(index) == '\n') {
                break;
            }
            column++;
        }
        return column;
    }

    private boolean startsAtLineIndent(int offset)
    {
        int lineStart = offset;
        while (lineStart > 0 && sourceContext.source().charAt(lineStart - 1) != '\n') {
            lineStart--;
        }
        for (int index = lineStart; index < offset; index++) {
            char current = sourceContext.source().charAt(index);
            if (current != ' ' && current != '\t') {
                return false;
            }
        }
        return true;
    }

    private static int firstNonWhitespace(String text, int start, int end)
    {
        int index = start;
        while (index < end && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index;
    }

    private static boolean containsOnlyWhitespace(String text, int start, int end)
    {
        for (int index = start; index < end; index++) {
            if (!Character.isWhitespace(text.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private static Block leaf(int start, int end, String debugName, Indent indent)
    {
        JavaBlock.Builder leaf = JavaBlock.builder(start, end, debugName);
        if (indent != null) {
            leaf.indent(indent);
        }
        return leaf.build();
    }

    private enum TextBlockMarginPolicy
    {
        CANONICAL,
        PRESERVE_POSITIVE,
        PRESERVE_NEGATIVE,
        PRESERVE_FULL;

        private int baseIndentOffset(int rawBaseIndentOffset)
        {
            return switch (this) {
                case CANONICAL -> 0;
                case PRESERVE_POSITIVE -> Math.max(0, rawBaseIndentOffset);
                case PRESERVE_NEGATIVE -> Math.min(0, rawBaseIndentOffset);
                case PRESERVE_FULL -> rawBaseIndentOffset;
            };
        }
    }

    private record LineRange(int start, int end, int indentOffset) {}
}
