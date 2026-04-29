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
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import static io.airlift.airstyle.AirstyleFormatter.CONTINUATION_INDENT;
import static io.airlift.airstyle.AirstyleFormatter.CONTINUATION_INDENT_SIZE;
import static io.airlift.airstyle.AirstyleFormatter.INDENTATION_SIZE;
import static java.lang.Math.max;

/// Normalizes wrapped parenthesized argument / parameter lists so the wrapped
/// form is one item per line. Decides whether the first item stays inline with
/// the opening `(` or drops to the next line by comparing the inline column
/// against the continuation column (method-call owners tolerate some drift).
///
/// The engine (FORMAT stage) owns final column via its block tree; WLN emits
/// bare `\n` separators and the engine re-indents. Standalone comments inside
/// argument lists keep their source-derived indent (the engine does not
/// re-indent them), so WLN still computes a wrapped indent for those paths.
///
/// ### Example
///
/// Before:
/// ```java
/// void call(String first, String second,
///           String third,
///     String fourth);
/// ```
///
/// After:
/// ```java
/// void call(
///         String first,
///         String second,
///         String third,
///         String fourth);
/// ```
public final class WrappedListNormalizer
{
    /// Method-call argument lists tolerate some indent drift for an inline
    /// first arg; other parenthesized lists do not.
    private enum FirstArgumentPolicy
    {
        NONE,
        METHOD_CALL,
    }

    private WrappedListNormalizer() {}

    private static boolean shouldKeepFirstArgumentInline(
            FirstArgumentPolicy policy,
            boolean lambdaFirstArgument,
            boolean simpleSingleLineFirstArgument,
            boolean hasTrailingMultilineLambdaArgument,
            boolean hasOnlyTrailingMultilineArgument,
            int inlineColumn,
            int wrappedColumn)
    {
        if (lambdaFirstArgument || policy == FirstArgumentPolicy.NONE) {
            return inlineColumn <= wrappedColumn;
        }
        if (simpleSingleLineFirstArgument
                && !hasTrailingMultilineLambdaArgument
                && !hasOnlyTrailingMultilineArgument) {
            return inlineColumn - wrappedColumn < INDENTATION_SIZE / 2;
        }
        return inlineColumn <= wrappedColumn + CONTINUATION_INDENT_SIZE * 2;
    }

    public static String normalize(String source)
    {
        String normalized = source;
        var seen = new HashSet<String>();
        while (seen.add(normalized)) {
            String next = normalizeOnce(normalized);
            if (next.equals(normalized)) {
                return normalized;
            }
            SourceModel.pruneCacheKeeping(next, source);
            normalized = next;
        }
        return normalized;
    }

    private static String normalizeOnce(String source)
    {
        SourceModel sourceModel = SourceModel.create(source);
        List<ListRegion> regions = collectRegions(source, sourceModel);
        var replacements = new ArrayList<Replacement>();
        for (ListRegion region : regions) {
            addListReplacements(source, sourceModel, region, replacements);
        }
        if (replacements.isEmpty()) {
            return source;
        }
        return applyReplacements(source, replacements);
    }

    private static List<ListRegion> collectRegions(String source, SourceModel sourceModel)
    {
        var regions = new ArrayList<ListRegion>();
        sourceModel.compilationUnit().accept(new ASTVisitor()
        {
            @Override
            public boolean visit(MethodInvocation node)
            {
                boolean pairWrap = isMapFactoryInvocation(node);
                addRegion(
                        source,
                        sourceModel,
                        regions,
                        node.getName().getStartPosition() + node.getName().getLength(),
                        node.getStartPosition() + node.getLength(),
                        astNodes(node.arguments()),
                        node,
                        pairWrap ? 2 : 1,
                        pairWrap,
                        FirstArgumentPolicy.METHOD_CALL);
                return true;
            }

            @Override
            public boolean visit(SuperMethodInvocation node)
            {
                addRegion(
                        source,
                        sourceModel,
                        regions,
                        node.getName().getStartPosition() + node.getName().getLength(),
                        node.getStartPosition() + node.getLength(),
                        astNodes(node.arguments()),
                        node,
                        1,
                        false,
                        FirstArgumentPolicy.METHOD_CALL);
                return true;
            }

            @Override
            public boolean visit(ClassInstanceCreation node)
            {
                addRegion(
                        source,
                        sourceModel,
                        regions,
                        node.getStartPosition(),
                        node.getStartPosition() + node.getLength(),
                        astNodes(node.arguments()),
                        node);
                return true;
            }

            @Override
            public boolean visit(ConstructorInvocation node)
            {
                addRegion(
                        source,
                        sourceModel,
                        regions,
                        node.getStartPosition(),
                        node.getStartPosition() + node.getLength(),
                        astNodes(node.arguments()),
                        node);
                return true;
            }

            @Override
            public boolean visit(SuperConstructorInvocation node)
            {
                addRegion(
                        source,
                        sourceModel,
                        regions,
                        node.getStartPosition(),
                        node.getStartPosition() + node.getLength(),
                        astNodes(node.arguments()),
                        node);
                return true;
            }

            @Override
            public boolean visit(EnumConstantDeclaration node)
            {
                if (!node.arguments().isEmpty()) {
                    addRegion(
                            source,
                            sourceModel,
                            regions,
                            node.getName().getStartPosition() + node.getName().getLength(),
                            node.getStartPosition() + node.getLength(),
                            astNodes(node.arguments()),
                            node);
                }
                return true;
            }

            @Override
            public boolean visit(MethodDeclaration node)
            {
                var parameters = new ArrayList<>(astNodes(node.parameters()));
                if (node.getReceiverType() != null) {
                    int receiverStart = node.getReceiverType().getStartPosition();
                    int receiverEnd = receiverStart + node.getReceiverType().getLength();
                    if (node.getReceiverQualifier() != null) {
                        receiverEnd = node.getReceiverQualifier().getStartPosition() + node.getReceiverQualifier().getLength();
                    }
                    parameters.addFirst(new AstRange(receiverStart, receiverEnd, node.getReceiverType()));
                }
                addRegion(
                        source,
                        sourceModel,
                        regions,
                        node.getName().getStartPosition() + node.getName().getLength(),
                        node.getStartPosition() + node.getLength(),
                        parameters,
                        node);
                return true;
            }

            @Override
            public boolean visit(RecordDeclaration node)
            {
                addRegion(
                        source,
                        sourceModel,
                        regions,
                        node.getName().getStartPosition() + node.getName().getLength(),
                        node.getStartPosition() + node.getLength(),
                        astNodes(node.recordComponents()),
                        node);
                return true;
            }

            @Override
            public boolean visit(NormalAnnotation node)
            {
                addRegion(
                        source,
                        sourceModel,
                        regions,
                        node.getTypeName().getStartPosition() + node.getTypeName().getLength(),
                        node.getStartPosition() + node.getLength(),
                        astNodes(node.values()),
                        node);
                return true;
            }

            @Override
            public boolean visit(SingleMemberAnnotation node)
            {
                addRegion(
                        source,
                        sourceModel,
                        regions,
                        node.getTypeName().getStartPosition() + node.getTypeName().getLength(),
                        node.getStartPosition() + node.getLength(),
                        astNodes(List.of(node.getValue())),
                        node);
                return true;
            }
        });
        return regions;
    }

