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
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.lang.Math.max;

final class JavaExpressionBuilder
{
    private final JavaBlockBuilder owner;
    private final JavaSourceContext sourceContext;
    private final JavaChainExpressionBuilder chainBuilder;
    private final String source;
    private final List<JavaTokens.Token> tokens;
    private final Set<Integer> switchGuardWhens;
    private final Map<Integer, Annotation> wrappedAnnotations;

    JavaExpressionBuilder(JavaBlockBuilder owner, JavaSourceContext sourceContext, Set<Integer> switchGuardWhens, Map<Integer, Annotation> wrappedAnnotations)
    {
        this.owner = owner;
        this.sourceContext = sourceContext;
        this.chainBuilder = new JavaChainExpressionBuilder(owner, sourceContext, this);
        this.source = sourceContext.source();
        this.tokens = sourceContext.tokens();
        this.switchGuardWhens = switchGuardWhens;
        this.wrappedAnnotations = wrappedAnnotations;
    }

    private boolean containsLineBreak(int start, int end)
    {
        return sourceContext.containsLineBreak(start, end);
    }

    private List<JavaTokens.Token> tokensIn(int start, int end)
    {
        return sourceContext.tokensIn(start, end);
    }

    private static void addSibling(JavaBlock.Builder parent, Block prev, Block child, Spacing spacing)
    {
        JavaBlockBuilder.addSibling(parent, prev, child, spacing);
    }

    private static boolean isTextBlockArgument(Object argument)
    {
        return JavaBlockBuilder.isTextBlockArgument(argument);
    }

    private static boolean leadingExpressionIsTextBlock(Object expression)
    {
        return JavaBlockBuilder.leadingExpressionIsTextBlock(expression);
    }

    private int extendThroughTrailingInlineComments(int currentEnd, int boundary)
    {
        return owner.extendThroughTrailingInlineComments(currentEnd, boundary);
    }

    private int findArrowBefore(int bodyStart, int lowerBound)
    {
        return owner.findArrowBefore(bodyStart, lowerBound);
    }

    private Spacing spacingBeforeTrailingTokens(int start, int end)
    {
        return owner.spacingBeforeTrailingTokens(start, end);
    }

    /// Does this call expression's argument list span multiple source lines?
    /// Only worth decomposing if the arguments are wrapped.
    private boolean hasWrappedArguments(List<?> arguments, ASTNode callNode)
    {
        if (arguments.isEmpty()) {
            return false;
        }
        ASTNode first = (ASTNode) arguments.getFirst();
        ASTNode last = (ASTNode) arguments.getLast();
        int firstStart = first.getStartPosition();
        int lastEnd = last.getStartPosition() + last.getLength();
        // Consider wrapping only within the argument list itself: from the
        // `(` preceding the first arg to the end of the last arg. Checking
        // from callNode.getStartPosition() would false-positive for chained
        // calls whose receiver chain wraps (e.g. `foo()\n.bar("x")` — the
        // `.bar("x")` MI's start includes `foo()` and the \n, but "x" is
        // inline within `.bar(`).
        int lparen = firstStart;
        while (lparen > callNode.getStartPosition() && source.charAt(lparen - 1) != '(') {
            lparen--;
        }
        return containsLineBreak(lparen, lastEnd);
    }

    boolean needsStructuredCallArguments(List<?> arguments, ASTNode callNode)
    {
        return hasWrappedArguments(arguments, callNode)
                || hasInlineBlockLambdaBodyToExpand(arguments);
    }

    private boolean hasInlineBlockLambdaBodyToExpand(List<?> arguments)
    {
        for (Object argument : arguments) {
            if (!(argument instanceof LambdaExpression lambda)) {
                continue;
            }
            if (!(lambda.getBody() instanceof org.eclipse.jdt.core.dom.Block blockBody)) {
                continue;
            }
            if (blockBody.statements().isEmpty() || isInlineSingleThrowLambdaBody(blockBody)) {
                continue;
            }
            int bodyStart = blockBody.getStartPosition();
            int bodyEnd = bodyStart + blockBody.getLength();
            if (!containsLineBreak(bodyStart, bodyEnd)) {
                return true;
            }
        }
        return false;
    }

    /// Is this StringLiteral a text block (`"""..."""`)?
    private static boolean isTextBlockLiteral(StringLiteral sl)
    {
        String esc = sl.getEscapedValue();
        return esc != null && esc.startsWith("\"\"\"");
    }

    Block buildTextBlockRhsBlock(int start, int end, int rhsStart, int rhsEnd, String debugName, Indent indent)
    {
        return buildTextBlockRhsBlock(start, end, rhsStart, rhsEnd, debugName, indent, rhsStart);
    }

    Block buildTextBlockRhsBlock(int start, int end, int rhsStart, int rhsEnd, String debugName, Indent indent, int prefixEnd)
    {
        JavaBlock.Builder statement = JavaBlock.builder(start, end, debugName);
        if (indent != null) {
            statement.indent(indent);
        }
        Block prev = null;
        if (rhsStart > start) {
            Block prefix = owner.buildTokensRange(start, prefixEnd, "TextBlockPrefix");
            statement.child(prefix);
            prev = prefix;
        }
        Block textBlock = JavaBlock.builder(rhsStart, rhsEnd, "TextBlockBody")
                .indent(Indent.continuationIndent())
                .child(owner.buildTokensRangePreservingTextBlockMargin(rhsStart, rhsEnd, "TextBlockTokens"))
                .build();
        if (prev != null) {
            // Force a line break before the text block (CONTINUATION indent
            // applies once it's on its own line).
            addSibling(statement, prev, textBlock, Spacing.createSpacing(0, 0, 1, false, 0));
        }
        else {
            statement.child(textBlock);
        }
        prev = textBlock;
        if (rhsEnd < end) {
            // If the trailing `;` is on its own line (after the closing `"""`),
            // wrap it in CONTINUATION so it lands at the same indent as the
            // text block.
            if (containsLineBreak(rhsEnd, end)) {
                Block trailing = owner.buildTokensRange(rhsEnd, end, "TextBlockTrailing", false);
                Block trailingWrapped = JavaBlock.continuationWrap(rhsEnd, end, trailing, "TextBlockTrailingWrap");
                addSibling(statement, prev, trailingWrapped, Spacing.none());
            }
            else {
                Block trailing = owner.buildTokensRange(rhsEnd, end, "TextBlockTrailing");
                addSibling(statement, prev, trailing, Spacing.none());
            }
        }
        return statement.build();
    }

    /// Decompose a call expression (MethodInvocation or ClassInstanceCreation)
    /// into: prefix-up-to-lparen + `(` + per-argument blocks with CONTINUATION
    /// indent and shared CHOP_IF_NEEDED wrap + `)`.
    ///
    /// The 16-column threshold heuristic: if the `(` position within the line
    /// is at or past the CONTINUATION indent from the statement start, all
    /// arguments wrap with `(` at end-of-line (first argument on next line).
    /// Otherwise, the first argument stays inline with `(`.
    Block buildCallExpression(Expression callExpr, List<?> arguments, int exprStart, int exprEnd)
    {
        JavaBlock.Builder call = JavaBlock.builder(exprStart, exprEnd, "CallExpression");

        // Find ( and ) positions.
        int lparen = findLParen(arguments, exprStart);
        int rparen = findMatchingRParen(lparen, exprEnd);
        if (lparen < 0 || rparen < 0) {
            call.child(owner.buildTokensRange(exprStart, exprEnd, "CallTokens"));
            return call.build();
        }

        // Prefix: everything up to (but NOT including) `(`. The `(`, args,
        // and `)` are wrapped in a synthetic inline "ArgList" composite that
        // starts at `(` on the same line as the method name. This mirrors
        // IntelliJ's JavaBlock(EXPRESSION_LIST) layer: because the synthetic
        // block's first leaf is `(` (never preceded by a line feed), the
        // "inline-sibling inherits wrapped sibling's CONTINUATION" gate in
        // AbstractBlockWrapper.getChildOffset opens reliably for deeply
        // nested arguments. If the prefix contains wrapped annotations
        // (e.g. `@McpTool(\n ...)`), decompose them so their value lists
        // carry CONTINUATION indent.
        Block prefixBlock;
        if (hasWrappedAnnotationIn(exprStart, lparen)) {
            JavaBlock.Builder pb = JavaBlock.builder(exprStart, lparen, "CallPrefix");
            emitPrefixWithAnnotations(pb, exprStart, lparen);
            prefixBlock = pb.build();
        }
        else {
            prefixBlock = owner.buildTokensRange(exprStart, lparen, "CallPrefix");
        }
        call.child(prefixBlock);

        // Synthetic inline arg-list composite: `(` ... args ... `)`.
        JavaBlock.Builder argList = JavaBlock.builder(lparen, rparen + 1, "ArgList");
        Block lparenBlock = owner.buildTokensRange(lparen, lparen + 1, "(");
        argList.child(lparenBlock);
        Block prev = lparenBlock;

        // Emit any leading comments inside the call (between `(` and the
        // first arg) that are on a SEPARATE line from the first argument.
        // Inline block comments (`(/* tag */ arg`) are absorbed into the
        // first argument's range below; line comments (`//`) always own
        // their line and are emitted here.
        if (!arguments.isEmpty()) {
            int firstArgStart = ((ASTNode) arguments.getFirst()).getStartPosition();
            for (JavaTokens.Token tok : tokensIn(lparen + 1, firstArgStart)) {
                if (!tok.isComment()) {
                    continue;
                }
                boolean isLineComment = tok.type() == ITerminalSymbols.TokenNameCOMMENT_LINE;
                boolean ownsLine = isLineComment || containsLineBreak(tok.end(), firstArgStart);
                if (ownsLine) {
                    int cEnd = (tok.text().endsWith("\n")) ? tok.end() - 1 : tok.end();
                    Block commentBlock = JavaBlock.builder(tok.start(), cEnd, "CallLeadingComment")
                            .indent(Indent.continuationIndent())
                            .child(JavaBlockBuilder.commentLeaf(tok))
                            .build();
                    addSibling(argList, prev, commentBlock, Spacing.createSpacing(0, 0, 1, true, 1));
                    prev = commentBlock;
                }
            }
        }

        // Detect whether source has arguments on SEPARATE lines (line break
        // between any two consecutive arguments). This triggers CHOP behavior:
        // if any arg has its own line, all args get their own lines.
        boolean sourceArgsOnMultipleLines = false;
        for (int i = 1; i < arguments.size(); i++) {
            ASTNode prevArg = (ASTNode) arguments.get(i - 1);
            ASTNode curArg = (ASTNode) arguments.get(i);
            int prevArgEnd = prevArg.getStartPosition() + prevArg.getLength();
            if (containsLineBreak(prevArgEnd, curArg.getStartPosition())) {
                sourceArgsOnMultipleLines = true;
                break;
            }
        }

        // Does source already have the first argument on its own line?
        boolean sourceFirstArgOnNewLine = !arguments.isEmpty()
                && containsLineBreak(lparen, ((ASTNode) arguments.getFirst()).getStartPosition());

        // Source shape is authoritative for the first argument's placement:
        //   - Source has `(` then `\n`  → first arg on new line (all args wrap).
        //   - Source has `(arg1` inline → first arg stays inline (even if
        //     later args are on separate lines, "compact parent" pattern).
        // EXCEPTION: text block args — including chains whose leading-left
        //   receiver is a text block, like `foo("""…""".formatted(x), …)` —
        //   always wrap onto their own line so structural text-block line
        //   leaves can align content at the wrapped arg indent.
        boolean firstArgOnNewLine = sourceFirstArgOnNewLine
                || (!arguments.isEmpty() && leadingExpressionIsTextBlock(arguments.getFirst()));

        // Track the END of the previous arg's block (after extension for
        // trailing comma/comment) so the next iteration's inter-arg scan
        // doesn't double-emit a comment that's already part of the previous arg.
        int prevArgBlockEnd = lparen + 1;
        for (int i = 0; i < arguments.size(); i++) {
            ASTNode arg = (ASTNode) arguments.get(i);
            int argStart = arg.getStartPosition();
            int argRangeStart = argStart;
            // Look at the gap between the previous arg block's end and this
            // arg's start. Comments in the gap that are on the SAME LINE as
            // this arg get absorbed into the arg's range (so `/* tag */ value`
            // and `// note\n value` stay attached). Comments on a separate
            // line above the arg are emitted as standalone inter-arg blocks.
            int searchStart = prevArgBlockEnd;
            for (JavaTokens.Token tok : tokensIn(searchStart, argStart)) {
                if (!tok.isComment()) {
                    continue;
                }
                // A comment is "same line as arg" only if it's a block
                // comment (`/* */`) AND no newline between its end and the
                // argument. Line comments (`//`) always end with `\n` and
                // therefore live on their own line.
                boolean isLineComment = tok.type() == ITerminalSymbols.TokenNameCOMMENT_LINE;
                boolean sameLineAsArg = !isLineComment
                        && tok.type() == ITerminalSymbols.TokenNameCOMMENT_BLOCK
                        && !containsLineBreak(tok.end(), argStart);
                if (sameLineAsArg) {
                    if (tok.start() < argRangeStart) {
                        argRangeStart = tok.start();
                    }
                }
                else if (i > 0) {
                    int cEnd = (tok.text().endsWith("\n")) ? tok.end() - 1 : tok.end();
                    Block commentBlock = JavaBlock.builder(tok.start(), cEnd, "InterArgComment")
                            .indent(Indent.continuationIndent())
                            .child(JavaBlockBuilder.commentLeaf(tok))
                            .build();
                    addSibling(argList, prev, commentBlock, Spacing.createSpacing(0, 0, 1, true, 1));
                    prev = commentBlock;
                }
            }
            int argEnd;
            if (i + 1 < arguments.size()) {
                // Include trailing comma in this argument's range — locate
                // the comma directly so leading comments in the gap before
                // the next arg aren't accidentally absorbed.
                argEnd = arg.getStartPosition() + arg.getLength();
                ASTNode nextArg = (ASTNode) arguments.get(i + 1);
                int nextArgStart = nextArg.getStartPosition();
                for (int j = argEnd; j < nextArgStart && j < source.length(); j++) {
                    if (source.charAt(j) == ',') {
                        argEnd = j + 1;
                        // Extend through any same-line trailing comment after
                        // the comma (`arg, // note\n nextArg`).
                        for (int k = argEnd; k < nextArgStart && k < source.length(); k++) {
                            char cc = source.charAt(k);
                            if (cc == '\n') {
                                break;
                            }
                            if (cc == '/' && k + 1 < source.length()
                                    && (source.charAt(k + 1) == '/' || source.charAt(k + 1) == '*')) {
                                for (JavaTokens.Token tok : tokensIn(k, nextArgStart)) {
                                    if (tok.start() == k && tok.isComment()) {
                                        argEnd = tok.text().endsWith("\n") ? tok.end() - 1 : tok.end();
                                        break;
                                    }
                                }
                                break;
                            }
                            if (cc != ' ' && cc != '\t') {
                                break;
                            }
                        }
                        break;
                    }
                }
            }
            else {
                argEnd = arg.getStartPosition() + arg.getLength();
            }
            argStart = argRangeStart;

            // First-arg indent strategy mirrors IntelliJ's AST-type-based
            // dispatch in AbstractJavaBlock.processParenthesisBlock:
            //   - MethodInvocation args: SmartIndent(CONTINUATION) — expandable,
            //     enforces CONTINUATION to descendants when the group wraps.
            //   - ClassInstanceCreation (new Foo(...)) args: CONTINUATION_WITHOUT_FIRST
            //     — NOT expandable, does NOT enforce. This prevents nested chains
            //     inside constructor args from accumulating double CONTINUATION.
            // When the first arg is inline and subsequent args wrap, we still
            // need to prime descendants with the arg-list CONTINUATION for
            // method calls, but NOT for constructors (matching IntelliJ).
            Indent argIndent;
            if (i == 0 && !firstArgOnNewLine) {
                boolean isConstructor = callExpr instanceof ClassInstanceCreation;
                argIndent = (sourceArgsOnMultipleLines && !isConstructor)
                        ? Indent.continuationEnforcedIndent()
                        : Indent.noneIndent();
            }
            else {
                argIndent = Indent.continuationIndent();
            }
            // Arguments can be Expression (call args) or SingleVariableDeclaration
            // (method params). Only Expression types get recursive decomposition.
            Object argObj = arguments.get(i);
            Block argTokens;
            if (argObj instanceof Expression expr) {
                argTokens = buildExpressionBlock(expr, argStart, argEnd, "Arg" + i);
            }
            else if (argObj instanceof MemberValuePair mvp) {
                // NormalAnnotation value: `name = value`. Decompose the value
                // expression so a wrapped ArrayInitializer gets CONTINUATION.
                Expression value = mvp.getValue();
                int valueStart = value.getStartPosition();
                JavaBlock.Builder mvpBlock = JavaBlock.builder(argStart, argEnd, "Arg" + i + "MVP");
                // Emit `name =` as flat tokens up to the value's start.
                Block namePart = owner.buildTokensRange(argStart, valueStart, "MVPPrefix");
                mvpBlock.child(namePart);
                Block valueBlock = buildExpressionBlock(value, valueStart, argEnd, "MVPValue");
                // When the value wraps (line break between `=` and value),
                // wrap it in CONTINUATION so the value lands at the right
                // column.
                if (containsLineBreak(argStart, valueStart)) {
                    valueBlock = JavaBlock.continuationWrap(valueStart, argEnd, valueBlock, "MVPValueCont");
                }
                addSibling(mvpBlock, namePart, valueBlock, Spacing.createSpacing(1, 1, 0, true, 0));
                argTokens = mvpBlock.build();
            }
            else if (argObj instanceof SingleVariableDeclaration
                    && hasWrappedAnnotationIn(argStart, argEnd)) {
                // Method parameter with a wrapped annotation (e.g.
                // `@OperatorDependency(\n  a = ...,\n  b = ...)` on a
                // method parameter). Decompose the annotation so its
                // wrapped values get CONTINUATION indent. Mirrors
                // AbstractJavaBlock.processParenthesisBlock dispatching
                // annotation parameter lists to AnnotationInitializerBlocksBuilder.
                JavaBlock.Builder svdBlock = JavaBlock.builder(argStart, argEnd, "Arg" + i + "Param");
                emitPrefixWithAnnotations(svdBlock, argStart, argEnd);
                argTokens = svdBlock.build();
            }
            else {
                argTokens = owner.buildTokensRange(argStart, argEnd, "Arg" + i);
            }
            // Source-shape-driven wrapping: per-arg spacing (below) carries
            // the minLineFeeds when source had this arg on its own line.
            // Airstyle never chops for line-length reasons.
            Block argBlock = JavaBlock.builder(argStart, argEnd, "ArgBlock" + i)
                    .indent(argIndent)
                    .child(argTokens)
                    .build();
            boolean textBlockArg = leadingExpressionIsTextBlock(argObj);
            // First argument placement: preserve source shape.
            // Subsequent args: one space or keep source line break.
            Spacing sp;
            if (i == 0 && firstArgOnNewLine) {
                sp = Spacing.createSpacing(0, 0, 1, false, 0);
            }
            else if (i == 0 && arguments.size() > 1) {
                sp = Spacing.createSpacing(0, 0, 0, false, 0);
            }
            else if (i == 0) {
                sp = Spacing.createSpacing(0, 0, 0, true, 0);
            }
            else if (sourceArgsOnMultipleLines) {
                // CHOP with pair-grouping: preserve source's per-line grouping.
                // keepBlankLines=1 so authors can group related arguments with
                // a blank line (common in long stat/constructor arg lists).
                ASTNode prevArg = (ASTNode) arguments.get(i - 1);
                int prevArgEnd = prevArg.getStartPosition() + prevArg.getLength();
                if (containsLineBreak(prevArgEnd, arg.getStartPosition())) {
                    sp = Spacing.createSpacing(0, 0, 1, true, 1);
                }
                else {
                    sp = Spacing.createSpacing(1, 1, 0, false, 0);
                }
            }
            else if (textBlockArg) {
                // IntelliJ builds text blocks as dedicated children inside
                // the parenthesized argument list. Keep the argument list
                // shape, but force the text block argument itself onto its
                // own continuation line so the host indent and text block
                // margin logic apply correctly.
                sp = Spacing.createSpacing(0, 0, 1, false, 0);
            }
            else {
                sp = Spacing.createSpacing(1, 1, 0, true, 0);
            }
            addSibling(argList, prev, argBlock, sp);
            prev = argBlock;
            prevArgBlockEnd = argEnd;
        }

        // Trailing comments between the last argument and `)`. Without this,
        // any block/line comment in the gap [lastArg.end, rparen) is silently
        // dropped: the `)` leaf's whitespace covers the gap but WhiteSpace
        // only counts line feeds / spaces, discarding non-whitespace content
        // when rendered.
        if (prevArgBlockEnd < rparen) {
            for (JavaTokens.Token tok : tokensIn(prevArgBlockEnd, rparen)) {
                if (!tok.isComment()) {
                    continue;
                }
                boolean isLineComment = tok.type() == ITerminalSymbols.TokenNameCOMMENT_LINE;
                int cEnd = (isLineComment && tok.text().endsWith("\n")) ? tok.end() - 1 : tok.end();
                // Preserve source shape: if the comment sits on the same line
                // as the preceding block, keep it inline; otherwise it owns
                // its own line at CONTINUATION indent.
                boolean ownsLine = containsLineBreak(prevArgBlockEnd, tok.start());
                Block commentBlock = JavaBlock.builder(tok.start(), cEnd, "TrailingArgComment")
                        .indent(Indent.continuationIndent())
                        .child(JavaBlockBuilder.commentLeaf(tok))
                        .build();
                Spacing sp = ownsLine
                        ? Spacing.createSpacing(0, 0, 1, true, 1)
                        : Spacing.createSpacing(1, 1, 0, true, 0);
                addSibling(argList, prev, commentBlock, sp);
                prev = commentBlock;
                prevArgBlockEnd = cEnd;
            }
        }

        // `)` token as last child of the synthetic arg-list composite.
        Block rparenBlock = owner.buildTokensRange(rparen, rparen + 1, ")");
        addSibling(argList, prev, rparenBlock, Spacing.createSpacing(0, 0, 0, false, 0));
        Block argListBlock = argList.build();
        // Attach the arg-list to the outer call expression. The spacing
        // between prefix (ending just before `(`) and the `(` must be zero
        // (no space between method name and `(`).
        addSibling(call, prefixBlock, argListBlock, Spacing.createSpacing(0, 0, 0, false, 0));

        // Any tokens between `)` and the end of the expression (e.g. for a
        // MethodInvocation that's the qualifier of another call — not common
        // in the top-level-expression path but safe to handle). Skip when the
        // range is pure whitespace: an empty-tokens buildTokensRange produces
        // a composite with no children, which InitialInfoBuilder treats as a
        // phantom leaf (Block.isLeaf() = subBlocks().isEmpty()) — that resets
        // pendingWhitespace past the gap and destroys the line-feed tracking
        // for a following chain selector after a wrapped-arg CIC head.
        if (rparen + 1 < exprEnd && !tokensIn(rparen + 1, exprEnd).isEmpty()) {
            Block postParen = owner.buildTokensRange(rparen + 1, exprEnd, "PostParen");
            addSibling(call, argListBlock, postParen, Spacing.none());
        }

        return call.build();
    }

