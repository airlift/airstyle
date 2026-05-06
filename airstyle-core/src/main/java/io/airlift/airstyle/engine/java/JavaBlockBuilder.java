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
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SwitchExpression;
import org.eclipse.jdt.core.dom.TextBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.Math.max;

/// Walks a JDT [CompilationUnit] plus the scanner token stream and
/// produces an engine [Block] tree. Port of IntelliJ's
/// `AbstractJavaBlock` family, specialized to Airlift's fixed style.
///
/// Structured as a per-AST-node dispatch. Each handler owns the decisions
/// for its construct: which tokens belong to the composite, what
/// [Indent] the composite has, and what [Spacing] applies between
/// each pair of children.
public final class JavaBlockBuilder
{
    private final String source;
    private final List<JavaTokens.Token> tokens;
    private final JavaSourceContext sourceContext;
    /// Source offsets of generic-type opening `<` brackets (ParameterizedType / TypeParameter).
    private final Set<Integer> genericOpens;
    /// Source offsets of `when` identifiers that are switch-case guard keywords (not Mockito's when method).
    private final Set<Integer> switchGuardWhens;
    /// Annotations whose value list spans multiple source lines — decomposed in headers.
    private final Map<Integer, Annotation> wrappedAnnotations;
    private final JavaBlockFactory blockFactory;
    private final JavaTokenRunBuilder tokenRunBuilder;
    private final JavaExpressionBuilder expressionBuilder;
    private final JavaDeclarationBuilder declarationBuilder;
    private final JavaConstructBuilder<ASTNode> loopConstructBuilder;
    private final JavaStatementBuilder statementBuilder;

    private JavaBlockBuilder(JavaSourceContext sourceContext, JavaFormatMetadata metadata)
    {
        this.sourceContext = sourceContext;
        this.source = sourceContext.source();
        this.tokens = sourceContext.tokens();
        this.genericOpens = metadata.genericOpens();
        this.switchGuardWhens = metadata.switchGuardWhens();
        this.wrappedAnnotations = metadata.wrappedAnnotations();
        this.blockFactory = new JavaBlockFactory();
        this.tokenRunBuilder = new JavaTokenRunBuilder(sourceContext, new JavaSpacingPolicy(sourceContext, metadata));
        this.expressionBuilder = new JavaExpressionBuilder(this, sourceContext, switchGuardWhens, wrappedAnnotations);
        this.declarationBuilder = new JavaDeclarationBuilder(this, sourceContext, genericOpens, wrappedAnnotations);
        this.loopConstructBuilder = createLoopConstructBuilder();
        this.statementBuilder = new JavaStatementBuilder(this, sourceContext, switchGuardWhens, loopConstructBuilder);
    }

    public static Block build(CompilationUnit unit, String source)
    {
        List<JavaTokens.Token> tokens = JavaTokens.scan(source);
        JavaSourceContext sourceContext = new JavaSourceContext(source, tokens);
        JavaBlockBuilder builder = new JavaBlockBuilder(sourceContext, JavaFormatMetadata.from(unit, sourceContext));

        return builder.buildCompilationUnit(unit);
    }

    static JavaBlockBuilder forTesting(CompilationUnit unit, String source)
    {
        List<JavaTokens.Token> tokens = JavaTokens.scan(source);
        JavaSourceContext sourceContext = new JavaSourceContext(source, tokens);
        return new JavaBlockBuilder(sourceContext, JavaFormatMetadata.from(unit, sourceContext));
    }

    boolean hasWrappedParametersForTesting(MethodDeclaration node)
    {
        return declarationBuilder.hasWrappedParameters(node);
    }

    private JavaConstructBuilder<ASTNode> createLoopConstructBuilder()
    {
        return new JavaLoopConstructBuilder(blockFactory, new JavaLoopConstructBuilder.Support()
        {
            @Override
            public boolean containsLineBreak(int start, int end)
            {
                return JavaBlockBuilder.this.containsLineBreak(start, end);
            }

            @Override
            public int firstChar(int start, int end, char target)
            {
                return JavaBlockBuilder.this.firstChar(start, end, target);
            }

            @Override
            public int lastChar(int start, int end, char target)
            {
                return JavaBlockBuilder.this.lastChar(start, end, target);
            }

            @Override
            public int findMatchingRParen(int lparenOffset, int maxEnd)
            {
                return JavaBlockBuilder.this.findMatchingRParen(lparenOffset, maxEnd);
            }

            @Override
            public int[] findTopLevelSemicolons(int start, int end)
            {
                return JavaBlockBuilder.this.findTopLevelSemicolons(start, end);
            }

            @Override
            public int lastNonWhitespaceBefore(int endExclusive, int minimumStart)
            {
                return JavaBlockBuilder.this.lastNonWhitespaceBefore(endExclusive, minimumStart);
            }

            @Override
            public int extendThroughTrailingInlineComments(int currentEnd, int boundary)
            {
                return JavaBlockBuilder.this.extendThroughTrailingInlineComments(currentEnd, boundary);
            }

            @Override
            public Block buildTokensRange(int start, int end, String debugName)
            {
                return JavaBlockBuilder.this.buildTokensRange(start, end, debugName);
            }

            @Override
            public Block buildTokensRange(int start, int end, String debugName, boolean canUseFirstChildIndent)
            {
                return JavaBlockBuilder.this.buildTokensRange(start, end, debugName, canUseFirstChildIndent);
            }

            @Override
            public Block buildExpressionBlock(Expression expression, int start, int end, String debugName)
            {
                return JavaBlockBuilder.this.buildExpressionBlock(expression, start, end, debugName);
            }

            @Override
            public Block buildControlStatement(Statement statement)
            {
                return JavaBlockBuilder.this.buildControlStatement(statement);
            }

            @Override
            public void addSibling(JavaBlock.Builder parent, Block first, Block second, Spacing spacing)
            {
                JavaBlockBuilder.this.addSibling(parent, first, second, spacing);
            }
        });
    }

