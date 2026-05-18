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
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

final class JavaChainExpressionBuilder
{
    private final JavaBlockBuilder owner;
    private final JavaSourceContext sourceContext;
    private final JavaExpressionBuilder expressionBuilder;
    private final String source;

    JavaChainExpressionBuilder(JavaBlockBuilder owner, JavaSourceContext sourceContext, JavaExpressionBuilder expressionBuilder)
    {
        this.owner = owner;
        this.sourceContext = sourceContext;
        this.expressionBuilder = expressionBuilder;
        this.source = sourceContext.source();
    }

    boolean isNontrivialChain(MethodInvocation mi)
    {
        // A chain with at least two `.method()` calls is worth decomposing.
        // ALSO treat `wrappedCall.method(...)` as a chain — the receiver
        // (ClassInstanceCreation or MethodInvocation) with wrapped args
        // gets selector-style continuation for the trailing `.method`.
        Expression recv = mi.getExpression();
        if (recv instanceof MethodInvocation) {
            return true;
        }
        if (recv instanceof ClassInstanceCreation cic
                && !cic.arguments().isEmpty()
                && expressionBuilder.needsStructuredCallArguments(cic.arguments(), cic)) {
            return true;
        }
        // `new Foo() { body }.method(...)` — the receiver is a CIC whose
        // anonymous class body requires decomposition to indent its members
        // correctly. Without chain routing, the whole expression falls
        // through to flat tokens and the body loses its block indent.
        if (recv instanceof ClassInstanceCreation cicAnon
                && cicAnon.getAnonymousClassDeclaration() != null) {
            return true;
        }
        // Single-selector chain where the selector wraps to its own line —
        // e.g. `"x:%s"\n        .formatted(value)`. The selector needs
        // CONTINUATION indent; treat as a nontrivial chain.
        if (recv != null) {
            int recvEnd = recv.getStartPosition() + recv.getLength();
            int nameStart = mi.getName().getStartPosition();
            return containsLineBreak(recvEnd, nameStart);
        }
        return false;
    }

    Block buildConstructorSelectorChunks(ConstructorSelectorRange range, String debugName)
    {
        JavaBlock.Builder composite = JavaBlock.builder(range.start(), range.end(), debugName);
        Block prev = owner.buildLeafTokenRun(range.headRange(debugName + "Head"), debugName + "Head");
        composite.child(prev);
        for (int i = 0; i < range.selectorCount(); i++) {
            int chunkStart = range.chunkStart(i);
            int chunkEnd = range.chunkEnd(i);
            Block chunkContent = buildConstructorSelectorChunkContent(
                    range,
                    chunkStart,
                    chunkEnd,
                    range.isLastSelector(i),
                    debugName + "Selector" + i);
            Block chunk = JavaBlock.builder(chunkStart, chunkEnd, debugName + "SelectorWrap" + i)
                    .indent(Indent.continuationIndent())
                    .canUseFirstChildIndent(false)
                    .child(chunkContent)
                    .build();
            JavaBlockBuilder.addSibling(composite, prev, chunk, JavaSpacingRules.keepLineOrSpace());
            prev = chunk;
        }
        return composite.build();
    }

    Block emitConstructorSelectorChunksAsSiblings(
            JavaBlock.Builder composite,
            Block previous,
            ConstructorSelectorRange range,
            Indent selectorIndent,
            String debugName)
    {
        Block prev = previous;
        for (int i = 0; i < range.selectorCount(); i++) {
            int chunkStart = range.chunkStart(i);
            int chunkEnd = range.chunkEnd(i);
            Block chunkContent = buildConstructorSelectorChunkContent(
                    range,
                    chunkStart,
                    chunkEnd,
                    range.isLastSelector(i),
                    debugName + i);
            Block chunk = JavaBlock.builder(chunkStart, chunkEnd, debugName + "Wrap" + i)
                    .indent(selectorIndent)
                    .canUseFirstChildIndent(false)
                    .child(chunkContent)
                    .build();
            JavaBlockBuilder.addSibling(composite, prev, chunk, JavaSpacingRules.keepLineOrSpace());
            prev = chunk;
        }
        return prev;
    }

    int countWrappedSelectors(LinkedList<MethodInvocation> chain, Expression head, int chainStart)
    {
        int startIndex = (head == null) ? 1 : 0;
        int count = 0;
        for (int i = startIndex; i < chain.size(); i++) {
            MethodInvocation mi = chain.get(i);
            int dotPos = selectorStart(mi, (i == 0) ? head.getStartPosition() : 0);
            if (dotPos < 0) {
                dotPos = mi.getName().getStartPosition();
            }
            if (containsLineBreak(chainStart, dotPos)) {
                count++;
            }
        }
        return count;
    }

    Optional<ConstructorSelectorRange> constructorSelectorRange(ClassInstanceCreation creation, int start, int end)
    {
        ASTNode type = creation.getType();
        if (type == null) {
            return Optional.empty();
        }
        int typeEnd = type.getStartPosition() + type.getLength();
        if (typeEnd <= start || typeEnd > end) {
            return Optional.empty();
        }
        List<Integer> selectorStarts = wrappedSelectorStartsInOwnedRange(start, typeEnd);
        if (selectorStarts.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ConstructorSelectorRange(creation, start, end, selectorStarts));
    }