    /// Are there any wrapped-annotation starts in the given source range?
    boolean hasWrappedAnnotationIn(int start, int end)
    {
        for (Integer off : wrappedAnnotations.keySet()) {
            if (off >= start && off < end) {
                return true;
            }
        }
        return false;
    }

    /// Emit the tokens in `[start, end)` as a sequence of blocks,
    /// decomposing any wrapped annotations found within the range. Returns
    /// the last block emitted (becomes `prev` in the caller).
    Block emitPrefixWithAnnotations(JavaBlock.Builder parent, int start, int end)
    {
        Block prev = null;
        int cursor = start;
        // Using buildTokensRange with canUseFirstChildIndent=false for the
        // "post-annotation" tokens so the walk for the first leaf after an
        // annotation (e.g. `void` / `interface` / `boolean`) continues up to
        // the parent composite (Member wrap) rather than short-circuiting on
        // HeaderPost's empty whitespaceBefore.indentSpaces.
        // Collect annotation starts in the range sorted.
        List<Integer> starts = new ArrayList<>();
        for (Integer off : wrappedAnnotations.keySet()) {
            if (off >= start && off < end) {
                starts.add(off);
            }
        }
        Collections.sort(starts);
        for (int aStart : starts) {
            Annotation ann = wrappedAnnotations.get(aStart);
            int aEnd = aStart + ann.getLength();
            // Must be fully inside the range.
            if (aEnd > end) {
                continue;
            }
            // Skip annotations nested inside an outer one that has already
            // been emitted (e.g. `@App(...)` inside `@Tool(app = @App(...))`).
            // The outer annotation's buildAnnotationBlock already emits the
            // inner tokens via buildCallExpression → MVP fallback.
            if (aStart < cursor) {
                continue;
            }
            // Emit any tokens before the annotation. Skip when the gap is
            // pure whitespace — a zero-token HeaderPre block would otherwise
            // emit phantom whitespaces during ApplyChangesState.apply and
            // duplicate the inter-annotation `\n`.
            if (cursor < aStart && !owner.sourceOnlyWhitespace(cursor, aStart)) {
                Block pre = owner.buildTokensRange(cursor, aStart, "HeaderPre", false);
                if (prev == null) {
                    parent.child(pre);
                }
                else {
                    addSibling(parent, prev, pre, Spacing.createSpacing(0, 0, 0, true, 0));
                }
                prev = pre;
            }
            Block annBlock = buildAnnotationBlock(ann);
            if (prev == null) {
                parent.child(annBlock);
            }
            else {
                // Annotations are on their own line by convention — force a
                // line break between the previous block and the annotation.
                addSibling(parent, prev, annBlock, Spacing.createSpacing(0, 0, 1, true, 0));
            }
            prev = annBlock;
            cursor = aEnd;
        }
        // Emit any tokens after the last annotation.
        if (cursor < end) {
            Block post = owner.buildTokensRange(cursor, end, "HeaderPost", false);
            if (prev == null) {
                parent.child(post);
            }
            else {
                addSibling(parent, prev, post, Spacing.createSpacing(0, 0, 1, true, 0));
            }
            prev = post;
        }
        return prev;
    }

    Block buildAnnotationBlock(Annotation ann)
    {
        int start = ann.getStartPosition();
        int end = start + ann.getLength();
        if (ann instanceof SingleMemberAnnotation sma) {
            Expression value = sma.getValue();
            if (value instanceof ArrayInitializer arrayInitializer
                    && containsLineBreak(value.getStartPosition(), value.getStartPosition() + value.getLength())) {
                return buildSingleMemberAnnotationArrayInitializer(arrayInitializer, start, end);
            }
            // Treat the single value as the call argument. ArrayInitializer
            // values with line breaks get decomposed via buildExpressionBlock.
            return buildCallExpression(null, List.of(value), start, end);
        }
        if (ann instanceof NormalAnnotation na && !na.values().isEmpty()) {
            return buildCallExpression(null, na.values(), start, end);
        }
        return owner.buildTokensRange(start, end, "Annotation");
    }

    private Block buildSingleMemberAnnotationArrayInitializer(
            ArrayInitializer arrayInitializer,
            int start,
            int end)
    {
        int arrayStart = arrayInitializer.getStartPosition();
        int arrayEnd = arrayStart + arrayInitializer.getLength();
        JavaBlock.Builder annotationBlock = JavaBlock.builder(start, end, "SingleMemberAnnotationArray");

        Block prefix = owner.buildTokensRange(start, arrayStart, "AnnotationArrayPrefix");
        annotationBlock.child(prefix);

        Block arrayBlock = buildArrayInitializer(arrayInitializer, arrayStart, arrayEnd);
        addSibling(annotationBlock, prefix, arrayBlock, Spacing.none());

        if (arrayEnd < end) {
            Block trailing = owner.buildTokensRange(arrayEnd, end, "AnnotationArrayTrailing");
            addSibling(annotationBlock, arrayBlock, trailing, Spacing.none());
        }
        return annotationBlock.build();
    }

    /// Build the block for an enum constant whose constructor arguments
    /// include a block lambda or multi-line expression. Structure:
    /// name-and-`(` prefix + per-argument blocks with CONTINUATION indent +
    /// `)` + trailing (comma/semicolon).
    Block buildEnumConstantCall(EnumConstantDeclaration c, int cStart, int cEnd)
    {
        List<?> arguments = c.arguments();
        int firstArgStart = ((ASTNode) arguments.getFirst()).getStartPosition();
        int lparen = -1;
        for (int i = firstArgStart - 1; i >= cStart; i--) {
            if (source.charAt(i) == '(') {
                lparen = i;
                break;
            }
        }
        if (lparen < 0) {
            return owner.buildTokensRange(cStart, cEnd, "EnumConstant");
        }
        int rparen = findMatchingRParen(lparen, cEnd);
        if (rparen < 0) {
            return owner.buildTokensRange(cStart, cEnd, "EnumConstant");
        }
        JavaBlock.Builder call = JavaBlock.builder(cStart, cEnd, "EnumConstantCall");
        Block prefix = owner.buildTokensRange(cStart, lparen + 1, "EnumConstPrefix");
        call.child(prefix);
        Block prev = prefix;

        boolean firstArgOnNewLine = containsLineBreak(lparen, firstArgStart);

        // Emit any comments between `(` and the first argument so they don't
        // fall into a gap and get dropped.
        for (JavaTokens.Token tok : tokensIn(lparen + 1, firstArgStart)) {
            if (!tok.isComment()) {
                continue;
            }
            int cmtEnd = tok.text().endsWith("\n") ? tok.end() - 1 : tok.end();
            Block commentBlock = JavaBlock.builder(tok.start(), cmtEnd, "EnumArgLeadingComment")
                    .indent(Indent.continuationIndent())
                    .child(JavaBlockBuilder.commentLeaf(tok))
                    .build();
            addSibling(call, prev, commentBlock, Spacing.createSpacing(0, 0, 1, true, 0));
            prev = commentBlock;
        }

        for (int i = 0; i < arguments.size(); i++) {
            ASTNode arg = (ASTNode) arguments.get(i);
            int argStart = arg.getStartPosition();
            int argEnd;
            if (i + 1 < arguments.size()) {
                ASTNode next = (ASTNode) arguments.get(i + 1);
                int commaEnd = owner.lastNonWhitespaceBefore(next.getStartPosition(), argStart);
                argEnd = commaEnd > 0 ? commaEnd + 1 : argStart + arg.getLength();
            }
            else {
                argEnd = extendThroughTrailingInlineComments(argStart + arg.getLength(), rparen);
            }
            Indent argIndent = (i == 0 && !firstArgOnNewLine)
                    ? Indent.noneIndent()
                    : Indent.continuationIndent();
            Block argTokens = (arg instanceof Expression expr)
                    ? buildExpressionBlock(expr, argStart, argEnd, "EnumArg" + i)
                    : owner.buildTokensRange(argStart, argEnd, "EnumArg" + i);
            Block argBlock = JavaBlock.builder(argStart, argEnd, "EnumArgBlock" + i)
                    .indent(argIndent)
                    .child(argTokens)
                    .build();
            Spacing sp;
            if (i == 0 && firstArgOnNewLine) {
                sp = Spacing.createSpacing(0, 0, 1, false, 0);
            }
            else if (i == 0) {
                sp = Spacing.createSpacing(0, 0, 0, false, 0);
            }
            else {
                sp = Spacing.createSpacing(1, 1, 0, true, 0);
            }
            addSibling(call, prev, argBlock, sp);
            prev = argBlock;
        }

        Block rparenBlock = owner.buildTokensRange(rparen, rparen + 1, ")");
        addSibling(call, prev, rparenBlock, Spacing.createSpacing(0, 0, 0, false, 0));
        if (rparen + 1 < cEnd) {
            Block trailing = owner.buildTokensRange(rparen + 1, cEnd, "EnumConstTrailing");
            addSibling(call, rparenBlock, trailing, Spacing.none());
        }
        return call.build();
    }