    /// Find the first occurrence of `target` character in any token in
    /// `[start, end)`. Token-based scanning automatically skips
    /// comments, strings, char literals, and markdown javadoc since those are
    /// their own token types (not code). Supports multi-character tokens like
    /// `>>` / `>>>` / `<=` / `>=` — scans each token's text.
    private static int findBracket(List<JavaTokens.Token> tokens, int start, int end, char target)
    {
        for (JavaTokens.Token tok : tokens) {
            if (tok.start() >= end) {
                break;
            }
            if (tok.end() <= start) {
                continue;
            }
            if (tok.isComment()) {
                continue;
            }
            String text = tok.text();
            for (int i = 0; i < text.length(); i++) {
                int pos = tok.start() + i;
                if (pos < start || pos >= end) {
                    continue;
                }
                if (text.charAt(i) == target) {
                    return pos;
                }
            }
        }
        return -1;
    }

    /// Like [#findBracket] but returns the LAST occurrence.
    private static int findLastBracket(List<JavaTokens.Token> tokens, int start, int end, char target)
    {
        int last = -1;
        for (JavaTokens.Token tok : tokens) {
            if (tok.start() >= end) {
                break;
            }
            if (tok.end() <= start) {
                continue;
            }
            if (tok.isComment()) {
                continue;
            }
            String text = tok.text();
            for (int i = 0; i < text.length(); i++) {
                int pos = tok.start() + i;
                if (pos < start || pos >= end) {
                    continue;
                }
                if (text.charAt(i) == target) {
                    last = pos;
                }
            }
        }
        return last;
    }

    private Block buildCompilationUnit(CompilationUnit unit)
    {
        JavaBlock.Builder root = JavaBlock.builder(0, source.length(), "CompilationUnit");
        PackageDeclaration pkg = unit.getPackage();
        Block prev = null;
        int cursor = 0;
        int firstSignificant = firstSignificantOffset(unit);
        boolean hasPreamble = false;
        // Pre-package content (copyright header / license comments). Skip
        // when the preamble is pure whitespace — a zero-token block would
        // interact badly with ApplyChangesState's source-passthrough and
        // duplicate leading blank lines.
        if (firstSignificant > 0 && containsNonWhitespace(0, firstSignificant)) {
            Block preamble = buildTokensRange(0, firstSignificant, "Preamble");
            root.child(preamble);
            prev = preamble;
            cursor = firstSignificant;
            hasPreamble = true;
        }
        if (pkg != null) {
            int packageEnd = pkg.getStartPosition() + pkg.getLength();
            BuiltTopLevel builtPackage = buildTopLevelWithTrailingComment(
                    buildTokensSpanning(pkg, "package"),
                    packageEnd,
                    source.length(),
                    "PackageWithTrailingComment");
            // After a preamble (typically a comment), no forced blank line.
            Spacing pkgSpacing = Spacing.createSpacing(0, 0, 1, true, 1);
            prev = attachTopLevel(
                    root,
                    prev,
                    cursor,
                    pkg.getStartPosition(),
                    builtPackage.block(),
                    pkgSpacing);
            cursor = builtPackage.endOffset();
            hasPreamble = false;
        }
        for (Object imp : unit.imports()) {
            ASTNode node = (ASTNode) imp;
            int importEnd = endOfImportDeclaration(node);
            BuiltTopLevel builtImport = buildTopLevelWithTrailingComment(
                    buildImportDeclaration(node, importEnd),
                    importEnd,
                    source.length(),
                    "ImportWithTrailingComment");
            prev = attachTopLevel(
                    root,
                    prev,
                    cursor,
                    node.getStartPosition(),
                    builtImport.block(),
                    Spacing.createSpacing(0, 0, 1, true, 1));
            cursor = builtImport.endOffset();
            hasPreamble = false;
        }
        for (Object type : unit.types()) {
            ASTNode node = (ASTNode) type;
            int typeEnd = node.getStartPosition() + node.getLength();
            BuiltTopLevel builtType = buildTopLevelWithTrailingComment(
                    buildTypeOrDeclaration((BodyDeclaration) type),
                    typeEnd,
                    source.length(),
                    "TopLevelTypeWithTrailingComment");
            // After a preamble (comment directly preceding the type), the
            // type follows on the next line without forcing a blank line.
            Spacing typeSpacing = hasPreamble
                    ? Spacing.createSpacing(0, 0, 1, true, 1)
                    : Spacing.createSpacing(0, 0, 2, true, 1);
            prev = attachTopLevel(
                    root,
                    prev,
                    cursor,
                    node.getStartPosition(),
                    builtType.block(),
                    typeSpacing);
            cursor = builtType.endOffset();
            hasPreamble = false;
        }
        if (cursor < source.length() && containsNonWhitespace(cursor, source.length())) {
            Block tail = buildTokensRange(cursor, source.length(), "TrailingTopLevelTokens");
            if (prev != null) {
                addSibling(root, prev, tail, Spacing.createSpacing(0, 0, 1, true, 1));
            }
            else {
                root.child(tail);
            }
        }
        return root.build();
    }