    private static List<AstRange> astNodes(List<?> nodes)
    {
        var ranges = new ArrayList<AstRange>();
        for (Object node : nodes) {
            if (node instanceof ASTNode astNode) {
                ranges.add(new AstRange(astNode.getStartPosition(), astNode.getStartPosition() + astNode.getLength(), astNode));
            }
        }
        return ranges;
    }

    private static void addRegion(String source, SourceModel sourceModel, List<ListRegion> regions, int searchStart, int searchEnd, List<AstRange> items, ASTNode owner)
    {
        addRegion(source, sourceModel, regions, searchStart, searchEnd, items, owner, 1, false, FirstArgumentPolicy.NONE);
    }

    private static void addRegion(String source, SourceModel sourceModel, List<ListRegion> regions, int searchStart, int searchEnd, List<AstRange> items, ASTNode owner, int groupSize, boolean forceWrapFirst, FirstArgumentPolicy firstArgumentPolicy)
    {
        if (items.isEmpty()) {
            return;
        }

        int boundedStart = Math.clamp(searchStart, 0, source.length());
        int boundedEnd = Math.clamp(searchEnd, boundedStart, source.length());
        int openParen = sourceModel.findOpeningParen(boundedStart, boundedEnd);
        if (openParen < 0) {
            return;
        }
        int closeParen = sourceModel.findMatchingParen(openParen, boundedEnd);
        if (closeParen < 0) {
            return;
        }
        if (!sourceModel.containsLineBreak(openParen + 1, closeParen)) {
            return;
        }

        regions.add(new ListRegion(openParen, closeParen, items, owner, max(1, groupSize), forceWrapFirst, firstArgumentPolicy));
    }

    private static boolean isMapFactoryInvocation(MethodInvocation node)
    {
        if (!"of".equals(node.getName().getIdentifier())) {
            return false;
        }
        if (node.getExpression() == null) {
            return false;
        }

        return switch (terminalIdentifier(node.getExpression())) {
            case "Map", "ImmutableMap", "ImmutableListMultimap", "Attributes" -> true;
            default -> false;
        };
    }

    private static String terminalIdentifier(Expression expression)
    {
        return switch (expression) {
            case QualifiedName qualifiedName -> qualifiedName.getName().getIdentifier();
            case SimpleName simpleName -> simpleName.getIdentifier();
            case FieldAccess fieldAccess -> fieldAccess.getName().getIdentifier();
            default -> "";
        };
    }