    /// Build a block for an arbitrary expression. If the expression is a
    /// nontrivial method chain, decompose it into chain selectors. If it's
    /// a call with wrapped arguments, decompose the argument list. Otherwise,
    /// flat-tokenize.
    ///
    /// The range `[start, end)` may extend beyond the expression's
    /// own range to include trailing commas or other separator tokens.
    Block buildExpressionBlock(Expression expr, int start, int end, String debugName)
    {
        // MethodInvocation whose receiver chain ends in a parenthesized
        // SwitchExpression (`(switch (x) { ... }).method(...)`): decompose
        // so the switch body cases get NORMAL indent. Without this, the
        // `.method(...)` is not a nontrivial chain (recv is ParenExpr not
        // MI), so the formatter flat-tokenizes everything and switch cases
        // keep their source column.
        if (expr instanceof MethodInvocation chainMi) {
            ParenthesizedExpression parenRecv = findParenSwitchReceiver(chainMi);
            if (parenRecv != null
                    && parenRecv.getExpression() instanceof SwitchExpression parenSwitch
                    && containsLineBreak(parenRecv.getStartPosition(), parenRecv.getStartPosition() + parenRecv.getLength())) {
                JavaBlock.Builder composite = JavaBlock.builder(start, end, "ParenSwitchChain");
                int switchStart = parenSwitch.getStartPosition();
                int switchEnd = switchStart + parenSwitch.getLength();
                // Prefix = everything up to and including `(` of the paren.
                Block prefix = owner.buildTokensRange(start, switchStart, "ParenSwitchPrefix");
                composite.child(prefix);
                Block prev = prefix;
                // Switch expression decomposed so cases get NORMAL indent.
                Block switchBlock = owner.buildSwitchExpression(parenSwitch, switchStart, switchEnd);
                addSibling(composite, prev, switchBlock, Spacing.none());
                prev = switchBlock;
                // Trailing = everything after the switch body's `}` through
                // the outer expression's end — includes the paren's `)` and
                // the chain's `.method(...)`.
                if (switchEnd < end) {
                    Block trailing = owner.buildTokensRange(switchEnd, end, "ParenSwitchTrailing");
                    addSibling(composite, prev, trailing, Spacing.none());
                }
                return composite.build();
            }
        }
        // Method chain: decompose selectors.
        if (expr instanceof MethodInvocation mi && isNontrivialChain(mi)) {
            return buildChainExpression(mi, start, end);
        }
        // Receiver wraps internally (e.g. `blah\n.x.y()`): wrap the tail in
        // CONTINUATION so it lands at receiver-line + 8.
        if (expr instanceof MethodInvocation mi && mi.getExpression() != null) {
            Expression recv = mi.getExpression();
            int recvStart = recv.getStartPosition();
            int recvEnd = recvStart + recv.getLength();
            if (containsLineBreak(recvStart, recvEnd)) {
                int wrapPoint = chainBuilder.firstWrappedSelectorStart(recvStart, recvEnd);
                if (wrapPoint > start) {
                    return buildReceiverWrappedExpression(mi, start, wrapPoint, end);
                }
            }
        }
        // Call with wrapped args: decompose argument list.
        if (expr instanceof MethodInvocation mi && !mi.arguments().isEmpty() && needsStructuredCallArguments(mi.arguments(), mi)) {
            return buildCallExpression(mi, mi.arguments(), start, end);
        }
        if (expr instanceof ClassInstanceCreation cic && !cic.arguments().isEmpty() && needsStructuredCallArguments(cic.arguments(), cic)) {
            // CIC with BOTH wrapped args and anonymous class body: decompose
            // the call up to `)`, then attach the anonymous body so its
            // members pick up NORMAL indent from the class-header anchor.
            if (cic.getAnonymousClassDeclaration() != null) {
                AnonymousClassDeclaration anon = cic.getAnonymousClassDeclaration();
                int anonStart = anon.getStartPosition();
                int anonEnd = anonStart + anon.getLength();
                Optional<JavaChainExpressionBuilder.ConstructorSelectorRange> selectorRange = chainBuilder.constructorSelectorRange(cic, start, anonStart);
                JavaBlock.Builder cicBlock = JavaBlock.builder(start, end, "NewWithAnonAndArgs");
                Block callPart = selectorRange
                        .map(range -> chainBuilder.buildConstructorSelectorChunks(range, "NewWithAnonSelector"))
                        .orElseGet(() -> buildCallExpression(cic, cic.arguments(), start, anonStart));
                cicBlock.child(callPart);
                Block anonBlock = owner.buildAnonymousClassDeclaration(anon);
                addSibling(cicBlock, callPart, anonBlock, owner.spacingBeforeAnonymousClass(anon));
                if (anonEnd < end) {
                    Block trailing = owner.buildTokensRange(anonEnd, end, "NewTrailing");
                    addSibling(cicBlock, anonBlock, trailing, Spacing.none());
                }
                return cicBlock.build();
            }
            Optional<JavaChainExpressionBuilder.ConstructorSelectorRange> selectorRange = chainBuilder.constructorSelectorRange(cic, start, end);
            if (selectorRange.isPresent()) {
                return chainBuilder.buildConstructorSelectorChunks(selectorRange.orElseThrow(), "NewWrappedSelector");
            }
            return buildCallExpression(cic, cic.arguments(), start, end);
        }
        // super.method(...) with wrapped args.
        if (expr instanceof SuperMethodInvocation smi
                && !smi.arguments().isEmpty() && needsStructuredCallArguments(smi.arguments(), smi)) {
            return buildCallExpression(null, smi.arguments(), start, end);
        }
        // Nested annotation (e.g. `@App(...)` as a member value). Decompose
        // via buildAnnotationBlock so wrapped args get CONTINUATION indent.
        if (expr instanceof Annotation ann && containsLineBreak(start, start + expr.getLength())) {
            return buildAnnotationBlock(ann);
        }
        // ClassInstanceCreation with anonymous class body.
        if (expr instanceof ClassInstanceCreation cic && cic.getAnonymousClassDeclaration() != null) {
            AnonymousClassDeclaration anon = cic.getAnonymousClassDeclaration();
            int anonStart = anon.getStartPosition();
            JavaBlock.Builder cicBlock = JavaBlock.builder(start, end, "NewWithAnon");
            Block prefix = owner.buildTokensRange(start, anonStart, "NewPrefix");
            cicBlock.child(prefix);
            Block anonBlock = owner.buildAnonymousClassDeclaration(anon);
            addSibling(cicBlock, prefix, anonBlock, owner.spacingBeforeAnonymousClass(anon));
            if (anon.getStartPosition() + anon.getLength() < end) {
                Block trailing = owner.buildTokensRange(anon.getStartPosition() + anon.getLength(), end, "NewTrailing");
                addSibling(cicBlock, anonBlock, trailing, Spacing.none());
            }
            return cicBlock.build();
        }
        // Qualified constructor type split across lines:
        //
        //     new Factory
        //             .Builder(...)
        //
        // IntelliJ does not treat the wrapped `.Builder` as a flat token
        // continuation. It is a selector-like child after the first chunk,
        // so it receives continuation indent like call-chain chunks.
        if (expr instanceof ClassInstanceCreation cic
                && containsLineBreak(start, start + expr.getLength())) {
            Optional<JavaChainExpressionBuilder.ConstructorSelectorRange> selectorRange = chainBuilder.constructorSelectorRange(cic, start, end);
            if (selectorRange.isPresent()) {
                return chainBuilder.buildConstructorSelectorChunks(selectorRange.orElseThrow(), "NewWrappedSelector");
            }
        }
        // Lambda with expression body: decompose `params -> expr` so the
        // body expression's chain/call structure is preserved.
        if (expr instanceof LambdaExpression lambda && lambda.getBody() instanceof Expression bodyExpr) {
            int bodyStart = bodyExpr.getStartPosition();
            int bodyEnd = bodyStart + bodyExpr.getLength();
            if (bodyExpr instanceof MethodInvocation bmi
                    && (isNontrivialChain(bmi) || leadingExpressionIsTextBlock(bmi))) {
                return buildLambdaExpressionWithBody(lambda, start, end, bodyStart, bodyEnd, bmi);
            }
            // Text block as lambda body: force `params ->` then text block on
            // own line at CONTINUATION.
            if (isTextBlockArgument(bodyExpr)) {
                JavaBlock.Builder lb = JavaBlock.builder(start, end, "LambdaTextBlockExpr");
                int headerEnd = owner.firstCommentStart(start, bodyStart);
                Block header = owner.buildTokensRange(start, headerEnd, "LambdaHeader");
                lb.child(header);
                Block tbBlock = JavaBlock.builder(bodyStart, end, "LambdaTextBlock")
                        .child(owner.buildTokensRangePreservingFullTextBlockMargin(bodyStart, end, "LambdaTextBlockTokens"))
                        .build();
                Block body = buildLambdaBodyWrapper(headerEnd, bodyStart, end, tbBlock);
                addSibling(lb, header, body, Spacing.createSpacing(0, 0, 1, false, 0));
                return lb.build();
            }
            // Multi-line InfixExpression/ConditionalExpression as lambda body:
            // wrap body at CONTINUATION so wrapped operands accumulate.
            if ((bodyExpr instanceof InfixExpression || bodyExpr instanceof ConditionalExpression)
                    && containsLineBreak(bodyStart, end)) {
                JavaBlock.Builder lb = JavaBlock.builder(start, end, "LambdaExprBody");
                int headerEnd = owner.firstCommentStart(start, bodyStart);
                Block header = owner.buildTokensRange(start, headerEnd, "LambdaHeader");
                lb.child(header);
                Block bodyBlock = buildExpressionBlock(bodyExpr, bodyStart, end, "LambdaBodyExpr");
                Block bodyWrapped = buildLambdaBodyWrapper(headerEnd, bodyStart, end, bodyBlock);
                addSibling(lb, header, bodyWrapped, JavaSpacingRules.keepLineOrSpace());
                return lb.build();
            }
            // Switch expression as lambda body: decompose so cases get NORMAL.
            // When the switch sits on its own line after `->` (source wrapped),
            // also wrap the body in a CONTINUATION block so the `switch` keyword
            // lands at lambda_line + 8 instead of the ancestor margin.
            if (bodyExpr instanceof SwitchExpression switchBody) {
                JavaBlock.Builder lb = JavaBlock.builder(start, end, "LambdaSwitchExpr");
                int headerEnd = owner.firstCommentStart(start, bodyStart);
                Block header = owner.buildTokensRange(start, headerEnd, "LambdaHeader");
                lb.child(header);
                int switchEnd = bodyStart + switchBody.getLength();
                Block switchBlock = owner.buildSwitchExpression(switchBody, bodyStart, switchEnd);
                Block attachedSwitch = containsLineBreak(start, bodyStart)
                        ? buildLambdaBodyWrapper(headerEnd, bodyStart, switchEnd, switchBlock)
                        : switchBlock;
                addSibling(lb, header, attachedSwitch, JavaSpacingRules.keepLineOrSpace());
                if (switchEnd < end) {
                    Block trailing = owner.buildTokensRange(switchEnd, end, "LambdaTrailing");
                    addSibling(lb, attachedSwitch, trailing, Spacing.none());
                }
                return lb.build();
            }
            // ClassInstanceCreation with anonymous class body: decompose so
            // the anonymous class body gets proper NORMAL-indent members.
            if (bodyExpr instanceof ClassInstanceCreation bcicAnon
                    && bcicAnon.getAnonymousClassDeclaration() != null) {
                JavaBlock.Builder lb = JavaBlock.builder(start, end, "LambdaAnonBody");
                int headerEnd = owner.firstCommentStart(start, bodyStart);
                Block header = owner.buildTokensRange(start, headerEnd, "LambdaHeader");
                lb.child(header);
                Block bodyBlock = buildExpressionBlock(bodyExpr, bodyStart, end, "LambdaBodyExpr");
                Block bodyWrapped = headerEnd < bodyStart
                        ? buildLambdaBodyWrapper(headerEnd, bodyStart, end, bodyBlock)
                        : bodyBlock;
                addSibling(lb, header, bodyWrapped, JavaSpacingRules.keepLineOrSpace());
                return lb.build();
            }
            // MethodInvocation / ClassInstanceCreation body with wrapped args:
            // wrap body at CONTINUATION so the call header aligns correctly
            // when `params ->` sits on its own line, and the wrapped args
            // render via buildCallExpression.
            if ((bodyExpr instanceof MethodInvocation bmi2
                    && !bmi2.arguments().isEmpty()
                    && needsStructuredCallArguments(bmi2.arguments(), bmi2))
                    || (bodyExpr instanceof ClassInstanceCreation bcic
                    && !bcic.arguments().isEmpty()
                    && needsStructuredCallArguments(bcic.arguments(), bcic))) {
                JavaBlock.Builder lb = JavaBlock.builder(start, end, "LambdaCallBody");
                int headerEnd = owner.firstCommentStart(start, bodyStart);
                Block header = owner.buildTokensRange(start, headerEnd, "LambdaHeader");
                lb.child(header);
                @SuppressWarnings("unchecked")
                List<Expression> args = (bodyExpr instanceof MethodInvocation)
                        ? ((MethodInvocation) bodyExpr).arguments()
                        : ((ClassInstanceCreation) bodyExpr).arguments();
                Block bodyBlock = buildCallExpression(bodyExpr, args, bodyStart, end);
                Block bodyWrapped = buildLambdaBodyWrapper(headerEnd, bodyStart, end, bodyBlock);
                addSibling(lb, header, bodyWrapped, JavaSpacingRules.keepLineOrSpace());
                return lb.build();
            }
            // Catch-all: any multi-line expression lambda body gets CONTINUATION
            // so the body expression on a new line lands at +8 from the lambda.
            // Also applies when the body is single-line but wraps to its own
            // line after `->`. When `->` itself wraps onto its own line
            // (`(params)\n -> body`), the arrow + body together carry
            // CONTINUATION — mirrors IntelliJ, which treats `->` as a
            // sibling of the params list and applies ContinuationIndent to
            // every child after the first.
            if (containsLineBreak(start, end)) {
                int arrowStart = findArrowBefore(bodyStart, start);
                boolean arrowOnNewLine = arrowStart > start
                        && containsLineBreak(start, arrowStart);
                JavaBlock.Builder lb = JavaBlock.builder(start, end, "LambdaExprBody");
                if (arrowOnNewLine) {
                    Block paramsPart = owner.buildTokensRange(start, arrowStart, "LambdaParams");
                    lb.child(paramsPart);
                    Block arrowAndBody = JavaBlock.builder(arrowStart, end, "LambdaArrowAndBody")
                            .indent(Indent.continuationIndent())
                            .child(owner.buildTokensRange(arrowStart, end, "LambdaArrowAndBodyTokens"))
                            .build();
                    addSibling(lb, paramsPart, arrowAndBody, JavaSpacingRules.keepLineOrSpace());
                }
                else {
                    int headerEnd = owner.firstCommentStart(start, bodyStart);
                    Block header = owner.buildTokensRange(start, headerEnd, "LambdaHeader");
                    lb.child(header);
                    Block bodyBlock = buildExpressionBlock(bodyExpr, bodyStart, end, "LambdaBodyExpr");
                    Block bodyWrapped = buildLambdaBodyWrapper(headerEnd, bodyStart, end, bodyBlock);
                    addSibling(lb, header, bodyWrapped, JavaSpacingRules.keepLineOrSpace());
                }
                return lb.build();
            }
        }
        // Lambda with block body (`() -> { ... }`): decompose the block so
        // statements inside get NORMAL indent.
        if (expr instanceof LambdaExpression lambda && lambda.getBody() instanceof org.eclipse.jdt.core.dom.Block blockBody) {
            return buildBlockLambdaExpression(blockBody, start, end);
        }
        // Multi-line InfixExpression: decompose operands so wrapped lines
        // carry CONTINUATION indent.
        if (expr instanceof InfixExpression infix && containsLineBreak(start, start + expr.getLength())) {
            return buildInfixExpression(infix, start, end);
        }
        // Multi-line ConditionalExpression (ternary): condition + ? then + : else.
        if (expr instanceof ConditionalExpression cond && containsLineBreak(start, start + expr.getLength())) {
            return buildConditionalExpression(cond, start, end);
        }
        // Single-argument MethodInvocation (not a chain, not wrapped args)
        // that spans multiple lines — route through buildExpressionBlock
        // for the inner expression.
        if (expr instanceof MethodInvocation mi && !isNontrivialChain(mi)
                && mi.arguments().size() == 1 && containsLineBreak(start, start + expr.getLength())) {
            Expression arg = (Expression) mi.arguments().getFirst();
            if (arg instanceof LambdaExpression || arg instanceof InfixExpression
                    || arg instanceof ConditionalExpression || arg instanceof ClassInstanceCreation) {
                return buildCallExpression(mi, mi.arguments(), start, end);
            }
        }
        // ArrayInitializer: decompose `{ elements }` with CONTINUATION indent.
        if (expr instanceof ArrayInitializer arrayInit
                && containsLineBreak(start, start + expr.getLength())) {
            return buildArrayInitializer(arrayInit, start, end);
        }
        // ArrayCreation with initializer: `new Type[] { ... }`.
        if (expr instanceof ArrayCreation arrayCreate
                && arrayCreate.getInitializer() != null
                && containsLineBreak(start, start + expr.getLength())) {
            return buildArrayInitializer(arrayCreate.getInitializer(), start, end);
        }
        // `new T[EXPR]` where either the dim or the wrapping `[`/`]` spans
        // multiple lines: wrap the dim expression (and the closing `]` when
        // it's on its own line) in CONTINUATION so wrapped content lands +8
        // from the statement.
        if (expr instanceof ArrayCreation arrayCreate
                && arrayCreate.getInitializer() == null
                && arrayCreate.dimensions().size() == 1
                && containsLineBreak(start, start + expr.getLength())) {
            Expression dim = (Expression) arrayCreate.dimensions().getFirst();
            int dimStart = dim.getStartPosition();
            int dimEnd = dimStart + dim.getLength();
            int lbracket = owner.firstChar(start, dimStart, '[');
            int rbracket = owner.firstChar(dimEnd, end, ']');
            boolean dimOnOwnLine = lbracket >= 0 && containsLineBreak(lbracket, dimStart);
            boolean dimItselfWraps = containsLineBreak(dimStart, dimEnd);
            if (lbracket >= 0 && rbracket >= 0 && !dimOnOwnLine && dimItselfWraps) {
                // Dim starts inline with `[` but its internal content (e.g.
                // multi-line InfixExpression operands) needs CONTINUATION.
                // Build prefix up to and including the inline opening, then
                // decompose the dim expression so operand wraps fire.
                JavaBlock.Builder composite = JavaBlock.builder(start, end, "ArrayDimCreateInline");
                Block prefix = owner.buildTokensRange(start, dimStart, "ArrayDimPrefix");
                composite.child(prefix);
                Block dimInner = buildExpressionBlock(dim, dimStart, dimEnd, "ArrayDimInner");
                Block dimWrap = JavaBlock.continuationWrap(dimStart, dimEnd, dimInner, "ArrayDimWrap");
                addSibling(composite, prefix, dimWrap, Spacing.none());
                if (dimEnd < end) {
                    Block tail = owner.buildTokensRange(dimEnd, end, "ArrayDimTail");
                    addSibling(composite, dimWrap, tail, Spacing.none());
                }
                return composite.build();
            }
            if (lbracket >= 0 && rbracket >= 0 && dimOnOwnLine) {
                // Fold any trailing inline comment on dim's last source line
                // into the dim region; otherwise the [dimEnd, rbracket) gap
                // would drop the comment (WhiteSpace.render emits only
                // whitespace).
                int dimRegionEnd = dimEnd;
                for (JavaTokens.Token tok : tokensIn(dimEnd, rbracket)) {
                    if (tok.isComment() && !containsLineBreak(dimEnd, tok.start())) {
                        dimRegionEnd = tok.end();
                    }
                    else if (containsLineBreak(dimEnd, tok.start())) {
                        break;
                    }
                }
                Block dimExpr = buildExpressionBlock(dim, dimStart, dimEnd, "ArrayDimInner");
                Block dimContent = dimExpr;
                if (dimRegionEnd > dimEnd) {
                    JavaBlock.Builder withComment = JavaBlock.builder(dimStart, dimRegionEnd, "ArrayDimWithComment")
                            .child(dimExpr);
                    Block comment = owner.buildTokensRange(dimEnd, dimRegionEnd, "ArrayDimTrailingComment");
                    addSibling(withComment, dimExpr, comment, Spacing.createSpacing(1, 1, 0, false, 0));
                    dimContent = withComment.build();
                }
                Block dimWrap = JavaBlock.builder(dimStart, dimRegionEnd, "ArrayDimWrap")
                        .indent(Indent.continuationIndent())
                        .child(dimContent)
                        .build();
                Block tailWrap = JavaBlock.builder(rbracket, end, "ArrayDimTailWrap")
                        .indent(Indent.continuationIndent())
                        .child(owner.buildTokensRange(rbracket, end, "ArrayDimTail"))
                        .build();
                JavaBlock.Builder composite = JavaBlock.builder(start, end, "ArrayDimCreate");
                Block prefix = owner.buildTokensRange(start, lbracket + 1, "ArrayDimPrefix");
                composite.child(prefix);
                Block lastBeforeDim = prefix;
                // Leading comments between `[` and the dim expression.
                for (JavaTokens.Token tok : tokensIn(lbracket + 1, dimStart)) {
                    if (!tok.isComment()) {
                        continue;
                    }
                    int cEnd = tok.text().endsWith("\n") ? tok.end() - 1 : tok.end();
                    Block commentBlock = JavaBlock.builder(tok.start(), cEnd, "ArrayDimLeadingComment")
                            .indent(Indent.continuationIndent())
                            .child(JavaBlockBuilder.commentLeaf(tok))
                            .build();
                    addSibling(composite, lastBeforeDim, commentBlock, Spacing.createSpacing(0, 0, 1, true, 0));
                    lastBeforeDim = commentBlock;
                }
                addSibling(composite, lastBeforeDim, dimWrap, Spacing.createSpacing(0, 0, 1, false, 0));
                Spacing tailSpacing = containsLineBreak(dimRegionEnd, rbracket)
                        ? Spacing.createSpacing(0, 0, 1, false, 0)
                        : Spacing.none();
                addSibling(composite, dimWrap, tailWrap, tailSpacing);
                return composite.build();
            }
        }
        // PrefixExpression: `!expr`, `-expr`, `+expr`, `~expr`, `++x`, `--x`.
        // The operator is adjacent to its operand (no space). If the operand
        // sits on its own line after a line break, wrap it in CONTINUATION.
        if (expr instanceof PrefixExpression prefix) {
            Expression operand = prefix.getOperand();
            int operandStart = operand.getStartPosition();
            boolean multiLine = containsLineBreak(start, start + expr.getLength());
            JavaBlock.Builder pb = JavaBlock.builder(start, end, "PrefixExpr");
            Block opPrefix = owner.buildTokensRange(start, operandStart, "PrefixOp");
            pb.child(opPrefix);
            Block operandBlock = buildExpressionBlock(operand, operandStart, end, "PrefixOperand");
            // Two cases for multi-line PrefixExpression:
            //   (a) line break BETWEEN operator and operand (`!\n (expr)`):
            //       wrap operand at CONTINUATION so it lands at +8.
            //   (b) operand spans multiple lines but is inline after the
            //       operator (`!verifySaltedHash(\n args)`): operator and
            //       operand stay adjacent; line breaks live inside the operand.
            boolean lineBreakBetweenOpAndOperand = containsLineBreak(start, operandStart);
            if (multiLine && lineBreakBetweenOpAndOperand) {
                Block operandWrapped = JavaBlock.builder(operandStart, end, "PrefixOperandWrap")
                        .indent(Indent.continuationIndent())
                        .child(operandBlock)
                        .build();
                addSibling(pb, opPrefix, operandWrapped, JavaSpacingRules.keepLineOrSpace());
            }
            else {
                addSibling(pb, opPrefix, operandBlock, JavaSpacingRules.noSpace());
            }
            return pb.build();
        }
        // CastExpression: recurse into the cast target expression.
        if (expr instanceof CastExpression cast) {
            Expression target = cast.getExpression();
            boolean targetIsTextBlock = isTextBlockArgument(target);
            if (containsLineBreak(start, start + expr.getLength()) || targetIsTextBlock) {
                int targetStart = target.getStartPosition();
                JavaBlock.Builder castBlock = JavaBlock.builder(start, end, "CastExpr");
                Block prefix = owner.buildTokensRange(start, targetStart, "CastPrefix");
                castBlock.child(prefix);
                // Text block target: force line break + CONTINUATION; other
                // multi-line targets preserve source shape.
                if (targetIsTextBlock) {
                    Block tbBlock = JavaBlock.builder(targetStart, end, "CastTextBlock")
                            .indent(Indent.continuationIndent())
                            .child(owner.buildTokensRange(targetStart, end, "CastTextBlockTokens"))
                            .build();
                    addSibling(castBlock, prefix, tbBlock, Spacing.createSpacing(0, 0, 1, false, 0));
                }
                else {
                    Block targetBlock = buildExpressionBlock(target, targetStart, end, "CastTarget");
                    // Wrap target in CONTINUATION so when the cast target sits
                    // on its own line (`(Type)\n    target`), it lands +8 from
                    // the cast prefix's line start.
                    Block targetWrapped = JavaBlock.builder(targetStart, end, "CastTargetWrap")
                            .indent(Indent.continuationIndent())
                            .child(targetBlock)
                            .build();
                    addSibling(castBlock, prefix, targetWrapped, JavaSpacingRules.keepLineOrSpace());
                }
                return castBlock.build();
            }
        }
        // SwitchExpression inside another expression.
        if (expr instanceof SwitchExpression switchExpr) {
            return owner.buildSwitchExpression(switchExpr, start, end);
        }
        // ExpressionMethodReference: `expr::method`. When the qualifier is
        // a complex multi-line expression (e.g. `((Type) () -> call(
        //     wrapped args))::run`), recurse into the qualifier so its
        // composite structure survives; `::method` is emitted as trailing
        // flat tokens. Without this, the whole method reference falls
        // through to buildTokensRange and the qualifier's nested wrap
        // indents are lost.
        // ExpressionMethodReference — `qualifier::method`. When the
        // qualifier is a multi-line chain, extend the chain's range past
        // `::method` so the chain's last selector absorbs the tail and the
        // chain selector indent model owns the whole expression. Otherwise
        // recurse into the qualifier for its nested structure and append
        // `::method` as a flat tail.
        if (expr instanceof ExpressionMethodReference emr
                && containsLineBreak(start, start + expr.getLength())) {
            Expression qualifier = emr.getExpression();
            int qualStart = qualifier.getStartPosition();
            int qualEnd = qualStart + qualifier.getLength();
            JavaBlock.Builder mrBlock = JavaBlock.builder(start, end, "MethodRefExpr");
            Block qualBlock = (qualStart == start
                    && qualifier instanceof MethodInvocation qualMi
                    && isNontrivialChain(qualMi))
                    ? buildChainExpression(qualMi, start, qualEnd)
                    : buildExpressionBlock(qualifier, qualStart, qualEnd, "MethodRefQualifier");
            Block pre = null;
            if (qualStart > start) {
                pre = owner.buildTokensRange(start, qualStart, "MethodRefPrefix");
                mrBlock.child(pre);
                addSibling(mrBlock, pre, qualBlock, JavaSpacingRules.noSpace());
            }
            else {
                mrBlock.child(qualBlock);
            }
            if (qualEnd < end) {
                int tailStart = owner.firstNonWhitespaceAtOrAfter(qualEnd, end);
                Block tail = owner.buildTokensRange(tailStart, end, "MethodRefTail");
                if (tailStart > qualEnd && containsLineBreak(qualEnd, tailStart)) {
                    tail = JavaBlock.builder(tailStart, end, "MethodRefTailWrap")
                            .indent(Indent.continuationIndent())
                            .canUseFirstChildIndent(false)
                            .child(tail)
                            .build();
                    addSibling(mrBlock, qualBlock, tail, JavaSpacingRules.keepLineOrSpace());
                }
                else {
                    addSibling(mrBlock, qualBlock, tail, JavaSpacingRules.noSpace());
                }
            }
            return mrBlock.build();
        }
        // Assignment: decompose the right-hand side.
        if (expr instanceof Assignment assign && containsLineBreak(start, start + expr.getLength())) {
            Expression rhs = assign.getRightHandSide();
            int rhsStart = rhs.getStartPosition();
            int rhsEnd = rhsStart + rhs.getLength();
            int operatorEnd = assignmentOperatorEnd(assign);
            if (leadingExpressionIsTextBlock(rhs)) {
                return buildTextBlockRhsBlock(start, end, rhsStart, rhsEnd, "AssignTextBlockExpr", null, operatorEnd >= 0 ? operatorEnd : rhsStart);
            }
            JavaBlock.Builder assignBlock = JavaBlock.builder(start, end, "AssignExpr");
            int prefixEnd = operatorEnd >= 0 ? owner.firstCommentStart(operatorEnd, rhsStart) : rhsStart;
            Block lhs = owner.buildTokensRange(start, prefixEnd, "AssignLHS");
            assignBlock.child(lhs);
            Block rhsBlock = buildExpressionBlock(rhs, rhsStart, max(rhsEnd, end), "AssignRHS");
            Block rhsWrapped = buildIndentedBodyWrapper(
                    "AssignRHSWrap",
                    prefixEnd,
                    rhsStart,
                    max(rhsEnd, end),
                    rhsBlock,
                    Indent.continuationIndent());
            addSibling(assignBlock, lhs, rhsWrapped, Spacing.createSpacing(1, 1, 0, true, 0));
            return assignBlock.build();
        }
        // ParenthesizedExpression: recurse into inner expression.
        if (expr instanceof ParenthesizedExpression paren) {
            Expression inner = paren.getExpression();
            // Text block inner: force line break + CONTINUATION between `(`
            // and the text block, then `)` after.
            if (isTextBlockArgument(inner)) {
                int innerStart = inner.getStartPosition();
                int innerEnd = innerStart + inner.getLength();
                JavaBlock.Builder pb = JavaBlock.builder(start, end, "ParenTextBlock");
                Block lparenBlock = owner.buildTokensRange(start, innerStart, "ParenLParen");
                pb.child(lparenBlock);
                Block tbBlock = JavaBlock.builder(innerStart, innerEnd, "ParenTextBlockBody")
                        .indent(Indent.continuationIndent())
                        .child(owner.buildTokensRange(innerStart, innerEnd, "ParenTextBlockTokens"))
                        .build();
                addSibling(pb, lparenBlock, tbBlock, Spacing.createSpacing(0, 0, 1, false, 0));
                if (innerEnd < end) {
                    Block rparenBlock = owner.buildTokensRange(innerEnd, end, "ParenRParen");
                    addSibling(pb, tbBlock, rparenBlock, Spacing.none());
                }
                return pb.build();
            }
            if (containsLineBreak(start, start + expr.getLength())) {
                // For multi-line InfixExpression/ConditionalExpression inside
                // parens, wrap inner block in CONTINUATION so wrapped operands
                // get the extra +8 from the parenthesized layer.
                if (inner instanceof InfixExpression || inner instanceof ConditionalExpression) {
                    int innerStart = inner.getStartPosition();
                    int innerEnd = innerStart + inner.getLength();
                    int actualEnd = max(end, innerEnd);
                    JavaBlock.Builder pb = JavaBlock.builder(start, actualEnd, "ParenInfix");
                    Block lparenBlock = owner.buildTokensRange(start, innerStart, "ParenLParen");
                    pb.child(lparenBlock);
                    Block innerBlock = buildExpressionBlock(inner, innerStart, innerEnd, "ParenInfixInner");
                    Block innerWrapped = JavaBlock.builder(innerStart, innerEnd, "ParenInfixWrap")
                            .indent(Indent.continuationIndent())
                            .child(innerBlock)
                            .build();
                    addSibling(pb, lparenBlock, innerWrapped, Spacing.none());
                    if (innerEnd < actualEnd) {
                        int openParenCol = owner.columnOf(start);
                        // Find the `)` token's exact position (might differ
                        // from innerEnd if there's whitespace between).
                        int rparenOffset = -1;
                        for (JavaTokens.Token tok : tokensIn(innerEnd, actualEnd)) {
                            if (tok.type() == ITerminalSymbols.TokenNameRPAREN) {
                                rparenOffset = tok.start();
                                break;
                            }
                        }
                        JavaBlock.Builder rb = JavaBlock.builder(innerEnd, actualEnd, "ParenRParen")
                                .canUseFirstChildIndent(false);
                        if (rparenOffset >= 0) {
                            Block rparenLeaf = JavaBlock.builder(rparenOffset, rparenOffset + 1, ")")
                                    .indent(Indent.absoluteSpaceIndent(openParenCol))
                                    .canUseFirstChildIndent(false)
                                    .child(JavaBlock.leaf(rparenOffset, rparenOffset + 1, ")"))
                                    .build();
                            rb.child(rparenLeaf);
                            if (rparenOffset + 1 < actualEnd) {
                                Block trailing = owner.buildTokensRange(rparenOffset + 1, actualEnd, "ParenRParenTrailing");
                                addSibling(rb, rparenLeaf, trailing, spacingBeforeTrailingTokens(rparenOffset + 1, actualEnd));
                            }
                        }
                        else {
                            rb.child(owner.buildTokensRange(innerEnd, actualEnd, "ParenRParenTokens"));
                        }
                        addSibling(pb, innerWrapped, rb.build(), Spacing.none());
                    }
                    return pb.build();
                }
                int innerStart = inner.getStartPosition();
                int innerEnd = innerStart + inner.getLength();
                JavaBlock.Builder pb = JavaBlock.builder(start, end, "ParenExpr");
                Block prefix = owner.buildTokensRange(start, innerStart, "ParenPrefix");
                pb.child(prefix);
                Block innerBlock = buildExpressionBlock(inner, innerStart, innerEnd, debugName);
                if (containsLineBreak(start, innerStart)) {
                    innerBlock = JavaBlock.builder(innerStart, innerEnd, "ParenExprInnerWrap")
                            .indent(Indent.continuationIndent())
                            .child(innerBlock)
                            .build();
                }
                Spacing innerSpacing = containsLineBreak(start, innerStart)
                        ? JavaSpacingRules.keepLineOrSpace()
                        : Spacing.none();
                addSibling(pb, prefix, innerBlock, innerSpacing);
                if (innerEnd < end) {
                    Block suffix = owner.buildTokensRange(innerEnd, end, "ParenSuffix");
                    addSibling(pb, innerBlock, suffix, Spacing.none());
                }
                return pb.build();
            }
        }
        // MethodInvocation with a multi-line ParenthesizedExpression receiver
        // — e.g. `((Type) chain).method(...)`. Recurse into the receiver so
        // any inner chain keeps its CONTINUATION selectors, then append the
        // `.methodName(args)` tail.
        if (expr instanceof MethodInvocation miParen
                && miParen.getExpression() instanceof ParenthesizedExpression recv
                && containsLineBreak(recv.getStartPosition(), recv.getStartPosition() + recv.getLength())) {
            int recvStart = recv.getStartPosition();
            int recvEnd = recvStart + recv.getLength();
            JavaBlock.Builder composite = JavaBlock.builder(start, end, "ParenRecvChain");
            Block prev = null;
            if (recvStart > start) {
                Block prefix = owner.buildTokensRange(start, recvStart, "ParenRecvPrefix");
                composite.child(prefix);
                prev = prefix;
            }
            Block recvBlock = buildExpressionBlock(recv, recvStart, recvEnd, "ParenRecv");
            if (prev != null) {
                addSibling(composite, prev, recvBlock, Spacing.none());
            }
            else {
                composite.child(recvBlock);
            }
            prev = recvBlock;
            if (recvEnd < end) {
                Block tail = owner.buildTokensRange(recvEnd, end, "ParenRecvTail");
                addSibling(composite, prev, tail, Spacing.none());
            }
            return composite.build();
        }
        return owner.buildTokensRange(start, end, debugName);
    }