    private BuiltTopLevel buildTopLevelWithTrailingComment(Block block, int elementEnd, int rangeEnd, String debugName)
    {
        JavaTokens.Token trailingComment = findTrailingLineComment(elementEnd, rangeEnd);
        if (trailingComment == null) {
            return new BuiltTopLevel(block, elementEnd);
        }

        int wrapperEnd = trailingComment.text().endsWith("\n") ? trailingComment.end() - 1 : trailingComment.end();
        JavaBlock.Builder wrapper = JavaBlock.builder(block.startOffset(), wrapperEnd, debugName);
        wrapper.child(block);
        Block commentLeafBlock = commentLeaf(trailingComment);
        int sourceSpaces = max(1, trailingComment.start() - elementEnd);
        wrapper.spacing(block, commentLeafBlock, Spacing.createSpacing(sourceSpaces, sourceSpaces, 0, false, 0));
        wrapper.child(commentLeafBlock);
        return new BuiltTopLevel(wrapper.build(), wrapperEnd);
    }

    private record BuiltTopLevel(Block block, int endOffset) {}

    /// Attach a top-level `child` to `root`, first emitting any
    /// inter-block tokens (Javadoc comments between imports and types, etc.)
    /// as a leading block so their text is preserved.
    private Block attachTopLevel(
            JavaBlock.Builder root,
            Block prev,
            int gapStart,
            int childStart,
            Block child,
            Spacing spacingAfterGap)
    {
        boolean hadGap = false;
        if (gapStart < childStart && containsNonWhitespace(gapStart, childStart)) {
            Block gapBlock = buildTokensRange(gapStart, childStart, "InterBlockTokens");
            if (prev != null) {
                addSibling(root, prev, gapBlock, Spacing.createSpacing(0, 0, 1, true, 1));
            }
            else {
                root.child(gapBlock);
            }
            prev = gapBlock;
            hadGap = true;
        }
        // If a comment block immediately precedes the child, decide based on
        // source shape: if source has a blank line between the gap block
        // (e.g. license header) and the child, preserve it; if they're
        // adjacent (javadoc documenting the child), force `\n` with no blank.
        Spacing effective;
        if (hadGap) {
            int keepBlankLines = containsBlankLine(gapStart, childStart) ? 1 : 0;
            effective = Spacing.createSpacing(0, 0, 1, true, keepBlankLines);
        }
        else {
            effective = spacingAfterGap;
        }
        if (prev != null) {
            addSibling(root, prev, child, effective);
        }
        else {
            root.child(child);
        }
        return child;
    }