    int uniformWrappedSelectorColumn(LinkedList<MethodInvocation> chain, Expression head, int chainStart)
    {
        int startIndex = (head == null) ? 1 : 0;
        int uniform = -1;
        for (int i = startIndex; i < chain.size(); i++) {
            MethodInvocation mi = chain.get(i);
            int dotPos = selectorStart(mi, (i == 0) ? head.getStartPosition() : 0);
            if (dotPos < 0) {
                dotPos = mi.getName().getStartPosition();
            }
            if (!containsLineBreak(chainStart, dotPos)) {
                continue;
            }
            int col = owner.columnOf(dotPos);
            if (uniform < 0) {
                uniform = col;
            }
            else if (uniform != col) {
                return -1;
            }
        }
        return uniform;
    }

    boolean inlineSelectorsPrecedeWrap(LinkedList<MethodInvocation> chain, Expression head, int chainStart)
    {
        int probeIndex = (head == null) ? 2 : 1;
        if (probeIndex >= chain.size()) {
            return false;
        }
        MethodInvocation mi = chain.get(probeIndex);
        int dotPos = selectorStart(mi, 0);
        if (dotPos < 0) {
            dotPos = mi.getName().getStartPosition();
        }
        return !containsLineBreak(chainStart, dotPos);
    }

    int selectorStart(MethodInvocation mi, int lowerBound)
    {
        if (!mi.typeArguments().isEmpty()) {
            ASTNode firstTypeArgument = (ASTNode) mi.typeArguments().getFirst();
            int dot = findDotBefore(firstTypeArgument.getStartPosition(), lowerBound);
            if (dot >= 0) {
                return dot;
            }
        }
        return findDotBefore(mi.getName().getStartPosition(), lowerBound);
    }

    private Block buildConstructorSelectorChunkContent(
            ConstructorSelectorRange range,
            int chunkStart,
            int chunkEnd,
            boolean lastSelectorChunk,
            String debugName)
    {
        if (lastSelectorChunk && !range.creation().arguments().isEmpty()) {
            return expressionBuilder.buildCallExpression(range.creation(), range.creation().arguments(), chunkStart, chunkEnd);
        }
        return owner.buildLeafTokenRun(range.chunkRange(chunkStart, chunkEnd), debugName);
    }

    int firstWrappedSelectorStart(int start, int end)
    {
        List<Integer> starts = wrappedSelectorStartsInOwnedRange(start, end);
        return starts.isEmpty() ? -1 : starts.getFirst();
    }

    /// Low-level token scanner for selector-like dots. Callers must first
    /// narrow the range to the AST child that owns those dots; scanning a
    /// whole expression also sees nested argument expressions.
    private List<Integer> wrappedSelectorStartsInOwnedRange(int start, int end)
    {
        List<Integer> result = new ArrayList<>();
        int previousCodeEnd = -1;
        for (JavaTokens.Token tok : sourceContext.tokensIn(start, end)) {
            if (tok.start() <= start) {
                if (!tok.isComment()) {
                    previousCodeEnd = tok.end();
                }
                continue;
            }
            if (isSelectorToken(tok) && previousCodeEnd >= 0 && containsLineBreak(previousCodeEnd, tok.start())) {
                result.add(tok.start());
            }
            if (!tok.isComment()) {
                previousCodeEnd = tok.end();
            }
        }
        return result;
    }

    private boolean containsLineBreak(int start, int end)
    {
        return sourceContext.containsLineBreak(start, end);
    }

    private static boolean isSelectorToken(JavaTokens.Token tok)
    {
        return tok.type() == ITerminalSymbols.TokenNameDOT
                || tok.type() == ITerminalSymbols.TokenNameCOLON_COLON;
    }

    /// Find the `.` token that immediately precedes `namePos`, searching
    /// backward until `lowerBound` (exclusive).
    private int findDotBefore(int namePos, int lowerBound)
    {
        for (int i = Math.min(namePos - 1, source.length() - 1); i >= lowerBound && i >= 0; i--) {
            if (source.charAt(i) == '.') {
                return i;
            }
        }
        return -1;
    }

    record ConstructorSelectorRange(ClassInstanceCreation creation, int start, int end, List<Integer> selectorStarts)
    {
        ConstructorSelectorRange
        {
            selectorStarts = List.copyOf(selectorStarts);
        }

        int firstSelectorStart()
        {
            return selectorStarts.getFirst();
        }

        int selectorCount()
        {
            return selectorStarts.size();
        }

        int chunkStart(int index)
        {
            return selectorStarts.get(index);
        }

        int chunkEnd(int index)
        {
            return isLastSelector(index) ? end : selectorStarts.get(index + 1);
        }

        boolean isLastSelector(int index)
        {
            return index + 1 == selectorStarts.size();
        }

        JavaSourceRange headRange(String owner)
        {
            return JavaSourceRange.leaf(owner, start, firstSelectorStart());
        }

        JavaSourceRange chunkRange(int chunkStart, int chunkEnd)
        {
            return JavaSourceRange.ownedBy(creation, chunkStart, chunkEnd);
        }
    }
}