    /// Array initializer: `{ elem1, elem2, ... }`. Each element line gets
    /// CONTINUATION indent. Source-shape is preserved (elements that share
    /// a line stay on the same line).
    Block buildArrayInitializer(ArrayInitializer node, int start, int end)
    {
        JavaBlock.Builder composite = JavaBlock.builder(start, end, "ArrayInit");
        int openBrace = owner.firstChar(start, end, '{');
        int closeBrace = owner.lastChar(start, end, '}');
        if (openBrace < 0 || closeBrace < 0) {
            return owner.buildTokensRange(start, end, "ArrayInit");
        }
        Block prev;
        // Emit prefix tokens (e.g. `new Object[][]` for ArrayCreation) before
        // the `{`. Without this, the prefix is dropped.
        if (start < openBrace) {
            Block prefix = owner.buildTokensRange(start, openBrace, "ArrayInitPrefix");
            composite.child(prefix);
            Block openBraceBlock = owner.buildTokensRange(openBrace, openBrace + 1, "{");
            addSibling(composite, prefix, openBraceBlock, Spacing.createSpacing(1, 1, 0, false, 0));
            prev = openBraceBlock;
        }
        else {
            Block openBraceBlock = owner.buildTokensRange(openBrace, openBrace + 1, "{");
            composite.child(openBraceBlock);
            prev = openBraceBlock;
        }

        // Emit leading comments between `{` and the first element. Without
        // this the comments fall into the `{`-to-first-element gap and
        // silently disappear when the renderer emits only whitespace.
        List<?> expressions = node.expressions();
        int leadingScanEnd = expressions.isEmpty() ? closeBrace : ((ASTNode) expressions.getFirst()).getStartPosition();
        for (JavaTokens.Token tok : tokensIn(openBrace + 1, leadingScanEnd)) {
            if (!tok.isComment()) {
                continue;
            }
            int cEnd = (tok.text().endsWith("\n")) ? tok.end() - 1 : tok.end();
            Block commentBlock = JavaBlock.builder(tok.start(), cEnd, "ArrayLeadingComment")
                    .indent(Indent.continuationIndent())
                    .child(JavaBlockBuilder.commentLeaf(tok))
                    .build();
            addSibling(composite, prev, commentBlock, Spacing.createSpacing(0, 0, 1, true, 0));
            prev = commentBlock;
        }

        // Each element at CONTINUATION indent.
        for (int i = 0; i < expressions.size(); i++) {
            ASTNode elem = (ASTNode) expressions.get(i);
            int elemStart = elem.getStartPosition();
            int elemEnd;
            if (i + 1 < expressions.size()) {
                elemEnd = ((ASTNode) expressions.get(i + 1)).getStartPosition();
                int commaEnd = owner.lastNonWhitespaceBefore(elemEnd, elemStart);
                if (commaEnd > 0) {
                    elemEnd = commaEnd + 1;
                }
            }
            else {
                // Last element: extend to include trailing comma AND any
                // same-line trailing comment (after the comma but before a
                // newline). Comments on subsequent lines are emitted
                // separately below as inter-element comments.
                elemEnd = elemStart + elem.getLength();
                // Scan forward from elemEnd: optionally consume a trailing
                // `,`, then a same-line trailing comment. Either may be
                // absent. Mirrors IntelliJ's approach of treating each
                // comment as a child of the array initializer — when the
                // source has the comment on the same line as the element,
                // keep it attached to the element block so the inline
                // layout is preserved; when on a subsequent line, leave it
                // for the trailing-comments loop below.
                for (int j = elemEnd; j < closeBrace && j < source.length(); j++) {
                    char c = source.charAt(j);
                    if (c == ',') {
                        elemEnd = j + 1;
                        continue;
                    }
                    if (c == '\n') {
                        break;
                    }
                    if (c == '/' && j + 1 < source.length() && (source.charAt(j + 1) == '/' || source.charAt(j + 1) == '*')) {
                        for (JavaTokens.Token tok : tokensIn(j, closeBrace)) {
                            if (tok.start() == j && tok.isComment()) {
                                elemEnd = tok.text().endsWith("\n") ? tok.end() - 1 : tok.end();
                                break;
                            }
                        }
                        break;
                    }
                    if (c != ' ' && c != '\t') {
                        break;
                    }
                }
            }
            Block elemBlock = JavaBlock.builder(elemStart, elemEnd, "ArrayElem" + i)
                    .indent(Indent.continuationIndent())
                    .child(buildExpressionBlock((Expression) expressions.get(i), elemStart, elemEnd, "Elem" + i))
                    .build();
            boolean mixedNestedBoundary = i > 0
                    && containsLineBreak(openBrace, closeBrace + 1)
                    && (((ASTNode) expressions.get(i - 1)) instanceof ArrayInitializer ^ elem instanceof ArrayInitializer);
            Spacing sp = (i == 0)
                    ? Spacing.createSpacing(0, 0, 0, true, 0)
                    : mixedNestedBoundary
                      ? Spacing.createSpacing(0, 0, 1, true, 0)
                      : Spacing.createSpacing(1, 1, 0, true, 0);
            addSibling(composite, prev, elemBlock, sp);
            prev = elemBlock;
        }

        // Emit any trailing comments between the last element and `}` so they
        // get CONTINUATION indent rather than collapsing to source whitespace.
        // Start from the END of the LAST element block (which already includes
        // any same-line trailing comma + comment from the per-element loop
        // above). Comments BEYOND that — on subsequent lines — are emitted
        // here as separate inter-element comment blocks.
        int trailingScanStart = prev.endOffset();
        for (JavaTokens.Token tok : tokensIn(trailingScanStart, closeBrace)) {
            if (tok.isComment()) {
                int cEnd = (tok.text().endsWith("\n")) ? tok.end() - 1 : tok.end();
                Block commentBlock = JavaBlock.builder(tok.start(), cEnd, "ArrayTrailingComment")
                        .indent(Indent.continuationIndent())
                        .child(JavaBlockBuilder.commentLeaf(tok))
                        .build();
                addSibling(composite, prev, commentBlock, Spacing.createSpacing(0, 0, 1, true, 0));
                prev = commentBlock;
            }
        }
        Block closeBraceBlock = owner.buildTokensRange(closeBrace, closeBrace + 1, "}");
        addSibling(composite, prev, closeBraceBlock, Spacing.createSpacing(0, 0, 0, true, 0));
        // Trailing tokens after `}`.
        if (closeBrace + 1 < end) {
            Block trailing = owner.buildTokensRange(closeBrace + 1, end, "ArrayTrailing");
            addSibling(composite, closeBraceBlock, trailing, Spacing.none());
        }
        return composite.build();
    }