    private static void addListReplacements(
            String source,
            SourceModel sourceModel,
            ListRegion region,
            List<Replacement> replacements)
    {
        List<AstRange> items = region.items();
        int openParen = region.openParen();
        int closeParen = region.closeParen();

        if (items.isEmpty() || openParen < 0 || closeParen <= openParen) {
            return;
        }

        AstRange first = items.getFirst();
        String prefix = source.substring(openParen + 1, first.start());
        boolean prefixHasComment = sourceModel.commentOverlaps(openParen + 1, first.start());
        for (AstRange item : items) {
            if (item.start() < 0 || item.end() > source.length() || item.start() >= item.end()) {
                return;
            }
        }

        WrappedListIntentModel intent = analyzeWrappedListIntent(source, sourceModel, region, prefix);
        if (!intent.listContainsStructuralLineBreak() && !(items.size() == 1 && intent.lastMultiline())) {
            return;
        }

        if (intent.preserveLastMultilineArgument()) {
            return;
        }

        if (intent.skipForLeadingMultilineBlockLambda()) {
            return;
        }
        String wrappedIndent = wrappedIndent(sourceModel, openParen);
        int wrappedColumn = SourceModel.visualWidth(wrappedIndent);
        int inlineColumn = sourceModel.visualColumn(openParen) + 1;
        boolean firstInline = shouldPlaceFirstArgumentInline(
                sourceModel,
                region,
                intent,
                prefix,
                first,
                items,
                inlineColumn,
                wrappedColumn);

        if (intent.preserveWrappedTogetherOnNextLine()) {
            String normalizedTogetherPrefix = normalizePrefix(prefix, true);
            if (!prefixHasComment && !prefix.equals(normalizedTogetherPrefix)) {
                replacements.add(new Replacement(openParen + 1, first.start(), normalizedTogetherPrefix));
            }
            addWrappedTogetherSeparatorReplacements(source, sourceModel, region, replacements);
            return;
        }

        String normalizedPrefix = normalizePrefix(prefix, !firstInline);
        if (prefixHasComment) {
            // The engine does not re-indent standalone comments inside arg
            // lists, so WLN emits their column directly.
            String normalizedPrefixWithComment = normalizePrefixWithStandaloneComment(sourceModel, openParen + 1, first.start(), wrappedIndent);
            if (normalizedPrefixWithComment != null && !prefix.equals(normalizedPrefixWithComment)) {
                replacements.add(new Replacement(openParen + 1, first.start(), normalizedPrefixWithComment));
            }
        }
        else if (!prefix.equals(normalizedPrefix)) {
            replacements.add(new Replacement(openParen + 1, first.start(), normalizedPrefix));
        }

        for (int index = 0; index < items.size() - 1; index++) {
            AstRange left = items.get(index);
            AstRange right = items.get(index + 1);
            if (left.end() > right.start()) {
                continue;
            }

            int separatorStart = left.end();
            int separatorEnd = right.start();
            if (sourceModel.commentOverlaps(separatorStart, separatorEnd)) {
                // Comments keep source-derived indent (engine doesn't re-indent
                // comments inside arg lists).
                String normalizedSeparator = normalizeSeparatorWithInlineComment(sourceModel, separatorStart, separatorEnd, wrappedIndent);
                if (normalizedSeparator != null && !source.substring(separatorStart, separatorEnd).equals(normalizedSeparator)) {
                    replacements.add(new Replacement(separatorStart, separatorEnd, normalizedSeparator));
                }
                continue;
            }

            String separator = source.substring(left.end(), right.start());
            String normalizedSeparator = normalizeSeparator(separator);
            if (shouldUseGroupedSeparator(sourceModel, region, index)) {
                normalizedSeparator = normalizeGroupedSeparator(separator);
            }
            if (!separator.equals(normalizedSeparator)) {
                replacements.add(new Replacement(left.end(), right.start(), normalizedSeparator));
            }
        }

        if (items.size() == 1 && firstInline) {
            AstRange only = items.getFirst();
            if (only.end() <= closeParen) {
                String suffix = source.substring(only.end(), closeParen);
                if (suffix.trim().isEmpty() && containsLineBreak(suffix)) {
                    replacements.add(new Replacement(only.end(), closeParen, ""));
                }
            }
        }
    }

    private static WrappedListIntentModel analyzeWrappedListIntent(String source, SourceModel sourceModel, ListRegion region, String prefix)
    {
        List<AstRange> items = region.items();
        boolean prefixContainsLineBreak = prefix.indexOf('\n') >= 0;
        boolean listContainsStructuralLineBreak = prefixContainsLineBreak;
        boolean nonLastMultiline = false;
        boolean lastMultiline = false;

        for (int index = 0; index < items.size(); index++) {
            AstRange item = items.get(index);
            boolean multiline = sourceModel.containsLineBreak(item.start(), item.end());
            if (index == items.size() - 1) {
                lastMultiline = multiline;
            }
            else if (multiline) {
                nonLastMultiline = true;
            }

            if (index < items.size() - 1) {
                AstRange next = items.get(index + 1);
                if (sourceModel.containsLineBreak(item.end(), next.start())) {
                    listContainsStructuralLineBreak = true;
                }
            }
        }

        AstRange first = items.getFirst();
        if (sourceModel.containsLineBreak(items.getLast().end(), region.closeParen())) {
            listContainsStructuralLineBreak = true;
        }

        boolean preserveLastMultilineArgument = items.size() > 1
                && !nonLastMultiline
                && lastMultiline
                && !hasWrappedNonLastArguments(sourceModel, items, prefixContainsLineBreak);

        boolean skipForLeadingMultilineBlockLambda = !prefixContainsLineBreak
                && first.node() instanceof LambdaExpression lambda
                && lambda.getBody() instanceof Block
                && isMultilineLambda(source, first)
                && !sourceModel.containsLineBreak(first.end(), region.closeParen())
                && (first.node().getParent() instanceof MethodInvocation || first.node().getParent() instanceof SuperMethodInvocation
                || first.node().getParent() instanceof ClassInstanceCreation);

        boolean compactWrappedLayout = shouldPreserveArgumentsWrappedTogetherOnNextLine(sourceModel, region, prefix);

        return new WrappedListIntentModel(
                prefixContainsLineBreak,
                listContainsStructuralLineBreak,
                lastMultiline,
                compactWrappedLayout,
                preserveLastMultilineArgument,
                skipForLeadingMultilineBlockLambda);
    }

