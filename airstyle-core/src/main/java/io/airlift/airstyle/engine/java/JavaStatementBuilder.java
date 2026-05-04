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
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EitherOrMultiPattern;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchExpression;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.YieldStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static java.lang.Math.max;

final class JavaStatementBuilder
{
    private final JavaBlockBuilder owner;
    private final JavaSourceContext sourceContext;
    private final String source;
    private final List<JavaTokens.Token> tokens;
    private final Set<Integer> switchGuardWhens;
    private final JavaConstructBuilder<ASTNode> loopConstructBuilder;

    JavaStatementBuilder(JavaBlockBuilder owner, JavaSourceContext sourceContext, Set<Integer> switchGuardWhens, JavaConstructBuilder<ASTNode> loopConstructBuilder)
    {
        this.owner = owner;
        this.sourceContext = sourceContext;
        this.source = sourceContext.source();
        this.tokens = sourceContext.tokens();
        this.switchGuardWhens = switchGuardWhens;
        this.loopConstructBuilder = loopConstructBuilder;
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

    private JavaTokens.Token findTrailingLineComment(int stmtEnd, int rangeEnd)
    {
        return owner.findTrailingLineComment(stmtEnd, rangeEnd);
    }

    private boolean needsStructuredCallArguments(List<?> arguments, ASTNode callNode)
    {
        return owner.needsStructuredCallArguments(arguments, callNode);
    }

    private static boolean isTextBlockArgument(Object argument)
    {
        return JavaBlockBuilder.isTextBlockArgument(argument);
    }

    private static boolean leadingExpressionIsTextBlock(Object expression)
    {
        return JavaBlockBuilder.leadingExpressionIsTextBlock(expression);
    }

    /// Decompose a `Block` AST node (method body, then-branch, etc.)
    /// into an opening brace + per-statement children + closing brace.
    /// Each statement carries [Indent#normalIndent()] so the engine
    /// indents it correctly from the enclosing block.
    Block buildStatementBlock(org.eclipse.jdt.core.dom.Block block)
    {
        return buildStatementBlock(block, false, -1);
    }

    Block buildStatementBlock(org.eclipse.jdt.core.dom.Block block, boolean preserveEmptyBody)
    {
        return buildStatementBlock(block, preserveEmptyBody, -1);
    }

    /// @param preserveEmptyBody when true, an empty block body keeps `{\n}`
    ///         instead of collapsing to `{}`. Used for catch/finally/if blocks
    ///         in Airlift style.
    /// @param anchorColumn when non-negative, the column of the enclosing
    ///         line start — block statements and `}` get an absolute indent of
    ///         `anchorColumn + NORMAL_SIZE` / `anchorColumn` so the
    ///         body layouts relative to the enclosing expression rather than the
    ///         ancestor code-block indent chain. Used for lambda / anonymous-class
    ///         bodies. When `-1`, statements get [Indent#normalIndent()]
    ///         and inherit from the walk-up chain.
    Block buildStatementBlock(org.eclipse.jdt.core.dom.Block block, boolean preserveEmptyBody, int anchorColumn)
    {
        int start = block.getStartPosition();
        int end = start + block.getLength();
        JavaBlock.Builder composite = JavaBlock.builder(start, end, "BlockBody");
        int openBraceOffset = owner.firstChar(start, end, '{');
        int closeBraceOffset = owner.lastChar(start, end, '}');
        if (openBraceOffset < 0 || closeBraceOffset < 0) {
            return owner.buildTokensRange(start, end, "BlockBody");
        }
        Block openBrace = owner.buildTokensRange(openBraceOffset, openBraceOffset + 1, "{");
        composite.child(openBrace);
        Block prev = openBrace;

        // Check for a trailing line comment on the same line as `{` —
        // `if (...) { // comment`. Include it adjacent to the brace.
        int scanStart = openBraceOffset + 1;
        JavaTokens.Token braceTrailingComment = null;
        for (JavaTokens.Token tok : tokensIn(scanStart, closeBraceOffset)) {
            if (!tok.isComment()) {
                break;
            }
            if (tok.type() == ITerminalSymbols.TokenNameCOMMENT_LINE
                    && !containsLineBreak(scanStart, tok.start())) {
                braceTrailingComment = tok;
                break;
            }
            if (containsLineBreak(scanStart, tok.start())) {
                break;
            }
        }
        if (braceTrailingComment != null) {
            Block commentLeafBlock = JavaBlockBuilder.commentLeaf(braceTrailingComment);
            // Preserve source spacing between `{` and the comment.
            int sourceSpaces = max(1, braceTrailingComment.start() - (openBraceOffset + 1));
            addSibling(composite, openBrace, commentLeafBlock, Spacing.createSpacing(sourceSpaces, sourceSpaces, 0, false, 0));
            prev = commentLeafBlock;
            // Skip this comment in subsequent processing by advancing prevEnd past it.
            // We do this via a wrapper int that the loop uses.
        }
        int braceCommentEnd = braceTrailingComment != null
                ? (braceTrailingComment.text().endsWith("\n") ? braceTrailingComment.end() - 1 : braceTrailingComment.end())
                : -1;

        if (block.statements().isEmpty()) {
            // Check for comments inside an otherwise-empty block (e.g.
            // `catch (Exception e) { // ignore }`). Without this, the
            // engine would collapse the block to `{}` and lose comments.
            List<JavaTokens.Token> bodyComments = new ArrayList<>();
            for (JavaTokens.Token tok : tokensIn(openBraceOffset + 1, closeBraceOffset)) {
                if (tok.isComment()) {
                    bodyComments.add(tok);
                }
            }
            if (bodyComments.isEmpty()) {
                Block closeBrace = owner.buildTokensRange(closeBraceOffset, closeBraceOffset + 1, "}");
                // Collapse to `{}` for methods/classes (KEEP_SIMPLE), but
                // preserve `{\n}` for control-flow blocks (catch/finally/if).
                Spacing braceSpacing = preserveEmptyBody
                        ? Spacing.createSpacing(0, 0, 1, false, 0)
                        : Spacing.createSpacing(0, 0, 0, false, 0);
                addSibling(composite, prev, closeBrace, braceSpacing);
                return composite.build();
            }
            for (JavaTokens.Token comment : bodyComments) {
                int cEnd = (comment.text().endsWith("\n")) ? comment.end() - 1 : comment.end();
                Block commentBlock = JavaBlock.builder(comment.start(), cEnd, "EmptyBlockComment")
                        .indent(owner.commentIndent(comment, Indent.normalIndent()))
                        .child(JavaBlockBuilder.commentLeaf(comment))
                        .build();
                addSibling(composite, prev, commentBlock, Spacing.createSpacing(0, 0, 1, true, 0));
                prev = commentBlock;
            }
            Block closeBrace = owner.buildTokensRange(closeBraceOffset, closeBraceOffset + 1, "}");
            addSibling(composite, prev, closeBrace, Spacing.createSpacing(0, 0, 1, true, 0));
            return composite.build();
        }

        int prevEnd = (braceCommentEnd > 0) ? braceCommentEnd : openBraceOffset + 1;
        ASTNode prevStatementNode = null;
        for (Object stmt : block.statements()) {
            ASTNode stmtNode = (ASTNode) stmt;
            int stmtStart = stmtNode.getStartPosition();
            // Emit any inter-statement comment tokens between the previous
            // element and this statement. Each comment gets its own block
            // with NORMAL indent, matching the statement indent level.
            List<JavaTokens.Token> gapTokens = tokensIn(prevEnd, stmtStart);
            for (JavaTokens.Token tok : gapTokens) {
                if (tok.isComment()) {
                    int cEnd = (tok.text().endsWith("\n")) ? tok.end() - 1 : tok.end();
                    Block commentBlock = JavaBlock.builder(tok.start(), cEnd, "InterStatementComment")
                            .indent(owner.commentIndent(tok, Indent.normalIndent()))
                            .child(JavaBlockBuilder.commentLeaf(tok))
                            .build();
                    // keepBlankLines=1: preserve a blank line before the
                    // comment if the source had one (paragraph separator).
                    addSibling(composite, prev, commentBlock, Spacing.createSpacing(0, 0, 1, true, 1));
                    prev = commentBlock;
                }
            }
            Block statementBlock = buildStatement(stmtNode);
            if (anchorColumn >= 0) {
                // Wrap each statement with an absolute-indent parent anchored
                // at the enclosing expression's line column. The statement
                // itself carries NORMAL (+4), which combines with the absolute
                // anchor to land the first leaf at {@code anchorColumn + NORMAL}.
                statementBlock = JavaBlock.builder(statementBlock.startOffset(), statementBlock.endOffset(), "BodyStmtAnchor")
                        .indent(Indent.absoluteSpaceIndent(anchorColumn))
                        .child(statementBlock)
                        .build();
            }
            int stmtEnd = stmtNode.getStartPosition() + stmtNode.getLength();
            // Check for a trailing line comment on the same line as the
            // statement (e.g. `int x = 1; // comment`). These are outside
            // the AST node's range but logically part of the statement.
            JavaTokens.Token trailingComment = findTrailingLineComment(stmtEnd, closeBraceOffset);
            if (trailingComment != null) {
                // Wrap statement + trailing comment in a composite. The
                // wrapper carries no indent; the inner statement provides
                // its own NORMAL indent and any nested CONTINUATION.
                int wrapperEnd = trailingComment.text().endsWith("\n") ? trailingComment.end() - 1 : trailingComment.end();
                JavaBlock.Builder wrapper = JavaBlock.builder(stmtStart, wrapperEnd, "StatementWithTrailingComment");
                wrapper.child(statementBlock);
                Block commentLeafBlock = JavaBlockBuilder.commentLeaf(trailingComment);
                // Preserve the source spacing between the statement and
                // the trailing comment (may be aligned with extra spaces).
                int sourceSpaces = max(1, trailingComment.start() - stmtEnd);
                wrapper.spacing(wrapper.lastChild(), commentLeafBlock, Spacing.createSpacing(sourceSpaces, sourceSpaces, 0, false, 0));
                wrapper.child(commentLeafBlock);
                statementBlock = wrapper.build();
                stmtEnd = wrapperEnd;
            }
            // Preserve source blank lines between statements and after
            // inter-statement comments. keepBlankLines=1 keeps a single blank
            // line when source had one.
            boolean localTypeBoundary = prevStatementNode != null
                    && (prevStatementNode instanceof TypeDeclarationStatement || stmtNode instanceof TypeDeclarationStatement);
            Spacing stmtSpacing = Spacing.createSpacing(0, 0, localTypeBoundary ? 2 : 1, true, 1);
            addSibling(composite, prev, statementBlock, stmtSpacing);
            prev = statementBlock;
            prevEnd = stmtEnd;
            prevStatementNode = stmtNode;
        }

        // Trailing comments between the last statement and `}`.
        prev = owner.emitInterBlockComments(composite, prev, prevEnd, closeBraceOffset);

        Block closeBrace = owner.buildTokensRange(closeBraceOffset, closeBraceOffset + 1, "}");
        if (anchorColumn >= 0) {
            // Anchor `}` to the enclosing line column so it closes at the
            // right column regardless of ancestor indent.
            closeBrace = JavaBlock.builder(closeBraceOffset, closeBraceOffset + 1, "BodyCloseBraceAnchor")
                    .indent(Indent.absoluteSpaceIndent(anchorColumn))
                    .child(closeBrace)
                    .build();
        }
        addSibling(composite, prev, closeBrace, Spacing.createSpacing(0, 0, 1, true, 0));
        return composite.build();
    }

    /// Dispatch on statement kind. The default is a flat token run with
    /// [Indent#normalIndent()]; specific kinds (like
    /// [IfStatement]) decompose further so nested blocks carry their
    /// own indent.
    Block buildStatement(ASTNode stmt)
    {
        int start = stmt.getStartPosition();
        int end = start + stmt.getLength();
        switch (stmt) {
            case LabeledStatement labeled -> {
                return buildLabeledStatement(labeled);
            }
            case IfStatement ifs -> {
                return buildIfStatement(ifs);
            }
            case TryStatement tryStmt -> {
                return buildTryStatement(tryStmt);
            }
            case WhileStatement whileStmt -> {
                return loopConstructBuilder.build(whileStmt);
            }
            case ForStatement forStmt -> {
                return loopConstructBuilder.build(forStmt);
            }
            case EnhancedForStatement enhancedFor -> {
                return loopConstructBuilder.build(enhancedFor);
            }
            case DoStatement doStmt -> {
                return buildDoStatement(doStmt);
            }
            case SynchronizedStatement syncStmt -> {
                return loopConstructBuilder.build(syncStmt);
            }
            case SwitchStatement switchStmt -> {
                return buildSwitchStatement(switchStmt);
            }

            // super(...) and this(...) constructor calls — decompose argument list.
            case SuperConstructorInvocation sci when !sci.arguments().isEmpty() && needsStructuredCallArguments(sci.arguments(), sci) -> {
                return buildCallExpressionStatement(stmt, null, sci.arguments());
            }
            case ConstructorInvocation ci -> {
                if (!ci.arguments().isEmpty() && needsStructuredCallArguments(ci.arguments(), ci)) {
                    return buildCallExpressionStatement(stmt, null, ci.arguments());
                }
            }
            default -> {}
        }
        if (stmt instanceof org.eclipse.jdt.core.dom.Block nestedBlock) {
            Block bodyBlock = buildStatementBlock(nestedBlock);
            return JavaBlock.builder(start, end, "NestedBlockStatement")
                    .indent(Indent.normalIndent())
                    .child(bodyBlock)
                    .build();
        }
        // Local type declaration (class/enum/interface/record declared inside
        // a method body). JDT wraps the declaration in TypeDeclarationStatement;
        // delegate to the same builder used for top-level types so enum
        // constants / class members get proper body indent.
        if (stmt instanceof TypeDeclarationStatement typeStmt) {
            return JavaBlock.builder(start, end, "LocalTypeStatement")
                    .indent(Indent.normalIndent())
                    .child(owner.buildTypeOrDeclaration(typeStmt.getDeclaration()))
                    .build();
        }
        // Statements whose top-level expression is a method chain: use the
        // chain decomposition so selectors carry an expandable CONTINUATION
        // indent.
        Expression topExpression = topLevelChainExpression(stmt);
        if (topExpression instanceof SwitchExpression switchExpr) {
            return buildSwitchExpressionStatement(stmt, switchExpr);
        }
        if ((stmt instanceof ReturnStatement || stmt instanceof YieldStatement)
                && topExpression != null
                && containsLineBreak(topExpression.getStartPosition(), topExpression.getStartPosition() + topExpression.getLength())
                && leadingExpressionIsTextBlock(topExpression)
                && owner.startsLine(topExpression.getStartPosition())) {
            return buildTextBlockGeneric(stmt, topExpression.getStartPosition(), topExpression.getStartPosition() + topExpression.getLength());
        }
        // Text block as the entire RHS of `String sql = """..."""`, or as the
        // leading-left receiver of a chain like `String x = """...""".trim()`
        // / `String x = """...""".formatted(y).strip()`: force the text block
        // onto a new line at CONTINUATION indent. This inline-source case must
        // run before method-chain dispatch: a multi-selector text-block
        // initializer is also a nontrivial chain, but Airlift style still wants
        // the opening delimiter on the line after `=`.
        if (topExpression != null
                && containsLineBreak(topExpression.getStartPosition(), topExpression.getStartPosition() + topExpression.getLength())
                && isTextBlockPrefixStatement(stmt)
                && leadingExpressionIsTextBlock(topExpression)
                && !owner.startsLine(topExpression.getStartPosition())) {
            return buildTextBlockGeneric(stmt, topExpression.getStartPosition(), topExpression.getStartPosition() + topExpression.getLength());
        }
        if (topExpression instanceof MethodInvocation mi && owner.isNontrivialChain(mi)) {
            return owner.buildChainStatement(stmt, mi);
        }
        // Lambda with a block body as the top-level expression — decompose
        // the body so statements get NORMAL indent.
        if (topExpression instanceof LambdaExpression lambda && lambda.getBody() instanceof org.eclipse.jdt.core.dom.Block) {
            return buildLambdaStatement(stmt, lambda);
        }
        // MI / CIC / SuperMI with wrapped arguments: delegate to
        // `buildExpressionStatement`, which routes the expression through
        // `buildExpressionBlock`. That function already dispatches each of
        // these expression kinds to `buildCallExpression` (same output as
        // the old `buildCallExpressionStatement` path) and, for a CIC that
        // also carries an anonymous class body, composes
        // `buildCallExpression` with `buildAnonymousClassDeclaration` so
        // the body members get proper absolute indent. Going through
        // `buildExpressionBlock` is how IntelliJ's formatter handles it too
        // — each AST kind is dispatched once; statement-level dispatch just
        // delegates. The catch-all multi-line dispatch further down would
        // catch these as well, but listing them here keeps the intent
        // explicit: wrapped-arg calls always decompose, regardless of
        // whether a line break sits inside the expression range.
        if ((topExpression instanceof MethodInvocation mi && needsStructuredCallArguments(mi.arguments(), mi))
                || (topExpression instanceof ClassInstanceCreation cic && needsStructuredCallArguments(cic.arguments(), cic))
                || (topExpression instanceof SuperMethodInvocation smi
                && !smi.arguments().isEmpty() && needsStructuredCallArguments(smi.arguments(), smi))) {
            return buildExpressionStatement(stmt, topExpression);
        }
        // AssertStatement with text block message: force `assert cond :`
        // then text block on its own line at CONTINUATION.
        if (stmt instanceof AssertStatement assertStmt
                && assertStmt.getMessage() != null
                && isTextBlockArgument(assertStmt.getMessage())) {
            Expression msg = assertStmt.getMessage();
            int msgStart = msg.getStartPosition();
            int msgEnd = msgStart + msg.getLength();
            JavaBlock.Builder ab = JavaBlock.builder(start, end, "AssertStatement")
                    .indent(Indent.normalIndent());
            Block prefix = owner.buildTokensRange(start, msgStart, "AssertPrefix");
            ab.child(prefix);
            Block msgBlock = JavaBlock.builder(msgStart, msgEnd, "AssertTextBlock")
                    .indent(Indent.continuationIndent())
                    .child(owner.buildTokensRange(msgStart, msgEnd, "AssertTextBlockTokens"))
                    .build();
            addSibling(ab, prefix, msgBlock, Spacing.createSpacing(0, 0, 1, false, 0));
            if (msgEnd < end) {
                Block trailing = owner.buildTokensRange(msgEnd, end, "AssertTrailing");
                addSibling(ab, msgBlock, trailing, Spacing.none());
            }
            return ab.build();
        }
        // PrefixExpression (unary operator) always dispatches through
        // buildExpressionStatement so the operator / operand spacing is
        // explicit (no-space between `-` and `1`) — the default flat
        // tokens path uses a binary-operator rule for `-`.
        if (topExpression instanceof PrefixExpression) {
            return buildExpressionStatement(stmt, topExpression);
        }
        // Non-chain variable initializers with text blocks still need the
        // generic text-block statement builder. Already-wrapped method-chain
        // initializers intentionally reached buildChainStatement above, which
        // preserves selector indentation after the closing delimiter.
        if (topExpression != null
                && containsLineBreak(topExpression.getStartPosition(), topExpression.getStartPosition() + topExpression.getLength())
                && stmt instanceof VariableDeclarationStatement
                && leadingExpressionIsTextBlock(topExpression)) {
            return buildTextBlockGeneric(stmt, topExpression.getStartPosition(), topExpression.getStartPosition() + topExpression.getLength());
        }
        // Assignment with comment between `=` and expression: decompose so the
        // comment and expression each land at CONTINUATION indent.
        if (topExpression != null) {
            int exprStart = topExpression.getStartPosition();
            int eqEnd = -1;
            for (JavaTokens.Token tok : tokensIn(start, exprStart)) {
                if (tok.type() == ITerminalSymbols.TokenNameEQUAL) {
                    eqEnd = tok.end();
                }
            }
            if (eqEnd >= 0) {
                boolean hasCommentAfterEq = false;
                for (JavaTokens.Token tok : tokensIn(eqEnd, exprStart)) {
                    if (tok.isComment()) {
                        hasCommentAfterEq = true;
                        break;
                    }
                }
                if (hasCommentAfterEq || containsLineBreak(eqEnd, exprStart)) {
                    return buildAssignmentLikeStatement(stmt, topExpression, eqEnd);
                }
            }
        }
        // Multi-line expressions at statement level — decompose for CONTINUATION indent.
        if (topExpression != null && containsLineBreak(topExpression.getStartPosition(), topExpression.getStartPosition() + topExpression.getLength())) {
            if (topExpression instanceof InfixExpression
                    || topExpression instanceof ConditionalExpression
                    || topExpression instanceof ClassInstanceCreation
                    || topExpression instanceof MethodInvocation
                    || topExpression instanceof ExpressionMethodReference
                    || topExpression instanceof Assignment
                    || topExpression instanceof LambdaExpression
                    || topExpression instanceof InstanceofExpression
                    || topExpression instanceof CastExpression
                    || topExpression instanceof ParenthesizedExpression
                    || topExpression instanceof ArrayInitializer
                    || topExpression instanceof ArrayCreation) {
                return buildExpressionStatement(stmt, topExpression);
            }
        }
        // Default: if the statement has a top-level expression followed by
        // trailing tokens with a line-broken comment, route through
        // buildExpressionStatement so the CONTINUATION wrap applies.
        if (topExpression != null) {
            int exprEnd = topExpression.getStartPosition() + topExpression.getLength();
            if (exprEnd < end) {
                for (JavaTokens.Token tok : tokensIn(exprEnd, end)) {
                    if (tok.isComment() && containsLineBreak(exprEnd, tok.start())) {
                        return buildExpressionStatement(stmt, topExpression);
                    }
                }
            }
        }
        return JavaBlock.builder(start, end, "Statement")
                .indent(Indent.normalIndent())
                .child(owner.buildTokensRange(start, end, "StatementTokens"))
                .build();
    }

    private static boolean isTextBlockPrefixStatement(ASTNode stmt)
    {
        return switch (stmt) {
            case VariableDeclarationStatement _ -> true;
            default -> false;
        };
    }

    /// Build a statement with an assignment-like prefix and a separate RHS.
    /// Whitespace and comments between `=` and the expression belong to the
    /// RHS wrapper, so blank lines collapse and the expression gets
    /// CONTINUATION indent structurally.
    private Block buildAssignmentLikeStatement(ASTNode stmt, Expression expr, int eqEnd)
    {
        int stmtStart = stmt.getStartPosition();
        int stmtEnd = stmtStart + stmt.getLength();
        int exprStart = expr.getStartPosition();
        int exprEnd = exprStart + expr.getLength();
        JavaBlock.Builder statement = JavaBlock.builder(stmtStart, stmtEnd, "AssignStatement")
                .indent(Indent.normalIndent());
        // Prefix: up to and including `=`.
        Block prefix = owner.buildTokensRange(stmtStart, eqEnd, "AssignPrefix");
        statement.child(prefix);

        Block exprBlock = owner.buildExpressionBlock(expr, exprStart, exprEnd, "AssignExpr");
        Block exprWrapped = owner.buildIndentedBodyWrapper(
                "AssignExprWrap",
                eqEnd,
                exprStart,
                exprEnd,
                exprBlock,
                Indent.continuationIndent());
        addSibling(statement, prefix, exprWrapped, Spacing.createSpacing(1, 1, 0, true, 0));
        // Trailing `;`.
        if (exprEnd < stmtEnd) {
            Block trailing = owner.buildTokensRange(exprEnd, stmtEnd, "AssignTrailing");
            addSibling(statement, exprWrapped, trailing, Spacing.none());
        }
        return statement.build();
    }

    /// Build a statement whose top-level expression is a lambda with a block
    /// body. The lambda's block body is decomposed via buildStatementBlock so
    /// its statements get NORMAL indent.
    private Block buildLambdaStatement(ASTNode stmt, LambdaExpression lambda)
    {
        int stmtStart = stmt.getStartPosition();
        int stmtEnd = stmtStart + stmt.getLength();
        org.eclipse.jdt.core.dom.Block body = (org.eclipse.jdt.core.dom.Block) lambda.getBody();
        int lambdaStart = lambda.getStartPosition();
        int bodyStart = body.getStartPosition();
        int bodyEnd = bodyStart + body.getLength();

        JavaBlock.Builder statement = JavaBlock.builder(stmtStart, stmtEnd, "LambdaStatement")
                .indent(Indent.normalIndent());
        Block prev = null;

        // Statement prefix (e.g. `Runnable r = `).
        if (lambdaStart > stmtStart) {
            Block prefix = owner.buildTokensRange(stmtStart, lambdaStart, "StatementPrefix");
            statement.child(prefix);
            prev = prefix;
        }

        // Lambda params + arrow: from lambdaStart to bodyStart.
        Block paramsAndArrow = owner.buildTokensRange(lambdaStart, bodyStart, "LambdaHeader");
        if (prev != null) {
            statement.spacing(prev, paramsAndArrow, JavaSpacingRules.keepLineOrSpace());
        }
        statement.child(paramsAndArrow);
        Block prev2 = paramsAndArrow;

        // Body as a statement block.
        Block bodyBlock = buildStatementBlock(body);
        // LAMBDA_BRACE_STYLE=END_OF_LINE for IntelliJ default; Airlift uses
        // a space between the arrow and the `{`.
        statement.spacing(prev2, bodyBlock, Spacing.oneSpace());
        statement.child(bodyBlock);
        prev2 = bodyBlock;

        // Trailing `;`.
        if (bodyEnd < stmtEnd) {
            Block trailing = owner.buildTokensRange(bodyEnd, stmtEnd, "StatementTrailing");
            statement.spacing(prev2, trailing, Spacing.none());
            statement.child(trailing);
        }
        return statement.build();
    }

    /// Return the direct expression of a statement if the statement is a
    /// simple expression-bearing kind. Used to detect chains.
    private static Expression topLevelChainExpression(ASTNode stmt)
    {
        if (stmt instanceof ExpressionStatement expr) {
            return expr.getExpression();
        }
        if (stmt instanceof ReturnStatement ret) {
            return ret.getExpression();
        }
        if (stmt instanceof ThrowStatement thr) {
            return thr.getExpression();
        }
        if (stmt instanceof YieldStatement ys) {
            return ys.getExpression();
        }
        if (stmt instanceof VariableDeclarationStatement vds && vds.fragments().size() == 1) {
            Object f = vds.fragments().getFirst();
            if (f instanceof VariableDeclarationFragment vdf) {
                return vdf.getInitializer();
            }
        }
        return null;
    }

    /// Build a statement whose top-level expression is a text block (Airlift
    /// style: text block always starts on its own line at CONTINUATION indent).
    /// Structure: prefix (`int sql =`) + forced line break + text block at
    /// CONTINUATION + trailing (`;`).
    private Block buildTextBlockGeneric(ASTNode stmt, int exprStart, int exprEnd)
    {
        int stmtStart = stmt.getStartPosition();
        int stmtEnd = stmtStart + stmt.getLength();
        return owner.buildTextBlockRhsBlock(stmtStart, stmtEnd, exprStart, exprEnd, "TextBlockStatement", Indent.normalIndent());
    }

    private Block buildExpressionStatement(ASTNode stmt, Expression expr)
    {
        int stmtStart = stmt.getStartPosition();
        int stmtEnd = stmtStart + stmt.getLength();
        int exprStart = expr.getStartPosition();
        int exprEnd = exprStart + expr.getLength();
        JavaBlock.Builder statement = JavaBlock.builder(stmtStart, stmtEnd, "ExpressionStatement")
                .indent(Indent.normalIndent());
        Block prev = null;
        if (exprStart > stmtStart) {
            Block prefix = owner.buildTokensRange(stmtStart, exprStart, "ExprStmtPrefix");
            statement.child(prefix);
            prev = prefix;
        }
        Block exprBlock = owner.buildExpressionBlock(expr, exprStart, exprEnd, "Expr");
        // When the expression sits on a new line after the statement prefix
        // (e.g. `double x =\n   expr` or `return\n   expr`), wrap it in
        // CONTINUATION so the first line lands at stmtCol+8 and operand
        // wraps inside cascade another +8 beyond.
        if (prev != null && containsLineBreak(stmtStart, exprStart)) {
            exprBlock = JavaBlock.continuationWrap(exprStart, exprEnd, exprBlock, "ExprContWrap");
        }
        if (prev != null) {
            addSibling(statement, prev, exprBlock, JavaSpacingRules.keepLineOrSpace());
        }
        else {
            statement.child(exprBlock);
        }
        prev = exprBlock;
        if (exprEnd < stmtEnd) {
            // If the trailing has a comment preceded by a line break, wrap
            // it in CONTINUATION so the comment lands at +8.
            boolean hasLineBreakedComment = false;
            for (JavaTokens.Token tok : tokensIn(exprEnd, stmtEnd)) {
                if (tok.isComment() && containsLineBreak(exprEnd, tok.start())) {
                    hasLineBreakedComment = true;
                    break;
                }
            }
            if (hasLineBreakedComment) {
                Block trailing = owner.buildTokensRange(exprEnd, stmtEnd, "ExprStmtTrailing", false);
                Block trailingWrapped = JavaBlock.continuationWrap(exprEnd, stmtEnd, trailing, "ExprStmtTrailingWrap");
                addSibling(statement, prev, trailingWrapped, Spacing.none());
            }
            else {
                Block trailing = owner.buildTokensRange(exprEnd, stmtEnd, "ExprStmtTrailing");
                addSibling(statement, prev, trailing, Spacing.none());
            }
        }
        return statement.build();
    }

    /// Build a statement whose top-level expression is a method invocation or
    /// constructor call with a wrapped argument list. The statement is
    /// decomposed as: prefix + call-with-args + trailing.
    private Block buildCallExpressionStatement(ASTNode stmt, Expression callExpr, List<?> arguments)
    {
        int stmtStart = stmt.getStartPosition();
        int stmtEnd = stmtStart + stmt.getLength();
        // For super()/this() constructor calls, callExpr is null — the
        // statement itself is the call.
        int exprStart = callExpr != null ? callExpr.getStartPosition() : stmtStart;
        int exprEnd = callExpr != null ? exprStart + callExpr.getLength() : stmtEnd;

        JavaBlock.Builder statement = JavaBlock.builder(stmtStart, stmtEnd, "CallExpressionStatement")
                .indent(Indent.normalIndent());
        Block prev = null;

        // Statement prefix before the expression (e.g. `return `, `int x = `).
        if (exprStart > stmtStart) {
            Block prefix = owner.buildTokensRange(stmtStart, exprStart, "StmtPrefix");
            statement.child(prefix);
            prev = prefix;
        }

        Block callBlock = owner.buildCallExpression(callExpr, arguments, exprStart, exprEnd);
        if (prev != null) {
            addSibling(statement, prev, callBlock, JavaSpacingRules.keepLineOrSpace());
        }
        else {
            statement.child(callBlock);
        }
        prev = callBlock;

        // Trailing tokens after the expression (e.g. `;`). If the trailing
        // region contains a comment on its own line (`return x\n // note\n ;`),
        // wrap it in CONTINUATION so the comment lands at +8 from the
        // statement's line start.
        if (exprEnd < stmtEnd) {
            Block trailing = owner.buildTokensRange(exprEnd, stmtEnd, "StmtTrailing");
            boolean hasLineBreakedComment = false;
            for (JavaTokens.Token tok : tokensIn(exprEnd, stmtEnd)) {
                if (tok.isComment() && containsLineBreak(exprEnd, tok.start())) {
                    hasLineBreakedComment = true;
                    break;
                }
            }
            if (hasLineBreakedComment) {
                Block trailingWrapped = JavaBlock.builder(exprEnd, stmtEnd, "StmtTrailingWrap")
                        .indent(Indent.continuationIndent())
                        .child(trailing)
                        .build();
                addSibling(statement, prev, trailingWrapped, Spacing.none());
            }
            else {
                addSibling(statement, prev, trailing, Spacing.none());
            }
        }
        return statement.build();
    }

    private Block buildTryStatement(TryStatement node)
    {
        int start = node.getStartPosition();
        int end = start + node.getLength();
        JavaBlock.Builder composite = JavaBlock.builder(start, end, "TryStatement")
                .indent(Indent.normalIndent());

        int tryBodyStart = node.getBody().getStartPosition();
        // Try-with-resources: decompose each resource so chained calls in
        // initializers get CONTINUATION indent. Otherwise plain `try {` body —
        // flat-tokenize the header.
        Block tryHeader;
        if (!node.resources().isEmpty()) {
            tryHeader = buildTryResourcesHeader(node, start, tryBodyStart);
        }
        else {
            tryHeader = owner.buildTokensRange(start, tryBodyStart, "TryHeader");
        }
        composite.child(tryHeader);
        Block tryBody = buildStatementBlock(node.getBody());
        addSibling(composite, tryHeader, tryBody, JavaConstructSpacing.sameLineBeforeBlock());
        Block prev = tryBody;

        for (Object clauseObj : node.catchClauses()) {
            CatchClause clause = (CatchClause) clauseObj;
            int clauseStart = clause.getStartPosition();
            int clauseEnd = clauseStart + clause.getLength();
            // Preserve comments between the previous block's `}` and the
            // `catch` keyword (e.g. `} // comment\ncatch ...`).
            prev = owner.emitInterBlockComments(composite, prev, prev.endOffset(), clauseStart, Indent.noneIndent());
            Block clauseBlock = buildCatchClause(clause, clauseStart, clauseEnd);
            addSibling(composite, prev, clauseBlock, JavaConstructSpacing.nextLineKeepingOneBlank());
            prev = clauseBlock;
        }

        if (node.getFinally() != null) {
            int finallyStart = owner.findKeyword(prev.endOffset(), end, "finally");
            if (finallyStart < 0) {
                finallyStart = node.getFinally().getStartPosition();
            }
            prev = owner.emitInterBlockComments(composite, prev, prev.endOffset(), finallyStart, Indent.noneIndent());
            Block finallyBlock = buildFinallyBlock(node.getFinally(), finallyStart);
            addSibling(composite, prev, finallyBlock, JavaConstructSpacing.nextLineKeepingOneBlank());
        }
        return composite.build();
    }

    /// Build the header of a try-with-resources statement up to the body
    /// brace: `try (` + per-resource block (CONTINUATION) + `)`. Each resource
    /// may itself contain a method chain that needs CONTINUATION on its
    /// selectors.
    private Block buildTryResourcesHeader(TryStatement node, int start, int headerEnd)
    {
        List<?> resources = node.resources();
        ASTNode firstRes = (ASTNode) resources.getFirst();
        int lparen = -1;
        for (int i = firstRes.getStartPosition() - 1; i >= start; i--) {
            if (source.charAt(i) == '(') {
                lparen = i;
                break;
            }
        }
        if (lparen < 0) {
            return owner.buildTokensRange(start, headerEnd, "TryHeader");
        }
        int rparen = owner.findMatchingRParen(lparen, headerEnd);
        if (rparen < 0) {
            return owner.buildTokensRange(start, headerEnd, "TryHeader");
        }
        JavaBlock.Builder header = JavaBlock.builder(start, headerEnd, "TryResourcesHeader");
        Block prefix = owner.buildTokensRange(start, lparen + 1, "TryPrefix");
        header.child(prefix);
        Block prev = prefix;
        for (int i = 0; i < resources.size(); i++) {
            ASTNode res = (ASTNode) resources.get(i);
            int resStart = res.getStartPosition();
            int resEnd;
            if (i + 1 < resources.size()) {
                ASTNode next = (ASTNode) resources.get(i + 1);
                int nextStart = next.getStartPosition();
                int leadingCommentBeforeNext = nextStart;
                for (JavaTokens.Token tok : tokensIn(resStart + res.getLength(), nextStart)) {
                    if (tok.isComment() && containsLineBreak(resStart + res.getLength(), tok.start())) {
                        leadingCommentBeforeNext = tok.start();
                        break;
                    }
                }
                int sepEnd = owner.lastNonWhitespaceBefore(leadingCommentBeforeNext, resStart);
                resEnd = sepEnd > 0 ? sepEnd : resStart + res.getLength();
            }
            else {
                resEnd = owner.extendThroughTrailingInlineComments(resStart + res.getLength(), rparen);
            }
            // For VariableDeclarationExpression with a single fragment whose
            // initializer is an Expression, recurse for chain decomposition.
            Block resTokens;
            if (res instanceof VariableDeclarationExpression vde
                    && vde.fragments().size() == 1
                    && ((VariableDeclarationFragment) vde.fragments().getFirst()).getInitializer() != null) {
                VariableDeclarationFragment frag = (VariableDeclarationFragment) vde.fragments().getFirst();
                Expression init = frag.getInitializer();
                int initStart = init.getStartPosition();
                int initEnd = initStart + init.getLength();
                JavaBlock.Builder rb = JavaBlock.builder(resStart, resEnd, "Resource" + i);
                Block prePart = owner.buildTokensRange(resStart, initStart, "ResPrefix" + i);
                rb.child(prePart);
                Block initBlock = owner.buildExpressionBlock(init, initStart, initEnd, "ResInit" + i);
                addSibling(rb, prePart, initBlock, JavaSpacingRules.keepLineOrSpace());
                if (initEnd < resEnd) {
                    Block tailPart = owner.buildTokensRange(initEnd, resEnd, "ResTail" + i);
                    addSibling(rb, initBlock, tailPart, Spacing.none());
                }
                resTokens = rb.build();
            }
            else {
                resTokens = owner.buildTokensRange(resStart, resEnd, "Resource" + i);
            }
            // Source-shape first-resource placement: if source has a line
            // break between `(` and the first resource, place it on its own
            // line with CONTINUATION indent; otherwise keep it inline.
            boolean firstResOnOwnLine = (i == 0) && containsLineBreak(lparen + 1, resStart);
            Indent resIndent;
            if (i == 0) {
                resIndent = firstResOnOwnLine ? Indent.continuationIndent() : Indent.noneIndent();
            }
            else {
                resIndent = Indent.continuationIndent();
            }
            int wrapperStart = i == 0
                    ? owner.firstCommentStart(lparen + 1, resStart)
                    : owner.firstCommentStart(prev.endOffset(), resStart);
            Block wrapped = owner.buildIndentedBodyWrapper("ResWrap" + i, wrapperStart, resStart, resEnd, resTokens, resIndent);
            Spacing sp;
            if (i == 0 && firstResOnOwnLine) {
                sp = Spacing.createSpacing(0, 0, 1, false, 0);
            }
            else if (i == 0) {
                sp = Spacing.createSpacing(0, 0, 0, false, 0);
            }
            else {
                sp = Spacing.createSpacing(1, 1, 0, true, 0);
            }
            addSibling(header, prev, wrapped, sp);
            prev = wrapped;
        }
        Block rparenBlock = owner.buildTokensRange(rparen, rparen + 1, ")");
        addSibling(header, prev, rparenBlock, Spacing.createSpacing(0, 0, 0, false, 0));
        if (rparen + 1 < headerEnd) {
            Block trailing = owner.buildTokensRange(rparen + 1, headerEnd, "TryHeaderTrailing");
            addSibling(header, rparenBlock, trailing, Spacing.none());
        }
        return header.build();
    }

    private Block buildCatchClause(CatchClause clause, int clauseStart, int clauseEnd)
    {
        int bodyStart = clause.getBody().getStartPosition();
        JavaBlock.Builder catchBlock = JavaBlock.builder(clauseStart, clauseEnd, "CatchClause");
        Block header = buildCatchHeader(clauseStart, bodyStart);
        catchBlock.child(header);
        Block body = buildStatementBlock(clause.getBody(), true);
        addSibling(catchBlock, header, body, JavaConstructSpacing.sameLineBeforeBlock());
        return catchBlock.build();
    }

    private Block buildCatchHeader(int clauseStart, int bodyStart)
    {
        int firstWrappedTokenStart = sourceContext.firstTokenAfterLineBreak(clauseStart, bodyStart);
        if (firstWrappedTokenStart < 0) {
            return owner.buildTokensRange(clauseStart, bodyStart, "CatchHeader");
        }

        JavaBlock.Builder header = JavaBlock.builder(clauseStart, bodyStart, "CatchHeader");
        Block prefix = owner.buildTokensRange(clauseStart, firstWrappedTokenStart, "CatchHeaderPrefix");
        header.child(prefix);
        Block continuation = owner.buildTokensRangeWithLineStartIndent(
                firstWrappedTokenStart,
                bodyStart,
                "CatchHeaderContinuation",
                Indent.continuationIndent());
        addSibling(header, prefix, continuation, Spacing.createSpacing(0, 0, 1, true, 0));
        return header.build();
    }

    private Block buildFinallyBlock(org.eclipse.jdt.core.dom.Block finallyBody, int finallyKeywordStart)
    {
        int bodyStart = finallyBody.getStartPosition();
        int bodyEnd = bodyStart + finallyBody.getLength();
        JavaBlock.Builder finallyBlock = JavaBlock.builder(finallyKeywordStart, bodyEnd, "FinallyClause");
        Block keyword = owner.buildTokensRange(finallyKeywordStart, bodyStart, "finally");
        finallyBlock.child(keyword);
        Block body = buildStatementBlock(finallyBody, true);
        addSibling(finallyBlock, keyword, body, JavaConstructSpacing.sameLineBeforeBlock());
        return finallyBlock.build();
    }

    /// Decompose a SwitchStatement into header + `{` + case-group blocks + `}`.
    /// Each case group (`case X:` or `default:` + its body statements) is
    /// a composite with NORMAL indent relative to the switch body. The body
    /// statements within a group then get their own NORMAL indent, yielding
    /// 2x NORMAL total from the `switch` keyword — matching Airlift's
    /// INDENT_CASE_FROM_SWITCH=true convention.
    private Block buildSwitchStatement(SwitchStatement node)
    {
        int start = node.getStartPosition();
        int end = start + node.getLength();
        JavaBlock.Builder composite = JavaBlock.builder(start, end, "SwitchStatement")
                .indent(Indent.normalIndent());
        int openBrace = owner.firstChar(start, end, '{');
        int closeBrace = owner.lastChar(start, end, '}');
        if (openBrace < 0 || closeBrace < 0) {
            composite.child(owner.buildTokensRange(start, end, "SwitchTokens"));
            return composite.build();
        }
        Block header = owner.buildTokensRange(start, openBrace, "SwitchHeader");
        composite.child(header);
        Block openBraceBlock = owner.buildTokensRange(openBrace, openBrace + 1, "{");
        // END_OF_LINE brace style for switch: `switch (x) {` same line.
        addSibling(composite, header, openBraceBlock, Spacing.createSpacing(1, 1, 0, false, 0));
        Block prev = openBraceBlock;

        // Group statements into case groups: a SwitchCase followed by the
        // statements until the next SwitchCase.
        List<Object> statements = node.statements();
        List<List<Statement>> groups = new ArrayList<>();
        List<Statement> current = null;
        for (Object stmtObj : statements) {
            Statement stmt = (Statement) stmtObj;
            if (stmt instanceof SwitchCase) {
                if (current != null) {
                    groups.add(current);
                }
                current = new ArrayList<>();
                current.add(stmt);
            }
            else if (current != null) {
                current.add(stmt);
            }
        }
        if (current != null) {
            groups.add(current);
        }

        int groupScanStart = openBrace + 1;
        for (List<Statement> group : groups) {
            int groupStart = group.getFirst().getStartPosition();
            // Emit inter-group comments. If the FIRST comment is on the same
            // line as the previous group's last line (trailing comment like
            // `case 1 -> "one"; // first case`), attach it inline.
            boolean hasInlineFirstComment = false;
            for (JavaTokens.Token tok : tokensIn(groupScanStart, groupStart)) {
                if (tok.isComment()) {
                    hasInlineFirstComment = !containsLineBreak(groupScanStart, tok.start());
                    break;
                }
            }
            if (hasInlineFirstComment) {
                prev = owner.emitInterBlockCommentsArrowInline(composite, prev, groupScanStart, groupStart);
            }
            else {
                prev = emitSwitchInterGroupComments(composite, prev, groupScanStart, groupStart, groupBefore(groups, group));
            }
            Block groupBlock = buildSwitchCaseGroup(group);
            addSibling(composite, prev, groupBlock, Spacing.createSpacing(0, 0, 1, true, 1));
            prev = groupBlock;
            Statement last = group.getLast();
            groupScanStart = last.getStartPosition() + last.getLength();
        }
        // Trailing comments between the last group and `}`. Attach the
        // first comment inline if on the same line as the last group.
        boolean trailingFirstInline = false;
        for (JavaTokens.Token tok : tokensIn(groupScanStart, closeBrace)) {
            if (tok.isComment()) {
                trailingFirstInline = !containsLineBreak(groupScanStart, tok.start());
                break;
            }
        }
        if (trailingFirstInline) {
            prev = owner.emitInterBlockCommentsArrowInline(composite, prev, groupScanStart, closeBrace);
        }
        else {
            prev = emitSwitchInterGroupComments(composite, prev, groupScanStart, closeBrace, groups.isEmpty() ? null : groups.getLast());
        }

        Block closeBraceBlock = owner.buildTokensRange(closeBrace, closeBrace + 1, "}");
        addSibling(composite, prev, closeBraceBlock, Spacing.createSpacing(0, 0, 1, true, 0));
        return composite.build();
    }

    /// A statement whose top-level expression is a [SwitchExpression].
    /// The statement's prefix (e.g. `return`), then the switch (decomposed
    /// into header + `{` + case groups + `}`), then the trailing semicolon.
    private Block buildSwitchExpressionStatement(ASTNode stmt, SwitchExpression switchExpr)
    {
        int stmtStart = stmt.getStartPosition();
        int stmtEnd = stmtStart + stmt.getLength();
        int switchStart = switchExpr.getStartPosition();
        int switchEnd = switchStart + switchExpr.getLength();

        JavaBlock.Builder statement = JavaBlock.builder(stmtStart, stmtEnd, "SwitchExpressionStatement")
                .indent(Indent.normalIndent());
        Block prev = null;

        if (switchStart > stmtStart) {
            Block prefix = owner.buildTokensRange(stmtStart, switchStart, "SwitchExpressionPrefix");
            statement.child(prefix);
            prev = prefix;
        }

        Block switchBlock = buildSwitchExpression(switchExpr, switchStart, switchEnd);
        if (prev != null) {
            addSibling(statement, prev, switchBlock, JavaSpacingRules.keepLineOrSpace());
        }
        else {
            statement.child(switchBlock);
        }
        prev = switchBlock;

        if (switchEnd < stmtEnd) {
            Block trailing = owner.buildTokensRange(switchEnd, stmtEnd, "SwitchExpressionTrailing");
            addSibling(statement, prev, trailing, Spacing.none());
        }
        return statement.build();
    }

    /// Switch expression: `switch (expr) { case ... -> ...; case ... -> ...; }`.
    /// Shares the case-group decomposition logic with SwitchStatement so case
    /// bodies carry 2x NORMAL indent.
    Block buildSwitchExpression(SwitchExpression node, int start, int end)
    {
        JavaBlock.Builder composite = JavaBlock.builder(start, end, "SwitchExpression");
        int openBrace = owner.firstChar(start, end, '{');
        int closeBrace = owner.lastChar(start, end, '}');
        if (openBrace < 0 || closeBrace < 0) {
            composite.child(owner.buildTokensRange(start, end, "SwitchExprTokens"));
            return composite.build();
        }
        Block header = owner.buildTokensRange(start, openBrace, "SwitchExprHeader");
        composite.child(header);
        Block openBraceBlock = owner.buildTokensRange(openBrace, openBrace + 1, "{");
        addSibling(composite, header, openBraceBlock, Spacing.createSpacing(1, 1, 0, false, 0));
        Block prev = openBraceBlock;

        List<Object> statements = node.statements();
        List<List<Statement>> groups = new ArrayList<>();
        List<Statement> current = null;
        for (Object stmtObj : statements) {
            Statement stmt = (Statement) stmtObj;
            if (stmt instanceof SwitchCase) {
                if (current != null) {
                    groups.add(current);
                }
                current = new ArrayList<>();
                current.add(stmt);
            }
            else if (current != null) {
                current.add(stmt);
            }
        }
        if (current != null) {
            groups.add(current);
        }

        int groupScanStart = openBrace + 1;
        for (List<Statement> group : groups) {
            int groupStart = group.getFirst().getStartPosition();
            boolean hasInlineFirstComment = false;
            for (JavaTokens.Token tok : tokensIn(groupScanStart, groupStart)) {
                if (tok.isComment()) {
                    hasInlineFirstComment = !containsLineBreak(groupScanStart, tok.start());
                    break;
                }
            }
            if (hasInlineFirstComment) {
                prev = owner.emitInterBlockCommentsArrowInline(composite, prev, groupScanStart, groupStart);
            }
            else {
                prev = emitSwitchInterGroupComments(composite, prev, groupScanStart, groupStart, groupBefore(groups, group));
            }
            Block groupBlock = buildSwitchCaseGroup(group);
            addSibling(composite, prev, groupBlock, Spacing.createSpacing(0, 0, 1, true, 1));
            prev = groupBlock;
            Statement last = group.getLast();
            groupScanStart = last.getStartPosition() + last.getLength();
        }
        boolean trailingFirstInline = false;
        for (JavaTokens.Token tok : tokensIn(groupScanStart, closeBrace)) {
            if (tok.isComment()) {
                trailingFirstInline = !containsLineBreak(groupScanStart, tok.start());
                break;
            }
        }
        if (trailingFirstInline) {
            prev = owner.emitInterBlockCommentsArrowInline(composite, prev, groupScanStart, closeBrace);
        }
        else {
            prev = emitSwitchInterGroupComments(composite, prev, groupScanStart, closeBrace, groups.isEmpty() ? null : groups.getLast());
        }

        Block closeBraceBlock = owner.buildTokensRange(closeBrace, closeBrace + 1, "}");
        addSibling(composite, prev, closeBraceBlock, Spacing.createSpacing(0, 0, 1, true, 0));
        if (closeBrace + 1 < end) {
            Block trailing = owner.buildTokensRange(closeBrace + 1, end, "SwitchExprTrailing");
            addSibling(composite, closeBraceBlock, trailing, Spacing.none());
        }
        return composite.build();
    }

    private static List<Statement> groupBefore(List<List<Statement>> groups, List<Statement> group)
    {
        int index = groups.indexOf(group);
        if (index <= 0) {
            return null;
        }
        return groups.get(index - 1);
    }

    private Block emitSwitchInterGroupComments(JavaBlock.Builder composite, Block prev, int gapStart, int gapEnd, List<Statement> previousGroup)
    {
        boolean previousClassicLabelOnlyGroup = previousGroup != null
                && previousGroup.stream().allMatch(SwitchCase.class::isInstance)
                && previousGroup.stream()
                .map(SwitchCase.class::cast)
                .noneMatch(SwitchCase::isSwitchLabeledRule);
        for (JavaTokens.Token tok : tokensIn(gapStart, gapEnd)) {
            if (!tok.isComment()) {
                continue;
            }
            int cEnd = tok.text().endsWith("\n") ? tok.end() - 1 : tok.end();
            Indent defaultIndent = (previousClassicLabelOnlyGroup || isFallThroughComment(tok))
                    ? Indent.continuationIndent()
                    : Indent.normalIndent();
            Indent ind = owner.commentIndent(tok, defaultIndent);
            Block commentBlock = JavaBlock.builder(tok.start(), cEnd, "SwitchInterGroupComment")
                    .indent(ind)
                    .child(JavaBlockBuilder.commentLeaf(tok))
                    .build();
            addSibling(composite, prev, commentBlock, Spacing.createSpacing(0, 0, 1, true, 1));
            prev = commentBlock;
        }
        return prev;
    }

    private static boolean isFallThroughComment(JavaTokens.Token tok)
    {
        if (tok.type() != ITerminalSymbols.TokenNameCOMMENT_LINE) {
            return false;
        }
        String text = tok.text().toLowerCase(Locale.ROOT);
        return text.contains("fall through") || text.contains("fallthrough");
    }

    /// Build a switch case label block. If the label contains a guarded
    /// pattern (`case X when expr`) whose guard expression is multi-line,
    /// model `when` and the guard as case-label-list children. This mirrors
    /// IntelliJ's CASE_LABEL_ELEMENT_LIST handling: `when` is spacing inside
    /// the case label, not a separate continuation-indent group.
    private Block buildSwitchCaseLabel(SwitchCase caseLabel, int labelStart, int labelEnd)
    {
        // Find the `when` keyword only when JDT identified it as a guarded
        // pattern keyword. A plain identifier named `when` in a case label
        // must stay inside the case label element list.
        int whenOffset = owner.findKeyword(labelStart, labelEnd, "when", false);
        if (whenOffset >= 0 && !switchGuardWhens.contains(whenOffset)) {
            whenOffset = -1;
        }
        if (whenOffset < 0) {
            // Multi-pattern case wrapped across lines: align each pattern
            // after the first to the first pattern's column
            // (= labelStart + "case ".length()). JDT models this in two
            // shapes:
            //   1. `case Foo x, Bar y ->` — SwitchCase.expressions() has a
            //      single EitherOrMultiPattern child; its .patterns() is
            //      the list to align.
            //   2. `case Kind.BOOL, Kind.BOOLEAN ->` — SwitchCase.expressions()
            //      has the enum constants directly (QualifiedName / Name).
            // Mirrors IntelliJ's processSwitchExpression which aligns
            // CASE_LABEL_ELEMENT_LIST children via AlignmentStrategy.wrap(...,
            // JavaTokenType.COMMA).
            if (!caseLabel.expressions().isEmpty()
                    && caseLabel.expressions().getFirst() instanceof EitherOrMultiPattern multi
                    && multi.patterns().size() >= 2
                    && containsLineBreakBetweenPatterns(multi.patterns())) {
                return buildAlignedMultiPatternCaseLabel(multi.patterns(), labelStart, labelEnd);
            }
            if (caseLabel.expressions().size() >= 2
                    && containsLineBreakBetweenPatterns(caseLabel.expressions())) {
                return buildAlignedMultiPatternCaseLabel(caseLabel.expressions(), labelStart, labelEnd);
            }
            // Multi-line case label (e.g. record pattern `case Pair(\n var
            // left,\n var right)`) — decompose the parenthesized portion so
            // components get CONTINUATION indent.
            if (containsLineBreak(labelStart, labelEnd)) {
                return buildWrappedCaseLabel(labelStart, labelEnd);
            }
            return flatSwitchCaseLabel(labelStart, labelEnd);
        }
        // The guard expression starts after `when ` and runs to the end of
        // the case label. If it's multi-line, decompose; else flat.
        int guardStart = whenOffset + "when".length();
        // Skip whitespace.
        while (guardStart < labelEnd && guardStart < source.length()
                && Character.isWhitespace(source.charAt(guardStart))) {
            guardStart++;
        }
        if (guardStart >= labelEnd || !containsLineBreak(guardStart, labelEnd)) {
            return flatSwitchCaseLabel(labelStart, labelEnd);
        }
        // Locate the guard Expression by scanning the SwitchCase's
        // expressions(); JDT 21+ wraps `pattern when expr` in a
        // GuardedPattern node — we look for an Expression whose source
        // range starts at guardStart.
        Expression guardExpr = null;
        for (Object e : caseLabel.expressions()) {
            ASTNode n = (ASTNode) e;
            // GuardedPattern has its own structure; search recursively
            // for the guard Expression that starts at our guardStart.
            Expression candidate = findExpressionAt(n, guardStart);
            if (candidate != null) {
                guardExpr = candidate;
                break;
            }
        }
        int whenEnd = whenOffset + "when".length();
        boolean guardStartsAfterWhenLineBreak = containsLineBreak(whenEnd, guardStart);
        JavaBlock.Builder lb = JavaBlock.builder(labelStart, labelEnd, "SwitchCaseLabelGuard");
        Block prefix = owner.buildTokensRange(labelStart, whenOffset, "CaseLabelPrefix");
        lb.child(prefix);
        Block whenKwBlock = JavaBlock.builder(whenOffset, guardStart, "CaseWhenKwWrap")
                .indent(Indent.continuationIndent())
                .canUseFirstChildIndent(false)
                .child(owner.buildTokensRange(whenOffset, guardStart, "CaseWhenKw"))
                .build();
        Block guardContent;
        if (guardExpr instanceof InfixExpression infix) {
            guardContent = owner.buildInfixExpression(
                    infix,
                    guardStart,
                    labelEnd,
                    Indent.continuationIndent());
        }
        else {
            guardContent = owner.buildTokensRange(guardStart, labelEnd, "CaseWhenGuard");
        }
        if (guardStartsAfterWhenLineBreak) {
            guardContent = JavaBlock.builder(guardStart, labelEnd, "CaseWhenGuardWrap")
                    .indent(Indent.continuationIndent())
                    .canUseFirstChildIndent(false)
                    .child(guardContent)
                    .build();
        }
        addSibling(lb, prefix, whenKwBlock, JavaSpacingRules.keepLineOrSpace());
        addSibling(lb, whenKwBlock, guardContent, JavaSpacingRules.keepLineOrSpace());
        return lb.build();
    }

    private boolean containsLineBreakBetweenPatterns(List<?> patterns)
    {
        if (patterns.size() < 2) {
            return false;
        }
        ASTNode first = (ASTNode) patterns.getFirst();
        ASTNode last = (ASTNode) patterns.getLast();
        int firstStart = first.getStartPosition();
        int lastEnd = last.getStartPosition() + last.getLength();
        return containsLineBreak(firstStart, lastEnd);
    }

    /// Build a multi-pattern case label where patterns wrap across lines.
    /// Align each pattern after the first to the first pattern's column,
    /// which sits at `labelStart + "case ".length()`. Mirrors IntelliJ's
    /// `AbstractJavaBlock.processSwitchExpression`, which wraps
    /// `CASE_LABEL_ELEMENT_LIST` children with
    /// `AlignmentStrategy.wrap(createAlignment(true, null), JavaTokenType.COMMA)`.
    private Block buildAlignedMultiPatternCaseLabel(List<?> patterns, int labelStart, int labelEnd)
    {
        JavaBlock.Builder composite = JavaBlock.builder(labelStart, labelEnd, "SwitchCaseMultiPattern");
        // Find the `case` keyword end and the first pattern's start.
        ASTNode first = (ASTNode) patterns.getFirst();
        int firstPatternStart = first.getStartPosition();
        // Emit `case ` as prefix (without per-pattern alignment — its column
        // comes from the enclosing switch body).
        Block prefix = owner.buildTokensRange(labelStart, firstPatternStart, "CasePrefix");
        composite.child(prefix);
        Block prev = prefix;

        // Align each pattern after the first to the first-pattern column.
        // Using relativeSpaceIndent (IntelliJ's Indent.getSpaceIndent(n, true)):
        // the child lands at composite_column + prefix_width rather than at
        // an absolute column. This tracks changes to the composite column
        // if an outer fix shifts the case label (no absolute pins).
        int prefixWidth = firstPatternStart - labelStart;
        int prevEnd = firstPatternStart;
        for (int i = 0; i < patterns.size(); i++) {
            ASTNode pat = (ASTNode) patterns.get(i);
            int pStart = pat.getStartPosition();
            int pEnd = pStart + pat.getLength();
            // Extend to include trailing comma.
            if (i < patterns.size() - 1) {
                ASTNode next = (ASTNode) patterns.get(i + 1);
                int nextStart = next.getStartPosition();
                for (int p = pEnd; p < nextStart && p < labelEnd; p++) {
                    if (source.charAt(p) == ',') {
                        pEnd = p + 1;
                        break;
                    }
                }
            }
            else {
                // Last pattern: extend to labelEnd to capture the `->`/`:`.
                pEnd = labelEnd;
            }
            // Emit any comments between this pattern and the previous one so
            // they ride at the alignment column rather than falling in a gap.
            if (i > 0) {
                for (JavaTokens.Token tok : tokensIn(prevEnd, pStart)) {
                    if (!tok.isComment()) {
                        continue;
                    }
                    int cEnd = tok.text().endsWith("\n") ? tok.end() - 1 : tok.end();
                    Block commentBlock = JavaBlock.builder(tok.start(), cEnd, "CasePatternComment" + i)
                            .indent(Indent.relativeSpaceIndent(prefixWidth))
                            .child(JavaBlockBuilder.commentLeaf(tok))
                            .build();
                    addSibling(composite, prev, commentBlock, Spacing.createSpacing(0, 0, 1, true, 0));
                    prev = commentBlock;
                }
            }
            Block patContent = owner.buildTokensRange(pStart, pEnd, "CasePattern" + i);
            Block patBlock;
            if (i == 0) {
                patBlock = patContent;
            }
            else {
                patBlock = JavaBlock.builder(pStart, pEnd, "CasePatternAlign" + i)
                        .indent(Indent.relativeSpaceIndent(prefixWidth))
                        .child(patContent)
                        .build();
            }
            Spacing sp = (i == 0)
                    ? Spacing.oneSpace()
                    : (containsLineBreak(prev.endOffset(), pStart)
                       ? Spacing.createSpacing(0, 0, 1, false, 0)
                       : Spacing.oneSpace());
            addSibling(composite, prev, patBlock, sp);
            prev = patBlock;
            prevEnd = pEnd;
        }
        return composite.build();
    }

    /// Build a case label with a multi-line parenthesized pattern (e.g.
    /// `case Pair(\\n    var left,\\n    var right)`). The portion
    /// inside the parens gets CONTINUATION indent.
    private Block buildWrappedCaseLabel(int labelStart, int labelEnd)
    {
        // Find the first `(` and its matching `)` in the label.
        int lparen = -1;
        int parenDepth = 0;
        int rparen = -1;
        for (JavaTokens.Token tok : tokensIn(labelStart, labelEnd)) {
            if (tok.type() == ITerminalSymbols.TokenNameLPAREN) {
                if (lparen < 0) {
                    lparen = tok.start();
                }
                parenDepth++;
            }
            else if (tok.type() == ITerminalSymbols.TokenNameRPAREN) {
                parenDepth--;
                if (parenDepth == 0 && lparen >= 0) {
                    rparen = tok.start();
                    break;
                }
            }
        }
        if (lparen < 0 || rparen < 0 || !containsLineBreak(lparen, rparen)) {
            return flatSwitchCaseLabel(labelStart, labelEnd);
        }
        // Prefix: `case Pair` (up to but NOT including `(`).
        JavaBlock.Builder lb = JavaBlock.builder(labelStart, labelEnd, "SwitchCaseLabel");
        Block prefix = owner.buildTokensRange(labelStart, lparen, "CaseLabelPrefix");
        lb.child(prefix);

        // Synthetic inline component-list: `(` arg1, arg2, … `)`. Structure
        // mirrors buildCallExpression's ArgList composite: the composite
        // starts at `(` (inline with the pattern name), each component is a
        // child with CONTINUATION, and `)` is the trailing leaf. The inline
        // start ensures that when a component wraps, the `whiteSpace.containsLineFeeds()`
        // gate in AbstractBlockWrapper.getChildOffset picks up the
        // CONTINUATION via indentAlreadyUsedBefore.
        JavaBlock.Builder argList = JavaBlock.builder(lparen, rparen + 1, "CaseLabelArgList");
        Block lparenBlock = owner.buildTokensRange(lparen, lparen + 1, "(");
        argList.child(lparenBlock);
        Block argPrev = lparenBlock;

        // Split the body into depth-0 comma-separated components.
        // Between `(` and first component, and between commas and subsequent
        // components: 0 spaces, but keep source line breaks (so source
        // `Pair(\n int x` stays wrapped while `Comparison(_,\n var …` keeps
        // its first component inline).
        Spacing tight = Spacing.createSpacing(0, 0, 0, false, 0);
        Spacing keepLineNoSpace = Spacing.createSpacing(0, 0, 0, true, 0);
        int componentStart = lparen + 1;
        int depth = 0;
        for (JavaTokens.Token tok : tokensIn(lparen + 1, rparen)) {
            if (tok.type() == ITerminalSymbols.TokenNameLPAREN) {
                depth++;
            }
            else if (tok.type() == ITerminalSymbols.TokenNameRPAREN) {
                depth--;
            }
            else if (tok.type() == ITerminalSymbols.TokenNameCOMMA && depth == 0) {
                Block comp = JavaBlock.builder(componentStart, tok.start(), "CaseLabelArg")
                        .indent(Indent.continuationIndent())
                        .child(owner.buildTokensRange(componentStart, tok.start(), "CaseLabelArgTokens", false))
                        .build();
                addSibling(argList, argPrev, comp, keepLineNoSpace);
                Block commaBlock = owner.buildTokensRange(tok.start(), tok.start() + 1, ",");
                addSibling(argList, comp, commaBlock, tight);
                argPrev = commaBlock;
                componentStart = tok.start() + 1;
            }
        }
        // Last component [componentStart, rparen).
        if (componentStart < rparen) {
            Block lastComp = JavaBlock.builder(componentStart, rparen, "CaseLabelArg")
                    .indent(Indent.continuationIndent())
                    .child(owner.buildTokensRange(componentStart, rparen, "CaseLabelArgTokens", false))
                    .build();
            addSibling(argList, argPrev, lastComp, keepLineNoSpace);
            argPrev = lastComp;
        }
        Block rparenBlock = owner.buildTokensRange(rparen, rparen + 1, ")");
        addSibling(argList, argPrev, rparenBlock, tight);

        Block argListBlock = argList.build();
        addSibling(lb, prefix, argListBlock, tight);

        // Tail: anything after `)` — typically ` -> ...` or `:`. Bare trailing
        // `:` is the case-label colon and must hug the `)`; `->` and longer
        // tails use keep-line-or-space.
        if (rparen + 1 < labelEnd) {
            boolean bareTrailingColon = labelEnd - (rparen + 1) <= 2
                    && source.charAt(labelEnd - 1) == ':';
            Block tail = owner.buildTokensRange(rparen + 1, labelEnd, "CaseLabelTail");
            Spacing sp = bareTrailingColon
                    ? JavaSpacingRules.noSpace()
                    : JavaSpacingRules.keepLineOrSpace();
            addSibling(lb, argListBlock, tail, sp);
        }
        return lb.build();
    }

    /// Flat case-label builder that strips any space preceding a trailing
    /// `:` — JDT's source range includes the label's terminator (`:` for
    /// colon-cases), and plain buildTokensRange preserves source spacing
    /// between the label expression and that colon.
    private Block flatSwitchCaseLabel(int labelStart, int labelEnd)
    {
        if (labelEnd - labelStart >= 2
                && source.charAt(labelEnd - 1) == ':'
                && source.charAt(labelEnd - 2) != ':') {
            JavaBlock.Builder lb = JavaBlock.builder(labelStart, labelEnd, "SwitchCaseLabel");
            Block prefix = owner.buildTokensRange(labelStart, labelEnd - 1, "CaseLabelPrefix");
            Block colon = JavaBlock.leaf(labelEnd - 1, labelEnd, ":");
            lb.child(prefix);
            addSibling(lb, prefix, colon, JavaSpacingRules.noSpace());
            return lb.build();
        }
        return owner.buildTokensRange(labelStart, labelEnd, "SwitchCaseLabel");
    }

    /// Walk an ASTNode looking for a child Expression whose source position
    /// matches `targetOffset`. Used to locate the guard expression
    /// inside a GuardedPattern.
    private static Expression findExpressionAt(ASTNode node, int targetOffset)
    {
        if (node instanceof Expression e && e.getStartPosition() == targetOffset) {
            return e;
        }
        Expression[] result = {null};
        node.accept(new ASTVisitor()
        {
            @Override
            public void preVisit(ASTNode n)
            {
                if (result[0] != null) {
                    return;
                }
                if (n instanceof Expression e && e.getStartPosition() == targetOffset) {
                    result[0] = e;
                }
            }
        });
        return result[0];
    }

    private Block buildSwitchCaseGroup(List<Statement> group)
    {
        int start = group.getFirst().getStartPosition();
        Statement last = group.getLast();
        int end = last.getStartPosition() + last.getLength();
        JavaBlock.Builder groupBlock = JavaBlock.builder(start, end, "SwitchCaseGroup")
                .indent(Indent.normalIndent());
        // First statement is the case label (SwitchCase). For arrow cases
        // (`case 1 -> ...`), the body follows the arrow on the same line.
        // For colon cases (`case 1:`), body statements go on subsequent
        // lines with an additional NORMAL indent.
        SwitchCase caseLabel = (SwitchCase) group.getFirst();
        boolean arrowCase = caseLabel.isSwitchLabeledRule();
        int labelStart = caseLabel.getStartPosition();
        int labelEnd = labelStart + caseLabel.getLength();
        // When case label contains a `when` guard whose expression is a
        // multi-line InfixExpression, decompose so wrapped operands carry
        // CONTINUATION indent.
        Block labelBlock = buildSwitchCaseLabel(caseLabel, labelStart, labelEnd);
        groupBlock.child(labelBlock);
        Block prev = labelBlock;
        boolean firstWasBlock = false;
        int caseScanStart = labelEnd;
        for (int i = 1; i < group.size(); i++) {
            Statement stmt = group.get(i);
            // Emit any comments in the gap between the case label (or
            // previous body statement) and this statement. Detect whether
            // the FIRST gap comment is inline with the previous block (same
            // line as the case label or arrow) — if so, emit it attached to
            // the previous block; otherwise emit it on its own line.
            // Inline detection for any iteration: if the first comment in
            // the gap is on the same line as the preceding code, attach it
            // inline (preserves trailing `// note` on statements like
            // `x = 1; // explain`).
            boolean firstCommentInline = false;
            for (JavaTokens.Token tok : tokensIn(caseScanStart, stmt.getStartPosition())) {
                if (tok.isComment()) {
                    firstCommentInline = !containsLineBreak(caseScanStart, tok.start());
                    break;
                }
            }
            Block prevBeforeComments = prev;
            if (firstCommentInline) {
                // Emit the first comment inline with the label/arrow, then
                // remaining content (body) on new lines via standard spacing.
                prev = owner.emitInterBlockCommentsArrowInline(groupBlock, prev, caseScanStart, stmt.getStartPosition());
            }
            else {
                prev = owner.emitInterBlockComments(groupBlock, prev, caseScanStart, stmt.getStartPosition());
            }
            boolean commentsInGap = prev != prevBeforeComments;
            // For arrow case with an empty Block body, collapse `{\n}` to
            // `{}` on the same line as the arrow. Mirrors IntelliJ's
            // KEEP_SIMPLE_BLOCKS_IN_ONE_LINE default.
            Block stmtBlock;
            if (arrowCase && i == 1 && stmt instanceof org.eclipse.jdt.core.dom.Block blk
                    && blk.statements().isEmpty()) {
                int blkStart = blk.getStartPosition();
                int blkEnd = blkStart + blk.getLength();
                int blkOpenBrace = owner.firstChar(blkStart, blkEnd, '{');
                int blkCloseBrace = owner.lastChar(blkStart, blkEnd, '}');
                // Check if the block has any comments inside; if so,
                // delegate to buildStatement (which preserves them).
                boolean hasComments = false;
                if (blkOpenBrace >= 0 && blkCloseBrace >= 0) {
                    for (JavaTokens.Token tok : tokensIn(blkOpenBrace + 1, blkCloseBrace)) {
                        if (tok.isComment()) {
                            hasComments = true;
                            break;
                        }
                    }
                }
                if (!hasComments && blkOpenBrace >= 0 && blkCloseBrace >= 0) {
                    JavaBlock.Builder emptyBody = JavaBlock.builder(blkStart, blkEnd, "EmptyArrowBody");
                    Block ob = owner.buildTokensRange(blkOpenBrace, blkOpenBrace + 1, "{");
                    emptyBody.child(ob);
                    Block cb = owner.buildTokensRange(blkCloseBrace, blkCloseBrace + 1, "}");
                    // Preserve source shape: `{}` inline stays inline; `{\n}`
                    // multi-line stays multi-line.
                    Spacing innerSpacing = containsLineBreak(blkOpenBrace + 1, blkCloseBrace)
                            ? Spacing.createSpacing(0, 0, 1, false, 0)
                            : Spacing.createSpacing(0, 0, 0, false, 0);
                    addSibling(emptyBody, ob, cb, innerSpacing);
                    stmtBlock = emptyBody.build();
                }
                else {
                    stmtBlock = buildStatement(stmt);
                }
            }
            else {
                stmtBlock = buildStatement(stmt);
            }
            // After a Block-bodied colon case (`case 1: { ... }`), subsequent
            // statements (typically `break;`) align with the case label, not
            // with the body. Strip the inner statement's NORMAL indent by
            // rebuilding it as a tokens-range without the wrapper.
            if (firstWasBlock) {
                stmtBlock = owner.buildTokensRange(
                        stmt.getStartPosition(),
                        stmt.getStartPosition() + stmt.getLength(),
                        "PostBlockStmt");
            }
            // Arrow case first body element: same line as `->` (space, no line
            // break). Subsequent elements (shouldn't happen for arrow cases,
            // which have exactly one body) would take line breaks.
            // Colon case with Block body as first stmt (`case 1: {`): inline
            // `{` on the case label line.
            // EXCEPTION: when a line comment sat between the arrow and the
            // body, the body MUST go on a new line (a line comment consumes
            // its trailing newline, so any subsequent token cannot be inline).
            Spacing sp;
            if (arrowCase && i == 1 && !commentsInGap) {
                sp = Spacing.createSpacing(1, 1, 0, false, 0);
            }
            else if (!arrowCase && i == 1 && stmt instanceof org.eclipse.jdt.core.dom.Block) {
                sp = Spacing.createSpacing(1, 1, 0, false, 0);
            }
            else {
                sp = Spacing.createSpacing(0, 0, 1, true, 1);
            }
            // When an inline comment forces the arrow body onto its own line,
            // the body belongs at CONTINUATION from the case label (not at
            // NORMAL, which is what the group's indent would give). Rebuild
            // the statement as a tokens range under a CONTINUATION wrapper —
            // this strips the statement's own NORMAL indent so we don't
            // stack NORMAL + CONTINUATION.
            if (arrowCase && i == 1 && commentsInGap) {
                int sStart = stmt.getStartPosition();
                int sEnd = sStart + stmt.getLength();
                stmtBlock = JavaBlock.continuationWrap(
                        sStart,
                        sEnd,
                        owner.buildTokensRange(sStart, sEnd, "ArrowBodyTokens"),
                        "ArrowBodyCont");
            }
            addSibling(groupBlock, prev, stmtBlock, sp);
            prev = stmtBlock;
            if (i == 1 && stmt instanceof org.eclipse.jdt.core.dom.Block) {
                firstWasBlock = true;
            }
            caseScanStart = stmt.getStartPosition() + stmt.getLength();
        }
        return groupBlock.build();
    }

    private Block buildDoStatement(DoStatement node)
    {
        int start = node.getStartPosition();
        int end = start + node.getLength();
        JavaBlock.Builder composite = JavaBlock.builder(start, end, "DoStatement")
                .indent(Indent.normalIndent());
        // `do` keyword, body, then `while (cond);` trailer.
        int bodyStart = node.getBody().getStartPosition();
        int bodyEnd = bodyStart + node.getBody().getLength();
        Block doKeyword = owner.buildTokensRange(start, bodyStart, "do");
        composite.child(doKeyword);
        Block body = buildControlStatement(node.getBody());
        addSibling(composite, doKeyword, body, JavaConstructSpacing.betweenControlHeaderAndBody(node.getBody()));
        // Find the `while` keyword after the body. Preserve any comments
        // between `}` and `while`.
        int whileKeyword = owner.findKeyword(bodyEnd, end, "while");
        Block prev = body;
        if (whileKeyword > bodyEnd) {
            prev = owner.emitInterBlockComments(composite, body, bodyEnd, whileKeyword);
        }
        // The `while (...);` trailer starts on its own line in this style.
        int trailerStart = whileKeyword >= 0 ? whileKeyword : bodyEnd;
        Block trailer = owner.buildTokensRange(trailerStart, end, "WhileTrailer");
        addSibling(composite, prev, trailer, Spacing.createSpacing(0, 0, 1, true, 0));
        return composite.build();
    }

    /// Labeled statement: `label:\n    body`. The label gets NORMAL indent;
    /// the body is dispatched recursively.
    private Block buildLabeledStatement(LabeledStatement node)
    {
        int start = node.getStartPosition();
        int end = start + node.getLength();
        // The composite carries no indent; label + body each carry NORMAL so
        // they line up at the same column without doubling.
        JavaBlock.Builder composite = JavaBlock.builder(start, end, "LabeledStatement");
        int labelEnd = node.getLabel().getStartPosition() + node.getLabel().getLength();
        int colonOffset = owner.firstChar(labelEnd, end, ':');
        int bodyStart = node.getBody().getStartPosition();
        Block labelInner = owner.buildTokensRange(start, colonOffset >= 0 ? colonOffset + 1 : bodyStart, "Label");
        Block label = JavaBlock.builder(labelInner.startOffset(), labelInner.endOffset(), "LabelShell")
                .indent(Indent.normalIndent())
                .child(labelInner)
                .build();
        composite.child(label);
        Block prev = label;
        // Comments between the label's `:` and the body statement. Without
        // this they fall into an uncovered gap and vanish.
        int labelContentEnd = label.endOffset();
        for (JavaTokens.Token tok : tokensIn(labelContentEnd, bodyStart)) {
            if (!tok.isComment()) {
                continue;
            }
            int cEnd = tok.text().endsWith("\n") ? tok.end() - 1 : tok.end();
            Block commentBlock = JavaBlock.builder(tok.start(), cEnd, "LabelComment")
                    .indent(Indent.normalIndent())
                    .child(JavaBlockBuilder.commentLeaf(tok))
                    .build();
            addSibling(composite, prev, commentBlock, Spacing.createSpacing(0, 0, 1, true, 0));
            prev = commentBlock;
        }
        // Body decomposed via buildStatement — block-bearing statements get
        // their inner content decomposed properly.
        Block body = buildStatement(node.getBody());
        addSibling(composite, prev, body, Spacing.createSpacing(0, 0, 1, false, 0));
        return composite.build();
    }

    private Block buildIfStatement(IfStatement node)
    {
        int start = node.getStartPosition();
        int end = start + node.getLength();
        // Find the `)` that closes the condition — tokens after it are the
        // then-branch. If there's an `else` statement, split at the `else`
        // keyword offset.
        int lparenOffset = owner.firstChar(start, end, '(');
        int rparenOffset = owner.findMatchingRParen(lparenOffset, end);
        if (lparenOffset < 0 || rparenOffset < 0) {
            return JavaBlock.builder(start, end, "IfStatement")
                    .indent(Indent.normalIndent())
                    .child(owner.buildTokensRange(start, end, "StatementTokens"))
                    .build();
        }
        JavaBlock.Builder composite = JavaBlock.builder(start, end, "IfStatement")
                .indent(Indent.normalIndent());
        // Header: `if (cond)` — if the condition is a multi-line expression
        // (InfixExpression, ConditionalExpression), decompose it so wrapped
        // operands get CONTINUATION indent.
        Expression condition = node.getExpression();
        Block header;
        int condStart = condition.getStartPosition();
        int condEnd = condStart + condition.getLength();
        if (containsLineBreak(condStart, condEnd)) {
            JavaBlock.Builder hb = JavaBlock.builder(start, rparenOffset + 1, "IfHeader");
            Block prefix = owner.buildTokensRange(start, condStart, "IfHeaderPrefix");
            hb.child(prefix);
            Block condBlock = owner.buildExpressionBlock(condition, condStart, condEnd, "IfCondition");
            // For InfixExpression / ConditionalExpression, wrap the condition
            // in a dedicated CONTINUATION block so wrapped operands sit at
            // +CONTINUATION from the `if` line. For call-like conditions
            // (MethodInvocation, ClassInstanceCreation, PrefixExpression whose
            // operand is a call), buildExpressionBlock already handles arg
            // CONTINUATION internally, so no extra wrap is needed (and adding
            // one would double-indent wrapped args).
            boolean needsCondWrap = condition instanceof InfixExpression
                    || condition instanceof ConditionalExpression;
            Block condChild = needsCondWrap
                    ? JavaBlock.builder(condStart, condEnd, "IfCondWrap")
                      .indent(Indent.continuationIndent())
                      .child(condBlock)
                      .build()
                    : condBlock;
            // No space between `(` and condition (e.g. `if (!present()`).
            addSibling(hb, prefix, condChild, Spacing.none());
            Block suffix = owner.buildTokensRange(condEnd, rparenOffset + 1, "IfHeaderSuffix");
            addSibling(hb, condChild, suffix, Spacing.none());
            header = hb.build();
        }
        else {
            header = owner.buildTokensRange(start, rparenOffset + 1, "IfHeader");
        }
        composite.child(header);
        Block prev = header;

        // Then-branch
        ASTNode thenStmt = node.getThenStatement();
        int thenEnd = node.getElseStatement() != null
                ? node.getElseStatement().getStartPosition()
                : end;
        // Scan backwards from thenEnd to find the `else` keyword — otherwise
        // the tokens preceding else include the `else` itself if we trim.
        int elseKeywordOffset = -1;
        if (node.getElseStatement() != null) {
            for (JavaTokens.Token tok : tokens) {
                if (tok.start() >= thenEnd) {
                    break;
                }
                if (tok.start() >= rparenOffset && "else".equals(tok.text())) {
                    elseKeywordOffset = tok.start();
                }
            }
        }
        // Comments between `)` and the then-branch `{` — preserve them as
        // NORMAL-indent siblings so `if (cond)\n //note\n { body }` keeps
        // the comment. If any were emitted, force the body onto its own
        // line (the default "one space" would put `{` on the comment line).
        Block beforeComments = prev;
        prev = owner.emitInterBlockComments(composite, prev, rparenOffset + 1, thenStmt.getStartPosition(), Indent.noneIndent());
        Block thenBlock = buildThenOrElseBranch(thenStmt);
        Spacing bodySpacing = (prev == beforeComments)
                ? spacingBetweenIfHeaderAndBranch(thenStmt)
                : JavaConstructSpacing.nextLine();
        addSibling(composite, prev, thenBlock, bodySpacing);
        prev = thenBlock;

        // Else-branch (if any).
        if (node.getElseStatement() != null && elseKeywordOffset >= 0) {
            // Preserve comments between `}` and `else`.
            prev = owner.emitInterBlockComments(composite, prev, prev.endOffset(), elseKeywordOffset, Indent.noneIndent());
            ASTNode elseStmt = node.getElseStatement();
            Block elseBlock = buildElseBranch(elseStmt, elseKeywordOffset, end);
            addSibling(composite, prev, elseBlock, JavaConstructSpacing.nextLineKeepingOneBlank());
        }
        return composite.build();
    }

    private Block buildThenOrElseBranch(ASTNode stmt)
    {
        if (!(stmt instanceof Statement statement)) {
            throw new IllegalArgumentException("Expected statement branch but got: " + stmt.getClass().getSimpleName());
        }
        return buildControlStatement(statement);
    }

    private Block buildElseBranch(ASTNode elseStmt, int branchStart, int branchEnd)
    {
        JavaBlock.Builder b = JavaBlock.builder(branchStart, branchEnd, "ElseBranch");
        // else keyword — first token.
        int elseEnd = branchStart + "else".length();
        Block elseKeyword = owner.buildTokensRange(branchStart, elseEnd, "else");
        b.child(elseKeyword);
        // Comments between `else` and the body — e.g. `else\n //note\n {...}`.
        Block prev = owner.emitInterBlockComments(b, elseKeyword, elseEnd, elseStmt.getStartPosition(), Indent.noneIndent());
        boolean hasComment = prev != elseKeyword;
        Block body;
        if (elseStmt instanceof IfStatement nestedIf) {
            // else-if: emit the if as an inline block following `else`.
            body = buildIfStatement(nestedIf);
            b.spacing(prev, body, hasComment ? JavaConstructSpacing.nextLine() : Spacing.oneSpace());
        }
        else if (elseStmt instanceof org.eclipse.jdt.core.dom.Block block) {
            body = buildStatementBlock(block);
            b.spacing(prev, body, JavaConstructSpacing.betweenElseAndBody(hasComment, elseStmt));
        }
        else {
            body = buildThenOrElseBranch(elseStmt);
            b.spacing(prev, body, JavaConstructSpacing.nextLine());
        }
        b.child(body);
        return b.build();
    }

    Block buildControlStatement(Statement statement)
    {
        if (statement instanceof org.eclipse.jdt.core.dom.Block block) {
            return buildStatementBlock(block);
        }
        return buildStatement(statement);
    }

    private static Spacing spacingBetweenIfHeaderAndBranch(ASTNode branch)
    {
        return JavaConstructSpacing.betweenControlHeaderAndBody(branch);
    }
}