    /// Find the operator token between the end of one operand and the start
    /// of the next. The operator is the LAST non-comment, non-whitespace
    /// code token in this gap.
    private int findOperatorBeforeOperand(int gapStart, int gapEnd)
    {
        int operatorStart = gapStart;
        for (JavaTokens.Token tok : tokensIn(gapStart, gapEnd)) {
            if (!tok.isComment()) {
                operatorStart = tok.start();
            }
        }
        return operatorStart;
    }

    private int lastNonCommentTokenEnd(int start, int end)
    {
        int tokenEnd = start;
        for (JavaTokens.Token tok : tokensIn(start, end)) {
            if (!tok.isComment()) {
                tokenEnd = tok.end();
            }
        }
        return tokenEnd;
    }

    Block buildInfixExpression(InfixExpression infix, int start, int end)
    {
        return buildInfixExpression(infix, start, end, Indent.continuationIndent());
    }

    Block buildInfixExpression(InfixExpression infix, int start, int end, Indent operandIndent)
    {
        JavaBlock.Builder composite = JavaBlock.builder(start, end, "InfixExpr");
        boolean lambdaBodyInsideConditionalBranch = isLambdaBodyInsideConditionalBranch(infix);

        // Collect all operand ranges: left, right, extended.
        List<int[]> operandRanges = new ArrayList<>();
        Expression left = infix.getLeftOperand();
        operandRanges.add(new int[] {left.getStartPosition(), left.getStartPosition() + left.getLength()});
        Expression right = infix.getRightOperand();
        operandRanges.add(new int[] {right.getStartPosition(), right.getStartPosition() + right.getLength()});
        for (Object ext : infix.extendedOperands()) {
            Expression e = (Expression) ext;
            operandRanges.add(new int[] {e.getStartPosition(), e.getStartPosition() + e.getLength()});
        }

        Block prev = null;
        int prevSegEnd = start;
        for (int i = 0; i < operandRanges.size(); i++) {
            int[] range = operandRanges.get(i);
            // Each segment: from the operator token (`+`, `&&`, etc.) of this
            // operand to the END of this operand. Segment 0 starts at the
            // outer expression start. For i>0, find the operator position so
            // any trailing comment from the previous operand stays in the
            // PREVIOUS segment — this lets segment i start on a new line.
            int segStart;
            if (i == 0) {
                segStart = start;
            }
            else {
                int prevEnd = operandRanges.get(i - 1)[1];
                segStart = findOperatorBeforeOperand(prevEnd, range[0]);
                // Between the previous segment's end and the operator, emit
                // any standalone-line comments as their own children. Without
                // this the comment token sits in a gap between leaf blocks
                // and ApplyChangesState silently drops it (the whitespace
                // renderer only emits line feeds + indent spaces).
                for (JavaTokens.Token tok : tokensIn(prevSegEnd, segStart)) {
                    if (!tok.isComment()) {
                        continue;
                    }
                    int cEnd = tok.text().endsWith("\n") ? tok.end() - 1 : tok.end();
                    Block commentBlock = JavaBlock.builder(tok.start(), cEnd, "InfixComment")
                            .indent(operandIndent)
                            .child(JavaBlockBuilder.commentLeaf(tok))
                            .build();
                    if (prev != null) {
                        addSibling(composite, prev, commentBlock, Spacing.createSpacing(0, 0, 1, true, 1));
                    }
                    else {
                        composite.child(commentBlock);
                    }
                    prev = commentBlock;
                }
            }
            int segEnd = range[1];
            // Extend segEnd to include any same-line trailing comment after
            // this operand. The comment belongs to this operand visually,
            // and excluding it from the segment makes the next segment's
            // operator appear inline with the previous operand instead of
            // starting on a new line.
            if (i < operandRanges.size() - 1) {
                int nextOpStart = findOperatorBeforeOperand(segEnd, operandRanges.get(i + 1)[0]);
                int extended = segEnd;
                for (int j = segEnd; j < nextOpStart && j < source.length(); j++) {
                    char c = source.charAt(j);
                    if (c == '\n') {
                        break;
                    }
                    if (c == '/' && j + 1 < source.length() && (source.charAt(j + 1) == '/' || source.charAt(j + 1) == '*')) {
                        for (JavaTokens.Token tok : tokensIn(j, nextOpStart)) {
                            if (tok.start() == j && tok.isComment()) {
                                extended = tok.text().endsWith("\n") ? tok.end() - 1 : tok.end();
                                break;
                            }
                        }
                        break;
                    }
                    if (c != ' ' && c != '\t') {
                        break;
                    }
                }
                segEnd = extended;
            }
            // Last operand: extend to the full range end to capture trailing tokens.
            if (i == operandRanges.size() - 1 && segEnd < end) {
                segEnd = end;
            }

            Block opBlock;
            if (i == 0) {
                // First operand: no indent, may include prefix tokens.
                opBlock = buildExpressionBlock(left, segStart, segEnd, "InfixOp0");
            }
            else {
                // Subsequent operands: the segment wraps `[op + operand]`. The
                // OPERATOR carries CONTINUATION (so it's at +8 if it ends up
                // on a new line) and the operand also carries CONTINUATION
                // (so it's at +8 when it starts on its own line, e.g.
                // `a &&\n  b`). With both at CONTINUATION, the engine's
                // RELATIVE_INDENT_TYPES handling treats them consistently
                // whether the operator is end-of-line or start-of-line.
                Expression operand = (i == 1) ? right : (Expression) infix.extendedOperands().get(i - 2);
                // Text block operands always land on their own line so
                // structural text-block line leaves can align content.
                boolean operandIsTextBlock = isTextBlockArgument(operand);
                Indent contentIndent = (i == 1 && lambdaBodyInsideConditionalBranch)
                        ? Indent.absoluteSpaceIndent(owner.columnOf(left.getStartPosition()))
                        : operandIndent;
                Block content = buildExpressionBlock(operand, range[0], segEnd, "InfixOpExpr" + i);
                Block contentWrapped = JavaBlock.builder(range[0], segEnd, "InfixOperand" + i)
                        .indent(contentIndent)
                        .child(content)
                        .build();
                List<JavaTokens.Token> prefixComments = tokensIn(segStart, range[0]).stream()
                        .filter(JavaTokens.Token::isComment)
                        .toList();
                int opTokensEnd = prefixComments.isEmpty() ? range[0] : prefixComments.getFirst().start();
                Block opTokens = owner.buildTokensRange(segStart, opTokensEnd, "InfixOpToken" + i);
                Block opTokensWrapped = JavaBlock.builder(segStart, opTokensEnd, "InfixOpTokenWrap" + i)
                        .indent(operandIndent)
                        .child(opTokens)
                        .build();
                JavaBlock.Builder seg = JavaBlock.builder(segStart, segEnd, "InfixSeg" + i);
                seg.child(opTokensWrapped);
                Spacing opToOperand = operandIsTextBlock
                        ? Spacing.createSpacing(0, 0, 1, false, 0)
                        : Spacing.createSpacing(1, 1, 0, true, 0);
                Block prefixPrev = opTokensWrapped;
                for (JavaTokens.Token tok : prefixComments) {
                    int cEnd = tok.text().endsWith("\n") ? tok.end() - 1 : tok.end();
                    int previousEnd = prefixPrev == opTokensWrapped
                            ? lastNonCommentTokenEnd(segStart, tok.start())
                            : prefixPrev.endOffset();
                    boolean inlineWithPrev = !containsLineBreak(previousEnd, tok.start());
                    Block commentBlock = JavaBlock.builder(tok.start(), cEnd, "InfixPrefixComment")
                            .indent(inlineWithPrev ? Indent.noneIndent() : owner.commentIndent(tok, contentIndent))
                            .canUseFirstChildIndent(false)
                            .child(JavaBlockBuilder.commentLeaf(tok))
                            .build();
                    Spacing commentSpacing = inlineWithPrev
                            ? Spacing.createSpacing(max(1, tok.start() - previousEnd), max(1, tok.start() - previousEnd), 0, false, 0)
                            : Spacing.createSpacing(0, 0, 1, true, 1);
                    addSibling(seg, prefixPrev, commentBlock, commentSpacing);
                    prefixPrev = commentBlock;
                }
                Spacing prefixToContent = prefixComments.isEmpty()
                        ? opToOperand
                        : spacingAfterInfixPrefixComments(prefixComments.getLast(), range[0]);
                addSibling(seg, prefixPrev, contentWrapped, prefixToContent);
                opBlock = seg.build();
            }

            if (prev != null) {
                addSibling(composite, prev, opBlock, JavaSpacingRules.keepLineOrSpace());
            }
            else {
                composite.child(opBlock);
            }
            prev = opBlock;
            prevSegEnd = segEnd;
        }
        return composite.build();
    }

