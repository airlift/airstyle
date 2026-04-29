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

import io.airlift.airstyle.JavaLanguageSupport;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextBlock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static java.lang.Math.max;

public final class SourceModel
{
    private static final ThreadLocal<CacheState> CACHE_STATE = new ThreadLocal<>();
    private static final Map<String, String> BASE_PARSER_OPTIONS = JavaLanguageSupport.compilerOptions();

    private final String source;
    private final char[] sourceChars;
    private final CompilationUnit compilationUnit;
    private final List<Comment> comments;
    private final List<CommentRange> commentRanges;
    private final int[] lineStarts;
    private final Map<Object, Object> modelCache = new HashMap<>();
    private final Map<RangeQuery, List<CommentRange>> commentsOverlappingRangeCache = new HashMap<>();
    private final Map<RangeQuery, List<CommentRange>> commentsContainedInCache = new HashMap<>();
    private final Map<RangeQuery, RewriteSafety> rewriteSafetyCache = new HashMap<>();
    private boolean tokenIndexInitialized;
    private boolean tokenIndexAvailable;
    private int[] tokenStarts = new int[0];
    private int[] tokenTypes = new int[0];
    private final Map<Integer, Integer> matchingParenByOpen = new HashMap<>();
    private final Map<TokenQuery, Integer> firstTokenQueryCache = new HashMap<>();
    private final Map<TokenQuery, Integer> lastTokenQueryCache = new HashMap<>();

    private SourceModel(String source)
    {
        this.source = source;
        this.sourceChars = source.toCharArray();
        this.compilationUnit = parseCompilationUnit(sourceChars);
        this.comments = collectComments(compilationUnit);
        this.commentRanges = collectCommentRanges(comments);
        this.lineStarts = computeLineStarts(source);
    }

    public static SourceModel create(String source)
    {
        CacheState cacheState = CACHE_STATE.get();
        if (cacheState == null) {
            return new SourceModel(source);
        }

        SourceModel cached = cacheState.get(source);
        if (cached != null) {
            return cached;
        }

        SourceModel created = new SourceModel(source);
        cacheState.put(source, created);
        return created;
    }

    public static CacheScope openCache()
    {
        return openCache(new SourceModel[0]);
    }

    public static CacheScope openCache(SourceModel... seededModels)
    {
        CacheState cacheState = CACHE_STATE.get();
        if (cacheState == null) {
            CACHE_STATE.set(new CacheState(seededModels));
        }
        else {
            cacheState.incrementDepth();
            cacheState.seed(seededModels);
        }
        return new CacheScope();
    }

    public static void pruneCacheKeeping(String... retainedSources)
    {
        CacheState cacheState = CACHE_STATE.get();
        if (cacheState == null) {
            return;
        }
        cacheState.pruneKeeping(retainedSources);
    }

    public String source()
    {
        return source;
    }

    public CompilationUnit compilationUnit()
    {
        return compilationUnit;
    }

    public boolean hasErrors()
    {
        for (IProblem problem : compilationUnit.getProblems()) {
            if (problem.isError()) {
                return true;
            }
        }
        return false;
    }

    public List<Comment> comments()
    {
        return comments;
    }

    public List<CommentRange> commentRanges()
    {
        return commentRanges;
    }

    @SuppressWarnings("unchecked")
    public <V> V cachedModel(Object key, Supplier<V> factory)
    {
        V cached = (V) modelCache.get(key);
        if (cached != null) {
            return cached;
        }
        V created = factory.get();
        modelCache.put(key, created);
        return created;
    }

    public int lineCount()
    {
        return lineStarts.length;
    }

