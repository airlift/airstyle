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
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WhileStatement;

import java.util.List;

final class JavaLoopConstructBuilder
        implements JavaConstructBuilder<ASTNode>
{
    interface Support
    {
        boolean containsLineBreak(int start, int end);

        int firstChar(int start, int end, char target);

        int lastChar(int start, int end, char target);

        int findMatchingRParen(int lparenOffset, int maxEnd);

        int[] findTopLevelSemicolons(int start, int end);

        int lastNonWhitespaceBefore(int endExclusive, int minimumStart);

        int extendThroughTrailingInlineComments(int currentEnd, int boundary);

        Block buildTokensRange(int start, int end, String debugName);

        Block buildTokensRange(int start, int end, String debugName, boolean canUseFirstChildIndent);

        Block buildExpressionBlock(Expression expression, int start, int end, String debugName);

        Block buildControlStatement(Statement statement);

        void addSibling(JavaBlock.Builder parent, Block first, Block second, Spacing spacing);
    }

    private final JavaBlockFactory blockFactory;
    private final Support support;

    JavaLoopConstructBuilder(JavaBlockFactory blockFactory, Support support)
    {
        this.blockFactory = blockFactory;
        this.support = support;
    }

    @Override
    public boolean supports(ASTNode node)
    {
        return node instanceof WhileStatement
                || node instanceof ForStatement
                || node instanceof EnhancedForStatement
                || node instanceof SynchronizedStatement;
    }

    @Override
    public Block build(ASTNode node)
    {
        return switch (node) {
            case WhileStatement whileStatement -> buildLoopWithHeader(whileStatement, whileStatement.getBody(), "WhileStatement");
            case ForStatement forStatement -> buildLoopWithHeader(forStatement, forStatement.getBody(), "ForStatement");
            case EnhancedForStatement enhancedForStatement -> buildLoopWithHeader(enhancedForStatement, enhancedForStatement.getBody(), "EnhancedForStatement");
            case SynchronizedStatement synchronizedStatement -> buildLoopWithHeader(synchronizedStatement, synchronizedStatement.getBody(), "SynchronizedStatement");
            default -> throw new IllegalArgumentException("Unsupported loop construct: " + node.getClass().getSimpleName());
        };
    }

    private Block buildLoopWithHeader(ASTNode node, Statement body, String debugName)
    {
        int start = node.getStartPosition();
        int end = start + node.getLength();
        JavaBlock.Builder composite = blockFactory.builder(start, end, debugName)
                .indent(Indent.normalIndent());
        if (body == null) {
            composite.child(support.buildTokensRange(start, end, debugName + "Tokens"));
            return composite.build();
        }

        int bodyStart = body.getStartPosition();
        Expression conditionExpression = extractConditionExpression(node);
        Block header;
        if (conditionExpression != null
                && support.containsLineBreak(conditionExpression.getStartPosition(), conditionExpression.getStartPosition() + conditionExpression.getLength())) {
            header = buildConditionHeader(node, bodyStart, debugName, conditionExpression);
        }
        else if (node instanceof ForStatement forStatement && shouldBuildForHeader(forStatement, start, bodyStart)) {
            header = buildForHeader(forStatement, start, bodyStart, debugName);
        }
        else if (node instanceof EnhancedForStatement enhancedForStatement && shouldBuildEnhancedForHeader(enhancedForStatement, start)) {
            header = buildEnhancedForHeader(enhancedForStatement, start, bodyStart, debugName);
        }
        else {
            header = support.buildTokensRange(start, bodyStart, debugName + "Header");
        }
        composite.child(header);

        Block bodyBlock = support.buildControlStatement(body);
        Spacing bodySpacing = JavaConstructSpacing.betweenControlHeaderAndBody(body);
        support.addSibling(composite, header, bodyBlock, bodySpacing);
        return composite.build();
    }

    private Expression extractConditionExpression(ASTNode node)
    {
        if (node instanceof WhileStatement whileStatement) {
            return whileStatement.getExpression();
        }
        if (node instanceof SynchronizedStatement synchronizedStatement) {
            return synchronizedStatement.getExpression();
        }
        return null;
    }

    private Block buildConditionHeader(ASTNode node, int bodyStart, String debugName, Expression conditionExpression)
    {
        int start = node.getStartPosition();
        int conditionStart = conditionExpression.getStartPosition();
        int conditionEnd = conditionStart + conditionExpression.getLength();
        JavaBlock.Builder header = blockFactory.builder(start, bodyStart, debugName + "Header");
        Block prefix = support.buildTokensRange(start, conditionStart, debugName + "HeaderPrefix");
        header.child(prefix);
        Block conditionBlock = support.buildExpressionBlock(conditionExpression, conditionStart, conditionEnd, debugName + "Cond");
        boolean needsConditionWrap = conditionExpression instanceof org.eclipse.jdt.core.dom.InfixExpression
                || conditionExpression instanceof org.eclipse.jdt.core.dom.ConditionalExpression;
        Block conditionChild = needsConditionWrap
                ? blockFactory.continuationWrap(conditionStart, conditionEnd, conditionBlock, debugName + "CondWrap")
                : conditionBlock;
        support.addSibling(header, prefix, conditionChild, Spacing.none());
        Block suffix = support.buildTokensRange(conditionEnd, bodyStart, debugName + "HeaderSuffix");
        support.addSibling(header, conditionChild, suffix, Spacing.none());
        return header.build();
    }

    private Block buildForHeader(ForStatement forStatement, int start, int bodyStart, String debugName)
    {
        int lparen = support.firstChar(start, bodyStart, '(');
        int rparen = support.findMatchingRParen(lparen, bodyStart);
        if (lparen < 0 || rparen < 0) {
            return support.buildTokensRange(start, bodyStart, debugName + "Header");
        }

        int[] semicolons = support.findTopLevelSemicolons(lparen + 1, rparen);
        JavaBlock.Builder header = blockFactory.builder(start, bodyStart, debugName + "Header");
        Block prefix = support.buildTokensRange(start, lparen + 1, debugName + "HeaderPrefix");
        header.child(prefix);
        Block prev = prefix;

        Expression condition = forStatement.getExpression();
        @SuppressWarnings("unchecked")
        List<ASTNode> initializers = (List<ASTNode>) (List<?>) forStatement.initializers();
        @SuppressWarnings("unchecked")
        List<ASTNode> updaters = (List<ASTNode>) (List<?>) forStatement.updaters();

        int[] boundaries = {
                lparen + 1,
                semicolons.length > 0 ? semicolons[0] + 1 : rparen,
                semicolons.length > 1 ? semicolons[1] + 1 : rparen,
                rparen,
        };
        for (int index = 0; index < 3; index++) {
            int clauseStart = boundaries[index];
            int clauseEnd = boundaries[index + 1];
            if (clauseStart >= clauseEnd) {
                continue;
            }

            List<ASTNode> clauseExpressions = switch (index) {
                case 0 -> initializers;
                case 1 -> condition == null ? List.of() : List.of(condition);
                case 2 -> updaters;
                default -> List.of();
            };
            Block clauseInner = support.containsLineBreak(clauseStart, clauseEnd) && !clauseExpressions.isEmpty()
                    ? buildForHeaderClause(clauseStart, clauseEnd, clauseExpressions, debugName + "HeaderClause" + index)
                    : support.buildTokensRange(clauseStart, clauseEnd, debugName + "HeaderClause" + index, false);
            Block clauseWrapped = blockFactory.continuationWrap(clauseStart, clauseEnd, clauseInner, debugName + "HeaderClauseWrap" + index);
            support.addSibling(header, prev, clauseWrapped, Spacing.none());
            prev = clauseWrapped;
        }

        if (rparen < bodyStart) {
            Block suffix = support.buildTokensRange(rparen, bodyStart, debugName + "HeaderSuffix");
            support.addSibling(header, prev, suffix, Spacing.none());
        }
        return header.build();
    }

    private boolean shouldBuildForHeader(ForStatement node, int start, int bodyStart)
    {
        int lparen = support.firstChar(start, bodyStart, '(');
        int rparen = support.findMatchingRParen(lparen, bodyStart);
        return lparen >= 0
                && rparen >= 0
                && support.containsLineBreak(start, rparen + 1);
    }

    private Block buildForHeaderClause(int clauseStart, int clauseEnd, List<ASTNode> expressions, String debugName)
    {
        JavaBlock.Builder clause = blockFactory.builder(clauseStart, clauseEnd, debugName)
                .canUseFirstChildIndent(false);
        Block prev = null;
        int cursor = clauseStart;
        for (int index = 0; index < expressions.size(); index++) {
            ASTNode expression = expressions.get(index);
            int expressionStart = expression.getStartPosition();
            if (cursor < expressionStart) {
                Block prefix = support.buildTokensRange(cursor, expressionStart, debugName + "Pre" + index, false);
                if (prev == null) {
                    clause.child(prefix);
                }
                else {
                    support.addSibling(clause, prev, prefix, Spacing.none());
                }
                prev = prefix;
            }

            int expressionEnd = expression.getStartPosition() + expression.getLength();
            int expressionBlockEnd;
            if (index + 1 < expressions.size()) {
                ASTNode next = expressions.get(index + 1);
                int separatorEnd = support.lastNonWhitespaceBefore(next.getStartPosition(), expressionEnd);
                expressionBlockEnd = separatorEnd > 0 ? separatorEnd + 1 : expressionEnd;
            }
            else {
                expressionBlockEnd = expressionEnd;
            }
            expressionBlockEnd = support.extendThroughTrailingInlineComments(expressionBlockEnd, clauseEnd);

            Block expressionBlock = buildForClauseExpression(expression, expressionStart, expressionBlockEnd, debugName + "Expr" + index);
            if (prev == null) {
                clause.child(expressionBlock);
            }
            else {
                support.addSibling(clause, prev, expressionBlock, Spacing.none());
            }
            prev = expressionBlock;
            cursor = expressionBlockEnd;
        }

        if (cursor < clauseEnd) {
            Block suffix = support.buildTokensRange(cursor, clauseEnd, debugName + "Tail", false);
            if (prev == null) {
                clause.child(suffix);
            }
            else {
                support.addSibling(clause, prev, suffix, Spacing.none());
            }
        }
        return clause.build();
    }

    private Block buildForClauseExpression(ASTNode expression, int start, int end, String debugName)
    {
        if (expression instanceof VariableDeclarationExpression declaration && declaration.fragments().size() == 1) {
            VariableDeclarationFragment fragment = (VariableDeclarationFragment) declaration.fragments().getFirst();
            Expression initializer = fragment.getInitializer();
            if (initializer != null) {
                int initializerStart = initializer.getStartPosition();
                int initializerEnd = support.extendThroughTrailingInlineComments(
                        initializer.getStartPosition() + initializer.getLength(),
                        end);
                JavaBlock.Builder builder = blockFactory.builder(start, end, debugName);
                Block prefix = support.buildTokensRange(start, initializerStart, debugName + "Prefix");
                builder.child(prefix);
                Block initializerBlock = support.buildExpressionBlock(initializer, initializerStart, initializerEnd, debugName + "Init");
                support.addSibling(builder, prefix, initializerBlock, JavaSpacingRules.keepLineOrSpace());
                if (initializerEnd < end) {
                    Block suffix = support.buildTokensRange(initializerEnd, end, debugName + "Tail");
                    support.addSibling(builder, initializerBlock, suffix, Spacing.none());
                }
                return builder.build();
            }
        }
        if (expression instanceof Expression expr) {
            return support.buildExpressionBlock(expr, start, end, debugName);
        }
        return support.buildTokensRange(start, end, debugName);
    }

    private Block buildEnhancedForHeader(EnhancedForStatement node, int start, int bodyStart, String debugName)
    {
        Expression iterable = node.getExpression();
        int iterableStart = iterable.getStartPosition();
        int iterableEnd = iterableStart + iterable.getLength();
        JavaBlock.Builder header = blockFactory.builder(start, bodyStart, debugName + "Header");
        Block prefix = support.buildTokensRange(start, iterableStart, debugName + "HeaderPrefix");
        header.child(prefix);
        Block iterableInner = support.buildExpressionBlock(iterable, iterableStart, iterableEnd, debugName + "Iterable");
        Block iterableWrapped = blockFactory.continuationWrap(iterableStart, iterableEnd, iterableInner, debugName + "IterableWrap");
        support.addSibling(header, prefix, iterableWrapped, JavaSpacingRules.keepLineOrSpace());
        if (iterableEnd < bodyStart) {
            Block suffix = support.buildTokensRange(iterableEnd, bodyStart, debugName + "HeaderSuffix");
            support.addSibling(header, iterableWrapped, suffix, Spacing.none());
        }
        return header.build();
    }

    private boolean shouldBuildEnhancedForHeader(EnhancedForStatement node, int start)
    {
        Expression iterable = node.getExpression();
        int iterableStart = iterable.getStartPosition();
        int iterableEnd = iterableStart + iterable.getLength();
        if (support.containsLineBreak(iterableStart, iterableEnd)) {
            return true;
        }
        int colon = support.lastChar(start, iterableStart, ':');
        return colon >= 0 && support.containsLineBreak(colon + 1, iterableStart);
    }
}