    private boolean isBlockComment(JavaTokens.Token tok)
    {
        return tok.type() == ITerminalSymbols.TokenNameCOMMENT_BLOCK
                || tok.type() == ITerminalSymbols.TokenNameCOMMENT_JAVADOC;
    }

    private Spacing spacingAfterInfixPrefixComments(JavaTokens.Token comment, int operandStart)
    {
        if (isBlockComment(comment) && !containsLineBreak(comment.end(), operandStart)) {
            return Spacing.oneSpace();
        }
        return Spacing.createSpacing(0, 0, 1, true, 1);
    }

    private static boolean isLambdaBodyInsideConditionalBranch(InfixExpression infix)
    {
        ASTNode parent = infix.getParent();
        return parent instanceof LambdaExpression lambda
                && lambda.getBody() == infix
                && isMethodCallInsideConditionalBranch(lambda);
    }

    private boolean ternaryBranchExpressionStartsAfterOperatorLineBreak(ASTNode node)
    {
        ASTNode child = node;
        ASTNode parent = child.getParent();
        while (parent != null) {
            if (parent instanceof ConditionalExpression cond) {
                if (cond.getThenExpression() == child) {
                    Expression condition = cond.getExpression();
                    int conditionEnd = condition.getStartPosition() + condition.getLength();
                    int question = findTokenBetween(conditionEnd, child.getStartPosition(), ITerminalSymbols.TokenNameQUESTION);
                    return question >= 0 && containsLineBreak(question + 1, child.getStartPosition());
                }
                if (cond.getElseExpression() == child) {
                    Expression thenExpression = cond.getThenExpression();
                    int thenEnd = thenExpression.getStartPosition() + thenExpression.getLength();
                    int colon = findTokenBetween(thenEnd, child.getStartPosition(), ITerminalSymbols.TokenNameCOLON);
                    return colon >= 0 && containsLineBreak(colon + 1, child.getStartPosition());
                }
                return false;
            }
            if (parent instanceof MethodDeclaration
                    || parent instanceof FieldDeclaration
                    || parent instanceof Initializer) {
                return false;
            }
            child = parent;
            parent = parent.getParent();
        }
        return false;
    }

    private int findTokenBetween(int start, int end, int tokenType)
    {
        for (JavaTokens.Token tok : tokensIn(start, end)) {
            if (tok.type() == tokenType) {
                return tok.start();
            }
        }
        return -1;
    }

    int assignmentOperatorEnd(Assignment assignment)
    {
        int lhsEnd = assignment.getLeftHandSide().getStartPosition() + assignment.getLeftHandSide().getLength();
        int rhsStart = assignment.getRightHandSide().getStartPosition();
        String operator = assignment.getOperator().toString();
        for (JavaTokens.Token tok : tokensIn(lhsEnd, rhsStart)) {
            if (tok.text().equals(operator)) {
                return tok.end();
            }
        }
        return -1;
    }

    /// Decompose a multi-line ConditionalExpression (ternary): condition +
    /// `? then` clause + `: else` clause. The `?` and `:` clauses carry
    /// CONTINUATION indent.
    private Block buildConditionalExpression(ConditionalExpression cond, int start, int end)
    {
        JavaBlock.Builder composite = JavaBlock.builder(start, end, "TernaryExpr");
        Expression condition = cond.getExpression();
        Expression thenExpr = cond.getThenExpression();
        Expression elseExpr = cond.getElseExpression();
        int condEnd = condition.getStartPosition() + condition.getLength();
        int thenStart = thenExpr.getStartPosition();
        int thenEnd = thenExpr.getStartPosition() + thenExpr.getLength();
        int elseStart = elseExpr.getStartPosition();
        int elseEnd = elseExpr.getStartPosition() + elseExpr.getLength();

        // IntelliJ's JavaFormatterConditionalExpressionUtil.isInsideConditionalExpression:
        // when this ternary is inside another ternary's then/else branch,
        // its operands get spaceIndent(0, relativeToDirectParent=true). That
        // anchors each nested operator to the direct parent expression column,
        // not to the original source column.
        boolean nestedInTernaryBranch = isInsideTernaryBranch(cond);
        Indent branchIndent = nestedInTernaryBranch
                ? Indent.relativeSpaceIndent(0)
                : Indent.continuationIndent();

        // Condition part (up to `?`).
        Block condBlock = buildExpressionBlock(condition, start, condEnd, "TernaryCondition");
        composite.child(condBlock);

        // Extend thenEnd past any trailing line comment on the then-branch's
        // line (e.g. `choose(...) // then`) so the comment stays with the
        // then-content and colonTokens starts at the `:` operator.
        for (JavaTokens.Token tok : tokensIn(thenEnd, elseStart)) {
            if (tok.isComment() && !containsLineBreak(thenEnd, tok.start())) {
                thenEnd = tok.text().endsWith("\n") ? tok.end() - 1 : tok.end();
            }
        }
        // `? then` clause: `?` operator and then-operand both at CONTINUATION
        // (or spaceIndent(0, relativeToDirectParent) for nested ternaries),
        // so each appears at +8 when it lands on its own line. Disable
        // canUseFirstChildIndent on the wrappers so the walk continues up
        // to the indent-carrying composite instead of terminating at the
        // empty-indent leaf whitespace.
        Block thenContent = buildExpressionBlock(thenExpr, thenStart, thenEnd, "TernaryThen");
        OperatorOperandBlocks thenBlocks = buildOperatorOperandBlocks(
                composite,
                condBlock,
                condEnd,
                thenStart,
                ITerminalSymbols.TokenNameQUESTION,
                thenStart,
                thenEnd,
                thenContent,
                branchIndent,
                "TernaryThen");
        // When either branch is a text block, force `? """` onto its own
        // line so structural text-block line leaves can align content. `?`
        // and `"""` stay on the same line; the line break is between
        // condition and the `? ...` segment.
        boolean anyTextBlock = isTextBlockArgument(thenExpr) || isTextBlockArgument(elseExpr);
        addSibling(
                composite,
                thenBlocks.previous(),
                thenBlocks.operator(),
                anyTextBlock ? Spacing.createSpacing(0, 0, 1, false, 0) : JavaSpacingRules.keepLineOrSpace());
        addSibling(composite, thenBlocks.operator(), thenBlocks.operand(), JavaSpacingRules.keepLineOrSpace());

        // `: else` clause: same treatment.
        int lastEnd = max(end, elseEnd);
        Block elseContent = buildExpressionBlock(elseExpr, elseStart, lastEnd, "TernaryElse");
        OperatorOperandBlocks elseBlocks = buildOperatorOperandBlocks(
                composite,
                thenBlocks.operand(),
                thenEnd,
                elseStart,
                ITerminalSymbols.TokenNameCOLON,
                elseStart,
                lastEnd,
                elseContent,
                branchIndent,
                "TernaryElse");
        addSibling(
                composite,
                elseBlocks.previous(),
                elseBlocks.operator(),
                anyTextBlock ? Spacing.createSpacing(0, 0, 1, false, 0) : JavaSpacingRules.keepLineOrSpace());
        addSibling(composite, elseBlocks.operator(), elseBlocks.operand(), JavaSpacingRules.keepLineOrSpace());

        return composite.build();
    }

    private OperatorOperandBlocks buildOperatorOperandBlocks(
            JavaBlock.Builder composite,
            Block previous,
            int operatorSearchStart,
            int operandStart,
            int operatorType,
            int operandBlockStart,
            int operandBlockEnd,
            Block operandContent,
            Indent indent,
            String debugName)
    {
        int operatorStart = findTokenBetween(operatorSearchStart, operandStart, operatorType);
        if (operatorStart < 0) {
            operatorStart = operatorSearchStart;
        }
        Block prevBeforeOperator = owner.emitInterBlockComments(composite, previous, operatorSearchStart, operatorStart, indent);
        int operandWrapperStart = owner.firstCommentStart(operatorStart, operandBlockStart);
        Block operatorTokens = owner.buildTokensRange(operatorStart, operandWrapperStart, debugName + "Operator", false);
        Block operatorWrapped = JavaBlock.builder(operatorStart, operandWrapperStart, debugName + "OperatorWrap")
                .indent(indent)
                .canUseFirstChildIndent(false)
                .child(operatorTokens)
                .build();
        Indent operandIndent = hasInlineCommentBeforeBody(operandWrapperStart, operandBlockStart)
                && indent.type() == Indent.Type.CONTINUATION
                ? Indent.continuationEnforcedIndent()
                : indent;
        Block operandWrapped = buildIndentedBodyWrapper(
                debugName + "OperandWrap",
                operandWrapperStart,
                operandBlockStart,
                operandBlockEnd,
                operandContent,
                operandIndent);
        Block operandSegment = JavaBlock.builder(operandWrapperStart, operandBlockEnd, debugName + "OperandSegment")
                .canUseFirstChildIndent(false)
                .child(operandWrapped)
                .build();
        return new OperatorOperandBlocks(prevBeforeOperator, operatorWrapped, operandSegment);
    }

    private record OperatorOperandBlocks(Block previous, Block operator, Block operand) {}

    /// Mirrors IntelliJ's
    /// `JavaFormatterConditionalExpressionUtil.isInsideConditionalExpression`.
    /// Returns true when `cond` is inside another conditional expression's
    /// then- or else-branch (not the condition).
    private static boolean isInsideTernaryBranch(ConditionalExpression cond)
    {
        ASTNode child = cond;
        ASTNode parent = cond.getParent();
        while (parent != null) {
            if (parent instanceof ConditionalExpression outer) {
                // Walk up until the immediate ASTNode child of the outer
                // ternary is known, to check whether we're in the condition
                // or in a branch. Here child IS the immediate descendant of
                // parent (because we updated child/parent in lockstep).
                return outer.getExpression() != child;
            }
            if (parent instanceof MethodDeclaration || parent instanceof VariableDeclarationFragment) {
                return false;
            }
            child = parent;
            parent = parent.getParent();
        }
        return false;
    }

    /// Lambda with expression body where the body is a method chain:
    /// `params -> expr.chain()...`. Emits prefix (`params ->`)
    /// then the chain body with its own selector indent.
    private Block buildLambdaExpressionWithBody(
            LambdaExpression lambda,
            int start,
            int end,
            int bodyStart,
            int bodyEnd,
            MethodInvocation bodyChain)
    {
        JavaBlock.Builder composite = JavaBlock.builder(start, end, "LambdaExpr");
        // Lambda prefix: `value -> ` up to the body. When the source wraps the
        // `->` onto its own line (`(x, y)\n        -> body`), split the arrow
        // into a CONTINUATION sibling so it lands at lambdaCol + CONT instead
        // of preserving the source column.
        int arrowOffset = -1;
        for (JavaTokens.Token tok : tokensIn(start, bodyStart)) {
            if (tok.type() == ITerminalSymbols.TokenNameARROW) {
                arrowOffset = tok.start();
                break;
            }
        }
        int prefixEnd = owner.firstCommentStart(start, bodyStart);
        Block prefix;
        if (arrowOffset > start && containsLineBreak(start, arrowOffset)) {
            Block params = owner.buildTokensRange(start, arrowOffset, "LambdaParams");
            Block arrowAndBody = JavaBlock.builder(arrowOffset, prefixEnd, "LambdaArrowWrap")
                    .indent(Indent.continuationIndent())
                    .child(owner.buildTokensRange(arrowOffset, prefixEnd, "LambdaArrow"))
                    .build();
            JavaBlock.Builder prefixBuilder = JavaBlock.builder(start, prefixEnd, "LambdaPrefix");
            prefixBuilder.child(params);
            addSibling(prefixBuilder, params, arrowAndBody, Spacing.createSpacing(0, 0, 1, false, 0));
            prefix = prefixBuilder.build();
        }
        else {
            prefix = owner.buildTokensRange(start, prefixEnd, "LambdaPrefix");
        }
        composite.child(prefix);
        // Body: chain expression. Extend range to capture trailing tokens
        // (commas etc.) that the caller included in [start, end).
        int chainEnd = max(bodyEnd, end);
        Block body = buildChainExpression(bodyChain, bodyStart, chainEnd);
        // Wrap body in CONTINUATION so when source has body on its own line
        // (`() ->\n  body...`), the body lands at +CONTINUATION from lambda.
        Block bodyWrapped = buildLambdaBodyWrapper(prefixEnd, bodyStart, chainEnd, body);
        // Airlift style: in multi-argument wrapped calls, chain-body lambdas
        // that are NOT the last argument pull the body's head inline with the
        // arrow (selectors may still wrap). Single-argument callers and the
        // last argument of a list keep source placement — those positions
        // legitimately want the body on its own line.
        Spacing bodySpacing = isNonLastLambdaOfMultiArgCall(lambda)
                ? Spacing.createSpacing(1, 1, 0, false, 0)
                : JavaSpacingRules.keepLineOrSpace();
        addSibling(composite, prefix, bodyWrapped, bodySpacing);
        return composite.build();
    }

    private Block buildLambdaBodyWrapper(int wrapperStart, int bodyStart, int bodyEnd, Block body)
    {
        Indent bodyIndent = hasInlineCommentBeforeBody(wrapperStart, bodyStart)
                ? Indent.continuationEnforcedIndent()
                : Indent.continuationIndent();
        return buildIndentedBodyWrapper(
                "LambdaBodyWrap",
                wrapperStart,
                bodyStart,
                bodyEnd,
                body,
                bodyIndent);
    }

    private boolean hasInlineCommentBeforeBody(int wrapperStart, int bodyStart)
    {
        for (JavaTokens.Token tok : tokensIn(wrapperStart, bodyStart)) {
            if (tok.isComment()) {
                return !containsLineBreak(wrapperStart, tok.start());
            }
        }
        return false;
    }

    Block buildIndentedBodyWrapper(
            String debugName,
            int wrapperStart,
            int bodyStart,
            int bodyEnd,
            Block body,
            Indent indent)
    {
        JavaBlock.Builder bodyWrapper = JavaBlock.builder(wrapperStart, bodyEnd, debugName)
                .indent(indent)
                .canUseFirstChildIndent(false);
        Block prev = null;
        for (JavaTokens.Token tok : tokensIn(wrapperStart, bodyStart)) {
            if (!tok.isComment()) {
                continue;
            }
            int cEnd = tok.text().endsWith("\n") ? tok.end() - 1 : tok.end();
            Block commentBlock = JavaBlock.builder(tok.start(), cEnd, debugName + "Comment")
                    .indent(owner.commentIndent(tok, Indent.noneIndent()))
                    .child(JavaBlockBuilder.commentLeaf(tok))
                    .canUseFirstChildIndent(false)
                    .build();
            if (prev == null) {
                bodyWrapper.child(commentBlock);
            }
            else {
                addSibling(bodyWrapper, prev, commentBlock, Spacing.createSpacing(0, 0, 1, true, 1));
            }
            prev = commentBlock;
        }
        if (prev == null) {
            bodyWrapper.child(body);
        }
        else {
            Spacing bodySpacing = containsLineBreak(prev.endOffset(), bodyStart)
                    ? Spacing.createSpacing(0, 0, 1, false, 0)
                    : Spacing.oneSpace();
            addSibling(bodyWrapper, prev, body, bodySpacing);
        }
        return bodyWrapper.build();
    }

    private static boolean isNonLastLambdaOfMultiArgCall(LambdaExpression lambda)
    {
        ASTNode parent = lambda.getParent();
        List<?> arguments = null;
        if (parent instanceof MethodInvocation mi) {
            arguments = mi.arguments();
        }
        else if (parent instanceof ClassInstanceCreation cic) {
            arguments = cic.arguments();
        }
        if (arguments == null || arguments.size() <= 1) {
            return false;
        }
        return arguments.getLast() != lambda;
    }

    private Block buildReceiverWrappedExpression(MethodInvocation mi, int start, int wrapPoint, int end)
    {
        JavaBlock.Builder composite = JavaBlock.builder(start, end, "ReceiverWrappedExpr");
        Block head = owner.buildTokensRange(start, wrapPoint, "ReceiverWrappedHead");
        composite.child(head);
        // When the outer call has structured args, build the tail via
        // buildCallExpression so wrapped args pick up the correct continuation
        // from within the ReceiverWrappedTail block. Only use mi.arguments()
        // (their positions are within [wrapPoint,end]); do NOT pass mi itself
        // as the expr start since mi's AST range predates wrapPoint.
        Block tailContent = (!mi.arguments().isEmpty() && needsStructuredCallArguments(mi.arguments(), mi))
                ? buildCallExpression(mi, mi.arguments(), wrapPoint, end)
                : owner.buildTokensRange(wrapPoint, end, "ReceiverWrappedTailTokens");
        Block tail = JavaBlock.builder(wrapPoint, end, "ReceiverWrappedTail")
                .indent(Indent.continuationIndent())
                .canUseFirstChildIndent(false)
                .child(tailContent)
                .build();
        addSibling(composite, head, tail, Spacing.createSpacing(0, 0, 0, true, 1));
        return composite.build();
    }

