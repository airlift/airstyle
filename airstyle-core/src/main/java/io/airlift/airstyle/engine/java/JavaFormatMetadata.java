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

import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.CreationReference;
import org.eclipse.jdt.core.dom.Dimension;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.GuardedPattern;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SuperMethodReference;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeMethodReference;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

record JavaFormatMetadata(
        Set<Integer> genericOpens,
        Set<Integer> genericCloses,
        Set<Integer> methodTypeArgCloses,
        Set<Integer> castRParens,
        Set<Integer> switchGuardWhens,
        Set<Integer> unaryPrefixOps,
        Set<Integer> ternaryColons,
        Set<Integer> assertColons,
        Set<Integer> typeUseArrayLBrackets,
        Map<Integer, Annotation> wrappedAnnotations)
{
    JavaFormatMetadata
    {
        genericOpens = Set.copyOf(genericOpens);
        genericCloses = Set.copyOf(genericCloses);
        methodTypeArgCloses = Set.copyOf(methodTypeArgCloses);
        castRParens = Set.copyOf(castRParens);
        switchGuardWhens = Set.copyOf(switchGuardWhens);
        unaryPrefixOps = Set.copyOf(unaryPrefixOps);
        ternaryColons = Set.copyOf(ternaryColons);
        assertColons = Set.copyOf(assertColons);
        typeUseArrayLBrackets = Set.copyOf(typeUseArrayLBrackets);
        wrappedAnnotations = Map.copyOf(wrappedAnnotations);
    }

    static JavaFormatMetadata from(CompilationUnit unit, JavaSourceContext sourceContext)
    {
        String source = sourceContext.source();
        List<JavaTokens.Token> tokens = sourceContext.tokens();
        Set<Integer> genericOpens = new HashSet<>();
        Set<Integer> genericCloses = new HashSet<>();
        Set<Integer> methodTypeArgCloses = new HashSet<>();
        Set<Integer> castRParens = new HashSet<>();
        Set<Integer> switchGuardWhens = new HashSet<>();
        Set<Integer> unaryPrefixOps = new HashSet<>();
        Set<Integer> ternaryColons = new HashSet<>();
        Set<Integer> assertColons = new HashSet<>();
        Set<Integer> typeUseArrayLBrackets = new HashSet<>();
        Map<Integer, Annotation> wrappedAnnotations = new HashMap<>();

        unit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(ParameterizedType node)
            {
                int start = node.getStartPosition();
                int end = start + node.getLength();
                int open = findBracket(tokens, start, end, '<');
                int close = findLastBracket(tokens, start, end, '>');
                if (open >= 0) {
                    genericOpens.add(open);
                }
                if (close >= 0) {
                    genericCloses.add(close);
                    // JDT may lex consecutive `>`s as `>>` / `>>>` tokens.
                    // The token's start is 1-2 chars earlier than `close`.
                    // Mark those positions too so spacing rules fire.
                    if (close - 1 >= 0 && source.charAt(close - 1) == '>') {
                        genericCloses.add(close - 1);
                    }
                    if (close - 2 >= 0 && source.charAt(close - 1) == '>' && source.charAt(close - 2) == '>') {
                        genericCloses.add(close - 2);
                    }
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(CastExpression node)
            {
                // Record the closing `)` of the cast so spacing rules can
                // treat it as NOT value-like (unary `-`/`+` after a cast
                // binds to the operand, not to the cast as a left-operand).
                int exprStart = node.getExpression().getStartPosition();
                for (int i = exprStart - 1; i >= node.getStartPosition(); i--) {
                    if (source.charAt(i) == ')') {
                        castRParens.add(i);
                        break;
                    }
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(ConditionalExpression node)
            {
                int thenEnd = node.getThenExpression().getStartPosition() + node.getThenExpression().getLength();
                int elseStart = node.getElseExpression().getStartPosition();
                int colon = findBracket(tokens, thenEnd, elseStart, ':');
                if (colon >= 0) {
                    ternaryColons.add(colon);
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(AssertStatement node)
            {
                if (node.getMessage() == null) {
                    return super.visit(node);
                }
                int expressionEnd = node.getExpression().getStartPosition() + node.getExpression().getLength();
                int messageStart = node.getMessage().getStartPosition();
                int colon = findBracket(tokens, expressionEnd, messageStart, ':');
                if (colon >= 0) {
                    assertColons.add(colon);
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(TypeDeclaration node)
            {
                collectTypeParameterBrackets(node.typeParameters(), node.getStartPosition(), node.getStartPosition() + node.getLength());
                return super.visit(node);
            }

            @Override
            public boolean visit(RecordDeclaration node)
            {
                collectTypeParameterBrackets(node.typeParameters(), node.getStartPosition(), node.getStartPosition() + node.getLength());
                return super.visit(node);
            }

            @Override
            public boolean visit(MethodDeclaration node)
            {
                collectTypeParameterBrackets(node.typeParameters(), node.getStartPosition(), node.getStartPosition() + node.getLength());
                return super.visit(node);
            }

            @Override
            public boolean visit(MethodInvocation node)
            {
                // Method invocation type arguments: `Builders.<Object>builder()`
                List<?> ta = node.typeArguments();
                if (ta != null && !ta.isEmpty()) {
                    // Use the first type argument's position to anchor the
                    // open `<` search — avoids confusion from `.` tokens
                    // inside nested qualified names (`X.<A.B>m()`) and from
                    // outer invocations in a chain (`a.<X>f().<Y>g()`).
                    int firstTaStart = ((ASTNode) ta.getFirst()).getStartPosition();
                    int searchEnd = node.getName().getStartPosition();
                    int open = findLastBracket(tokens, node.getStartPosition(), firstTaStart, '<');
                    int close = findLastBracket(tokens, firstTaStart, searchEnd, '>');
                    if (open >= 0) {
                        genericOpens.add(open);
                    }
                    if (close >= 0) {
                        genericCloses.add(close);
                        methodTypeArgCloses.add(close);
                        // If the closing is part of a `>>` or `>>>` token
                        // (e.g. `Foo.<Bar<Baz>>method()`), JDT may emit the
                        // `>>` as one token starting at `close - 1`. Mark
                        // that position too so the no-space rule fires.
                        if (close - 1 >= 0 && source.charAt(close - 1) == '>') {
                            methodTypeArgCloses.add(close - 1);
                        }
                        if (close - 2 >= 0 && source.charAt(close - 1) == '>' && source.charAt(close - 2) == '>') {
                            methodTypeArgCloses.add(close - 2);
                        }
                    }
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(ClassInstanceCreation node)
            {
                // Constructor type arguments: `new <T>Foo()`
                collectTypeParameterBrackets(node.typeArguments(), node.getStartPosition(), node.getType().getStartPosition());
                return super.visit(node);
            }

            @Override
            public boolean visit(ExpressionMethodReference node)
            {
                // Method reference type arguments: `Foo::<T>method`
                recordMethodReferenceTypeArgs(node.typeArguments(), node.getStartPosition(), node.getStartPosition() + node.getLength());
                return super.visit(node);
            }

            @Override
            public boolean visit(TypeMethodReference node)
            {
                // Type method reference type arguments: `Foo::<T>method` where LHS is a type.
                recordMethodReferenceTypeArgs(node.typeArguments(), node.getStartPosition(), node.getStartPosition() + node.getLength());
                return super.visit(node);
            }

            @Override
            public boolean visit(SuperMethodReference node)
            {
                recordMethodReferenceTypeArgs(node.typeArguments(), node.getStartPosition(), node.getStartPosition() + node.getLength());
                return super.visit(node);
            }

            @Override
            public boolean visit(CreationReference node)
            {
                // `Foo::<T>new` — the `new` keyword sits where `getName` would be;
                // search up to the end of the reference.
                recordMethodReferenceTypeArgs(node.typeArguments(), node.getStartPosition(), node.getStartPosition() + node.getLength());
                return super.visit(node);
            }

            private void recordMethodReferenceTypeArgs(List<?> ta, int searchStart, int searchEnd)
            {
                if (ta == null || ta.isEmpty()) {
                    return;
                }
                int open = findBracket(tokens, searchStart, searchEnd, '<');
                int close = findLastBracket(tokens, searchStart, searchEnd, '>');
                if (open >= 0) {
                    genericOpens.add(open);
                }
                if (close >= 0) {
                    genericCloses.add(close);
                    methodTypeArgCloses.add(close);
                    if (close - 1 >= 0 && source.charAt(close - 1) == '>') {
                        methodTypeArgCloses.add(close - 1);
                    }
                    if (close - 2 >= 0 && source.charAt(close - 1) == '>' && source.charAt(close - 2) == '>') {
                        methodTypeArgCloses.add(close - 2);
                    }
                }
            }

            @Override
            public boolean visit(SingleMemberAnnotation node)
            {
                recordIfWrapped(node);
                return super.visit(node);
            }

            @Override
            public boolean visit(NormalAnnotation node)
            {
                recordIfWrapped(node);
                return super.visit(node);
            }

            private void recordIfWrapped(Annotation ann)
            {
                int start = ann.getStartPosition();
                int end = start + ann.getLength();
                // Find the last newline within the annotation: if present,
                // the annotation spans multiple lines and warrants decomposition.
                for (int i = start; i < end && i < source.length(); i++) {
                    if (source.charAt(i) == '\n') {
                        wrappedAnnotations.put(start, ann);
                        return;
                    }
                }
            }

            private void collectTypeParameterBrackets(List<?> typeParams, int declStart, int declEnd)
            {
                if (typeParams == null || typeParams.isEmpty()) {
                    return;
                }
                ASTNode first = (ASTNode) typeParams.getFirst();
                ASTNode last = (ASTNode) typeParams.getLast();
                // `<` is right before the first type parameter.
                int open = findBracket(tokens, declStart, first.getStartPosition(), '<');
                // `>` is right after the last type parameter.
                int close = findBracket(tokens, last.getStartPosition() + last.getLength(), declEnd, '>');
                if (open >= 0) {
                    genericOpens.add(open);
                }
                if (close >= 0) {
                    genericCloses.add(close);
                    if (close - 1 >= 0 && source.charAt(close - 1) == '>') {
                        genericCloses.add(close - 1);
                    }
                    if (close - 2 >= 0 && source.charAt(close - 1) == '>' && source.charAt(close - 2) == '>') {
                        genericCloses.add(close - 2);
                    }
                }
            }
        });

        unit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(PrefixExpression node)
            {
                unaryPrefixOps.add(node.getStartPosition());
                return super.visit(node);
            }

            @Override
            public boolean visit(NumberLiteral node)
            {
                String token = node.getToken();
                if ((token.startsWith("-") || token.startsWith("+")) && token.length() > 1) {
                    unaryPrefixOps.add(node.getStartPosition());
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(Dimension node)
            {
                if (!node.annotations().isEmpty()) {
                    int dimStart = node.getStartPosition();
                    int dimEnd = dimStart + node.getLength();
                    for (int p = dimStart; p < dimEnd; p++) {
                        if (source.charAt(p) == '[') {
                            typeUseArrayLBrackets.add(p);
                            break;
                        }
                    }
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(GuardedPattern node)
            {
                int patternEnd = node.getPattern().getStartPosition() + node.getPattern().getLength();
                int guardStart = node.getExpression().getStartPosition();
                for (JavaTokens.Token tok : tokens) {
                    if (tok.start() < patternEnd || tok.start() >= guardStart) {
                        continue;
                    }
                    if (tok.type() == ITerminalSymbols.TokenNameIdentifier && "when".equals(tok.text())) {
                        switchGuardWhens.add(tok.start());
                        break;
                    }
                }
                return super.visit(node);
            }
        });

        return new JavaFormatMetadata(
                genericOpens,
                genericCloses,
                methodTypeArgCloses,
                castRParens,
                switchGuardWhens,
                unaryPrefixOps,
                ternaryColons,
                assertColons,
                typeUseArrayLBrackets,
                wrappedAnnotations);
    }

    /// Find the first occurrence of `target` character in any token in
    /// `[start, end)`. Token-based scanning automatically skips comments,
    /// strings, char literals, and markdown javadoc since those are their own
    /// token types (not code). Supports multi-character tokens like `>>` /
    /// `>>>` / `<=` / `>=` — scans each token's text.
    static int findBracket(List<JavaTokens.Token> tokens, int start, int end, char target)
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
    static int findLastBracket(List<JavaTokens.Token> tokens, int start, int end, char target)
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
}