    private static boolean hasWrappedNonLastArguments(SourceModel sourceModel, List<AstRange> items, boolean prefixContainsLineBreak)
    {
        if (items.size() <= 1) {
            return false;
        }

        if (prefixContainsLineBreak) {
            return true;
        }

        int lastNonLastIndex = items.size() - 2;
        for (int index = 0; index < lastNonLastIndex; index++) {
            AstRange left = items.get(index);
            AstRange right = items.get(index + 1);
            if (sourceModel.containsLineBreak(left.end(), right.start())) {
                return true;
            }
        }
        return false;
    }

    private static boolean shouldPreserveArgumentsWrappedTogetherOnNextLine(SourceModel sourceModel, ListRegion region, String prefix)
    {
        if (!isCallOwner(region.owner())) {
            return false;
        }

        List<AstRange> items = region.items();
        if (items.size() <= 1 || prefix.indexOf('\n') < 0) {
            return false;
        }

        boolean allowTrailingMultilineBlockLambda = isCompactWrappedTogetherWithTrailingBlockLambda(sourceModel, region);

        int lastBreak = lastLineBreak(prefix);
        if (lastBreak < 0 || !prefix.substring(lastBreak + 1).trim().isEmpty()) {
            return false;
        }

        for (int index = 0; index < items.size(); index++) {
            AstRange item = items.get(index);
            if (sourceModel.containsLineBreak(item.start(), item.end()) && !(allowTrailingMultilineBlockLambda && index == items.size() - 1)) {
                return false;
            }

            if (index < items.size() - 1) {
                AstRange next = items.get(index + 1);
                if (sourceModel.containsLineBreak(item.end(), next.start())) {
                    return false;
                }
            }
        }

        AstRange last = items.getLast();
        return !sourceModel.containsLineBreak(last.end(), region.closeParen());
    }

    private static boolean isCompactWrappedTogetherWithTrailingBlockLambda(SourceModel sourceModel, ListRegion region)
    {
        List<AstRange> items = region.items();
        if (items.size() <= 1) {
            return false;
        }

        AstRange last = items.getLast();
        if (!(last.node() instanceof LambdaExpression lambda) || !(lambda.getBody() instanceof Block) || !sourceModel.containsLineBreak(last.start(), last.end())) {
            return false;
        }

        for (int index = 0; index < items.size() - 1; index++) {
            AstRange item = items.get(index);
            if (sourceModel.containsLineBreak(item.start(), item.end())) {
                return false;
            }

            AstRange next = items.get(index + 1);
            if (sourceModel.containsLineBreak(item.end(), next.start())) {
                return false;
            }
        }

        return !sourceModel.containsLineBreak(last.end(), region.closeParen());
    }

    private static void addWrappedTogetherSeparatorReplacements(
            String source,
            SourceModel sourceModel,
            ListRegion region,
            List<Replacement> replacements)
    {
        List<AstRange> items = region.items();
        for (int index = 0; index < items.size() - 1; index++) {
            AstRange left = items.get(index);
            AstRange right = items.get(index + 1);
            if (sourceModel.commentOverlaps(left.end(), right.start())) {
                return;
            }

            String separator = source.substring(left.end(), right.start());
            if (!separator.equals(", ")) {
                replacements.add(new Replacement(left.end(), right.start(), ", "));
            }
        }

        AstRange last = items.getLast();
        if (sourceModel.commentOverlaps(last.end(), region.closeParen())) {
            return;
        }

        String suffix = source.substring(last.end(), region.closeParen());
        if (!suffix.isEmpty()) {
            replacements.add(new Replacement(last.end(), region.closeParen(), ""));
        }
    }

    private static boolean isMultilineLambda(String source, AstRange range)
    {
        if (!(range.node() instanceof LambdaExpression lambda)) {
            return false;
        }

        int start = lambda.getStartPosition();
        int end = start + lambda.getLength();
        return isMultiline(source, start, end);
    }

    private record WrappedListIntentModel(
            boolean prefixContainsLineBreak,
            boolean listContainsStructuralLineBreak,
            boolean lastMultiline,
            boolean preserveWrappedTogetherOnNextLine,
            boolean preserveLastMultilineArgument,
            boolean skipForLeadingMultilineBlockLambda) {}

    private record AstRange(int start, int end, ASTNode node) {}

    private record ListRegion(int openParen, int closeParen, List<AstRange> items, ASTNode owner, int groupSize, boolean forceWrapFirst, FirstArgumentPolicy firstArgumentPolicy) {}

    private static boolean isWithinGroupedLine(int separatorIndex, int groupSize)
    {
        return separatorIndex % groupSize != groupSize - 1;
    }