    public int lineNumber(int index)
    {
        int bounded = Math.clamp(index, 0, source.length());
        int low = 0;
        int high = lineStarts.length - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int lineStart = lineStarts[middle];
            if (lineStart == bounded) {
                return middle;
            }
            if (lineStart < bounded) {
                low = middle + 1;
            }
            else {
                high = middle - 1;
            }
        }
        return max(0, high);
    }

    public int lineStartForLine(int line)
    {
        if (line < 0 || line >= lineStarts.length) {
            return -1;
        }
        return lineStarts[line];
    }

    public int lineStart(int index)
    {
        return lineStartForLine(lineNumber(index));
    }

    public int lineEnd(int index)
    {
        int line = lineNumber(index);
        if (line < 0) {
            return -1;
        }
        if (line + 1 < lineStarts.length) {
            return max(lineStarts[line], lineStarts[line + 1] - 1);
        }
        return source.length();
    }

    public int firstNonWhitespace(int start, int endExclusive)
    {
        int boundedStart = Math.clamp(start, 0, source.length());
        int boundedEnd = Math.clamp(endExclusive, boundedStart, source.length());
        int index = boundedStart;
        while (index < boundedEnd) {
            char current = source.charAt(index);
            if (current != ' ' && current != '\t') {
                return index;
            }
            index++;
        }
        return boundedEnd;
    }

    public int firstNonWhitespaceOnLine(int lineStart)
    {
        return firstNonWhitespace(lineStart, lineEnd(lineStart));
    }

    public String lineIndent(int lineStart)
    {
        int indentEnd = firstNonWhitespaceOnLine(lineStart);
        return source.substring(lineStart, indentEnd);
    }

    public String leadingWhitespace(int start, int endExclusive)
    {
        int boundedStart = Math.clamp(start, 0, source.length());
        return source.substring(boundedStart, firstNonWhitespace(boundedStart, endExclusive));
    }

    public int indentWidth(int lineStart)
    {
        int indentEnd = firstNonWhitespaceOnLine(lineStart);
        return visualWidth(lineStart, indentEnd);
    }

    public int indentWidth(int start, int endExclusive)
    {
        int boundedStart = Math.clamp(start, 0, source.length());
        int boundedEnd = Math.clamp(endExclusive, boundedStart, source.length());
        return visualWidth(boundedStart, boundedEnd);
    }

    public int visualColumn(int index)
    {
        int lineStart = lineStart(index);
        return visualWidth(lineStart, Math.clamp(index, lineStart, source.length()));
    }

    public int nextLineStart(int lineStart)
    {
        int lineEnd = lineEnd(lineStart);
        if (lineEnd >= source.length()) {
            return -1;
        }
        return lineEnd + 1;
    }

    public boolean containsLineBreak(int start, int endExclusive)
    {
        int boundedStart = Math.clamp(start, 0, source.length());
        int boundedEnd = Math.clamp(endExclusive, boundedStart, source.length());
        for (int index = boundedStart; index < boundedEnd; index++) {
            if (source.charAt(index) == '\n') {
                return true;
            }
        }
        return false;
    }

    public static boolean containsLineBreak(CharSequence text)
    {
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) == '\n') {
                return true;
            }
        }
        return false;
    }

    public static boolean containsRepeatedLineBreaks(CharSequence text)
    {
        int lineBreaks = 0;
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) == '\n') {
                lineBreaks++;
                if (lineBreaks >= 2) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean containsOnlyWhitespace(int start, int endExclusive)
    {
        int boundedStart = Math.clamp(start, 0, source.length());
        int boundedEnd = Math.clamp(endExclusive, boundedStart, source.length());
        for (int index = boundedStart; index < boundedEnd; index++) {
            if (!Character.isWhitespace(source.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    public int lastNonWhitespace(int start, int endExclusive)
    {
        int boundedStart = Math.clamp(start, 0, source.length());
        int boundedEnd = Math.clamp(endExclusive, boundedStart, source.length());
        for (int index = boundedEnd - 1; index >= boundedStart; index--) {
            char current = source.charAt(index);
            if (current != ' ' && current != '\t') {
                return index;
            }
        }
        return boundedStart - 1;
    }

    boolean startsOnOwnLine(ASTNode node)
    {
        return startsAtLineIndent(node.getStartPosition());
    }

    public boolean startsAtLineIndent(int position)
    {
        if (position < 0 || position > source.length()) {
            return false;
        }
        int lineStart = lineStart(position);
        return firstNonWhitespace(lineStart, lineEnd(lineStart)) == position;
    }

    public boolean startsMidLine(int position)
    {
        if (position < 0 || position >= source.length()) {
            return false;
        }
        return !startsAtLineIndent(position);
    }

    public boolean commentOverlaps(int start, int endExclusive)
    {
        if (start >= endExclusive || commentRanges.isEmpty()) {
            return false;
        }

        int index = firstCommentIndexEndingAfter(start);
        if (index >= commentRanges.size()) {
            return false;
        }
        return commentRanges.get(index).start() < endExclusive;
    }

    public boolean isCommentLine(int lineStart, int lineEnd)
    {
        int first = firstNonWhitespace(lineStart, lineEnd);
        if (first >= lineEnd) {
            return false;
        }
        if (source.charAt(first) == '*') {
            return true;
        }
        return first + 1 < lineEnd
                && source.charAt(first) == '/'
                && (source.charAt(first + 1) == '/' || source.charAt(first + 1) == '*');
    }

    List<CommentRange> commentsOverlappingRange(int start, int endExclusive)
    {
        if (commentRanges.isEmpty()) {
            return List.of();
        }

        RangeQuery query = rangeQuery(start, endExclusive);
        if (query.start() >= query.endExclusive()) {
            return List.of();
        }

        return commentsOverlappingRangeCache.computeIfAbsent(query, _ -> {
            List<CommentRange> matches = new ArrayList<>();
            for (int index = firstCommentIndexEndingAfter(query.start()); index < commentRanges.size(); index++) {
                CommentRange commentRange = commentRanges.get(index);
                if (commentRange.start() >= query.endExclusive()) {
                    break;
                }
                matches.add(commentRange);
            }
            return List.copyOf(matches);
        });
    }

    List<CommentRange> commentsOverlappingLine(int line)
    {
        int lineStart = lineStartForLine(line);
        if (lineStart < 0) {
            return List.of();
        }
        return commentsOverlappingRange(lineStart, lineEnd(lineStart));
    }

    public List<CommentRange> commentsOverlappingLines(int startLine, int endLineInclusive)
    {
        if (startLine > endLineInclusive) {
            return List.of();
        }

        int firstLineStart = lineStartForLine(startLine);
        int lastLineStart = lineStartForLine(endLineInclusive);
        if (firstLineStart < 0 || lastLineStart < 0) {
            return List.of();
        }

        return commentsOverlappingRange(firstLineStart, lineEnd(lastLineStart));
    }

    public List<CommentRange> commentsContainedIn(int start, int endExclusive)
    {
        if (commentRanges.isEmpty()) {
            return List.of();
        }

        RangeQuery query = rangeQuery(start, endExclusive);
        if (query.start() >= query.endExclusive()) {
            return List.of();
        }

        return commentsContainedInCache.computeIfAbsent(query, _ ->
                commentsOverlappingRange(query.start(), query.endExclusive()).stream()
                        .filter(commentRange -> commentRange.start() >= query.start() && commentRange.end() <= query.endExclusive())
                        .toList());
    }

    public CommentRange commentContaining(int start, int endExclusive)
    {
        if (start >= endExclusive || commentRanges.isEmpty()) {
            return null;
        }
        int index = firstCommentIndexEndingAfter(start);
        if (index >= commentRanges.size()) {
            return null;
        }
        CommentRange commentRange = commentRanges.get(index);
        if (commentRange.start() >= endExclusive) {
            return null;
        }
        return commentRange;
    }

    public RewriteSafety rewriteSafety(int start, int endExclusive)
    {
        RangeQuery query = rangeQuery(start, endExclusive);
        return rewriteSafetyCache.computeIfAbsent(query, _ -> new RewriteSafety(
                query.start(),
                query.endExclusive(),
                commentsOverlappingRange(query.start(), query.endExclusive()),
                commentsContainedIn(query.start(), query.endExclusive())));
    }

    public String text(CommentRange commentRange)
    {
        return source.substring(commentRange.start(), commentRange.end());
    }

    public int attachedLeadingCommentStartLine(ASTNode node)
    {
        int declarationLine = lineNumber(node.getStartPosition());
        int anchorLine = declarationLine;
        int line = declarationLine - 1;

        while (line >= 0) {
            int lineStart = lineStartForLine(line);
            int lineEnd = lineEnd(lineStart);
            if (source.substring(lineStart, lineEnd).trim().isEmpty()) {
                break;
            }

            CommentRange commentRange = commentContaining(lineStart, lineEnd);
            if (commentRange == null) {
                break;
            }

            anchorLine = lineNumber(commentRange.start());
            line = anchorLine - 1;
        }
        return anchorLine;
    }

    List<CommentRange> commentsAttachedToNode(ASTNode node)
    {
        int declarationLine = lineNumber(node.getStartPosition());
        int anchorLine = attachedLeadingCommentStartLine(node);
        if (anchorLine >= declarationLine) {
            return List.of();
        }
        return commentsOverlappingLines(anchorLine, declarationLine - 1);
    }

    public List<TagElement> topLevelJavadocTags(Javadoc javadoc)
    {
        @SuppressWarnings("unchecked")
        List<TagElement> tags = javadoc.tags();
        return List.copyOf(tags);
    }

    List<LineRegion> javadocPreTagLineRegions(Javadoc javadoc)
    {
        int startLine = lineNumber(javadoc.getStartPosition());
        int endLine = lineNumber(max(javadoc.getStartPosition(), javadoc.getStartPosition() + javadoc.getLength() - 1));
        List<LineRegion> regions = new ArrayList<>();
        int openLine = -1;
        for (int line = startLine; line <= endLine; line++) {
            String content = javadocLineContent(line, javadoc.isMarkdown());
            if (content == null) {
                continue;
            }
            if (openLine < 0 && content.contains("<pre")) {
                openLine = line;
            }
            if (openLine >= 0 && content.contains("</pre>")) {
                regions.add(new LineRegion(openLine, line));
                openLine = -1;
            }
        }
        if (openLine >= 0) {
            regions.add(new LineRegion(openLine, endLine));
        }
        return List.copyOf(regions);
    }

    List<LineRegion> javadocPreformattedLineRegions(Javadoc javadoc)
    {
        int startLine = lineNumber(javadoc.getStartPosition());
        int endLine = lineNumber(max(javadoc.getStartPosition(), javadoc.getStartPosition() + javadoc.getLength() - 1));
        List<LineRegion> regions = new ArrayList<>();
        boolean inPre = false;
        boolean inMarkdownFence = false;
        int regionStart = -1;

        for (int line = startLine; line <= endLine; line++) {
            String content = javadocLineContent(line, javadoc.isMarkdown());
            if (content == null) {
                continue;
            }

            String trimmed = content.trim();
            boolean fenceLine = javadoc.isMarkdown() && trimmed.startsWith("```");
            boolean preformattedLine = inPre || inMarkdownFence;
            if (preformattedLine && regionStart < 0) {
                regionStart = line;
            }
            if (!preformattedLine && regionStart >= 0) {
                regions.add(new LineRegion(regionStart, line - 1));
                regionStart = -1;
            }

            if (content.contains("<pre")) {
                inPre = true;
            }
            if (content.contains("</pre>")) {
                inPre = false;
            }
            if (fenceLine) {
                inMarkdownFence = !inMarkdownFence;
            }
        }

        if (regionStart >= 0) {
            regions.add(new LineRegion(regionStart, endLine));
        }
        return List.copyOf(regions);
    }

    public boolean javadocContainsPreBlock(Javadoc javadoc)
    {
        return !javadocPreTagLineRegions(javadoc).isEmpty();
    }

    public boolean isInsideJavadocPreBlock(Javadoc javadoc, int position)
    {
        int targetLine = lineNumber(position);
        for (LineRegion region : javadocPreformattedLineRegions(javadoc)) {
            if (region.contains(targetLine)) {
                return true;
            }
        }
        return false;
    }

    public JavadocLineInfo javadocLineInfo(int line, boolean markdown)
    {
        int lineStart = lineStartForLine(line);
        if (lineStart < 0) {
            return null;
        }
        int lineEnd = lineEnd(lineStart);
        int first = firstNonWhitespace(lineStart, lineEnd);
        if (first >= lineEnd) {
            return null;
        }

        int prefixEnd;
        if (markdown) {
            if (!(first + 2 < lineEnd
                    && source.charAt(first) == '/'
                    && source.charAt(first + 1) == '/'
                    && source.charAt(first + 2) == '/')) {
                return null;
            }
            prefixEnd = first + 3;
        }
        else {
            if (source.charAt(first) != '*') {
                return null;
            }
            prefixEnd = first + 1;
        }

        int contentStart = prefixEnd;
        while (contentStart < lineEnd) {
            char current = source.charAt(contentStart);
            if (current != ' ' && current != '\t') {
                break;
            }
            contentStart++;
        }

        return new JavadocLineInfo(
                lineStart,
                first,
                contentStart,
                contentStart >= lineEnd ? "" : source.substring(contentStart, lineEnd));
    }

    boolean isQualified(Name name)
    {
        return name.isQualifiedName();
    }

    public boolean startsWithTextBlock(Expression expression)
    {
        Expression current = expression;
        while (true) {
            if (current instanceof ParenthesizedExpression parenthesizedExpression) {
                current = parenthesizedExpression.getExpression();
                continue;
            }
            if (current instanceof CastExpression castExpression) {
                current = castExpression.getExpression();
                continue;
            }
            break;
        }

        if (current instanceof TextBlock) {
            return true;
        }
        if (current instanceof MethodInvocation methodInvocation && methodInvocation.getExpression() != null) {
            return startsWithTextBlock(methodInvocation.getExpression());
        }
        if (current instanceof FieldAccess fieldAccess) {
            return startsWithTextBlock(fieldAccess.getExpression());
        }
        return false;
    }

    public List<TextBlockLine> textBlockLines(TextBlock textBlock)
    {
        int start = textBlock.getStartPosition();
        int end = start + textBlock.getLength();
        if (start < 0 || end > source.length() || end <= start) {
            return List.of();
        }

        int startLine = lineNumber(start);
        int endLine = lineNumber(max(start, end - 1));
        if (endLine <= startLine) {
            return List.of();
        }

        List<TextBlockLine> lines = new ArrayList<>();
        for (int line = startLine + 1; line <= endLine; line++) {
            int lineStart = lineStartForLine(line);
            int lineEnd = lineEnd(lineStart);
            int firstNonWhitespace = firstNonWhitespace(lineStart, lineEnd);
            lines.add(new TextBlockLine(lineStart, lineEnd, firstNonWhitespace));
        }
        return List.copyOf(lines);
    }

    public List<TextBlockLine> textBlockContentLines(TextBlock textBlock)
    {
        List<TextBlockLine> lines = textBlockLines(textBlock);
        if (lines.size() <= 1) {
            return List.of();
        }
        return List.copyOf(lines.subList(0, lines.size() - 1));
    }

    public TextBlockLine textBlockClosingLine(TextBlock textBlock)
    {
        List<TextBlockLine> lines = textBlockLines(textBlock);
        return lines.isEmpty() ? null : lines.getLast();
    }

    int textBlockMinimumContentIndent(TextBlock textBlock)
    {
        int minimum = Integer.MAX_VALUE;
        for (TextBlockLine line : textBlockContentLines(textBlock)) {
            if (line.blank()) {
                continue;
            }
            minimum = Math.min(minimum, line.indentWidth());
        }
        return minimum == Integer.MAX_VALUE ? -1 : minimum;
    }

    int textBlockMinimumMarginColumn(TextBlock textBlock)
    {
        int minimum = Integer.MAX_VALUE;
        for (TextBlockLine line : textBlockContentLines(textBlock)) {
            if (line.blank()) {
                continue;
            }
            minimum = Math.min(minimum, line.firstNonWhitespace());
        }
        return minimum == Integer.MAX_VALUE ? -1 : minimum;
    }

    public TrailingLineComment trailingLineComment(int line)
    {
        int lineStart = lineStartForLine(line);
        if (lineStart < 0) {
            return null;
        }
        int lineEnd = lineEnd(lineStart);
        for (CommentRange commentRange : commentsOverlappingLine(line)) {
            if (lineNumber(commentRange.start()) != line || commentRange.end() > lineEnd) {
                continue;
            }

            String commentText = source.substring(commentRange.start(), commentRange.end());
            if (!commentText.startsWith("//")) {
                continue;
            }

            int codeEnd = lastNonWhitespace(lineStart, commentRange.start());
            if (codeEnd < lineStart) {
                continue;
            }

            int spacing = max(0, commentRange.start() - (codeEnd + 1));
            return new TrailingLineComment(spacing, source.substring(commentRange.start(), lineEnd));
        }
        return null;
    }

    List<TrailingLineComment> trailingLineComments(int startLine, int endLineInclusive)
    {
        if (startLine > endLineInclusive) {
            return List.of();
        }
        List<TrailingLineComment> trailingComments = new ArrayList<>();
        for (int line = startLine; line <= endLineInclusive; line++) {
            TrailingLineComment trailing = trailingLineComment(line);
            if (trailing != null) {
                trailingComments.add(trailing);
            }
        }
        return List.copyOf(trailingComments);
    }

    public int findOpeningParen(int start, int endExclusive)
    {
        return findTokenBetween(start, endExclusive, ITerminalSymbols.TokenNameLPAREN);
    }

    public int findMatchingParen(int openParen, int endExclusive)
    {
        if (openParen < 0 || openParen >= source.length()) {
            return -1;
        }

        int boundedEnd = Math.clamp(endExclusive, openParen, source.length());
        if (ensureTokenIndex()) {
            Integer match = matchingParenByOpen.get(openParen);
            if (match == null || match >= boundedEnd) {
                return -1;
            }
            return match;
        }

        IScanner scanner = newScanner();
        scanner.setSource(sourceChars);
        scanner.resetTo(openParen, max(openParen, boundedEnd - 1));

        int depth = 0;
        try {
            while (true) {
                int token = scanner.getNextToken();
                if (token == ITerminalSymbols.TokenNameEOF) {
                    return -1;
                }
                if (token == ITerminalSymbols.TokenNameLPAREN) {
                    depth++;
                    continue;
                }
                if (token == ITerminalSymbols.TokenNameRPAREN) {
                    depth--;
                    if (depth == 0) {
                        return scanner.getCurrentTokenStartPosition();
                    }
                }
            }
        }
        catch (InvalidInputException _) {
            return -1;
        }
    }

    public int findDotBetween(int start, int endExclusive)
    {
        return findTokenBetween(start, endExclusive, ITerminalSymbols.TokenNameDOT);
    }

    public int findCommaBetween(int start, int endExclusive)
    {
        return findTokenBetween(start, endExclusive, ITerminalSymbols.TokenNameCOMMA);
    }

    public int findArrowBetween(int start, int endExclusive)
    {
        return findTokenBetween(start, endExclusive, ITerminalSymbols.TokenNameARROW);
    }

    public boolean containsTokenBetween(int start, int endExclusive, int token)
    {
        return findTokenBetween(start, endExclusive, token) >= 0;
    }

    boolean containsOnlyTokenBetween(int start, int endExclusive, int token)
    {
        int boundedStart = Math.clamp(start, 0, source.length());
        int boundedEnd = Math.clamp(endExclusive, boundedStart, source.length());
        if (boundedStart >= boundedEnd) {
            return false;
        }

        if (ensureTokenIndex()) {
            int index = firstTokenIndexAtOrAfter(boundedStart);
            if (index >= tokenStarts.length || tokenStarts[index] >= boundedEnd || tokenTypes[index] != token) {
                return false;
            }
            index++;
            return index >= tokenStarts.length || tokenStarts[index] >= boundedEnd;
        }

        IScanner scanner = newScanner();
        scanner.setSource(sourceChars);
        scanner.resetTo(boundedStart, boundedEnd - 1);
        boolean seenExpected = false;
        try {
            while (true) {
                int current = scanner.getNextToken();
                if (current == ITerminalSymbols.TokenNameEOF) {
                    return seenExpected;
                }
                if (!seenExpected && current == token) {
                    seenExpected = true;
                    continue;
                }
                return false;
            }
        }
        catch (InvalidInputException _) {
            return false;
        }
    }

    public boolean containsOnlyTokensWhitespaceAndComments(int start, int endExclusive, int... allowedTokens)
    {
        int boundedStart = Math.clamp(start, 0, source.length());
        int boundedEnd = Math.clamp(endExclusive, boundedStart, source.length());
        if (boundedStart >= boundedEnd) {
            return true;
        }

        int cursor = boundedStart;
        for (CommentRange comment : rewriteSafety(boundedStart, boundedEnd).containedComments()) {
            if (!containsOnlyAllowedTokensAndWhitespace(cursor, comment.start(), allowedTokens)) {
                return false;
            }
            cursor = comment.end();
        }
        return containsOnlyAllowedTokensAndWhitespace(cursor, boundedEnd, allowedTokens);
    }

    public boolean containsOnlyTokensAndWhitespace(int start, int endExclusive, int... allowedTokens)
    {
        int boundedStart = Math.clamp(start, 0, source.length());
        return containsOnlyAllowedTokensAndWhitespace(
                boundedStart,
                Math.clamp(endExclusive, boundedStart, source.length()),
                allowedTokens);
    }

    public int findTokenBetween(int start, int endExclusive, int targetToken)
    {
        int boundedStart = Math.clamp(start, 0, source.length());
        int boundedEnd = Math.clamp(endExclusive, boundedStart, source.length());
        if (boundedStart >= boundedEnd) {
            return -1;
        }

        TokenQuery query = new TokenQuery(boundedStart, boundedEnd, targetToken);
        Integer cached = firstTokenQueryCache.get(query);
        if (cached != null) {
            return cached;
        }

        if (ensureTokenIndex()) {
            int index = firstTokenIndexAtOrAfter(boundedStart);
            while (index < tokenStarts.length && tokenStarts[index] < boundedEnd) {
                if (tokenTypes[index] == targetToken) {
                    int result = tokenStarts[index];
                    firstTokenQueryCache.put(query, result);
                    return result;
                }
                index++;
            }
            firstTokenQueryCache.put(query, -1);
            return -1;
        }
        int result = scanForToken(boundedStart, boundedEnd, targetToken, null);
        firstTokenQueryCache.put(query, result);
        return result;
    }

    public int findLastTokenBetween(int start, int endExclusive, int targetToken)
    {
        int boundedStart = Math.clamp(start, 0, source.length());
        int boundedEnd = Math.clamp(endExclusive, boundedStart, source.length());
        if (boundedStart >= boundedEnd) {
            return -1;
        }

        TokenQuery query = new TokenQuery(boundedStart, boundedEnd, targetToken);
        Integer cached = lastTokenQueryCache.get(query);
        if (cached != null) {
            return cached;
        }

        if (ensureTokenIndex()) {
            int index = firstTokenIndexAtOrAfter(boundedStart);
            int lastMatch = -1;
            while (index < tokenStarts.length && tokenStarts[index] < boundedEnd) {
                if (tokenTypes[index] == targetToken) {
                    lastMatch = tokenStarts[index];
                }
                index++;
            }
            lastTokenQueryCache.put(query, lastMatch);
            return lastMatch;
        }

        int lastMatch = -1;
        IScanner scanner = newScanner();
        scanner.setSource(sourceChars);
        scanner.resetTo(boundedStart, boundedEnd - 1);
        try {
            while (true) {
                int token = scanner.getNextToken();
                if (token == ITerminalSymbols.TokenNameEOF) {
                    lastTokenQueryCache.put(query, lastMatch);
                    return lastMatch;
                }
                if (token == targetToken) {
                    lastMatch = scanner.getCurrentTokenStartPosition();
                }
            }
        }
        catch (InvalidInputException _) {
            lastTokenQueryCache.put(query, lastMatch);
            return lastMatch;
        }
    }

    public int findKeywordBetween(int start, int endExclusive, String keyword)
    {
        return scanForToken(start, endExclusive, -1, keyword);
    }

    public int findLastKeywordBetween(int start, int endExclusive, String keyword)
    {
        int boundedStart = Math.clamp(start, 0, source.length());
        int boundedEnd = Math.clamp(endExclusive, boundedStart, source.length());
        if (boundedStart >= boundedEnd) {
            return -1;
        }

        int lastMatch = -1;
        IScanner scanner = newScanner();
        scanner.setSource(sourceChars);
        scanner.resetTo(boundedStart, boundedEnd - 1);
        try {
            while (true) {
                int token = scanner.getNextToken();
                if (token == ITerminalSymbols.TokenNameEOF) {
                    return lastMatch;
                }
                if (tokenMatchesKeyword(scanner.getCurrentTokenSource(), keyword)) {
                    lastMatch = scanner.getCurrentTokenStartPosition();
                }
            }
        }
        catch (InvalidInputException _) {
            return lastMatch;
        }
    }

    public int findTokenTextBetween(int start, int endExclusive, String tokenText)
    {
        return scanForToken(start, endExclusive, -1, tokenText);
    }

    public int firstTokenTypeBetween(int start, int endExclusive)
    {
        int boundedStart = Math.clamp(start, 0, source.length());
        int boundedEnd = Math.clamp(endExclusive, boundedStart, source.length());
        if (boundedStart >= boundedEnd) {
            return ITerminalSymbols.TokenNameEOF;
        }

        if (ensureTokenIndex()) {
            int index = firstTokenIndexAtOrAfter(boundedStart);
            if (index < tokenStarts.length && tokenStarts[index] < boundedEnd) {
                return tokenTypes[index];
            }
            return ITerminalSymbols.TokenNameEOF;
        }

        IScanner scanner = newScanner();
        scanner.setSource(sourceChars);
        scanner.resetTo(boundedStart, boundedEnd - 1);
        try {
            return scanner.getNextToken();
        }
        catch (InvalidInputException _) {
            return ITerminalSymbols.TokenNameEOF;
        }
    }

    private int scanForToken(int start, int endExclusive, int targetToken, String keyword)
    {
        int boundedStart = Math.clamp(start, 0, source.length());
        int boundedEnd = Math.clamp(endExclusive, boundedStart, source.length());
        if (boundedStart >= boundedEnd) {
            return -1;
        }

        IScanner scanner = newScanner();
        scanner.setSource(sourceChars);
        scanner.resetTo(boundedStart, boundedEnd - 1);
        try {
            while (true) {
                int token = scanner.getNextToken();
                if (token == ITerminalSymbols.TokenNameEOF) {
                    return -1;
                }
                if (targetToken >= 0 && token == targetToken) {
                    return scanner.getCurrentTokenStartPosition();
                }
                if (keyword != null && tokenMatchesKeyword(scanner.getCurrentTokenSource(), keyword)) {
                    return scanner.getCurrentTokenStartPosition();
                }
            }
        }
        catch (InvalidInputException _) {
            return -1;
        }
    }

    private boolean containsOnlyAllowedTokensAndWhitespace(int start, int endExclusive, int... allowedTokens)
    {
        int boundedStart = Math.clamp(start, 0, source.length());
        int boundedEnd = Math.clamp(endExclusive, boundedStart, source.length());
        if (boundedStart >= boundedEnd) {
            return true;
        }

        IScanner scanner = newScanner();
        scanner.setSource(sourceChars);
        scanner.resetTo(boundedStart, boundedEnd - 1);
        try {
            while (true) {
                int token = scanner.getNextToken();
                if (token == ITerminalSymbols.TokenNameEOF) {
                    return true;
                }
                if (!containsToken(allowedTokens, token)) {
                    return false;
                }
            }
        }
        catch (InvalidInputException _) {
            return false;
        }
    }

    private static boolean containsToken(int[] allowedTokens, int token)
    {
        for (int allowedToken : allowedTokens) {
            if (allowedToken == token) {
                return true;
            }
        }
        return false;
    }

    private int firstCommentIndexEndingAfter(int start)
    {
        int low = 0;
        int high = commentRanges.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (commentRanges.get(middle).end() <= start) {
                low = middle + 1;
            }
            else {
                high = middle;
            }
        }
        return low;
    }

    private boolean ensureTokenIndex()
    {
        if (tokenIndexInitialized) {
            return tokenIndexAvailable;
        }

        tokenIndexInitialized = true;
        List<Integer> starts = new ArrayList<>();
        List<Integer> types = new ArrayList<>();
        List<Integer> parenStack = new ArrayList<>();

        IScanner scanner = newScanner();
        scanner.setSource(sourceChars);
        if (!source.isEmpty()) {
            scanner.resetTo(0, source.length() - 1);
        }

        try {
            while (true) {
                int token = scanner.getNextToken();
                if (token == ITerminalSymbols.TokenNameEOF) {
                    tokenStarts = toIntArray(starts);
                    tokenTypes = toIntArray(types);
                    tokenIndexAvailable = true;
                    return true;
                }
                int tokenStart = scanner.getCurrentTokenStartPosition();
                starts.add(tokenStart);
                types.add(token);
                if (token == ITerminalSymbols.TokenNameLPAREN) {
                    parenStack.add(tokenStart);
                }
                else if (token == ITerminalSymbols.TokenNameRPAREN && !parenStack.isEmpty()) {
                    int openParen = parenStack.removeLast();
                    matchingParenByOpen.put(openParen, tokenStart);
                }
            }
        }
        catch (InvalidInputException _) {
            tokenStarts = new int[0];
            tokenTypes = new int[0];
            matchingParenByOpen.clear();
            tokenIndexAvailable = false;
            return false;
        }
    }

    private int firstTokenIndexAtOrAfter(int start)
    {
        int low = 0;
        int high = tokenStarts.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (tokenStarts[middle] < start) {
                low = middle + 1;
            }
            else {
                high = middle;
            }
        }
        return low;
    }

    private static int[] toIntArray(List<Integer> values)
    {
        int[] result = new int[values.size()];
        for (int index = 0; index < values.size(); index++) {
            result[index] = values.get(index);
        }
        return result;
    }

    private static boolean tokenMatchesKeyword(char[] tokenSource, String keyword)
    {
        if (tokenSource.length != keyword.length()) {
            return false;
        }
        for (int index = 0; index < tokenSource.length; index++) {
            if (tokenSource[index] != keyword.charAt(index)) {
                return false;
            }
        }
        return true;
    }

    public int visualWidth(int start, int endExclusive)
    {
        int boundedStart = Math.clamp(start, 0, source.length());
        int boundedEnd = Math.clamp(endExclusive, boundedStart, source.length());
        int width = 0;
        for (int index = boundedStart; index < boundedEnd; index++) {
            width += sourceChars[index] == '\t' ? 4 : 1;
        }
        return width;
    }

    private static CompilationUnit parseCompilationUnit(char[] source)
    {
        ASTParser parser = ASTParser.newParser(JavaLanguageSupport.latestAstLevel());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(source);
        parser.setResolveBindings(false);
        parser.setCompilerOptions(BASE_PARSER_OPTIONS);
        return (CompilationUnit) parser.createAST(null);
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

    private static List<Comment> collectComments(CompilationUnit compilationUnit)
    {
        @SuppressWarnings("unchecked")
        List<Comment> rawComments = compilationUnit.getCommentList();
        if (rawComments == null || rawComments.isEmpty()) {
            return List.of();
        }
        return List.copyOf(rawComments);
    }

    private static List<CommentRange> collectCommentRanges(List<Comment> comments)
    {
        if (comments.isEmpty()) {
            return List.of();
        }

        List<CommentRange> ranges = new ArrayList<>(comments.size());
        for (Comment comment : comments) {
            ranges.add(new CommentRange(comment.getStartPosition(), comment.getStartPosition() + comment.getLength()));
        }
        ranges.sort(Comparator.comparingInt(CommentRange::start));
        return List.copyOf(ranges);
    }

    private static int[] computeLineStarts(String source)
    {
        List<Integer> starts = new ArrayList<>();
        starts.add(0);
        for (int index = 0; index < source.length(); index++) {
            if (source.charAt(index) == '\n') {
                starts.add(index + 1);
            }
        }
        return toIntArray(starts);
    }

    public static int visualWidth(CharSequence indentation)
    {
        int width = 0;
        for (int index = 0; index < indentation.length(); index++) {
            width += indentation.charAt(index) == '\t' ? 4 : 1;
        }
        return width;
    }

    private String javadocLineContent(int line, boolean markdown)
    {
        JavadocLineInfo lineInfo = javadocLineInfo(line, markdown);
        return lineInfo == null ? null : lineInfo.content();
    }

    public record LineRegion(int startLine, int endLine)
    {
        public boolean contains(int line)
        {
            return line >= startLine && line <= endLine;
        }
    }

    public record JavadocLineInfo(
            int lineStart,
            int firstNonWhitespace,
            int contentStart,
            String content) {}

    public record RewriteSafety(int start, int endExclusive, List<CommentRange> overlappingComments, List<CommentRange> containedComments)
    {
        boolean hasComments()
        {
            return !overlappingComments.isEmpty();
        }

        public boolean safeToReplace()
        {
            return overlappingComments.isEmpty();
        }
    }

    public record CommentRange(int start, int end) {}

    private RangeQuery rangeQuery(int start, int endExclusive)
    {
        int boundedStart = Math.clamp(start, 0, source.length());
        int boundedEnd = Math.clamp(endExclusive, boundedStart, source.length());
        return new RangeQuery(boundedStart, boundedEnd);
    }

    private record RangeQuery(int start, int endExclusive) {}

    public record TrailingLineComment(int spacingBeforeComment, String commentText) {}

    public record TextBlockLine(int lineStart, int lineEnd, int firstNonWhitespace)
    {
        public boolean blank()
        {
            return firstNonWhitespace >= lineEnd;
        }

        public int indentWidth()
        {
            return max(0, firstNonWhitespace - lineStart);
        }
    }

    public static final class CacheScope
            implements AutoCloseable
    {
        private boolean closed;

        private CacheScope() {}

        @Override
        public void close()
        {
            if (closed) {
                return;
            }
            closed = true;

            CacheState cacheState = CACHE_STATE.get();
            if (cacheState == null) {
                return;
            }

            cacheState.decrementDepth();
            if (cacheState.isExhausted()) {
                CACHE_STATE.remove();
            }
        }
    }

    private static final class CacheState
    {
        private final Map<String, SourceModel> identityCache = new IdentityHashMap<>();
        private final Map<String, SourceModel> textCache = new HashMap<>();
        private int depth = 1;

        private CacheState(SourceModel... seededModels)
        {
            seed(seededModels);
        }

        SourceModel get(String source)
        {
            SourceModel cached = identityCache.get(source);
            if (cached != null) {
                return cached;
            }
            cached = textCache.get(source);
            if (cached != null) {
                identityCache.put(source, cached);
            }
            return cached;
        }

        void put(String source, SourceModel model)
        {
            identityCache.put(source, model);
            textCache.put(source, model);
        }

        void incrementDepth()
        {
            depth++;
        }

        void decrementDepth()
        {
            depth--;
        }

        boolean isExhausted()
        {
            return depth <= 0;
        }

        void seed(SourceModel... seededModels)
        {
            if (seededModels == null) {
                return;
            }
            for (SourceModel model : seededModels) {
                if (model == null) {
                    continue;
                }
                put(model.source(), model);
            }
        }

        void pruneKeeping(String... retainedSources)
        {
            Map<String, SourceModel> retainedByIdentity = new IdentityHashMap<>();
            Map<String, SourceModel> retainedByText = new HashMap<>();
            for (String retainedSource : retainedSources) {
                if (retainedSource == null) {
                    continue;
                }
                SourceModel retained = identityCache.get(retainedSource);
                if (retained == null) {
                    retained = textCache.get(retainedSource);
                }
                if (retained == null) {
                    continue;
                }
                retainedByIdentity.put(retainedSource, retained);
                retainedByText.putIfAbsent(retainedSource, retained);
            }
            identityCache.clear();
            identityCache.putAll(retainedByIdentity);
            textCache.clear();
            textCache.putAll(retainedByText);
        }
    }

    private record TokenQuery(int start, int endExclusive, int token) {}
}
