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

import io.airlift.airstyle.model.LiteralLayoutModel;
import io.airlift.airstyle.model.SourceModel;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;

import java.util.ArrayList;
import java.util.List;

/// Removes leading and trailing blank lines inside multiline array
/// initializers so the content abuts the opening `{` and closing `}`.
///
/// ### Example
///
/// Before:
/// ```java
/// int[] values = {
///
///         1,
///         2,
///
/// };
/// ```
///
/// After:
/// ```java
/// int[] values = {
///         1,
///         2,
/// };
/// ```
public final class ArrayInitializerBlankLineNormalizer
{
    private ArrayInitializerBlankLineNormalizer() {}

    public static String normalize(String source)
    {
        SourceModel sourceModel = SourceModel.create(source);
        CompilationUnit compilationUnit = sourceModel.compilationUnit();
        List<Replacement> replacements = new ArrayList<>();

        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(ArrayInitializer node)
            {
                LiteralLayoutModel.ArrayLayout layout = LiteralLayoutModel.forArrayInitializer(sourceModel, node);
                addArrayInitializerReplacements(sourceModel, layout, replacements);
                return true;
            }
        });
        return Replacement.applyAll(source, replacements);
    }

    private static void addArrayInitializerReplacements(
            SourceModel sourceModel,
            LiteralLayoutModel.ArrayLayout layout,
            List<Replacement> replacements)
    {
        if (!layout.valid() || layout.expressions().isEmpty() || !layout.multiline()) {
            return;
        }

        addBracePrefixReplacement(sourceModel, layout, replacements);
        addTrailingSeparatorReplacement(sourceModel, layout, replacements);
        if (layout.nested()) {
            return;
        }

        if (layout.annotationContext()) {
            addAnnotationArrayLayoutReplacements(sourceModel, layout, replacements);
            addAnnotationOwnLineElementIndentReplacements(sourceModel, layout.expressions(), replacements);
            return;
        }
        addRegularArrayLayoutReplacements(sourceModel, layout, replacements);
        addMultilineIndentationReplacements(sourceModel, layout, replacements);
        addStandaloneCommentIndentationReplacements(sourceModel, layout, replacements);
    }

    private static void addBracePrefixReplacement(
            SourceModel sourceModel,
            LiteralLayoutModel.ArrayLayout layout,
            List<Replacement> replacements)
    {
        int openBrace = layout.openBrace();
        int previous = sourceModel.lastNonWhitespace(0, openBrace);
        if (previous < 0 || !sourceModel.containsOnlyWhitespace(previous + 1, openBrace)) {
            return;
        }

        String separator = sourceModel.source().substring(previous + 1, openBrace);
        if (!SourceModel.containsLineBreak(separator)) {
            return;
        }

        replacements.add(new Replacement(previous + 1, openBrace, " "));
    }

    private static void addTrailingSeparatorReplacement(
            SourceModel sourceModel,
            LiteralLayoutModel.ArrayLayout layout,
            List<Replacement> replacements)
    {
        int openBrace = layout.openBrace();
        int closeBrace = layout.closeBrace();
        int lastEnd = layout.lastExpressionEnd();
        if (lastEnd < 0 || closeBrace < lastEnd || closeBrace >= sourceModel.source().length()) {
            return;
        }

        String braceLineCommentReplacement = layout.nested() ? null : buildBraceLineCommentTailReplacement(sourceModel, openBrace, lastEnd, closeBrace);
        if (braceLineCommentReplacement != null) {
            int closeBraceLineEnd = sourceModel.lineEnd(closeBrace);
            replacements.add(new Replacement(lastEnd, closeBraceLineEnd, braceLineCommentReplacement));
            return;
        }

        String separator = sourceModel.source().substring(lastEnd, closeBrace);
        String replacement = buildTailSeparatorReplacement(sourceModel, lastEnd, closeBrace, layout.braceIndent());
        if (replacement == null) {
            return;
        }

        if (!separator.equals(replacement)) {
            replacements.add(new Replacement(lastEnd, closeBrace, replacement));
        }
    }

    private static void addAnnotationArrayLayoutReplacements(
            SourceModel sourceModel,
            LiteralLayoutModel.ArrayLayout layout,
            List<Replacement> replacements)
    {
        int openBrace = layout.openBrace();
        List<Expression> expressions = layout.expressions();
        Expression firstExpression = expressions.getFirst();
        int firstStart = firstExpression.getStartPosition();
        if (firstStart > openBrace && sourceModel.containsOnlyWhitespace(openBrace + 1, firstStart)) {
            String replacement = "\n" + layout.elementIndent();
            if (!sourceModel.source().substring(openBrace + 1, firstStart).equals(replacement)) {
                replacements.add(new Replacement(openBrace + 1, firstStart, replacement));
            }
        }
    }

    private static void addRegularArrayLayoutReplacements(
            SourceModel sourceModel,
            LiteralLayoutModel.ArrayLayout layout,
            List<Replacement> replacements)
    {
        int openBrace = layout.openBrace();
        List<Expression> expressions = layout.expressions();
        Expression firstExpression = expressions.getFirst();
        int firstStart = firstExpression.getStartPosition();
        if (firstStart > openBrace) {
            if (sourceModel.containsOnlyWhitespace(openBrace + 1, firstStart)) {
                String replacement = "\n" + layout.elementIndent();
                if (!sourceModel.source().substring(openBrace + 1, firstStart).equals(replacement)) {
                    replacements.add(new Replacement(openBrace + 1, firstStart, replacement));
                }
            }
        }
    }

    private static void addAnnotationOwnLineElementIndentReplacements(SourceModel sourceModel, List<Expression> expressions, List<Replacement> replacements)
    {
        String canonicalIndent = null;
        for (Expression expression : expressions) {
            int start = expression.getStartPosition();
            if (!sourceModel.startsAtLineIndent(start)) {
                continue;
            }

            int lineStart = sourceModel.lineStart(start);
            canonicalIndent = sourceModel.leadingWhitespace(lineStart, start);
            break;
        }

        if (canonicalIndent == null) {
            return;
        }

        for (Expression expression : expressions) {
            int start = expression.getStartPosition();
            if (!sourceModel.startsAtLineIndent(start)) {
                continue;
            }

            int lineStart = sourceModel.lineStart(start);
            String currentIndent = sourceModel.leadingWhitespace(lineStart, start);
            if (!currentIndent.equals(canonicalIndent)) {
                addReplacementIfRangeUnowned(replacements, lineStart, start, canonicalIndent);
            }
        }
    }

    private static String buildTailSeparatorReplacement(SourceModel sourceModel, int start, int endExclusive, String nextLineIndent)
    {
        SourceModel.RewriteSafety rewriteSafety = sourceModel.rewriteSafety(start, endExclusive);
        List<SourceModel.CommentRange> comments = rewriteSafety.containedComments();
        if (comments.isEmpty()) {
            return sourceModel.containsOnlyTokensWhitespaceAndComments(start, endExclusive, ITerminalSymbols.TokenNameCOMMA)
                    ? ",\n" + nextLineIndent
                    : null;
        }

        if (!sourceModel.containsOnlyTokensWhitespaceAndComments(start, endExclusive, ITerminalSymbols.TokenNameCOMMA)) {
            return null;
        }

        String inlineCommentTail = buildInlineSuffixCommentTailReplacement(sourceModel, start, comments, nextLineIndent);
        if (inlineCommentTail != null) {
            return inlineCommentTail;
        }

        // Source has no trailing comma and an inline comment on the last
        // element's line. Add the trailing comma while preserving the
        // author's alignment spacing between the element and the comment:
        //   0 or 1 spaces  →  `", "` (comma + 1 space)
        //   N ≥ 2 spaces   →  replace the first space with the comma so
        //                     the remaining (N−1) spaces stay. Keeps
        //                     column-aligned trailing comments aligned.
        return buildInlineSuffixAddCommaReplacement(sourceModel, start, comments, nextLineIndent);
    }

    private static String buildInlineSuffixAddCommaReplacement(
            SourceModel sourceModel,
            int anchor,
            List<SourceModel.CommentRange> comments,
            String nextLineIndent)
    {
        int anchorLine = sourceModel.lineNumber(anchor);
        int lineEnd = sourceModel.lineEnd(anchor);
        for (SourceModel.CommentRange comment : comments) {
            if (sourceModel.lineNumber(comment.start()) != anchorLine || comment.end() > lineEnd) {
                return null;
            }
        }
        int firstCommentStart = comments.getFirst().start();
        String spacing = sourceModel.source().substring(anchor, firstCommentStart);
        // Only plain-space spacing is handled here; tabs / other whitespace
        // are unusual and safer to leave unchanged.
        for (int i = 0; i < spacing.length(); i++) {
            if (spacing.charAt(i) != ' ') {
                return null;
            }
        }
        String commaWithSpacing = spacing.length() <= 1
                ? ", "
                : "," + spacing.substring(1);
        String inlineCommentText = sourceModel.source()
                .substring(firstCommentStart, lineEnd)
                .stripTrailing();
        return commaWithSpacing + inlineCommentText + "\n" + nextLineIndent;
    }

    private static String buildBraceLineCommentTailReplacement(SourceModel sourceModel, int openBrace, int lastEnd, int closeBrace)
    {
        int closeBraceLineEnd = sourceModel.lineEnd(closeBrace);
        SourceModel.RewriteSafety rewriteSafety = sourceModel.rewriteSafety(lastEnd, closeBraceLineEnd);
        List<SourceModel.CommentRange> comments = rewriteSafety.containedComments();
        if (comments.isEmpty()) {
            return null;
        }

        SourceModel.CommentRange firstComment = comments.getFirst();
        int closeBraceLine = sourceModel.lineNumber(closeBrace);
        if (sourceModel.lineNumber(firstComment.start()) != closeBraceLine) {
            return null;
        }
        for (SourceModel.CommentRange comment : comments) {
            if (sourceModel.lineNumber(comment.start()) != closeBraceLine) {
                return null;
            }
        }

        if (!sourceModel.containsOnlyTokensWhitespaceAndComments(lastEnd, closeBrace, ITerminalSymbols.TokenNameCOMMA)) {
            return null;
        }

        String closingSuffix = sourceModel.source().substring(closeBrace, firstComment.start()).stripTrailing();
        if (closingSuffix.isEmpty() || closingSuffix.charAt(0) != '}') {
            return null;
        }
        if (closingSuffix.indexOf('\n') >= 0) {
            return null;
        }

        String inlineCommentText = sourceModel.source()
                .substring(firstComment.start(), closeBraceLineEnd)
                .stripTrailing();
        return ", " + inlineCommentText + "\n" + braceIndent(sourceModel, openBrace) + closingSuffix;
    }

    private static String buildInlineSuffixCommentTailReplacement(
            SourceModel sourceModel,
            int anchor,
            List<SourceModel.CommentRange> comments,
            String nextLineIndent)
    {
        int anchorLine = sourceModel.lineNumber(anchor);
        int lineEnd = sourceModel.lineEnd(anchor);
        for (SourceModel.CommentRange comment : comments) {
            if (sourceModel.lineNumber(comment.start()) != anchorLine || comment.end() > lineEnd) {
                return null;
            }
        }

        int firstCommentStart = comments.getFirst().start();
        String prefix = sourceModel.source().substring(anchor, firstCommentStart);
        int commaIndex = prefix.indexOf(',');
        if (commaIndex < 0) {
            return null;
        }

        String spacingAfterComma = prefix.substring(commaIndex + 1);
        String inlineCommentText = sourceModel.source()
                .substring(firstCommentStart, lineEnd)
                .stripTrailing();
        return "," + spacingAfterComma + inlineCommentText + "\n" + nextLineIndent;
    }

    private static void addMultilineIndentationReplacements(
            SourceModel sourceModel,
            LiteralLayoutModel.ArrayLayout layout,
            List<Replacement> replacements)
    {
        if (!layout.hasOwnLineElement()) {
            return;
        }

        String elementIndent = layout.elementIndent();
        List<Expression> expressions = layout.expressions();
        for (Expression expression : expressions) {
            int start = expression.getStartPosition();
            if (!sourceModel.startsAtLineIndent(start)) {
                continue;
            }
            int lineStart = sourceModel.lineStart(start);
            String currentIndent = sourceModel.leadingWhitespace(lineStart, start);
            if (!currentIndent.equals(elementIndent)) {
                addReplacementIfRangeUnowned(replacements, lineStart, start, elementIndent);
            }
        }
    }

    private static void addStandaloneCommentIndentationReplacements(
            SourceModel sourceModel,
            LiteralLayoutModel.ArrayLayout layout,
            List<Replacement> replacements)
    {
        String elementIndent = layout.elementIndent();
        int openBrace = layout.openBrace();
        int closeBrace = layout.closeBrace();
        int openBraceLine = sourceModel.lineNumber(openBrace);
        int closeBraceLine = sourceModel.lineNumber(closeBrace);

        for (Comment comment : sourceModel.comments()) {
            int commentStart = comment.getStartPosition();
            if (commentStart <= openBrace || commentStart >= closeBrace) {
                continue;
            }

            int commentLine = sourceModel.lineNumber(commentStart);
            if (commentLine <= openBraceLine || commentLine >= closeBraceLine) {
                continue;
            }

            int lineStart = sourceModel.lineStart(commentStart);
            if (commentStart == lineStart) {
                continue;
            }

            int lineEnd = sourceModel.lineEnd(commentStart);
            int firstNonWhitespace = sourceModel.firstNonWhitespace(lineStart, lineEnd);
            if (firstNonWhitespace != commentStart) {
                continue;
            }

            String currentIndent = sourceModel.leadingWhitespace(lineStart, commentStart);
            if (!currentIndent.equals(elementIndent)) {
                addReplacementIfRangeUnowned(replacements, lineStart, commentStart, elementIndent);
            }
        }
    }

    private static void addReplacementIfRangeUnowned(List<Replacement> replacements, int start, int end, String value)
    {
        for (Replacement replacement : replacements) {
            if (replacement.start() <= start && replacement.end() >= end) {
                return;
            }
        }
        replacements.add(new Replacement(start, end, value));
    }

    private static String braceIndent(SourceModel sourceModel, int openBrace)
    {
        int lineStart = sourceModel.lineStart(openBrace);
        int nonWhitespace = sourceModel.firstNonWhitespaceOnLine(lineStart);
        if (nonWhitespace < lineStart) {
            nonWhitespace = lineStart;
        }
        if (nonWhitespace > openBrace) {
            nonWhitespace = openBrace;
        }
        return sourceModel.leadingWhitespace(lineStart, nonWhitespace);
    }
}