    private static boolean shouldUseGroupedSeparator(SourceModel sourceModel, ListRegion region, int separatorIndex)
    {
        if (region.groupSize() <= 1 || !isWithinGroupedLine(separatorIndex, region.groupSize())) {
            return false;
        }

        if (separatorIndex + 1 >= region.items().size()) {
            return false;
        }

        AstRange right = region.items().get(separatorIndex + 1);
        return !(right.node() instanceof Expression expression) || !sourceModel.startsWithTextBlock(expression);
    }

    private static String normalizePrefix(String prefix, boolean wrapFirst)
    {
        if (wrapFirst) {
            // If the source already ends in a line break, preserve the source's
            // whitespace verbatim — the engine will re-indent. Only touch the
            // prefix when we need to INSERT a line break.
            if (containsLineBreak(prefix) && prefix.substring(lastLineBreak(prefix) + 1).trim().isEmpty()) {
                return prefix;
            }
            if (prefix.trim().isEmpty()) {
                return "\n";
            }
            int lastLineBreak = lastLineBreak(prefix);
            if (lastLineBreak < 0) {
                return prefix + "\n";
            }

            String tail = prefix.substring(lastLineBreak + 1);
            if (tail.trim().isEmpty()) {
                return prefix.substring(0, lastLineBreak + 1);
            }
            return prefix + "\n";
        }

        if (prefix.trim().isEmpty()) {
            return "";
        }
        return prefix;
    }

    private static String normalizeSeparator(String separator)
    {
        // If source already ends with a line break followed by whitespace,
        // preserve it — the engine owns the column. Only touch the separator
        // when we need to INSERT a line break or remove non-whitespace noise.
        int lastLineBreak = lastLineBreak(separator);
        if (lastLineBreak >= 0 && separator.substring(lastLineBreak + 1).trim().isEmpty()) {
            if (separator.startsWith(",")) {
                return collapseRepeatedBlankLines(separator);
            }
        }

        if (separator.startsWith(",") && separator.substring(1).trim().isEmpty()) {
            return ",\n";
        }

        if (lastLineBreak < 0) {
            if (separator.startsWith(",")) {
                String tail = rstrip(separator.substring(1));
                if (tail.isEmpty()) {
                    return ",\n";
                }
                return "," + tail + "\n";
            }
            return rstrip(separator) + "\n";
        }

        String tail = separator.substring(lastLineBreak + 1);
        if (tail.trim().isEmpty()) {
            return separator.substring(0, lastLineBreak + 1);
        }
        return rstrip(separator) + "\n";
    }

    private static String collapseRepeatedBlankLines(String separator)
    {
        int lineBreaks = 0;
        int lastLineBreak = -1;
        for (int index = 0; index < separator.length(); index++) {
            if (separator.charAt(index) == '\n') {
                lineBreaks++;
                lastLineBreak = index;
            }
        }
        if (lineBreaks <= 2 || lastLineBreak < 0) {
            return separator;
        }

        String trailingIndent = separator.substring(lastLineBreak + 1);
        return separator.substring(0, separator.indexOf('\n')) + "\n\n" + trailingIndent;
    }

    private static String normalizeGroupedSeparator(String separator)
    {
        String tail = separator.substring(separator.indexOf(',') + 1).trim();
        if (tail.isEmpty()) {
            return ", ";
        }
        return ", " + tail;
    }

    private static String normalizeSeparatorWithInlineComment(SourceModel sourceModel, int start, int end, String wrappedIndent)
    {
        List<SourceModel.CommentRange> comments = sourceModel.rewriteSafety(start, end).containedComments();
        if (comments.size() != 1) {
            return null;
        }

        SourceModel.CommentRange comment = comments.getFirst();
        String commentText = sourceModel.text(comment);
        if (!commentText.startsWith("//")) {
            return null;
        }

        String separator = sourceModel.source().substring(start, end);
        int lastBreak = lastLineBreak(separator);
        if (lastBreak < 0) {
            return null;
        }

        int commentRelativeStart = comment.start() - start;
        int commentRelativeEnd = comment.end() - start;
        if (commentRelativeStart < 0 || commentRelativeEnd > separator.length()) {
            return null;
        }

        String afterComment = separator.substring(commentRelativeEnd);
        if (!containsLineBreak(afterComment)) {
            return null;
        }
        int lastAfterCommentBreak = lastLineBreak(afterComment);
        if (lastAfterCommentBreak < 0) {
            return null;
        }
        if (!afterComment.substring(lastAfterCommentBreak + 1).trim().isEmpty()) {
            return null;
        }

        return separator.substring(0, lastBreak + 1) + wrappedIndent;
    }

