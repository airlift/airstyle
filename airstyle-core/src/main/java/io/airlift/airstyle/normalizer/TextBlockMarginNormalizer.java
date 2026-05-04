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
        if (!startsMidLine(sourceModel, start)) {
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
        // directly under the first `"` of `"""`. (For the non-selector
        // path, the early-return at line 124 above means we only reach
        // here when isSelectorAttached(node) is true.)
        int targetMinimumIndentWidth = start - openLineStart;

        List<SourceModel.TextBlockLine> nonBlankLines = new ArrayList<>();
        int minimumIndentWidth = Integer.MAX_VALUE;
        for (SourceModel.TextBlockLine line : sourceModel.textBlockLines(node)) {
            if (line.blank()) {
                continue;
            }
            nonBlankLines.add(line);
            minimumIndentWidth = Math.min(minimumIndentWidth, line.indentWidth());
        }

        if (nonBlankLines.isEmpty()) {
            restoreOriginalClosingIndent(sourceModel, node, originalSourceModel, originalNode, replacements);
            restoreOriginalSelectorLineIndent(sourceModel, node, originalSourceModel, originalNode, replacements);
            return;
        }
        if (minimumIndentWidth == targetMinimumIndentWidth) {
            // Already aligned — no shift needed.
            return;
        }

        // Shift all lines by the same amount so the min aligns to target.
        // Positive shiftWidth: lines need to move LEFT (were too deep).
        // Negative shiftWidth: lines need to move RIGHT (were too shallow).
        int shiftWidth = minimumIndentWidth - targetMinimumIndentWidth;
        if (shiftWidth > 0) {
            // Shifting left — every line must have at least shiftWidth leading
            // spaces; otherwise the shift would clip real content.
            for (SourceModel.TextBlockLine line : nonBlankLines) {
                if (line.indentWidth() < shiftWidth) {
                    return;
                }
            }
        }

        for (SourceModel.TextBlockLine line : nonBlankLines) {
            int targetIndentWidth = max(0, line.indentWidth() - shiftWidth);
            replacements.add(new Replacement(line.lineStart(), line.firstNonWhitespace(), " ".repeat(targetIndentWidth)));
        }
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
        ASTNode contextNode = hostExpression != null ? hostExpression : node;
        ParenthesizedListLayoutModel.ParenthesizedListContext context = wrappedArgumentContext(sourceModel, contextNode);
        if (context == null
                || context.itemStartsInlineInParent()) {
            return;
        }

        int expectedIndentWidth = sourceModel.indentWidth(sourceModel.lineStart(start), start);
        if (hostExpression != null
                && !hostExpression.equals(node)
                && hostExpression.getStartPosition() >= 0
                && sourceModel.lineStart(hostExpression.getStartPosition()) != sourceModel.lineStart(start)) {
            int hostLineStart = sourceModel.lineStart(hostExpression.getStartPosition());
            int hostIndentWidth = sourceModel.indentWidth(hostLineStart, hostExpression.getStartPosition());
            expectedIndentWidth = max(expectedIndentWidth, hostIndentWidth + CONTINUATION_INDENT_SIZE);
        }
        boolean preserveOriginalLiteralMargin = hostExpression == null
                || !hostExpression.equals(node)
                || node.getParent() instanceof InfixExpression;
        if (isSelectorAttached(node)) {
            preserveOriginalLiteralMargin = false;
        }
        if (preserveOriginalLiteralMargin
                && originalNode != null
                && originalSourceModel.startsAtLineIndent(originalNode.getStartPosition())) {
            if (addTranslatedOriginalMarginReplacements(sourceModel, node, originalSourceModel, originalNode, expectedIndentWidth, replacements)) {
                if (isSelectorAttached(node)) {
                    replaceClosingIndent(sourceModel, node, expectedIndentWidth, replacements);
                }
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
        if (!startsMidLine(sourceModel, start)) {
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
        if (hostExpression == null) {
            return;
        }
        if (!(hostExpression.getParent() instanceof VariableDeclarationFragment fragment && Objects.equals(fragment.getInitializer(), hostExpression))
                && !(hostExpression.getParent() instanceof Assignment assignment && Objects.equals(assignment.getRightHandSide(), hostExpression))) {
            return;
        }

        int expectedIndentWidth = sourceModel.indentWidth(sourceModel.lineStart(start), start);
        if (!isSelectorAttached(node)
                && originalNode != null
                && originalSourceModel.startsAtLineIndent(originalNode.getStartPosition())) {
            if (addTranslatedOriginalMarginReplacements(sourceModel, node, originalSourceModel, originalNode, expectedIndentWidth, replacements)) {
                preserveOriginalDeeperClosingIndentForAssignment(sourceModel, node, originalSourceModel, originalNode, expectedIndentWidth, replacements);
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
        if (hostExpression == null) {
            return;
        }
        if (!(hostExpression.getParent() instanceof ReturnStatement returnStatement && Objects.equals(returnStatement.getExpression(), hostExpression))
                && !(hostExpression.getParent() instanceof YieldStatement yieldStatement && Objects.equals(yieldStatement.getExpression(), hostExpression))) {
            return;
        }

        int statementLineStart = sourceModel.lineStart(hostExpression.getParent().getStartPosition());
        int statementIndentEnd = sourceModel.firstNonWhitespace(statementLineStart, sourceModel.lineEnd(statementLineStart));
        int expectedIndentWidth = max(0, sourceModel.indentWidth(statementLineStart, statementIndentEnd) + CONTINUATION_INDENT_SIZE);
        if (!isSelectorAttached(node)
                && originalNode != null
                && originalSourceModel.startsAtLineIndent(originalNode.getStartPosition())) {
            if (addTranslatedOriginalMarginReplacements(sourceModel, node, originalSourceModel, originalNode, expectedIndentWidth, replacements)) {
                return;
            }
        }
        replaceOpeningIndent(sourceModel, node, expectedIndentWidth, replacements);
        addTextBlockLineIndentReplacements(sourceModel, node, expectedIndentWidth, replacements);
    }

    /// Handle inline text blocks in return/yield statements: `return """..."""`
    /// where the opening delimiter is on the same line as the keyword.
    /// Content should be aligned with the opening delimiter position.
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
        if (previousLineStart < 0) {
            return;
        }

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
        if (closingLine != null && !closingLine.blank() && closingLine.indentWidth() != canonicalClosingIndentWidth) {
            replacements.add(new Replacement(closingLine.lineStart(), closingLine.firstNonWhitespace(), " ".repeat(canonicalClosingIndentWidth)));
        }
    }

    private static void addTextBlockLineIndentReplacements(SourceModel sourceModel, TextBlock node, int expectedIndentWidth, List<Replacement> replacements)
    {
        List<SourceModel.TextBlockLine> nonBlankLines = new ArrayList<>();
        int minimumIndentWidth = Integer.MAX_VALUE;
        for (SourceModel.TextBlockLine line : sourceModel.textBlockContentLines(node)) {
            if (line.blank()) {
                continue;
            }
            nonBlankLines.add(line);
            minimumIndentWidth = Math.min(minimumIndentWidth, line.indentWidth());
        }

        if (!nonBlankLines.isEmpty() && minimumIndentWidth != expectedIndentWidth) {
            int shiftWidth = expectedIndentWidth - minimumIndentWidth;
            for (SourceModel.TextBlockLine line : nonBlankLines) {
                int targetIndentWidth = line.indentWidth() + shiftWidth;
                if (targetIndentWidth < 0) {
                    return;
                }
                replacements.add(new Replacement(line.lineStart(), line.firstNonWhitespace(), " ".repeat(targetIndentWidth)));
            }
        }

        replaceClosingIndent(sourceModel, node, expectedIndentWidth, replacements);
    }

    private static void addLiteralPreservingTextBlockLineIndentReplacements(SourceModel sourceModel, TextBlock node, int expectedIndentWidth, List<Replacement> replacements)
    {
        List<SourceModel.TextBlockLine> nonBlankLines = new ArrayList<>();
        int minimumIndentWidth = Integer.MAX_VALUE;
        for (SourceModel.TextBlockLine line : sourceModel.textBlockLines(node)) {
            if (line.blank()) {
                continue;
            }
            nonBlankLines.add(line);
            minimumIndentWidth = Math.min(minimumIndentWidth, line.indentWidth());
        }

        if (nonBlankLines.isEmpty() || minimumIndentWidth == expectedIndentWidth) {
            return;
        }

        int shiftWidth = expectedIndentWidth - minimumIndentWidth;
        for (SourceModel.TextBlockLine line : nonBlankLines) {
            int targetIndentWidth = line.indentWidth() + shiftWidth;
            if (targetIndentWidth < 0) {
                return;
            }
            replacements.add(new Replacement(line.lineStart(), line.firstNonWhitespace(), " ".repeat(targetIndentWidth)));
        }
    }

    private static boolean addTranslatedOriginalMarginReplacements(
            SourceModel sourceModel,
            TextBlock node,
            SourceModel originalSourceModel,
            TextBlock originalNode,
            int expectedOpeningIndentWidth,
            List<Replacement> replacements)
    {
        if (originalNode == null) {
            return false;
        }

        List<Replacement> pendingReplacements = new ArrayList<>();

        int start = node.getStartPosition();
        int lineStart = sourceModel.lineStart(start);
        int currentOpeningIndentWidth = sourceModel.indentWidth(lineStart, start);
        if (currentOpeningIndentWidth != expectedOpeningIndentWidth) {
            pendingReplacements.add(new Replacement(lineStart, start, " ".repeat(expectedOpeningIndentWidth)));
        }

        int originalStart = originalNode.getStartPosition();
        int originalLineStart = originalSourceModel.lineStart(originalStart);
        int originalOpeningIndentWidth = originalSourceModel.indentWidth(originalLineStart, originalStart);
        int shiftWidth = expectedOpeningIndentWidth - originalOpeningIndentWidth;

        List<SourceModel.TextBlockLine> currentLines = sourceModel.textBlockLines(node);
        List<SourceModel.TextBlockLine> originalLines = originalSourceModel.textBlockLines(originalNode);
        if (currentLines.size() != originalLines.size()) {
            return false;
        }

        for (int index = 0; index < currentLines.size(); index++) {
            SourceModel.TextBlockLine currentLine = currentLines.get(index);
            SourceModel.TextBlockLine originalLine = originalLines.get(index);
            if (currentLine.blank() != originalLine.blank()) {
                return false;
            }
        }

        List<Integer> targetContentIndentWidths = new ArrayList<>();
        for (int index = 0; index < currentLines.size(); index++) {
            SourceModel.TextBlockLine currentLine = currentLines.get(index);
            SourceModel.TextBlockLine originalLine = originalLines.get(index);
            if (currentLine.blank()) {
                continue;
            }
            int targetIndentWidth = originalLine.indentWidth() + shiftWidth;
            if (targetIndentWidth < 0) {
                return false;
            }
            if (index < currentLines.size() - 1) {
                targetContentIndentWidths.add(targetIndentWidth);
                if (currentLine.indentWidth() != targetIndentWidth) {
                    pendingReplacements.add(new Replacement(currentLine.lineStart(), currentLine.firstNonWhitespace(), " ".repeat(targetIndentWidth)));
                }
            }
        }
        SourceModel.TextBlockLine currentClosingLine = sourceModel.textBlockClosingLine(node);
        if (currentClosingLine != null && !currentClosingLine.blank()) {
            int targetClosingIndentWidth = canonicalClosingIndentWidth(sourceModel, node, expectedOpeningIndentWidth, targetContentIndentWidths);
            // If the current closing indent matches the original's translated indent
            // AND is deeper than canonical, preserve it — the original had it deeper
            // for a reason (e.g., assignment text blocks with indented content).
            SourceModel.TextBlockLine originalClosingLine = originalSourceModel.textBlockClosingLine(originalNode);
            int originalTargetClosingIndent = (originalClosingLine != null && !originalClosingLine.blank())
                    ? originalClosingLine.indentWidth() + shiftWidth
                    : -1;
            boolean preserveOriginalDeeper = originalTargetClosingIndent > targetClosingIndentWidth
                    && currentClosingLine.indentWidth() == originalTargetClosingIndent;
            if (!preserveOriginalDeeper && currentClosingLine.indentWidth() != targetClosingIndentWidth) {
                pendingReplacements.add(new Replacement(currentClosingLine.lineStart(), currentClosingLine.firstNonWhitespace(), " ".repeat(targetClosingIndentWidth)));
            }
        }
        replacements.addAll(pendingReplacements);
        return true;
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
        if (currentClosingLine == null || originalClosingLine == null || currentClosingLine.blank() || originalClosingLine.blank()) {
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

    private static void preserveOriginalDeeperClosingIndentForAssignment(
            SourceModel sourceModel,
            TextBlock node,
            SourceModel originalSourceModel,
            TextBlock originalNode,
            int expectedOpeningIndentWidth,
            List<Replacement> replacements)
    {
        if (originalNode == null) {
            return;
        }

        SourceModel.TextBlockLine currentClosingLine = sourceModel.textBlockClosingLine(node);
        SourceModel.TextBlockLine originalClosingLine = originalSourceModel.textBlockClosingLine(originalNode);
        if (currentClosingLine == null || originalClosingLine == null || currentClosingLine.blank() || originalClosingLine.blank()) {
            return;
        }
        if (originalClosingLine.indentWidth() <= expectedOpeningIndentWidth) {
            return;
        }
        int originalOpeningIndentWidth = originalSourceModel.indentWidth(
                originalSourceModel.lineStart(originalNode.getStartPosition()),
                originalNode.getStartPosition());
        int originalMinimumContentIndent = originalSourceModel.textBlockContentLines(originalNode).stream()
                .filter(line -> !line.blank())
                .mapToInt(SourceModel.TextBlockLine::indentWidth)
                .min()
                .orElse(originalOpeningIndentWidth);
        if (originalMinimumContentIndent <= originalOpeningIndentWidth) {
            return;
        }
        if (currentClosingLine.indentWidth() != originalClosingLine.indentWidth()) {
            replacements.add(new Replacement(currentClosingLine.lineStart(), currentClosingLine.firstNonWhitespace(), " ".repeat(originalClosingLine.indentWidth())));
        }
    }

    private static int canonicalClosingIndentWidth(SourceModel sourceModel, TextBlock node, int expectedOpeningIndentWidth)
    {
        SourceModel.TextBlockLine closingLine = sourceModel.textBlockClosingLine(node);
        if (closingLine == null || closingLine.blank()) {
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
                .orElse(expectedOpeningIndentWidth);
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
        if (nonBlankContentLines.isEmpty()) {
            return false;
        }
        for (SourceModel.TextBlockLine line : nonBlankContentLines) {
            if (line.lineEnd() <= line.firstNonWhitespace() || sourceModel.source().charAt(line.lineEnd() - 1) != '\\') {
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
        if (!(originalNode != null
                && originalNode.getParent() instanceof MethodInvocation originalInvocation
                && Objects.equals(originalInvocation.getExpression(), originalNode))) {
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

    private static boolean startsMidLine(SourceModel sourceModel, int position)
    {
        return sourceModel.startsMidLine(position);
    }

    private static ParenthesizedListLayoutModel.ParenthesizedListContext wrappedArgumentContext(SourceModel sourceModel, ASTNode node)
    {
        ASTNode current = node;
        while (current != null) {
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
        return null;
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
}