    /// Build a chain expression (not at statement level). Produces a composite
    /// with chain selectors carrying smart CONTINUATION indent. The range
    /// `[start, end)` may extend beyond the chain's own range.
    private Block buildChainExpression(MethodInvocation outermost, int start, int end)
    {
        LinkedList<MethodInvocation> chain = new LinkedList<>();
        Expression cursor = outermost;
        while (cursor instanceof MethodInvocation mi) {
            chain.addFirst(mi);
            cursor = mi.getExpression();
        }
        Expression head = cursor;

        JavaBlock.Builder composite = JavaBlock.builder(start, end, "ChainExpr");
        int chainStart = (head != null) ? head.getStartPosition() : chain.getFirst().getStartPosition();
        Block prev = null;

        // Airlift chain indent heuristic for non-statement chains.
        //
        // When a chain starts inline after a short prefix (like `? `, `: `),
        // selectors should land relative to the chain's own line-start, not
        // the enclosing method's. The offset depends on whether any inline
        // selectors precede the first wrapped one:
        //   - No inline selectors before the wrap -> single CONTINUATION.
        //   - Some inline selectors before the wrap -> double CONTINUATION
        //     (selectors visually hang off the inline prefix).
        // Default: selectors use CONTINUATION smart indent (head + CONT).
        Indent selectorIndent = Indent.smartIndent(Indent.Type.CONTINUATION);
        int chainLineStart = owner.lineStartColumn(chainStart);
        int chainCol = owner.columnOf(chainStart);
        int prefixWidth = chainCol - chainLineStart;
        // Text-block receivers have their own alignment rule (post-format
        // phase re-anchors the `.method` selector to the text block's
        // enclosing line), so skip the absolute-indent override for them
        // and let smartIndent fall through.
        boolean isTextBlockHead = head != null && leadingExpressionIsTextBlock(head);
        // Chain inside a ConditionalExpression then/else branch uses
        // IntelliJ's space-smart indent path. In practice that aligns
        // wrapped selector dots with the branch receiver column, regardless
        // of the incoming source indentation.
        boolean insideCondBranch = isMethodCallInsideConditionalBranch(outermost)
                && (!isTextBlockHead || ternaryBranchExpressionStartsAfterOperatorLineBreak(outermost));
        if (insideCondBranch) {
            selectorIndent = Indent.absoluteSpaceIndent(chainCol);
        }
        // Restrict the absolute-indent override to ternary branches
        // (prefix `? ` or `: `); other inline prefixes (`|| `, `&& `,
        // `+ `, etc.) have their own alignment rules that are better
        // served by smartIndent's default resolution.
        boolean isTernaryPrefix = chainLineStart >= 0 && chainLineStart < chainCol
                && prefixWidth < 4
                && isTernaryBranchPrefix(chainLineStart, chainStart);
        if (!insideCondBranch && !isTextBlockHead && isTernaryPrefix) {
            int offset = chainBuilder.inlineSelectorsPrecedeWrap(chain, head, chainStart)
                    ? 2 * Indent.CONTINUATION_SIZE
                    : Indent.CONTINUATION_SIZE;
            selectorIndent = Indent.absoluteSpaceIndent(chainLineStart + offset);
        }
        Indent commentIndent = insideCondBranch ? Indent.continuationIndent() : selectorIndent;

        int firstSelectorStart = chainBuilder.selectorStart(chain.getFirst(), chainStart);
        if (head != null && firstSelectorStart >= 0) {
            int headAstEnd = head.getStartPosition() + head.getLength();
            int headEnd = headAstEnd < firstSelectorStart && owner.hasCommentIn(headAstEnd, firstSelectorStart)
                    ? headAstEnd
                    : firstSelectorStart;
            // If head is a wrapped call (ClassInstanceCreation/MethodInvocation
            // with multi-line args), decompose so its args get CONTINUATION.
            // If head is a ClassInstanceCreation with an anonymous class body,
            // decompose so the body members get NORMAL indent relative to the
            // anon class `{`.
            Block headBlock;
            if (head instanceof ClassInstanceCreation cic
                    && cic.getAnonymousClassDeclaration() != null) {
                headBlock = buildExpressionBlock(cic, start, headEnd, "ChainExprHead");
            }
            else if (head instanceof ClassInstanceCreation cic
                    && !cic.arguments().isEmpty()
                    && needsStructuredCallArguments(cic.arguments(), cic)) {
                headBlock = buildCallExpression(cic, cic.arguments(), start, headEnd);
            }
            else if (head instanceof ParenthesizedExpression
                    && containsLineBreak(head.getStartPosition(), head.getStartPosition() + head.getLength())) {
                // Multi-line `(infix + expr)` receiver of a chain — route
                // through buildExpressionBlock so the inner infix operands
                // pick up CONTINUATION instead of flat-tokenizing.
                headBlock = buildExpressionBlock(head, start, headEnd, "ChainExprHead");
            }
            else if (isTextBlockHead) {
                headBlock = owner.buildTokensRangePreservingNegativeTextBlockMargin(start, headEnd, "ChainExprHead");
            }
            else {
                headBlock = owner.buildTokensRange(start, headEnd, "ChainExprHead");
            }
            ClassInstanceCreation constructorHead = (head instanceof ClassInstanceCreation cic) ? cic : null;
            Optional<JavaChainExpressionBuilder.ConstructorSelectorRange> selectorRange = (constructorHead != null)
                    ? chainBuilder.constructorSelectorRange(constructorHead, start, headAstEnd)
                    : Optional.empty();
            if (selectorRange.isPresent()) {
                JavaChainExpressionBuilder.ConstructorSelectorRange range = selectorRange.orElseThrow();
                int headSplit = range.firstSelectorStart();
                headBlock = owner.buildTokensRange(start, headSplit, "ChainExprHead");
                composite.child(headBlock);
                prev = chainBuilder.emitConstructorSelectorChunksAsSiblings(
                        composite,
                        headBlock,
                        range,
                        selectorIndent,
                        "ChainExprConstructorSelector");
            }
            else {
                composite.child(headBlock);
                prev = headBlock;
            }
        }
        if (head == null && !chain.isEmpty()) {
            MethodInvocation first = chain.getFirst();
            int firstEnd = first.getStartPosition() + first.getLength();
            Block firstContent;
            if (!first.arguments().isEmpty() && needsStructuredCallArguments(first.arguments(), first)) {
                firstContent = buildCallExpression(first, first.arguments(), first.getStartPosition(), firstEnd);
            }
            else {
                firstContent = owner.buildTokensRange(first.getStartPosition(), firstEnd, "ChainExprFirstTokens");
            }
            Block firstBlock = JavaBlock.builder(first.getStartPosition(), firstEnd, "ChainExprFirst")
                    .child(firstContent)
                    .build();
            composite.child(firstBlock);
            prev = firstBlock;
        }

        // When a chain has a selector whose argument is an expression-body
        // lambda that spans multiple lines AND there's already a wrapped
        // selector after it, force that selector onto its own line.
        int firstForcedWrapIndex = firstSelectorForcedWrapForLambdaChain(chain, head);
        int startIndex = (head == null) ? 1 : 0;
        int selectorScanStart = (prev != null) ? prev.endOffset() : start;
        for (int i = startIndex; i < chain.size(); i++) {
            MethodInvocation mi = chain.get(i);
            int selectorStart = chainBuilder.selectorStart(mi, (i == 0) ? head.getStartPosition() : 0);
            int selectorEnd = mi.getStartPosition() + mi.getLength();
            if (selectorStart < 0) {
                selectorStart = mi.getName().getStartPosition();
            }
            // Emit inter-selector comments (between previous selector and
            // this `.method` call) so chain comments are preserved. Trailing
            // comments on the same line as the previous selector stay inline.
            // Preserve source column alignment (2+ spaces before the comment).
            for (JavaTokens.Token tok : tokensIn(selectorScanStart, selectorStart)) {
                if (!tok.isComment()) {
                    continue;
                }
                int cEnd = (tok.text().endsWith("\n")) ? tok.end() - 1 : tok.end();
                boolean inlineWithPrev = prev != null
                        && !containsLineBreak(selectorScanStart, tok.start());
                Block commentBlock = JavaBlock.builder(tok.start(), cEnd, "ChainComment")
                        .indent(inlineWithPrev ? Indent.noneIndent() : owner.commentIndent(tok, commentIndent))
                        .canUseFirstChildIndent(false)
                        .build();
                if (prev != null) {
                    Spacing sp;
                    if (inlineWithPrev) {
                        int sourceSpaces = max(1, tok.start() - prev.endOffset());
                        sp = Spacing.createSpacing(sourceSpaces, sourceSpaces, 0, false, 0);
                    }
                    else {
                        sp = Spacing.createSpacing(0, 0, 1, true, 1);
                    }
                    addSibling(composite, prev, commentBlock, sp);
                }
                else {
                    composite.child(commentBlock);
                }
                prev = commentBlock;
            }
            // Last selector: extend to the full range end to capture trailing
            // tokens (commas, etc.) that the caller included.
            if (i == chain.size() - 1 && selectorEnd < end) {
                selectorEnd = end;
            }
            Block selectorContent;
            if (!mi.arguments().isEmpty() && needsStructuredCallArguments(mi.arguments(), mi)) {
                selectorContent = buildCallExpression(mi, mi.arguments(), selectorStart, selectorEnd);
            }
            else {
                selectorContent = owner.buildTokensRange(selectorStart, selectorEnd, "ChainExprSelectorTokens");
            }
            Block selector = JavaBlock.builder(selectorStart, selectorEnd, "ChainExprSelector")
                    .indent(selectorIndent)
                    .child(selectorContent)
                    .build();
            if (prev != null) {
                // Insert ONE line break before the first selector with a
                // multi-line lambda arg; later selectors follow source.
                int minLineFeeds = (i == firstForcedWrapIndex) ? 1 : 0;
                composite.spacing(prev, selector, Spacing.createSpacing(0, 0, minLineFeeds, true, 1));
            }
            composite.child(selector);
            prev = selector;
            selectorScanStart = selectorEnd;
        }
        return composite.build();
    }

    /// Lambda with block body (`() -> { ... }`): prefix + `{` + per-
    /// statement blocks via buildStatementBlock + `}`. The range may extend
    /// beyond the lambda to include trailing commas.
    private Block buildBlockLambdaExpression(org.eclipse.jdt.core.dom.Block blockBody, int start, int end)
    {
        JavaBlock.Builder composite = JavaBlock.builder(start, end, "BlockLambdaExpr");
        int bodyStart = blockBody.getStartPosition();
        int bodyEnd = bodyStart + blockBody.getLength();

        // Single-statement `throw` lambda whose source kept `{ throw ...; }`
        // inline — preserve the inline shape. FORMATTER_STYLE: "A block lambda
        // with a body that is only a single `throw` statement may stay inline
        // if it is already inline." Emitting the body as a flat token range
        // skips buildStatementBlock's mandatory newline-before-`}` rule.
        if (isInlineSingleThrowLambdaBody(blockBody)) {
            Block flat = owner.buildTokensRange(start, end, "InlineThrowLambdaExpr");
            return JavaBlock.builder(start, end, "BlockLambdaExpr")
                    .child(flat)
                    .build();
        }

        // Lambda prefix: `() -> ` up to the block `{`.
        Block prefix = owner.buildTokensRange(start, bodyStart, "LambdaBlockPrefix");
        composite.child(prefix);

        // Block body: decompose statements via buildStatementBlock.
        Block body = owner.buildStatementBlock(blockBody);
        addSibling(composite, prefix, body, Spacing.createSpacing(1, 1, 0, false, 0));

        // Trailing tokens after `}` (e.g. commas in arg list).
        if (bodyEnd < end) {
            Block trailing = owner.buildTokensRange(bodyEnd, end, "LambdaBlockTrailing");
            addSibling(composite, body, trailing, Spacing.none());
        }
        return composite.build();
    }

    boolean isInlineSingleThrowLambdaBody(org.eclipse.jdt.core.dom.Block blockBody)
    {
        @SuppressWarnings("unchecked")
        List<org.eclipse.jdt.core.dom.Statement> stmts = blockBody.statements();
        if (stmts.size() != 1 || !(stmts.getFirst() instanceof ThrowStatement)) {
            return false;
        }
        int bodyStart = blockBody.getStartPosition();
        int bodyEnd = bodyStart + blockBody.getLength();
        return !containsLineBreak(bodyStart, bodyEnd);
    }

    /// Find the `(` that opens the argument list of a call expression.
    int findLParen(List<?> arguments, int exprStart)
    {
        // For MethodInvocation: `(` is right after the method name.
        // For ClassInstanceCreation: `(` is after the type name.
        // Find the last LPAREN token in [exprStart, firstArgStart). Use
        // tokens rather than source.charAt() so that `(` characters inside
        // comments or string literals (e.g. a comment like
        // `// IamPolicy.builder()`) don't get mistaken for the call's `(`.
        if (arguments.isEmpty()) {
            return -1;
        }
        int firstArgStart = ((ASTNode) arguments.getFirst()).getStartPosition();
        int lparen = -1;
        for (JavaTokens.Token tok : tokens) {
            if (tok.start() < exprStart) {
                continue;
            }
            if (tok.start() >= firstArgStart) {
                break;
            }
            if (tok.type() == ITerminalSymbols.TokenNameLPAREN) {
                lparen = tok.start();
            }
        }
        return lparen;
    }

    /// Find the matching `)` for a `(` at `lparenOffset` by tracking
    /// parenthesis balance.
    int findMatchingRParen(int lparenOffset, int maxEnd)
    {
        int depth = 0;
        for (JavaTokens.Token tok : tokens) {
            if (tok.start() < lparenOffset) {
                continue;
            }
            if (tok.start() >= maxEnd) {
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
                if (depth == 0) {
                    return tok.start();
                }
            }
        }
        return -1;
    }

    /// Walk the receiver chain of `mi` inward; if any receiver is a
    /// ParenthesizedExpression wrapping a SwitchExpression, return that
    /// ParenthesizedExpression. Used to detect the shape
    /// `(switch (x) { ... }).method(...)`, where the switch body cases need
    /// NORMAL indent but the outer MI is not a regular chain (its receiver
    /// is a ParenExpr, not another MI).
    private static ParenthesizedExpression findParenSwitchReceiver(MethodInvocation mi)
    {
        Expression recv = mi.getExpression();
        while (recv instanceof MethodInvocation inner) {
            recv = inner.getExpression();
        }
        if (recv instanceof ParenthesizedExpression paren
                && paren.getExpression() instanceof SwitchExpression) {
            return paren;
        }
        return null;
    }

    boolean isNontrivialChain(MethodInvocation mi)
    {
        return chainBuilder.isNontrivialChain(mi);
    }