    private static String normalizePrefixWithStandaloneComment(SourceModel sourceModel, int start, int end, String wrappedIndent)
    {
        List<SourceModel.CommentRange> comments = sourceModel.rewriteSafety(start, end).containedComments();
        if (comments.size() != 1) {
            return null;
        }

        SourceModel.CommentRange comment = comments.getFirst();
        String commentText = sourceModel.text(comment);
        if (!commentText.startsWith("//")) {
            return null;
        }

        int commentLineStart = sourceModel.lineStart(comment.start());
        int commentLineEnd = sourceModel.lineEnd(comment.start());
        if (sourceModel.firstNonWhitespace(commentLineStart, commentLineEnd) != comment.start()) {
            return null;
        }
        if (!sourceModel.source().substring(comment.end(), commentLineEnd).trim().isEmpty()) {
            return null;
        }

        String prefix = sourceModel.source().substring(start, end);
        int commentRelativeStart = comment.start() - start;
        int commentRelativeEnd = comment.end() - start;
        if (commentRelativeStart < 0 || commentRelativeEnd > prefix.length()) {
            return null;
        }

        String beforeComment = prefix.substring(0, commentRelativeStart);
        String afterComment = prefix.substring(commentRelativeEnd);
        if (!beforeComment.trim().isEmpty() || !afterComment.trim().isEmpty()) {
            return null;
        }
        if (!containsLineBreak(beforeComment) || !containsLineBreak(afterComment)) {
            return null;
        }

        return "\n" + wrappedIndent + commentText + "\n" + wrappedIndent;
    }

    private static String wrappedIndent(SourceModel sourceModel, int openParen)
    {
        int openLineStart = sourceModel.lineStart(openParen);
        int openLineIndentEnd = sourceModel.firstNonWhitespaceOnLine(openLineStart);
        String openLineIndent = sourceModel.source().substring(openLineStart, openLineIndentEnd);

        return openLineIndent + CONTINUATION_INDENT;
    }

    private static boolean containsLineBreak(String text)
    {
        return SourceModel.containsLineBreak(text);
    }

    private static boolean containsRepeatedLineBreaks(String text)
    {
        return SourceModel.containsRepeatedLineBreaks(text);
    }

    private static boolean hasRepeatedLineBreakBetweenWrappedItems(String source, List<AstRange> items, String prefix)
    {
        if (containsRepeatedLineBreaks(prefix)) {
            return true;
        }

        for (int index = 0; index < items.size() - 1; index++) {
            AstRange left = items.get(index);
            AstRange right = items.get(index + 1);
            if (containsRepeatedLineBreaks(source.substring(left.end(), right.start()))) {
                return true;
            }
        }

        return false;
    }

    private static boolean isMultiline(String source, int start, int end)
    {
        return isMultiline(SourceModel.create(source), start, end);
    }

    private static boolean isMultiline(SourceModel sourceModel, int start, int end)
    {
        return sourceModel.containsLineBreak(start, end);
    }

    private static boolean isSimpleSingleLineFirstArgument(String source, AstRange first)
    {
        if (isMultiline(source, first.start(), first.end())) {
            return false;
        }

        return switch (first.node()) {
            case SimpleName _, QualifiedName _, FieldAccess _, ThisExpression _, StringLiteral _, NumberLiteral _, NullLiteral _ -> true;
            default -> false;
        };
    }

