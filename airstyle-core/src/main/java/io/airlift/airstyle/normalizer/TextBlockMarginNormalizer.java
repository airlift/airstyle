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

import io.airlift.airstyle.model.ParenthesizedListLayoutModel;
import io.airlift.airstyle.model.SourceModel;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.TextBlock;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.YieldStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static io.airlift.airstyle.AirstyleFormatter.CONTINUATION_INDENT;
import static io.airlift.airstyle.AirstyleFormatter.CONTINUATION_INDENT_SIZE;
import static java.lang.Math.max;

/// Aligns the content of an inline-opened text block with the opening `"""`
/// column (IntelliJ-style incidental indentation), shifting each content line
/// and the closing delimiter together by the same delta. Blank lines and
/// lines that already sit at or below the opening column are left alone.
///
/// ### Example
///
/// Before (content sits at the statement's base indent, not under the `"""`):
/// ```java
/// String x = foo.bar("""
///         hello
///         world
///         """);
/// ```
///
/// After (content aligned under the opening `"""`):
/// ```java
/// String x = foo.bar("""
///                    hello
///                    world
///                    """);
/// ```
public final class TextBlockMarginNormalizer
{
    private TextBlockMarginNormalizer() {}

    public static String normalize(String source, String originalSource)
    {
        SourceModel sourceModel = SourceModel.create(source);
        SourceModel originalSourceModel = (originalSource == null || originalSource.equals(source))
                ? sourceModel
                : SourceModel.create(originalSource);
        CompilationUnit compilationUnit = sourceModel.compilationUnit();
        List<TextBlock> originalTextBlocks = collectTextBlocks(originalSourceModel.compilationUnit());
        AtomicInteger textBlockIndex = new AtomicInteger();
        List<Replacement> replacements = new ArrayList<>();
        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(TextBlock node)
            {
                int index = textBlockIndex.getAndIncrement();
                TextBlock originalNode = index < originalTextBlocks.size() ? originalTextBlocks.get(index) : null;
                addReplacements(sourceModel, node, originalSourceModel, originalNode, replacements);
                return false;
            }
        });
        return Replacement.applyAll(source, replacements);
    }

    private static List<TextBlock> collectTextBlocks(CompilationUnit compilationUnit)
    {
        List<TextBlock> textBlocks = new ArrayList<>();
        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(TextBlock node)
            {
                textBlocks.add(node);
                return false;
            }
        });
        return List.copyOf(textBlocks);
    }

    private static void addReplacements(SourceModel sourceModel, TextBlock node, SourceModel originalSourceModel, TextBlock originalNode, List<Replacement> replacements)
    {
        int start = node.getStartPosition();
        int end = start + node.getLength();
        if (start < 0 || end > sourceModel.source().length() || end <= start) {
            return;
        }
        addAnnotationOpeningIndentReplacement(sourceModel, node, replacements);
        addWrappedArgumentTextBlockIndentReplacements(sourceModel, node, originalSourceModel, originalNode, replacements);
        addAssignmentTextBlockIndentReplacements(sourceModel, node, originalSourceModel, originalNode, replacements);
        addFlowStatementTextBlockIndentReplacements(sourceModel, node, originalSourceModel, originalNode, replacements);
        if (addInlineTextBlockIndentReplacements(sourceModel, node, replacements)) {
            return;
        }

        if (!isSelectorAttached(node)) {
            return;
        }
        if (!sourceModel.startsMidLine(start)) {
            return;
        }

        int startLine = sourceModel.lineNumber(start);
        int endLine = sourceModel.lineNumber(max(start, end - 1));
        if (endLine <= startLine) {
            return;
        }

        int openLineStart = sourceModel.lineStart(start);
        // Selector-attached text block inline with its host call (e.g.
        // `readValue("""content""".formatted(x), ...)`): align content to
        // the column of the opening `"""`, matching IntelliJ's
        // TextBlockBlock output where each inner line's first char sits
        // directly under the first `"` of `"""`.
        int targetMinimumIndentWidth = start - openLineStart;

        List<SourceModel.TextBlockLine> nonBlankLines = nonBlankLines(sourceModel.textBlockLines(node));
        if (nonBlankLines.isEmpty()) {
            restoreOriginalClosingIndent(sourceModel, node, originalSourceModel, originalNode, replacements);
            restoreOriginalSelectorLineIndent(sourceModel, node, originalSourceModel, originalNode, replacements);
            return;
        }
        if (minimumIndentWidth(nonBlankLines) == targetMinimumIndentWidth) {
            return;
        }
        addShiftedLineIndentReplacements(nonBlankLines, targetMinimumIndentWidth, replacements);
        restoreOriginalClosingIndent(sourceModel, node, originalSourceModel, originalNode, replacements);
        restoreOriginalSelectorLineIndent(sourceModel, node, originalSourceModel, originalNode, replacements);
    }

    private static void addWrappedArgumentTextBlockIndentReplacements(SourceModel sourceModel, TextBlock node, SourceModel originalSourceModel, TextBlock originalNode, List<Replacement> replacements)
    {
        int start = node.getStartPosition();
        if (isAnnotationValue(node) || !sourceModel.startsAtLineIndent(start)) {
            return;
        }

        Expression hostExpression = leadingTextBlockHostExpression(sourceModel, node);
        ParenthesizedListLayoutModel.ParenthesizedListContext context = wrappedArgumentContext(sourceModel, hostExpression);
        if (context == null
                || context.itemStartsInlineInParent()) {
            return;
        }

        int expectedIndentWidth = sourceModel.indentWidth(sourceModel.lineStart(start), start);
        if (!hostExpression.equals(node)
                && hostExpression.getStartPosition() >= 0
                && sourceModel.lineStart(hostExpression.getStartPosition()) != sourceModel.lineStart(start)) {
            int hostLineStart = sourceModel.lineStart(hostExpression.getStartPosition());
            int hostIndentWidth = sourceModel.indentWidth(hostLineStart, hostExpression.getStartPosition());
            expectedIndentWidth = max(expectedIndentWidth, hostIndentWidth + CONTINUATION_INDENT_SIZE);
        }
        boolean preserveOriginalLiteralMargin = !isSelectorAttached(node)
                && (!hostExpression.equals(node)
                || node.getParent() instanceof InfixExpression);
        if (preserveOriginalLiteralMargin
                && originalStartsAtLineIndent(originalSourceModel, originalNode)) {
            if (addTranslatedOriginalMarginReplacements(sourceModel, node, originalSourceModel, originalNode, expectedIndentWidth, replacements)) {
                return;
            }
        }
        addTextBlockLineIndentReplacements(sourceModel, node, expectedIndentWidth, replacements);
    }

    private static boolean addInlineTextBlockIndentReplacements(SourceModel sourceModel, TextBlock node, List<Replacement> replacements)
    {
        if (isAnnotationValue(node) || isSelectorAttached(node)) {
            return false;
        }

        int start = node.getStartPosition();
        int end = start + node.getLength();
        if (!sourceModel.startsMidLine(start)) {
            return false;
        }

        int startLine = sourceModel.lineNumber(start);
        int endLine = sourceModel.lineNumber(max(start, end - 1));
        if (endLine <= startLine) {
            return false;
        }

        int openColumn = start - sourceModel.lineStart(start);
        addTextBlockLineIndentReplacements(sourceModel, node, openColumn, replacements);
        return true;
    }

    private static void addAssignmentTextBlockIndentReplacements(SourceModel sourceModel, TextBlock node, SourceModel originalSourceModel, TextBlock originalNode, List<Replacement> replacements)
    {
        int start = node.getStartPosition();
        if (!sourceModel.startsAtLineIndent(start)) {
            return;
        }

        Expression hostExpression = leadingTextBlockHostExpression(sourceModel, node);
        if (!(hostExpression.getParent() instanceof VariableDeclarationFragment fragment && Objects.equals(fragment.getInitializer(), hostExpression))
                && !(hostExpression.getParent() instanceof Assignment assignment && Objects.equals(assignment.getRightHandSide(), hostExpression))) {
            return;
        }

        int expectedIndentWidth = sourceModel.indentWidth(sourceModel.lineStart(start), start);
        if (!isSelectorAttached(node)
                && originalStartsAtLineIndent(originalSourceModel, originalNode)) {
            if (addTranslatedOriginalMarginReplacements(sourceModel, node, originalSourceModel, originalNode, expectedIndentWidth, replacements)) {
                return;
            }
        }
        replaceOpeningIndent(sourceModel, node, expectedIndentWidth, replacements);
        addTextBlockLineIndentReplacements(sourceModel, node, expectedIndentWidth, replacements);
    }

    private static void addFlowStatementTextBlockIndentReplacements(SourceModel sourceModel, TextBlock node, SourceModel originalSourceModel, TextBlock originalNode, List<Replacement> replacements)
    {
        int start = node.getStartPosition();
        if (!sourceModel.startsAtLineIndent(start)) {
            // Inline text blocks (return """...""" on same line as keyword):
            // Selector-attached ones are handled by the main addReplacements flow.
            // Non-selector ones preserve their existing alignment.
            return;
        }

        Expression hostExpression = leadingTextBlockHostExpression(sourceModel, node);
        if (!(hostExpression.getParent() instanceof ReturnStatement returnStatement && Objects.equals(returnStatement.getExpression(), hostExpression))
                && !(hostExpression.getParent() instanceof YieldStatement yieldStatement && Objects.equals(yieldStatement.getExpression(), hostExpression))) {
            return;
        }

        int statementLineStart = sourceModel.lineStart(hostExpression.getParent().getStartPosition());
        int statementIndentEnd = sourceModel.firstNonWhitespace(statementLineStart, sourceModel.lineEnd(statementLineStart));
        int expectedIndentWidth = max(0, sourceModel.indentWidth(statementLineStart, statementIndentEnd) + CONTINUATION_INDENT_SIZE);
        if (!isSelectorAttached(node)
                && originalStartsAtLineIndent(originalSourceModel, originalNode)) {
            if (addTranslatedOriginalMarginReplacements(sourceModel, node, originalSourceModel, originalNode, expectedIndentWidth, replacements)) {
                return;
            }
        }
        replaceOpeningIndent(sourceModel, node, expectedIndentWidth, replacements);
        addTextBlockLineIndentReplacements(sourceModel, node, expectedIndentWidth, replacements);
    }

    /// Handles annotation values whose text block opening delimiter starts on
    /// its own continuation-indented line.
    private static void addAnnotationOpeningIndentReplacement(SourceModel sourceModel, TextBlock node, List<Replacement> replacements)
    {
        int start = node.getStartPosition();
        if (!isAnnotationValue(node) || !sourceModel.startsAtLineIndent(start)) {
            return;
        }

        int lineNumber = sourceModel.lineNumber(start);
        if (lineNumber <= 0) {
            return;
        }

        int currentLineStart = sourceModel.lineStart(start);
        int previousLineStart = sourceModel.lineStartForLine(lineNumber - 1);

        String expectedIndent = sourceModel.lineIndent(previousLineStart) + CONTINUATION_INDENT;
        String currentIndent = sourceModel.source().substring(currentLineStart, start);
        if (!currentIndent.equals(expectedIndent)) {
            replacements.add(new Replacement(currentLineStart, start, expectedIndent));
        }
        addLiteralPreservingTextBlockLineIndentReplacements(sourceModel, node, expectedIndent.length(), replacements);
    }

    private static void replaceOpeningIndent(SourceModel sourceModel, TextBlock node, int expectedIndentWidth, List<Replacement> replacements)
    {
        int start = node.getStartPosition();
        int lineStart = sourceModel.lineStart(start);
        int currentIndentWidth = sourceModel.indentWidth(lineStart, start);
        if (currentIndentWidth == expectedIndentWidth) {
            return;
        }

        replacements.add(new Replacement(lineStart, start, " ".repeat(expectedIndentWidth)));
    }

    private static void replaceClosingIndent(SourceModel sourceModel, TextBlock node, int expectedIndentWidth, List<Replacement> replacements)
    {
        SourceModel.TextBlockLine closingLine = sourceModel.textBlockClosingLine(node);
        int canonicalClosingIndentWidth = canonicalClosingIndentWidth(sourceModel, node, expectedIndentWidth);
        if (closingLine != null && closingLine.indentWidth() != canonicalClosingIndentWidth) {
            replacements.add(new Replacement(closingLine.lineStart(), closingLine.firstNonWhitespace(), " ".repeat(canonicalClosingIndentWidth)));
        }
    }

    private static void addTextBlockLineIndentReplacements(SourceModel sourceModel, TextBlock node, int expectedIndentWidth, List<Replacement> replacements)
    {
        addShiftedLineIndentReplacements(nonBlankLines(sourceModel.textBlockContentLines(node)), expectedIndentWidth, replacements);
        replaceClosingIndent(sourceModel, node, expectedIndentWidth, replacements);
    }

    private static void addLiteralPreservingTextBlockLineIndentReplacements(SourceModel sourceModel, TextBlock node, int expectedIndentWidth, List<Replacement> replacements)
    {
        addShiftedLineIndentReplacements(nonBlankLines(sourceModel.textBlockLines(node)), expectedIndentWidth, replacements);
    }

    private static void addShiftedLineIndentReplacements(List<SourceModel.TextBlockLine> nonBlankLines, int targetMinimumIndentWidth, List<Replacement> replacements)
    {
        if (nonBlankLines.isEmpty()) {
            return;
        }

        int minimumIndentWidth = minimumIndentWidth(nonBlankLines);
        if (minimumIndentWidth == targetMinimumIndentWidth) {
            return;
        }

        int shiftWidth = targetMinimumIndentWidth - minimumIndentWidth;
        for (SourceModel.TextBlockLine line : nonBlankLines) {
            int targetIndentWidth = line.indentWidth() + shiftWidth;
            replacements.add(new Replacement(line.lineStart(), line.firstNonWhitespace(), " ".repeat(targetIndentWidth)));
        }
    }

    private static int minimumIndentWidth(List<SourceModel.TextBlockLine> nonBlankLines)
    {
        return nonBlankLines.stream()
                .mapToInt(SourceModel.TextBlockLine::indentWidth)
                .min()
                .orElseThrow();
    }

    private static List<SourceModel.TextBlockLine> nonBlankLines(List<SourceModel.TextBlockLine> lines)
    {
        return lines.stream()
                .filter(line -> !line.blank())
                .toList();
    }

    private static boolean addTranslatedOriginalMarginReplacements(
            SourceModel sourceModel,
            TextBlock node,
            SourceModel originalSourceModel,
            TextBlock originalNode,
            int expectedOpeningIndentWidth,
            List<Replacement> replacements)
    {
        List<Replacement> pendingReplacements = new ArrayList<>();

        int start = node.getStartPosition();
        int lineStart = sourceModel.lineStart(start);
        int currentOpeningIndentWidth = sourceModel.indentWidth(lineStart, start);
        if (currentOpeningIndentWidth != expectedOpeningIndentWidth) {
            pendingReplacements.add(new Replacement(lineStart, start, " ".repeat(expectedOpeningIndentWidth)));
        }

        TextBlockIndentPlan indentPlan = textBlockIndentPlan(sourceModel, node, originalSourceModel, originalNode, expectedOpeningIndentWidth);
        if (indentPlan == null) {
            return false;
        }

        for (TextBlockLineIndent lineIndent : indentPlan.contentLineIndents()) {
            replaceTextBlockLineIndent(sourceModel, node, lineIndent.currentLine(), lineIndent.targetIndent(), pendingReplacements);
        }

        SourceModel.TextBlockLine closingLine = sourceModel.textBlockClosingLine(node);
        if (closingLine != null) {
            int targetClosingIndentWidth = canonicalClosingIndentWidth(
                    sourceModel,
                    node,
                    indentPlan.targetMinimumIndentWidth(),
                    indentPlan.contentLineIndents().stream()
                            .map(line -> line.targetIndent().length())
                            .toList());
            replaceTextBlockLineIndent(sourceModel, node, closingLine, " ".repeat(targetClosingIndentWidth), pendingReplacements);
        }
        replacements.addAll(pendingReplacements);
        return true;
    }

    private static TextBlockIndentPlan textBlockIndentPlan(
            SourceModel sourceModel,
            TextBlock node,
            SourceModel originalSourceModel,
            TextBlock originalNode,
            int expectedOpeningIndentWidth)
    {
        List<SourceModel.TextBlockLine> currentLines = sourceModel.textBlockLines(node);
        List<SourceModel.TextBlockLine> originalLines = originalSourceModel.textBlockLines(originalNode);
        if (currentLines.size() != originalLines.size()) {
            return null;
        }
        for (int index = 0; index < currentLines.size(); index++) {
            if (textBlockContentBlank(sourceModel, node, currentLines.get(index))
                    != textBlockContentBlank(originalSourceModel, originalNode, originalLines.get(index))) {
                return null;
            }
        }

        int originalMinimumIndentWidth = textBlockJlsMinimumIndent(originalSourceModel, originalNode, originalLines);
        if (originalMinimumIndentWidth < 0) {
            return null;
        }
        int originalStart = originalNode.getStartPosition();
        int originalLineStart = originalSourceModel.lineStart(originalStart);
        int originalOpeningIndentWidth = originalSourceModel.indentWidth(originalLineStart, originalStart);
        int targetMinimumIndentWidth = max(expectedOpeningIndentWidth, originalMinimumIndentWidth + expectedOpeningIndentWidth - originalOpeningIndentWidth);

        SourceModel.TextBlockLine closingLine = sourceModel.textBlockClosingLine(node);
        List<TextBlockLineIndent> contentLineIndents = new ArrayList<>();
        for (int index = 0; index < currentLines.size(); index++) {
            SourceModel.TextBlockLine originalLine = originalLines.get(index);
            if (closingLine != null && index == currentLines.size() - 1) {
                continue;
            }
            if (textBlockContentBlank(originalSourceModel, originalNode, originalLine)) {
                continue;
            }

            String originalIndent = textBlockLeadingWhitespace(originalSourceModel, originalNode, originalLine);
            String essentialIndent = originalIndent.substring(originalMinimumIndentWidth);
            contentLineIndents.add(new TextBlockLineIndent(currentLines.get(index), " ".repeat(targetMinimumIndentWidth) + essentialIndent));
        }
        return new TextBlockIndentPlan(targetMinimumIndentWidth, List.copyOf(contentLineIndents));
    }

    private static void replaceTextBlockLineIndent(SourceModel sourceModel, TextBlock node, SourceModel.TextBlockLine line, String targetIndent, List<Replacement> replacements)
    {
        int currentIndentEnd = textBlockLeadingWhitespaceEnd(sourceModel, node, line);
        String currentIndent = sourceModel.source().substring(line.lineStart(), currentIndentEnd);
        if (!currentIndent.equals(targetIndent)) {
            replacements.add(new Replacement(line.lineStart(), currentIndentEnd, targetIndent));
        }
    }

    private static int textBlockJlsMinimumIndent(SourceModel sourceModel, TextBlock node, List<SourceModel.TextBlockLine> lines)
    {
        if (lines.isEmpty()) {
            return -1;
        }

        int minimum = Integer.MAX_VALUE;
        for (SourceModel.TextBlockLine line : lines) {
            boolean lastLine = textBlockLineContainsClosingDelimiter(node, line);
            if (!lastLine && textBlockContentBlank(sourceModel, node, line)) {
                continue;
            }
            minimum = Math.min(minimum, textBlockLeadingWhitespace(sourceModel, node, line).length());
        }
        return minimum;
    }

    private static boolean textBlockContentBlank(SourceModel sourceModel, TextBlock node, SourceModel.TextBlockLine line)
    {
        int contentEnd = textBlockLineContentEnd(sourceModel, node, line);
        return textBlockLeadingWhitespaceEnd(sourceModel, node, line) >= contentEnd;
    }

    private static String textBlockLeadingWhitespace(SourceModel sourceModel, TextBlock node, SourceModel.TextBlockLine line)
    {
        return sourceModel.source().substring(line.lineStart(), textBlockLeadingWhitespaceEnd(sourceModel, node, line));
    }

    private static int textBlockLeadingWhitespaceEnd(SourceModel sourceModel, TextBlock node, SourceModel.TextBlockLine line)
    {
        int contentEnd = textBlockLineContentEnd(sourceModel, node, line);
        int index = line.lineStart();
        while (index < contentEnd && Character.isWhitespace(sourceModel.source().charAt(index))) {
            index++;
        }
        return index;
    }

    private static int textBlockLineContentEnd(SourceModel sourceModel, TextBlock node, SourceModel.TextBlockLine line)
    {
        int closingDelimiterStart = closingDelimiterStart(node);
        if (line.lineStart() <= closingDelimiterStart && closingDelimiterStart <= line.lineEnd()) {
            return closingDelimiterStart;
        }
        return line.lineEnd();
    }

    private static boolean textBlockLineContainsClosingDelimiter(TextBlock node, SourceModel.TextBlockLine line)
    {
        int closingDelimiterStart = closingDelimiterStart(node);
        return line.lineStart() <= closingDelimiterStart && closingDelimiterStart <= line.lineEnd();
    }

    private static int closingDelimiterStart(TextBlock node)
    {
        return node.getStartPosition() + node.getLength() - 3;
    }

    private static void restoreOriginalClosingIndent(
            SourceModel sourceModel,
            TextBlock node,
            SourceModel originalSourceModel,
            TextBlock originalNode,
            List<Replacement> replacements)
    {
        if (originalNode == null) {
            return;
        }

        SourceModel.TextBlockLine currentClosingLine = sourceModel.textBlockClosingLine(node);
        SourceModel.TextBlockLine originalClosingLine = originalSourceModel.textBlockClosingLine(originalNode);
        if (currentClosingLine == null || originalClosingLine == null) {
            return;
        }

        int expectedOpeningIndentWidth = sourceModel.indentWidth(sourceModel.lineStart(node.getStartPosition()), node.getStartPosition());
        if (originalClosingLine.indentWidth() != canonicalClosingIndentWidth(sourceModel, node, expectedOpeningIndentWidth)) {
            return;
        }

        if (currentClosingLine.indentWidth() != originalClosingLine.indentWidth()) {
            replacements.add(new Replacement(currentClosingLine.lineStart(), currentClosingLine.firstNonWhitespace(), " ".repeat(originalClosingLine.indentWidth())));
        }
    }

    private static int canonicalClosingIndentWidth(SourceModel sourceModel, TextBlock node, int expectedOpeningIndentWidth)
    {
        SourceModel.TextBlockLine closingLine = sourceModel.textBlockClosingLine(node);
        if (closingLine == null) {
            return expectedOpeningIndentWidth;
        }

        return canonicalClosingIndentWidth(
                sourceModel,
                node,
                expectedOpeningIndentWidth,
                sourceModel.textBlockContentLines(node).stream()
                        .filter(line -> !line.blank())
                        .map(SourceModel.TextBlockLine::indentWidth)
                        .toList());
    }

    private static int canonicalClosingIndentWidth(SourceModel sourceModel, TextBlock node, int expectedOpeningIndentWidth, List<Integer> nonBlankContentIndentWidths)
    {
        if (isSelectorAttached(node)) {
            return expectedOpeningIndentWidth;
        }
        if (ParenthesizedListLayoutModel.contextFor(sourceModel, node) != null) {
            return expectedOpeningIndentWidth;
        }

        if (nonBlankContentIndentWidths.size() <= 1) {
            return expectedOpeningIndentWidth;
        }
        if (!allNonBlankContentLinesEndWithContinuationEscape(sourceModel, node)) {
            return expectedOpeningIndentWidth;
        }

        int firstContentIndentWidth = nonBlankContentIndentWidths.getFirst();
        int laterMinimumIndentWidth = nonBlankContentIndentWidths.stream()
                .skip(1)
                .mapToInt(Integer::intValue)
                .min()
                .orElseThrow();
        if (firstContentIndentWidth == expectedOpeningIndentWidth && laterMinimumIndentWidth > expectedOpeningIndentWidth) {
            return laterMinimumIndentWidth;
        }
        return expectedOpeningIndentWidth;
    }

    private static boolean allNonBlankContentLinesEndWithContinuationEscape(SourceModel sourceModel, TextBlock node)
    {
        List<SourceModel.TextBlockLine> nonBlankContentLines = sourceModel.textBlockContentLines(node).stream()
                .filter(line -> !line.blank())
                .toList();
        for (SourceModel.TextBlockLine line : nonBlankContentLines) {
            if (sourceModel.source().charAt(line.lineEnd() - 1) != '\\') {
                return false;
            }
        }
        return true;
    }

    private static void restoreOriginalSelectorLineIndent(
            SourceModel sourceModel,
            TextBlock node,
            SourceModel originalSourceModel,
            TextBlock originalNode,
            List<Replacement> replacements)
    {
        if (!(node.getParent() instanceof MethodInvocation methodInvocation) || !Objects.equals(methodInvocation.getExpression(), node)) {
            return;
        }
        if (originalNode == null
                || !(originalNode.getParent() instanceof MethodInvocation originalInvocation)
                || !Objects.equals(originalInvocation.getExpression(), originalNode)) {
            return;
        }

        int currentSelectorLineStart = sourceModel.lineStart(methodInvocation.getName().getStartPosition());
        int currentSelectorLineEnd = sourceModel.lineEnd(currentSelectorLineStart);
        int currentSelectorFirstNonWhitespace = sourceModel.firstNonWhitespace(currentSelectorLineStart, currentSelectorLineEnd);
        int originalSelectorLineStart = originalSourceModel.lineStart(originalInvocation.getName().getStartPosition());
        int originalSelectorLineEnd = originalSourceModel.lineEnd(originalSelectorLineStart);
        int originalSelectorFirstNonWhitespace = originalSourceModel.firstNonWhitespace(originalSelectorLineStart, originalSelectorLineEnd);
        if (currentSelectorFirstNonWhitespace >= currentSelectorLineEnd || originalSelectorFirstNonWhitespace >= originalSelectorLineEnd) {
            return;
        }
        if (sourceModel.source().charAt(currentSelectorFirstNonWhitespace) != '.'
                || originalSourceModel.source().charAt(originalSelectorFirstNonWhitespace) != '.') {
            return;
        }

        String currentIndent = sourceModel.source().substring(currentSelectorLineStart, currentSelectorFirstNonWhitespace);
        String originalIndent = originalSourceModel.source().substring(originalSelectorLineStart, originalSelectorFirstNonWhitespace);
        if (!currentIndent.equals(originalIndent)) {
            replacements.add(new Replacement(currentSelectorLineStart, currentSelectorFirstNonWhitespace, originalIndent));
        }
    }

    private static boolean originalStartsAtLineIndent(SourceModel originalSourceModel, TextBlock originalNode)
    {
        return originalNode != null
                && originalSourceModel.startsAtLineIndent(originalNode.getStartPosition());
    }

    private static ParenthesizedListLayoutModel.ParenthesizedListContext wrappedArgumentContext(SourceModel sourceModel, ASTNode node)
    {
        ASTNode current = node;
        while (true) {
            ParenthesizedListLayoutModel.ParenthesizedListContext context = ParenthesizedListLayoutModel.contextFor(sourceModel, current);
            if (context != null) {
                return context;
            }

            ASTNode parent = current.getParent();
            if (!(parent instanceof Expression)) {
                return null;
            }
            current = parent;
        }
    }

    private static Expression leadingTextBlockHostExpression(SourceModel sourceModel, TextBlock node)
    {
        Expression current = node;
        while (current.getParent() instanceof Expression parentExpression && sourceModel.startsWithTextBlock(parentExpression)) {
            current = parentExpression;
        }
        return current;
    }

    private static boolean isAnnotationValue(TextBlock node)
    {
        return node.getParent() instanceof MemberValuePair
                || node.getParent() instanceof SingleMemberAnnotation;
    }

    private static boolean isSelectorAttached(TextBlock node)
    {
        if (node.getParent() instanceof MethodInvocation methodInvocation) {
            return Objects.equals(methodInvocation.getExpression(), node);
        }
        if (node.getParent() instanceof FieldAccess fieldAccess) {
            return Objects.equals(fieldAccess.getExpression(), node);
        }
        return false;
    }

    private record TextBlockIndentPlan(int targetMinimumIndentWidth, List<TextBlockLineIndent> contentLineIndents) {}

    private record TextBlockLineIndent(SourceModel.TextBlockLine currentLine, String targetIndent) {}
}