    /// Build a statement whose top-level expression is a method chain.
    /// The chain is decomposed into a CHAIN_HEAD block (the innermost
    /// expression) and one CHAIN_SELECTOR block per `.method(args)` call.
    /// Each selector carries [Indent#smartIndent(Indent.Type)] so the
    /// ExpandableIndent pass enforces CONTINUATION indent on every selector
    /// once any selector wraps to its own line.
    Block buildChainStatement(ASTNode stmt, MethodInvocation outermost)
    {
        int stmtStart = stmt.getStartPosition();
        int stmtEnd = stmtStart + stmt.getLength();
        // Walk from outermost to innermost, collecting method invocations.
        LinkedList<MethodInvocation> chain = new LinkedList<>();
        Expression cursor = outermost;
        while (cursor instanceof MethodInvocation mi) {
            chain.addFirst(mi);
            cursor = mi.getExpression();
        }
        Expression head = cursor; // innermost expression (possibly null for unqualified first call)
        int chainEnd = outermost.getStartPosition() + outermost.getLength();

        JavaBlock.Builder statement = JavaBlock.builder(stmtStart, stmtEnd, "ChainStatement")
                .indent(Indent.normalIndent());

        int chainStart = (head != null) ? head.getStartPosition() : chain.getFirst().getStartPosition();
        // Chain wraps onto a new line after an inline prefix (e.g.
        // `var = \n    new Foo()...`): give the chain CONTINUATION indent so
        // the head lands at stmt + CONTINUATION rather than stmt + 0. This
        // mirrors IntelliJ's RHS-of-assignment indent. When the `=` is on its
        // own line, the split path below handles it via eqPart instead and
        // the chain itself needs no indent.
        boolean chainWrappedAfterInlinePrefix = chainStart > stmtStart
                && containsLineBreak(stmtStart, chainStart)
                && findWrappedAssignmentInPrefix(stmtStart, chainStart) < 0;
        JavaBlock.Builder chainBlock = JavaBlock.builder(chainStart, chainEnd, "Chain");
        if (chainWrappedAfterInlinePrefix) {
            chainBlock.indent(Indent.continuationIndent());
        }

        // Airlift's preservation rule: when source has 2+ wrapped selectors
        // all at the same column AND that column matches stmtCol+16 (doubled
        // CONT), preserve via absolute indent. A single wrapped selector or
        // non-uniform columns fall through to smartIndent (single CONT).
        Indent selectorIndent = Indent.smartIndent(Indent.Type.CONTINUATION);
        if (chainStart > stmtStart && (head == null || head instanceof ClassInstanceCreation)) {
            int stmtCol = owner.columnOf(stmtStart);
            int doubled = stmtCol + 2 * Indent.CONTINUATION_SIZE;
            int wrappedCount = chainBuilder.countWrappedSelectors(chain, head, chainStart);
            if (wrappedCount >= 2) {
                int uniformCol = chainBuilder.uniformWrappedSelectorColumn(chain, head, chainStart);
                if (uniformCol == doubled) {
                    selectorIndent = Indent.absoluteSpaceIndent(uniformCol);
                }
            }
        }

        // Prefix: statement prefix tokens before the chain start (e.g. `return `,
        // `int x = `). When the prefix wraps with the `=` on its own line
        // (e.g. `Object result\n        = new Builder()`), split at `=` so
        // the continuation part gets CONTINUATION indent.
        Block prefixBlock = null;
        if (chainStart > stmtStart) {
            int wrappedEqPos = findWrappedAssignmentInPrefix(stmtStart, chainStart);
            if (wrappedEqPos >= 0) {
                Block declPart = owner.buildTokensRange(stmtStart, wrappedEqPos, "StatementDeclPart");
                statement.child(declPart);
                Block eqPart = JavaBlock.continuationWrap(
                        wrappedEqPos,
                        chainStart,
                        owner.buildTokensRange(wrappedEqPos, chainStart, "StatementEqTokens"),
                        "StatementEqPart");
                addSibling(statement, declPart, eqPart, JavaSpacingRules.keepLineOrSpace());
                prefixBlock = eqPart;
            }
            else {
                prefixBlock = owner.buildTokensRange(stmtStart, chainStart, "StatementPrefix");
                statement.child(prefixBlock);
            }
        }

        Block prevChainChild = null;
        // Head block: normally includes selector-adjacent generic type args;
        // split at the receiver AST end only when comments need to become
        // independent chain chunks before the first selector.
        int firstSelectorStart = chainBuilder.selectorStart(chain.getFirst(), chainStart);
        boolean isTextBlockHead = head != null && leadingExpressionIsTextBlock(head);
        if (head != null && firstSelectorStart >= 0) {
            int headStart = head.getStartPosition();
            int headAstEnd = headStart + head.getLength();
            int headEnd = headAstEnd < firstSelectorStart && owner.hasCommentIn(headAstEnd, firstSelectorStart)
                    ? headAstEnd
                    : firstSelectorStart;
            // Decompose multi-line heads so nested operands/args pick up
            // CONTINUATION indent — e.g. `new Response(\n status, ...)` or
            // `(first()\n + second())`.
            Block headContent;
            if (head instanceof ClassInstanceCreation cicHead
                    && cicHead.getAnonymousClassDeclaration() != null) {
                // `new Foo() { body }.chain()` — decompose the anon body so
                // its members get NORMAL indent relative to the anon `{`.
                headContent = buildExpressionBlock(cicHead, headStart, headEnd, "ChainHeadExpr");
            }
            else if (head instanceof ClassInstanceCreation cicHead
                    && !cicHead.arguments().isEmpty()
                    && needsStructuredCallArguments(cicHead.arguments(), cicHead)) {
                headContent = buildCallExpression(
                        cicHead,
                        cicHead.arguments(),
                        headStart,
                        headEnd);
            }
            else if (containsLineBreak(headStart, headEnd)) {
                // Generic multi-line head — dispatch through expression
                // handler so ParenthesizedExpression / InfixExpression /
                // CastExpression etc. get proper indent.
                headContent = isTextBlockHead
                        ? owner.buildTokensRangePreservingNegativeTextBlockMargin(headStart, headEnd, "ChainHeadTokens")
                        : buildExpressionBlock(head, headStart, headEnd, "ChainHeadExpr");
            }
            else {
                headContent = isTextBlockHead
                        ? owner.buildTokensRangePreservingNegativeTextBlockMargin(headStart, headEnd, "ChainHeadTokens")
                        : owner.buildTokensRange(headStart, headEnd, "ChainHeadTokens");
            }
            Block headBlock = JavaBlock.builder(headStart, headEnd, "ChainHead")
                    .child(headContent)
                    .build();
            ClassInstanceCreation constructorHead = (head instanceof ClassInstanceCreation cicHead) ? cicHead : null;
            Optional<JavaChainExpressionBuilder.ConstructorSelectorRange> selectorRange = (constructorHead != null)
                    ? chainBuilder.constructorSelectorRange(constructorHead, headStart, headAstEnd)
                    : Optional.empty();
            if (selectorRange.isPresent()) {
                JavaChainExpressionBuilder.ConstructorSelectorRange range = selectorRange.orElseThrow();
                int headSplit = range.firstSelectorStart();
                headBlock = JavaBlock.builder(headStart, headSplit, "ChainHead")
                        .child(owner.buildTokensRange(headStart, headSplit, "ChainHeadTokens"))
                        .build();
                chainBlock.child(headBlock);
                prevChainChild = chainBuilder.emitConstructorSelectorChunksAsSiblings(
                        chainBlock,
                        headBlock,
                        range,
                        selectorIndent,
                        "ChainConstructorSelector");
            }
            else {
                chainBlock.child(headBlock);
                prevChainChild = headBlock;
            }
        }
        // If there's no qualifier, the first MethodInvocation provides its own
        // prefix up to its name. Decompose its arguments if they wrap.
        if (head == null && !chain.isEmpty()) {
            MethodInvocation first = chain.getFirst();
            int firstEnd = first.getStartPosition() + first.getLength();
            Block firstContent;
            if (!first.arguments().isEmpty() && needsStructuredCallArguments(first.arguments(), first)) {
                firstContent = buildCallExpression(first, first.arguments(), first.getStartPosition(), firstEnd);
            }
            else {
                firstContent = owner.buildTokensRange(first.getStartPosition(), firstEnd, "ChainFirstTokens");
            }
            Block firstBlock = JavaBlock.builder(first.getStartPosition(), firstEnd, "ChainFirst")
                    .child(firstContent)
                    .build();
            chainBlock.child(firstBlock);
            prevChainChild = firstBlock;
        }

        int startIndex = (head == null) ? 1 : 0;
        int selectorScanStart = (prevChainChild != null) ? prevChainChild.endOffset() : chainStart;
        for (int i = startIndex; i < chain.size(); i++) {
            MethodInvocation mi = chain.get(i);
            int selectorStart = chainBuilder.selectorStart(mi, (i == 0) ? head.getStartPosition() : 0);
            int selectorEnd = mi.getStartPosition() + mi.getLength();
            if (selectorStart < 0) {
                selectorStart = mi.getName().getStartPosition();
            }
            // Emit inter-selector comments (between previous selector and
            // this `.method` call) so chain comments are preserved. Chain
            // selectors preserve at most one blank line, matching wrapped
            // list item grouping.
            for (JavaTokens.Token tok : tokensIn(selectorScanStart, selectorStart)) {
                if (!tok.isComment()) {
                    continue;
                }
                int cEnd = (tok.text().endsWith("\n")) ? tok.end() - 1 : tok.end();
                boolean inlineWithPrev = prevChainChild != null
                        && !containsLineBreak(selectorScanStart, tok.start());
                Block commentBlock = JavaBlock.builder(tok.start(), cEnd, "ChainComment")
                        .indent(inlineWithPrev ? Indent.noneIndent() : selectorIndent)
                        .canUseFirstChildIndent(false)
                        .build();
                if (prevChainChild != null) {
                    Spacing sp;
                    if (inlineWithPrev) {
                        int sourceSpaces = max(1, tok.start() - prevChainChild.endOffset());
                        sp = Spacing.createSpacing(sourceSpaces, sourceSpaces, 0, false, 0);
                    }
                    else {
                        sp = Spacing.createSpacing(0, 0, 1, true, 1);
                    }
                    chainBlock.spacing(prevChainChild, commentBlock, sp);
                }
                chainBlock.child(commentBlock);
                prevChainChild = commentBlock;
            }
            // If this selector's MethodInvocation has wrapped arguments,
            // decompose the argument list so each arg carries CONTINUATION
            // indent. Otherwise, flat-tokenize the whole selector.
            Block selectorContent;
            if (!mi.arguments().isEmpty() && needsStructuredCallArguments(mi.arguments(), mi)) {
                selectorContent = buildCallExpression(mi, mi.arguments(), selectorStart, selectorEnd);
            }
            else {
                selectorContent = owner.buildTokensRange(selectorStart, selectorEnd, "ChainSelectorTokens");
            }
            Block selector = JavaBlock.builder(selectorStart, selectorEnd, "ChainSelector")
                    .indent(selectorIndent)
                    .child(selectorContent)
                    .build();
            if (prevChainChild != null) {
                chainBlock.spacing(prevChainChild, selector, Spacing.createSpacing(0, 0, 0, true, 1));
            }
            chainBlock.child(selector);
            prevChainChild = selector;
            selectorScanStart = selectorEnd;
        }

        Block chain0 = chainBlock.build();
        // Statement prefix ends with a keyword (`return`) or identifier
        // (`int x =`) — one space between it and the chain head.
        if (prefixBlock != null) {
            statement.spacing(prefixBlock, chain0, JavaSpacingRules.keepLineOrSpace());
        }
        statement.child(chain0);

        // Trailing `;` (and any other tokens between chain end and statement end).
        if (chainEnd < stmtEnd) {
            Block trailing = owner.buildTokensRange(chainEnd, stmtEnd, "StatementTrailing");
            // `;` attaches directly to the chain.
            statement.spacing(chain0, trailing, Spacing.none());
            statement.child(trailing);
        }
        return statement.build();
    }

    /// Returns the offset of the `=` token in a statement prefix when the
    /// prefix has a line break BEFORE the `=`. Returns -1 if no wrap or no `=`.
    /// Used to detect `Object x\n        = value` patterns where `=` should
    /// get CONTINUATION indent.
    private int findWrappedAssignmentInPrefix(int stmtStart, int chainStart)
    {
        int eqPos = -1;
        for (JavaTokens.Token tok : tokensIn(stmtStart, chainStart)) {
            if (tok.type() == ITerminalSymbols.TokenNameEQUAL) {
                eqPos = tok.start();
                break;
            }
        }
        if (eqPos < 0) {
            return -1;
        }
        // Only wrap if there's a line break BEFORE the `=`.
        if (!containsLineBreak(stmtStart, eqPos)) {
            return -1;
        }
        return eqPos;
    }

    /// Return the chain index of the first selector whose argument is a
    /// multi-line expression-body lambda with a wrapped selector somewhere
    /// after it — the selector whose line break we need to force.
    private int firstSelectorForcedWrapForLambdaChain(LinkedList<MethodInvocation> chain, Expression head)
    {
        int startIndex = (head == null) ? 1 : 0;
        for (int i = startIndex; i < chain.size() - 1; i++) {
            MethodInvocation candidate = chain.get(i);
            if (candidate.getExpression() == null) {
                continue;
            }
            if (!hasWrappedSelectorAfter(chain, head, i)) {
                continue;
            }
            for (Object arg : candidate.arguments()) {
                if (!(arg instanceof LambdaExpression lambda)) {
                    continue;
                }
                if (lambda.getBody() instanceof org.eclipse.jdt.core.dom.Block) {
                    continue;
                }
                if (containsNestedLambda(lambda.getBody())) {
                    continue;
                }
                if (hasInlinePrefixedWrappedSelectorChain(lambda.getBody())) {
                    continue;
                }
                int bodyStart = lambda.getBody().getStartPosition();
                int bodyEnd = bodyStart + lambda.getBody().getLength();
                if (bodyStart >= bodyEnd) {
                    continue;
                }
                if (containsLineBreak(bodyStart, bodyEnd)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean hasWrappedSelectorAfter(LinkedList<MethodInvocation> chain, Expression head, int afterIndex)
    {
        // A selector is "wrapped" iff its dot owns its line.
        for (int j = afterIndex + 1; j < chain.size(); j++) {
            MethodInvocation later = chain.get(j);
            int dotPos = chainBuilder.selectorStart(later, (j == 0) ? head.getStartPosition() : 0);
            if (dotPos < 0) {
                continue;
            }
            if (dotAtLineStart(dotPos)) {
                return true;
            }
        }
        return false;
    }

    private boolean dotAtLineStart(int dotPos)
    {
        for (int i = dotPos - 1; i >= 0; i--) {
            char c = source.charAt(i);
            if (c == '\n') {
                return true;
            }
            if (c != ' ' && c != '\t') {
                return false;
            }
        }
        return true;
    }

    private static boolean containsNestedLambda(ASTNode node)
    {
        boolean[] found = new boolean[1];
        node.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(LambdaExpression lambdaExpression)
            {
                found[0] = true;
                return false;
            }
        });
        return found[0];
    }

    /// True when `body` is a chain with 2+ inline selectors before its
    /// first wrapped selector — the "inline-prefixed wrapped chain" pattern.
    private boolean hasInlinePrefixedWrappedSelectorChain(ASTNode body)
    {
        if (!(body instanceof MethodInvocation mi)) {
            return false;
        }
        // Walk to outermost.
        MethodInvocation outermost = mi;
        for (ASTNode parent = mi.getParent(); parent instanceof MethodInvocation p; parent = p.getParent()) {
            outermost = p;
        }
        // Decompose into chain.
        LinkedList<MethodInvocation> subChain = new LinkedList<>();
        Expression cursor = outermost;
        while (cursor instanceof MethodInvocation subMi) {
            subChain.addFirst(subMi);
            cursor = subMi.getExpression();
        }
        Expression subHead = cursor;
        int subStart = subHead != null ? subHead.getStartPosition() : subChain.getFirst().getStartPosition();
        // Count inline selectors before the first wrapped one.
        int inlineBeforeWrapped = 0;
        int startIdx = (subHead == null) ? 1 : 0;
        for (int i = startIdx; i < subChain.size(); i++) {
            MethodInvocation s = subChain.get(i);
            int dotPos = chainBuilder.selectorStart(s, (i == 0) ? subHead.getStartPosition() : 0);
            if (dotPos < 0) {
                continue;
            }
            if (containsLineBreak(subStart, dotPos)) {
                return inlineBeforeWrapped >= 2;
            }
            inlineBeforeWrapped++;
        }
        return false;
    }

    /// True when the text between the line's start column and the chain's
    /// start column is a ternary branch prefix (`? ` or `: `). Used to
    /// restrict the absolute-indent chain-selector override to ternary
    /// contexts where the double-continuation rule applies.
    private boolean isTernaryBranchPrefix(int lineStartCol, int chainStart)
    {
        int lineStart = chainStart - (owner.columnOf(chainStart) - lineStartCol);
        if (lineStart < 0 || chainStart > source.length()) {
            return false;
        }
        for (int i = lineStart; i < chainStart; i++) {
            char c = source.charAt(i);
            if (c == '?' || c == ':') {
                return true;
            }
            if (c != ' ' && c != '\t') {
                return false;
            }
        }
        return false;
    }

    /// Port of IntelliJ's `JavaFormatterConditionalExpressionUtil.isInsideConditionalExpression`.
    /// Walks up from `mi` looking for
    /// the first ancestor whose parent is a ConditionalExpression, a
    /// MethodDeclaration, or a VariableDeclaration; returns true iff that
    /// parent is a ConditionalExpression AND the ancestor is NOT the
    /// condition (i.e. the chain is in the then- or else-branch).
    ///
    /// When true, chain selectors align under the receiver's column
    /// rather than using CONTINUATION indent.
    private static boolean isMethodCallInsideConditionalBranch(ASTNode node)
    {
        ASTNode child = node;
        ASTNode parent = child.getParent();
        while (parent != null) {
            if (parent instanceof ConditionalExpression cond) {
                return cond.getExpression() != child;
            }
            // Stop at declaration/member boundaries — a chain inside a nested
            // method body, field initializer, or anon class isn't "inside"
            // any enclosing conditional from the chain's perspective.
            if (parent instanceof MethodDeclaration
                    || parent instanceof FieldDeclaration
                    || parent instanceof Initializer) {
                return false;
            }
            child = parent;
            parent = parent.getParent();
        }
        return false;
    }
}