    private static boolean hasTrailingMultilineLambdaArgument(String source, List<AstRange> items)
    {
        for (int index = 1; index < items.size(); index++) {
            AstRange item = items.get(index);
            if (item.node() instanceof LambdaExpression && isMultiline(source, item.start(), item.end())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasOnlyTrailingMultilineArgument(String source, List<AstRange> items)
    {
        if (items.size() < 2) {
            return false;
        }

        AstRange last = items.getLast();
        if (!isMultiline(source, last.start(), last.end())) {
            return false;
        }

        for (int index = 0; index < items.size() - 1; index++) {
            AstRange item = items.get(index);
            if (isMultiline(source, item.start(), item.end())) {
                return false;
            }
        }
        return true;
    }

    private static boolean shouldPlaceFirstArgumentInline(
            SourceModel sourceModel,
            ListRegion region,
            WrappedListIntentModel intent,
            String prefix,
            AstRange first,
            List<AstRange> items,
            int inlineColumn,
            int wrappedColumn)
    {
        boolean firstInline = shouldKeepFirstArgumentInline(sourceModel, region, first, items, inlineColumn, wrappedColumn);

        if (shouldWrapFirstArgumentForLayout(sourceModel, region, intent, first, items, firstInline)) {
            firstInline = false;
        }
        if (shouldPreserveWrappedFirstArgument(sourceModel, region, prefix, first, items, inlineColumn, wrappedColumn, firstInline)) {
            firstInline = false;
        }
        if (region.forceWrapFirst()) {
            firstInline = false;
        }
        if (items.size() == 1 && !containsLineBreak(prefix)) {
            firstInline = true;
        }
        return firstInline;
    }

    private static boolean shouldWrapFirstArgumentForLayout(
            SourceModel sourceModel,
            ListRegion region,
            WrappedListIntentModel intent,
            AstRange first,
            List<AstRange> items,
            boolean firstInline)
    {
        if (items.size() == 1 && intent.prefixContainsLineBreak() && isMultiline(sourceModel, first.start(), first.end())) {
            return true;
        }
        if (region.owner() instanceof NormalAnnotation
                && intent.prefixContainsLineBreak()
                && isMultiline(sourceModel, first.start(), first.end())) {
            return true;
        }
        if (items.size() > 1
                && first.node() instanceof LambdaExpression lambda
                && lambda.getBody() instanceof Block
                && sourceModel.containsLineBreak(lambda.getStartPosition(), lambda.getStartPosition() + lambda.getLength())
                && sourceModel.containsLineBreak(region.items().getFirst().end(), region.closeParen())) {
            return true;
        }
        if (items.size() > 1
                && intent.listContainsStructuralLineBreak()
                && isMultiline(sourceModel, first.start(), first.end())
                && !(first.node() instanceof LambdaExpression)
                && shouldWrapMultilineExpressionFirstArgument(sourceModel, first.node())
                && !isShortCallOwner(sourceModel, region.owner())) {
            return true;
        }
        return firstInline
                && region.owner() instanceof SuperConstructorInvocation
                && items.size() > 1
                && isMultiline(sourceModel, first.start(), first.end())
                && !shouldKeepShortSuperCallFirstArgumentInline(sourceModel, region, first);
    }

    private static boolean isShortCallOwner(SourceModel sourceModel, ASTNode owner)
    {
        int ownerStart = switch (owner) {
            case MethodInvocation invocation -> invocation.getName().getStartPosition();
            case SuperMethodInvocation invocation -> invocation.getStartPosition();
            case ConstructorInvocation invocation -> invocation.getStartPosition();
            case SuperConstructorInvocation invocation -> invocation.getStartPosition();
            default -> -1;
        };
        if (ownerStart < 0) {
            return false;
        }

        int ownerEnd = owner.getStartPosition() + owner.getLength();
        int openParen = sourceModel.findOpeningParen(ownerStart, ownerEnd);
        if (openParen < 0) {
            return false;
        }

        int lineStart = sourceModel.lineStart(ownerStart);
        int firstNonWhitespace = sourceModel.firstNonWhitespaceOnLine(lineStart);
        return openParen - firstNonWhitespace <= CONTINUATION_INDENT_SIZE;
    }

    private static boolean shouldWrapMultilineExpressionFirstArgument(SourceModel sourceModel, ASTNode node)
    {
        return switch (unwrapExpressionContinuationNode(node)) {
            case InfixExpression infix -> infix.getOperator() == InfixExpression.Operator.PLUS && !containsWrappedMethodChain(sourceModel, infix);
            default -> false;
        };
    }

    private static boolean containsWrappedMethodChain(SourceModel sourceModel, ASTNode node)
    {
        boolean[] containsWrappedMethodChain = {false};
        node.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(MethodInvocation invocation)
            {
                if (containsWrappedMethodChain[0]) {
                    return false;
                }
                if (invocation.getExpression() != null
                        && sourceModel.containsLineBreak(invocation.getStartPosition(), invocation.getStartPosition() + invocation.getLength())) {
                    containsWrappedMethodChain[0] = true;
                    return false;
                }
                return true;
            }
        });
        return containsWrappedMethodChain[0];
    }

    private static ASTNode unwrapExpressionContinuationNode(ASTNode node)
    {
        ASTNode current = node;
        while (true) {
            current = switch (current) {
                case ParenthesizedExpression parenthesizedExpression -> parenthesizedExpression.getExpression();
                case CastExpression castExpression -> castExpression.getExpression();
                default -> current;
            };
            if (!(current instanceof ParenthesizedExpression) && !(current instanceof CastExpression)) {
                return current;
            }
        }
    }

    private static boolean shouldKeepShortSuperCallFirstArgumentInline(SourceModel sourceModel, ListRegion region, AstRange first)
    {
        if (!(region.owner() instanceof SuperConstructorInvocation) || !isShortCallOwner(sourceModel, region.owner())) {
            return false;
        }
        // Plain nested call as first arg (no expression → no chain) → keep inline.
        return first.node() instanceof MethodInvocation invocation && invocation.getExpression() == null;
    }

    private static boolean shouldPreserveWrappedFirstArgument(
            SourceModel sourceModel,
            ListRegion region,
            String prefix,
            AstRange first,
            List<AstRange> items,
            int inlineColumn,
            int wrappedColumn,
            boolean firstInline)
    {
        if (!firstInline || region.firstArgumentPolicy() == FirstArgumentPolicy.NONE || !containsLineBreak(prefix)) {
            return false;
        }

        String source = sourceModel.source();
        if (region.owner() instanceof MethodInvocation invocation && isOnlyArgumentOfCallLike(invocation)) {
            return true;
        }

        if (isSelectorLineCallWithMultipleArgs(source, region)) {
            return true;
        }

        // Preserve an already-wrapped first argument when:
        // 1) unwrapping would move it further right, or
        // 2) the invocation itself is nested as a multi-argument list item.
        if (inlineColumn > wrappedColumn
                || (isMultiline(source, first.start(), first.end()) && isNodeInMultiArgumentList(first.node()))
                || isContainingInvocationNestedInMultiArgumentList(first.node())) {
            return true;
        }
        return hasRepeatedLineBreakBetweenWrappedItems(source, items, prefix);
    }

    private static boolean isOnlyArgumentOfCallLike(ASTNode node)
    {
        // Climb through chained invocations/field-accesses where `node` is the expression,
        // then check if the climbed root is the only argument of a call-like parent.
        ASTNode current = node;
        while (current.getParent() instanceof MethodInvocation parentMi
                && current.getLocationInParent() == MethodInvocation.EXPRESSION_PROPERTY) {
            current = parentMi;
        }
        ASTNode parent = current.getParent();
        if (parent == null) {
            return false;
        }
        List<?> arguments = switch (parent) {
            case MethodInvocation mi -> mi.arguments();
            case SuperMethodInvocation smi -> smi.arguments();
            case ClassInstanceCreation cic -> cic.arguments();
            case ConstructorInvocation ci -> ci.arguments();
            case SuperConstructorInvocation sci -> sci.arguments();
            default -> null;
        };
        return arguments != null && arguments.size() == 1 && arguments.getFirst().equals(current);
    }

    // Preserves wrapping when the region's owner is a selector-line invocation
    // (i.e., its `.method` appears on a different line than its receiver expression).
    private static boolean isSelectorLineCallWithMultipleArgs(String source, ListRegion region)
    {
        if (region.items().size() <= 1 || !(region.owner() instanceof MethodInvocation invocation)) {
            return false;
        }
        Expression expression = invocation.getExpression();
        if (expression == null) {
            return false;
        }
        int expressionEnd = expression.getStartPosition() + expression.getLength();
        int nameStart = invocation.getName().getStartPosition();
        return SourceModel.create(source).containsLineBreak(expressionEnd, nameStart);
    }

    private static boolean shouldKeepFirstArgumentInline(SourceModel sourceModel, ListRegion region, AstRange first, List<AstRange> items, int inlineColumn, int wrappedColumn)
    {
        if (shouldPreserveInlinePrefixedWrappedFirstArgument(sourceModel, region, first, items)) {
            return true;
        }
        String source = sourceModel.source();
        return shouldKeepFirstArgumentInline(
                region.firstArgumentPolicy(),
                first.node() instanceof LambdaExpression && first.node().getParent() instanceof MethodInvocation,
                isSimpleSingleLineFirstArgument(source, first),
                hasTrailingMultilineLambdaArgument(source, items),
                hasOnlyTrailingMultilineArgument(source, items),
                inlineColumn,
                wrappedColumn);
    }

    private static boolean shouldPreserveInlinePrefixedWrappedFirstArgument(
            SourceModel sourceModel,
            ListRegion region,
            AstRange first,
            List<AstRange> items)
    {
        if (items.size() <= 1
                || sourceModel.containsLineBreak(region.openParen() + 1, first.start())
                || !sourceModel.containsLineBreak(first.start(), first.end())
                || (!(first.node() instanceof MethodInvocation) && !(first.node() instanceof ClassInstanceCreation))
                || shouldWrapMultilineExpressionFirstArgument(sourceModel, first.node())) {
            return false;
        }

        for (int index = 1; index < items.size(); index++) {
            AstRange item = items.get(index);
            if (!sourceModel.containsLineBreak(first.end(), item.start())) {
                return false;
            }
        }
        return true;
    }

    private static int lastLineBreak(String text)
    {
        return text.lastIndexOf('\n');
    }

    private static String rstrip(String text)
    {
        int end = text.length();
        while (end > 0) {
            char value = text.charAt(end - 1);
            if (value != ' ' && value != '\t') {
                break;
            }
            end--;
        }
        return text.substring(0, end);
    }

    private static String applyReplacements(String source, List<Replacement> replacements)
    {
        // Coalesce same-range replacements to avoid applying duplicate edits twice.
        var coalesced = new LinkedHashMap<String, Replacement>();
        for (Replacement replacement : replacements) {
            coalesced.put(replacement.start() + ":" + replacement.end(), replacement);
        }

        var ordered = new ArrayList<>(coalesced.values());
        ordered.sort(Comparator.comparingInt(Replacement::start).reversed());
        var result = new StringBuilder(source);
        for (Replacement replacement : ordered) {
            result.replace(replacement.start(), replacement.end(), replacement.value());
        }
        return result.toString();
    }

    private static boolean isNodeInMultiArgumentList(ASTNode node)
    {
        ASTNode parent = node.getParent();
        if (parent == null) {
            return false;
        }

        List<?> items = argumentsOrValues(parent);
        return items != null && isNodeInMultiItemList(node, items);
    }

    private static boolean isContainingInvocationNestedInMultiArgumentList(ASTNode argumentNode)
    {
        ASTNode invocation = argumentNode.getParent();
        if (invocation == null) {
            return false;
        }

        ASTNode parent = invocation.getParent();
        if (parent == null) {
            return false;
        }

        List<?> items = argumentsOrValues(parent);
        return items != null && isNodeInMultiItemList(invocation, items);
    }

    private static boolean isCallOwner(ASTNode owner)
    {
        return switch (owner) {
            case MethodInvocation _, SuperMethodInvocation _, ClassInstanceCreation _,
                 ConstructorInvocation _, SuperConstructorInvocation _, EnumConstantDeclaration _ -> true;
            default -> false;
        };
    }

    private static List<?> argumentsOrValues(ASTNode node)
    {
        return switch (node) {
            case MethodInvocation n -> n.arguments();
            case SuperMethodInvocation n -> n.arguments();
            case ClassInstanceCreation n -> n.arguments();
            case ConstructorInvocation n -> n.arguments();
            case SuperConstructorInvocation n -> n.arguments();
            case EnumConstantDeclaration n -> n.arguments();
            case NormalAnnotation n -> n.values();
            default -> null;
        };
    }

    private static boolean isNodeInMultiItemList(ASTNode node, List<?> items)
    {
        if (items.size() < 2) {
            return false;
        }
        for (Object item : items) {
            if (item.equals(node)) {
                return true;
            }
        }
        return false;
    }
}