    private boolean containsNonWhitespace(int start, int end)
    {
        int limit = Math.min(end, source.length());
        for (int i = max(0, start); i < limit; i++) {
            char c = source.charAt(i);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                return true;
            }
        }
        return false;
    }

    private int firstSignificantOffset(CompilationUnit unit)
    {
        int earliest = source.length();
        if (unit.getPackage() != null) {
            earliest = Math.min(earliest, unit.getPackage().getStartPosition());
        }
        for (Object imp : unit.imports()) {
            earliest = Math.min(earliest, ((ASTNode) imp).getStartPosition());
        }
        for (Object type : unit.types()) {
            earliest = Math.min(earliest, ((ASTNode) type).getStartPosition());
        }
        return earliest;
    }

    private int endOfImportDeclaration(ASTNode node)
    {
        int start = node.getStartPosition();
        int end = start + node.getLength();
        if (tokensIn(start, end).stream().anyMatch(tok -> tok.type() == ITerminalSymbols.TokenNameSEMICOLON)) {
            return end;
        }
        for (JavaTokens.Token tok : tokensIn(end, source.length())) {
            if (tok.type() == ITerminalSymbols.TokenNameSEMICOLON) {
                return tok.end();
            }
        }
        return end;
    }

    private Block buildImportDeclaration(ASTNode node, int end)
    {
        int start = node.getStartPosition();
        List<JavaTokens.Token> importTokens = tokensIn(start, end);
        if (!tokensCoverNonWhitespaceText(start, end, importTokens)) {
            return JavaBlock.leaf(start, end, source.substring(start, end));
        }
        return buildTokensRange(start, end, "import");
    }

    private boolean tokensCoverNonWhitespaceText(int start, int end, List<JavaTokens.Token> rangeTokens)
    {
        int coveredUntil = start;
        for (JavaTokens.Token tok : rangeTokens) {
            if (containsNonWhitespace(coveredUntil, tok.start())) {
                return false;
            }
            coveredUntil = tok.end();
        }
        return !containsNonWhitespace(coveredUntil, end);
    }

    Block buildTypeOrDeclaration(BodyDeclaration node)
    {
        return declarationBuilder.buildTypeOrDeclaration(node);
    }

    boolean containsLineBreak(int start, int end)
    {
        return sourceContext.containsLineBreak(start, end);
    }

    /// Returns the offset AFTER the first line break in [start, end), or -1.
    int findFirstLineBreak(int start, int end)
    {
        int limit = Math.min(end, source.length());
        for (int i = max(0, start); i < limit; i++) {
            if (source.charAt(i) == '\n') {
                return i + 1;
            }
        }
        return -1;
    }

    /// True when `source[start..end)` contains at least two consecutive
    /// `\n` characters (a blank line).
    boolean containsBlankLine(int start, int end)
    {
        int limit = Math.min(end, source.length());
        int newlines = 0;
        for (int i = max(0, start); i < limit; i++) {
            char c = source.charAt(i);
            if (c == '\n') {
                newlines++;
                if (newlines >= 2) {
                    return true;
                }
            }
            else if (c != ' ' && c != '\t' && c != '\r') {
                newlines = 0;
            }
        }
        return false;
    }

    /// True when `source[start..end)` contains only whitespace
    /// characters (no tokens, no comments, no other content).
    boolean sourceOnlyWhitespace(int start, int end)
    {
        int limit = Math.min(end, source.length());
        for (int i = max(0, start); i < limit; i++) {
            if (!Character.isWhitespace(source.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    int firstNonWhitespaceAtOrAfter(int start, int end)
    {
        int limit = Math.min(end, source.length());
        for (int i = max(0, start); i < limit; i++) {
            if (!Character.isWhitespace(source.charAt(i))) {
                return i;
            }
        }
        return limit;
    }

    int firstNonCommentTokenStart(int start, int end)
    {
        for (JavaTokens.Token tok : tokensIn(start, end)) {
            if (!tok.isComment()) {
                return tok.start();
            }
        }
        return end;
    }

    int findKeyword(int start, int end, String keyword)
    {
        return findKeyword(start, end, keyword, false);
    }

    int findKeyword(int start, int end, String keyword, boolean skipInsideAngles)
    {
        int angleDepth = 0;
        for (JavaTokens.Token tok : tokens) {
            if (tok.start() >= end) {
                break;
            }
            if (tok.start() < start) {
                continue;
            }
            if (skipInsideAngles) {
                String text = tok.text();
                switch (text) {
                    case "<" -> {
                        angleDepth++;
                        continue;
                    }
                    case ">" -> {
                        if (angleDepth > 0) {
                            angleDepth--;
                        }
                        continue;
                    }
                    case ">>" -> {
                        angleDepth = max(0, angleDepth - 2);
                        continue;
                    }
                    case ">>>" -> {
                        angleDepth = max(0, angleDepth - 3);
                        continue;
                    }
                }
                if (angleDepth > 0) {
                    continue;
                }
            }
            if (tok.text().equals(keyword)) {
                return tok.start();
            }
        }
        return -1;
    }

    /// If a type-parameter list `<...>` opens before a line break in
    /// `[start, end)`, return the position of the opening `<`. Otherwise
    /// return -1. Used in `buildTypeHeader` to distinguish legitimate
    /// type-param wraps (e.g. `class Foo<T,\\n E>`) from source-shape
    /// annotation-on-own-line (e.g. `@Ann\\npublic class Foo`).
    int findTypeParamOpenBeforeLineBreak(int start, int end)
    {
        int limit = Math.min(end, source.length());
        for (int i = max(0, start); i < limit; i++) {
            char c = source.charAt(i);
            if (c == '\n') {
                return -1;
            }
            if (c == '<' && genericOpens.contains(i)) {
                return i;
            }
        }
        return -1;
    }

    /// Expanded column width of a tab character when measuring source columns.
    private static final int TAB_WIDTH = 4;

    /// Column of a source offset (0-based: how many characters since the last \n).
    int columnOf(int offset)
    {
        if (offset <= 0) {
            return 0;
        }
        int col = 0;
        for (int i = offset - 1; i >= 0; i--) {
            if (source.charAt(i) == '\n') {
                break;
            }
            col++;
        }
        return col;
    }

    /// Returns the column of the first non-whitespace character on the line containing `offset`.
    int lineStartColumn(int offset)
    {
        int lineStart = offset;
        while (lineStart > 0 && source.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }
        int col = 0;
        for (int i = lineStart; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == ' ') {
                col++;
            }
            else if (c == '\t') {
                col += TAB_WIDTH;
            }
            else {
                return col;
            }
        }
        return col;
    }

    boolean startsLine(int offset)
    {
        int lineStart = offset;
        while (lineStart > 0 && source.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }
        for (int i = lineStart; i < offset; i++) {
            char c = source.charAt(i);
            if (c != ' ' && c != '\t') {
                return false;
            }
        }
        return true;
    }

    /// Find a trailing line comment on the same line as the token at
    /// `stmtEnd`. Returns null if no such comment exists.
    JavaTokens.Token findTrailingLineComment(int stmtEnd, int rangeEnd)
    {
        // The trailing comment must be on the same line as the end of the
        // statement (no newline between stmtEnd and the comment start).
        // Both line comments (`//`) and block comments (`/* */`) qualify.
        for (JavaTokens.Token tok : tokens) {
            if (tok.start() >= rangeEnd) {
                break;
            }
            if (tok.start() >= stmtEnd && tok.isComment()
                    && (tok.type() == ITerminalSymbols.TokenNameCOMMENT_LINE
                    || tok.type() == ITerminalSymbols.TokenNameCOMMENT_BLOCK)) {
                // Check no newline between stmtEnd and this comment.
                if (!containsLineBreak(stmtEnd, tok.start())) {
                    return tok;
                }
                break; // Past the line — no trailing comment.
            }
            if (tok.start() >= stmtEnd && !tok.isComment()) {
                break; // Non-comment token after statement — no trailing comment.
            }
        }
        return null;
    }

    /// Indent for a comment: NORMAL indent for most comments, but
    /// first-column comments (column 0 in source) stay at column 0
    /// (KEEP_FIRST_COLUMN_COMMENT behavior).
    Indent commentIndent(JavaTokens.Token tok, Indent defaultIndent)
    {
        // Markdown javadoc (`///`) is documentation: always indent to match
        // the enclosing context, never preserved at column 0.
        if (tok.type() == ITerminalSymbols.TokenNameCOMMENT_MARKDOWN) {
            return defaultIndent;
        }
        return (columnOf(tok.start()) == 0) ? Indent.absoluteNoneIndent() : defaultIndent;
    }

    /// Create a leaf block for a comment token. Line comments (`//...`) in
    /// JDT include the trailing `\\n` in their token text; strip it so
    /// the newline becomes part of the whitespace managed by the engine
    /// rather than being baked into the leaf text.
    static Block commentLeaf(JavaTokens.Token tok)
    {
        int commentEnd = tok.end();
        String text = tok.text();
        // JDT line comments and markdown javadoc (`///`) tokens include a
        // trailing `\n`; strip it so the engine's whitespace computation can
        // place the next sibling with the proper indent.
        boolean trailingNewlineComment =
                tok.type() == ITerminalSymbols.TokenNameCOMMENT_LINE
                        || tok.type() == ITerminalSymbols.TokenNameCOMMENT_MARKDOWN;
        if (trailingNewlineComment && text.endsWith("\n")) {
            commentEnd--;
            text = text.substring(0, text.length() - 1);
        }
        return JavaBlock.leaf(tok.start(), commentEnd, text);
    }

    /// Scan for comment tokens between two blocks and emit them as children
    /// of the composite. Returns the last emitted block (or `prev` if
    /// no comments were found).
    Block emitInterBlockComments(JavaBlock.Builder composite, Block prev, int gapStart, int gapEnd)
    {
        return emitInterBlockComments(composite, prev, gapStart, gapEnd, Indent.normalIndent());
    }

    Block emitInterBlockComments(JavaBlock.Builder composite, Block prev, int gapStart, int gapEnd, Indent defaultIndent)
    {
        for (JavaTokens.Token tok : tokensIn(gapStart, gapEnd)) {
            if (tok.isComment()) {
                int cEnd = (tok.text().endsWith("\n")) ? tok.end() - 1 : tok.end();
                Indent ind = commentIndent(tok, defaultIndent);
                Block commentBlock = JavaBlock.builder(tok.start(), cEnd, "InterBlockComment")
                        .indent(ind)
                        .child(commentLeaf(tok))
                        .build();
                // Force at least one line break before each comment leaf so the
                // engine's indent computation places it at the proper column;
                // markdown javadoc lines must each render on their own line.
                JavaBlockBuilder.addSibling(composite, prev, commentBlock, Spacing.createSpacing(0, 0, 1, true, 1));
                prev = commentBlock;
            }
        }
        return prev;
    }

    int firstCommentStart(int start, int end)
    {
        for (JavaTokens.Token tok : tokensIn(start, end)) {
            if (tok.isComment()) {
                return tok.start();
            }
        }
        return end;
    }

    /// Like [#emitInterBlockComments] but emits the FIRST comment inline
    /// with the previous sibling (preserving source shape like
    /// `case VALUE -> // inline`). Subsequent comments go on new lines.
    Block emitInterBlockCommentsArrowInline(JavaBlock.Builder composite, Block prev, int gapStart, int gapEnd)
    {
        boolean first = true;
        for (JavaTokens.Token tok : tokensIn(gapStart, gapEnd)) {
            if (tok.isComment()) {
                int cEnd = (tok.text().endsWith("\n")) ? tok.end() - 1 : tok.end();
                Indent ind = commentIndent(tok, Indent.normalIndent());
                Block commentBlock = JavaBlock.builder(tok.start(), cEnd, "InterBlockComment")
                        .indent(ind)
                        .child(commentLeaf(tok))
                        .build();
                Spacing sp;
                if (first && prev != null) {
                    // Preserve source column alignment: `}   // note` with 3
                    // spaces stays as 3, not collapsed to 1.
                    int sourceSpaces = max(1, tok.start() - prev.endOffset());
                    sp = Spacing.createSpacing(sourceSpaces, sourceSpaces, 0, false, 0);
                }
                else if (first) {
                    sp = Spacing.createSpacing(1, 1, 0, false, 0);
                }
                else {
                    sp = Spacing.createSpacing(0, 0, 1, true, 1);
                }
                JavaBlockBuilder.addSibling(composite, prev, commentBlock, sp);
                prev = commentBlock;
                first = false;
            }
        }
        return prev;
    }

    /// Walk back from `before` to the last non-whitespace character position + 1 (exclusive).
    int lastNonWhitespaceBefore(int before, int lowerBound)
    {
        for (int i = Math.min(before, source.length()) - 1; i >= lowerBound; i--) {
            char c = source.charAt(i);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                return i + 1;
            }
        }
        return lowerBound;
    }

    Block buildStatementBlock(org.eclipse.jdt.core.dom.Block block)
    {
        return statementBuilder.buildStatementBlock(block);
    }

    Block buildStatementBlock(org.eclipse.jdt.core.dom.Block block, boolean preserveEmptyBody)
    {
        return statementBuilder.buildStatementBlock(block, preserveEmptyBody);
    }

    Block buildStatementBlock(org.eclipse.jdt.core.dom.Block block, boolean preserveEmptyBody, int anchorColumn)
    {
        return statementBuilder.buildStatementBlock(block, preserveEmptyBody, anchorColumn);
    }

    Block buildStatement(ASTNode stmt)
    {
        return statementBuilder.buildStatement(stmt);
    }

    boolean needsStructuredCallArguments(List<?> arguments, ASTNode callNode)
    {
        return expressionBuilder.needsStructuredCallArguments(arguments, callNode);
    }

    /// Is this StringLiteral a text block (`"""..."""`)?
    private static boolean isTextBlockLiteral(StringLiteral sl)
    {
        String esc = sl.getEscapedValue();
        return esc != null && esc.startsWith("\"\"\"");
    }

    /// Is this argument a text block (TextBlock node or StringLiteral with `"""`)?
    static boolean isTextBlockArgument(Object arg)
    {
        if (arg instanceof TextBlock) {
            return true;
        }
        if (arg instanceof StringLiteral sl) {
            return isTextBlockLiteral(sl);
        }
        return false;
    }

    /// True when `expr` is a text block, or a chain (`MethodInvocation`
    /// / `FieldAccess`) whose leading-left receiver is a text block. Used
    /// to decide whether Airlift style should wrap the `"""` to its own
    /// line in places like `String x = """...""".trim()` or
    /// `readValue("""...""".formatted(x), …)`. Explicitly does NOT walk
    /// through `ParenthesizedExpression` — wrapping `(""")` must
    /// keep the `(` attached to the preceding operator and only the
    /// inner `"""` moves to its own line, which is handled elsewhere.
    static boolean leadingExpressionIsTextBlock(Object expr)
    {
        Object current = expr;
        while (true) {
            if (current instanceof TextBlock) {
                return true;
            }
            if (current instanceof StringLiteral sl && isTextBlockLiteral(sl)) {
                return true;
            }
            if (current instanceof MethodInvocation mi && mi.getExpression() != null) {
                current = mi.getExpression();
                continue;
            }
            if (current instanceof FieldAccess fa) {
                current = fa.getExpression();
                continue;
            }
            return false;
        }
    }

    Block buildTextBlockRhsBlock(int start, int end, int rhsStart, int rhsEnd, String debugName, Indent indent)
    {
        return expressionBuilder.buildTextBlockRhsBlock(start, end, rhsStart, rhsEnd, debugName, indent);
    }

    Block buildTextBlockRhsBlock(int start, int end, int rhsStart, int rhsEnd, String debugName, Indent indent, int prefixEnd)
    {
        return expressionBuilder.buildTextBlockRhsBlock(start, end, rhsStart, rhsEnd, debugName, indent, prefixEnd);
    }

    Block buildCallExpression(Expression callExpr, List<?> arguments, int exprStart, int exprEnd)
    {
        return expressionBuilder.buildCallExpression(callExpr, arguments, exprStart, exprEnd);
    }

    boolean hasWrappedAnnotationIn(int start, int end)
    {
        return expressionBuilder.hasWrappedAnnotationIn(start, end);
    }

    Block emitPrefixWithAnnotations(JavaBlock.Builder parent, int start, int end)
    {
        return expressionBuilder.emitPrefixWithAnnotations(parent, start, end);
    }

    Block buildAnnotationBlock(Annotation ann)
    {
        return expressionBuilder.buildAnnotationBlock(ann);
    }

    Block buildEnumConstantCall(EnumConstantDeclaration c, int cStart, int cEnd)
    {
        return expressionBuilder.buildEnumConstantCall(c, cStart, cEnd);
    }

    Block buildExpressionBlock(Expression expr, int start, int end, String debugName)
    {
        return expressionBuilder.buildExpressionBlock(expr, start, end, debugName);
    }

    Block buildArrayInitializer(ArrayInitializer node, int start, int end)
    {
        return expressionBuilder.buildArrayInitializer(node, start, end);
    }

    Block buildInfixExpression(InfixExpression infix, int start, int end)
    {
        return expressionBuilder.buildInfixExpression(infix, start, end);
    }

    Block buildInfixExpression(InfixExpression infix, int start, int end, Indent operandIndent)
    {
        return expressionBuilder.buildInfixExpression(infix, start, end, operandIndent);
    }

    int assignmentOperatorEnd(Assignment assignment)
    {
        return expressionBuilder.assignmentOperatorEnd(assignment);
    }

    Block buildIndentedBodyWrapper(String name, int wrapperStart, int bodyStart, int bodyEnd, Block body, Indent indent)
    {
        return expressionBuilder.buildIndentedBodyWrapper(name, wrapperStart, bodyStart, bodyEnd, body, indent);
    }

    boolean isInlineSingleThrowLambdaBody(org.eclipse.jdt.core.dom.Block blockBody)
    {
        return expressionBuilder.isInlineSingleThrowLambdaBody(blockBody);
    }

    int findLParen(List<?> arguments, int exprStart)
    {
        return expressionBuilder.findLParen(arguments, exprStart);
    }

    int findMatchingRParen(int lparenOffset, int maxEnd)
    {
        return expressionBuilder.findMatchingRParen(lparenOffset, maxEnd);
    }

    boolean isNontrivialChain(MethodInvocation mi)
    {
        return expressionBuilder.isNontrivialChain(mi);
    }

    Block buildChainStatement(ASTNode stmt, MethodInvocation outermost)
    {
        return expressionBuilder.buildChainStatement(stmt, outermost);
    }

    /// Find the `->` token preceding `bodyStart`, not earlier than
    /// `lowerBound`. Token-based so string literals and comments are
    /// skipped. Returns -1 if no arrow is found.
    int findArrowBefore(int bodyStart, int lowerBound)
    {
        int found = -1;
        for (JavaTokens.Token tok : tokens) {
            if (tok.start() < lowerBound) {
                continue;
            }
            if (tok.start() >= bodyStart) {
                break;
            }
            if (tok.type() == ITerminalSymbols.TokenNameARROW) {
                found = tok.start();
            }
        }
        return found;
    }

    int extendThroughTrailingInlineComments(int currentEnd, int boundary)
    {
        int extendedEnd = currentEnd;
        for (JavaTokens.Token tok : tokensIn(currentEnd, boundary)) {
            if (!tok.isComment()) {
                break;
            }
            if (containsLineBreak(currentEnd, tok.start())) {
                break;
            }
            extendedEnd = tok.end();
            currentEnd = tok.end();
        }
        return extendedEnd;
    }

    /// Find offsets of `;` tokens at depth 0 within `[start, end)`.
    /// "Depth 0" means not inside any `(`, `[`, `<`, `{` or their counterparts.
    int[] findTopLevelSemicolons(int start, int end)
    {
        ArrayList<Integer> result = new ArrayList<>();
        int depth = 0;
        for (JavaTokens.Token tok : tokens) {
            if (tok.end() <= start) {
                continue;
            }
            if (tok.start() >= end) {
                break;
            }
            if (tok.isComment()) {
                continue;
            }
            int type = tok.type();
            if (type == ITerminalSymbols.TokenNameLPAREN
                    || type == ITerminalSymbols.TokenNameLBRACKET
                    || type == ITerminalSymbols.TokenNameLBRACE) {
                depth++;
            }
            else if (type == ITerminalSymbols.TokenNameRPAREN
                    || type == ITerminalSymbols.TokenNameRBRACKET
                    || type == ITerminalSymbols.TokenNameRBRACE) {
                if (depth > 0) {
                    depth--;
                }
            }
            else if (type == ITerminalSymbols.TokenNameSEMICOLON && depth == 0) {
                result.add(tok.start());
            }
        }
        int[] out = new int[result.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = result.get(i);
        }
        return out;
    }

    Block buildSwitchExpression(SwitchExpression node, int start, int end)
    {
        return statementBuilder.buildSwitchExpression(node, start, end);
    }

    Block buildControlStatement(Statement statement)
    {
        return statementBuilder.buildControlStatement(statement);
    }

    /// Find the first `target` character inside any non-comment token in
    /// `[start, end)`. Delegates to the static token-based helper.
    int firstChar(int start, int end, char target)
    {
        return findBracket(tokens, start, end, target);
    }

    /// Reverse of [#firstChar] — find the last occurrence.
    int lastChar(int start, int end, char target)
    {
        return findLastBracket(tokens, start, end, target);
    }

    Block buildTokensSpanning(ASTNode node, String debugName)
    {
        return buildTokensRange(node.getStartPosition(), node.getStartPosition() + node.getLength(), debugName);
    }

    /// Build a composite that holds every token in `[start, end)` as its
    /// own leaf child, with [JavaSpacingRules] applied between each pair.
    Block buildTokensRange(int start, int end, String debugName)
    {
        return tokenRunBuilder.buildTokensRange(start, end, debugName);
    }

    Block buildTokensRange(int start, int end, String debugName, boolean canUseFirstChildIndent)
    {
        return tokenRunBuilder.buildTokensRange(start, end, debugName, canUseFirstChildIndent);
    }

    Block buildTokensRangePreservingTextBlockMargin(int start, int end, String debugName)
    {
        return tokenRunBuilder.buildTokensRangePreservingTextBlockMargin(start, end, debugName, true);
    }

    Block buildTokensRangePreservingFullTextBlockMargin(int start, int end, String debugName)
    {
        return tokenRunBuilder.buildTokensRangePreservingFullTextBlockMargin(start, end, debugName, true);
    }

    Block buildTokensRangePreservingNegativeTextBlockMargin(int start, int end, String debugName)
    {
        return tokenRunBuilder.buildTokensRangePreservingNegativeTextBlockMargin(start, end, debugName, true);
    }

    Block buildTokensRangeWithLineStartIndent(int start, int end, String debugName, Indent lineStartIndent)
    {
        return tokenRunBuilder.buildTokensRangeWithLineStartIndent(start, end, debugName, lineStartIndent);
    }

    Block buildLeafTokenRun(JavaSourceRange range, String debugName)
    {
        return tokenRunBuilder.buildLeafTokenRun(range, debugName);
    }

    Block buildLeafTokenRun(JavaSourceRange range, String debugName, boolean canUseFirstChildIndent)
    {
        return tokenRunBuilder.buildLeafTokenRun(range, debugName, canUseFirstChildIndent);
    }

    List<JavaTokens.Token> tokensIn(int start, int end)
    {
        return sourceContext.tokensIn(start, end);
    }

    boolean hasCommentIn(int start, int end)
    {
        for (JavaTokens.Token tok : tokens) {
            if (tok.start() >= end) {
                return false;
            }
            if (tok.start() >= start && tok.isComment()) {
                return true;
            }
        }
        return false;
    }

    /// Find the first `{` in `[start, end)` that appears outside
    /// balanced parens. Skips `{` inside annotation values and record
    /// component lists. Iterates tokens so comments can't masquerade.
    int findFirstOpenBrace(int start, int end)
    {
        int depth = 0;
        for (JavaTokens.Token tok : tokens) {
            if (tok.start() < start) {
                continue;
            }
            if (tok.start() >= end) {
                break;
            }
            if (tok.isComment()) {
                continue;
            }
            int type = tok.type();
            if (type == ITerminalSymbols.TokenNameLPAREN) {
                depth++;
            }
            else if (type == ITerminalSymbols.TokenNameRPAREN) {
                depth--;
            }
            else if (type == ITerminalSymbols.TokenNameLBRACE && depth == 0) {
                return tok.start();
            }
        }
        return -1;
    }

    int findLastCloseBrace(int start, int end)
    {
        return findLastBracket(tokens, start, end, '}');
    }

    Spacing spacingBeforeTrailingTokens(int start, int end)
    {
        for (JavaTokens.Token tok : tokensIn(start, end)) {
            if (tok.isComment()) {
                return JavaSpacingRules.keepLineOrSpace();
            }
            return Spacing.none();
        }
        return Spacing.none();
    }

    static void addSibling(JavaBlock.Builder parent, Block prev, Block child, Spacing spacing)
    {
        if (prev != null) {
            parent.spacing(prev, child, spacing);
        }
        parent.child(child);
    }

    /// Anonymous class declaration: `{ body }`. Decompose body declarations
    /// with NORMAL indent and blank-line member-kind separation, same as
    /// TypeDeclaration body.
    Block buildAnonymousClassDeclaration(AnonymousClassDeclaration node)
    {
        int start = node.getStartPosition();
        int end = start + node.getLength();
        JavaBlock.Builder composite = JavaBlock.builder(start, end, "AnonymousClass");
        int openBrace = firstChar(start, end, '{');
        int closeBrace = lastChar(start, end, '}');
        if (openBrace < 0 || closeBrace < 0) {
            return buildTokensRange(start, end, "AnonymousClass");
        }
        // Anchor the anonymous body to the column where the enclosing
        // declaration starts on its line — not necessarily `{`'s column,
        // since `{` may sit mid-line (e.g. enum constant `INITIALIZED {`).
        // Use the first non-whitespace column of the line containing `{`.
        // When the anon class is nested inside a wrapped method argument,
        // the surrounding CONTINUATION would otherwise cascade into body
        // members. Using absolute indent on `{` triggers
        // clearFirstChildIndentFlagChain in getChildOffset, breaking that
        // cascade. Body members get absolute indent too (at anchorColumn +
        // NORMAL_SIZE) so they're placed regardless of outer continuation
        // context.
        int anchorColumn = lineStartColumn(openBrace);
        Block openBraceBlock = buildTokensRange(openBrace, openBrace + 1, "{");
        composite.child(openBraceBlock);
        Block prev = openBraceBlock;

        if (node.bodyDeclarations().isEmpty() && !hasCommentIn(openBrace + 1, closeBrace)) {
            Block closeBraceBlock = buildTokensRange(closeBrace, closeBrace + 1, "}");
            addSibling(composite, prev, closeBraceBlock, Spacing.createSpacing(0, 0, 0, false, 0));
            return composite.build();
        }

        BodyDeclaration prevMember = null;
        int anonScanCursor = openBrace + 1;
        for (Object member : node.bodyDeclarations()) {
            BodyDeclaration memberDecl = (BodyDeclaration) member;
            Block prevBeforeComments = prev;
            prev = emitInterBlockComments(composite, prev, anonScanCursor, memberDecl.getStartPosition());
            boolean commentAttached = prev != prevBeforeComments
                    && !containsBlankLine(prev.endOffset(), memberDecl.getStartPosition());
            BuiltAnonymousMember builtMember = buildAnonymousMember(memberDecl, closeBrace, anchorColumn);
            boolean needBlankLine = prevMember != null
                    && !(prevMember instanceof FieldDeclaration && memberDecl instanceof FieldDeclaration);
            int minLines = (needBlankLine && !commentAttached) ? 2 : 1;
            addSibling(composite, prev, builtMember.block(), Spacing.createSpacing(0, 0, minLines, true, 1));
            prev = builtMember.block();
            prevMember = memberDecl;
            anonScanCursor = builtMember.endOffset();
        }

        prev = emitInterBlockComments(composite, prev, anonScanCursor, closeBrace);
        Block closeBraceBlock = JavaBlock.builder(closeBrace, closeBrace + 1, "AnonCloseBrace")
                .indent(Indent.absoluteSpaceIndent(anchorColumn))
                .child(buildTokensRange(closeBrace, closeBrace + 1, "}"))
                .build();
        addSibling(composite, prev, closeBraceBlock, Spacing.createSpacing(0, 0, 1, true, 0));
        return composite.build();
    }

    private BuiltAnonymousMember buildAnonymousMember(BodyDeclaration memberDecl, int bodyEnd, int anchorColumn)
    {
        Block memberBlock = buildTypeOrDeclaration(memberDecl);
        int memberEnd = memberDecl.getStartPosition() + memberDecl.getLength();
        Indent memberIndent = Indent.absoluteSpaceIndent(anchorColumn + Indent.NORMAL_SIZE);

        JavaTokens.Token trailingComment = findTrailingLineComment(memberEnd, bodyEnd);
        if (trailingComment != null) {
            int wrapperEnd = trailingComment.text().endsWith("\n") ? trailingComment.end() - 1 : trailingComment.end();
            JavaBlock.Builder wrapper = JavaBlock.builder(memberBlock.startOffset(), wrapperEnd, "AnonMemberWithTrailingComment")
                    .indent(memberIndent);
            wrapper.child(memberBlock);
            Block commentLeafBlock = commentLeaf(trailingComment);
            int sourceSpaces = max(1, trailingComment.start() - memberEnd);
            wrapper.spacing(memberBlock, commentLeafBlock, Spacing.createSpacing(sourceSpaces, sourceSpaces, 0, false, 0));
            wrapper.child(commentLeafBlock);
            return new BuiltAnonymousMember(wrapper.build(), wrapperEnd);
        }

        Block wrapped = JavaBlock.builder(memberBlock.startOffset(), memberBlock.endOffset(), "AnonMember")
                .indent(memberIndent)
                .child(memberBlock)
                .build();
        return new BuiltAnonymousMember(wrapped, memberEnd);
    }

    private record BuiltAnonymousMember(Block block, int endOffset) {}

    Spacing spacingBeforeAnonymousClass(AnonymousClassDeclaration node)
    {
        if (isEmptyAnonymousClassWithoutComments(node)) {
            return Spacing.createSpacing(1, 1, 0, false, 0);
        }
        return Spacing.createSpacing(0, 0, 1, false, 0);
    }

    boolean isEmptyAnonymousClassWithoutComments(AnonymousClassDeclaration node)
    {
        if (!node.bodyDeclarations().isEmpty()) {
            return false;
        }
        int start = node.getStartPosition();
        int end = start + node.getLength();
        int openBrace = firstChar(start, end, '{');
        int closeBrace = lastChar(start, end, '}');
        return openBrace >= 0
                && closeBrace >= 0
                && !hasCommentIn(openBrace + 1, closeBrace);
    }
}
