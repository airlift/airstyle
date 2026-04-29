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
import io.airlift.airstyle.engine.FormatProcessor;
import io.airlift.airstyle.model.SourceModel;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// End-to-end test: source → JavaBlockBuilder → engine → formatted source.
/// Validates the engine plumbs through real Java input and produces output.
/// The rules are still minimal, so the assertions check structural round-trip
/// rather than full Airlift-style conformance.
public class TestJavaBlockBuilder
{
    @Test
    public void emptyClassRoundTrip()
    {
        String source = "class Foo {}\n";
        String formatted = format(source);
        // Engine preserves single-line empty class.
        assertEquals("class Foo {}\n", formatted);
    }

    @Test
    public void packageAndImportsSpacing()
    {
        String source = "package p;\nimport java.util.List;\nclass Foo {}\n";
        String formatted = format(source);
        // Preserves line breaks between package, import, and class.
        assertEquals("package p;\nimport java.util.List;\n\nclass Foo {}\n", formatted);
    }

    @Test
    public void trailingTopLevelCommentIsPreserved()
    {
        String source =
                """
                class Foo {}
                // trailing
                """;
        assertEquals(source, format(source));
    }

    @Test
    public void classWithSingleFieldIndented()
    {
        String source = "class Foo { int x; }";
        String formatted = format(source);
        // Airlift style: CLASS_BRACE_STYLE=NEXT_LINE, so `{` goes on its own line.
        // Member indented at NORMAL (4 spaces).
        assertEquals("class Foo\n{\n    int x;\n}", formatted);
    }

    @Test
    public void wrappedRecordEmptyBodyCollapsesInline()
    {
        // Airlift style: records with wrapped parameters and an empty body
        // collapse the body to inline `{}` regardless of source shape —
        // `) {}` on the closing-paren line.
        String source =
                """
                public record Tag(
                        String key,
                        String value)
                {
                }
                """;
        String formatted = format(source);
        assertEquals(
                """
                public record Tag(
                        String key,
                        String value) {}
                """,
                formatted);
    }

    @Test
    public void wrappedRecordWithInlineBraceStaysInline()
    {
        String source =
                """
                public record Tag(String key, String value) {}
                """;
        String formatted = format(source);
        assertEquals(
                """
                public record Tag(String key, String value) {}
                """,
                formatted);
    }

    @Test
    public void classWithMethodBodyFormattedCorrectly()
    {
        String source =
                """
                class Foo
                {
                    int x()
                    {
                        return 1;
                    }
                }
                """;
        String formatted = format(source);
        assertEquals(
                """
                class Foo
                {
                    int x()
                    {
                        return 1;
                    }
                }
                """,
                formatted);
    }

    @Test
    public void methodBodyWithMultipleStatements()
    {
        // Each statement indents at NORMAL (+4) from the body brace.
        String source =
                """
                class Foo
                {
                    void run()
                    {
                        int a = 1;
                        int b = 2;
                        return;
                    }
                }
                """;
        String formatted = format(source);
        assertEquals(
                """
                class Foo
                {
                    void run()
                    {
                        int a = 1;
                        int b = 2;
                        return;
                    }
                }
                """,
                formatted);
    }

    @Test
    public void emptyMethodBodyKeepsInlineBraces()
    {
        String source =
                """
                class Foo
                {
                    void run()
                    {
                    }
                }
                """;
        String formatted = format(source);
        // Empty method body collapses its braces inline after a single-line
        // signature.
        assertEquals(
                """
                class Foo
                {
                    void run() {}
                }
                """,
                formatted);
    }

    @Test
    public void adjacentModifierAndAnnotationAreSeparated()
    {
        String source =
                """
                class Foo
                {
                    public@Deprecated void run()
                    {}
                }
                """;
        String formatted = format(source);
        assertEquals(
                """
                class Foo
                {
                    public @Deprecated void run() {}
                }
                """,
                formatted);
    }

    @Test
    public void fieldWithArrayIndexAccess()
    {
        String source =
                """
                class Foo
                {
                    int read(int[] a)
                    {
                        return a[0];
                    }
                }
                """;
        String formatted = format(source);
        assertEquals(
                """
                class Foo
                {
                    int read(int[] a)
                    {
                        return a[0];
                    }
                }
                """,
                formatted);
    }

    @Test
    public void classWithExtendsClauseOnOwnLine()
    {
        // Empty body always renders as `{}` inline on the last header line —
        // even if source put `{` on its own line.
        String source =
                """
                class Foo extends Bar
                {
                }
                """;
        String formatted = format(source);
        assertEquals(
                """
                class Foo extends Bar {}
                """,
                formatted);
    }

    @Test
    public void interfaceWithMultipleSuperTypesOnOwnLine()
    {
        // Source has `extends` inline; engine preserves source shape.
        // POST_FORMAT typeHierarchyClause handles the wrap decision.
        // Empty body collapses to `{}` inline (source had it inline).
        String source =
                """
                interface Foo extends Bar, Baz {}
                """;
        String formatted = format(source);
        assertEquals(
                """
                interface Foo extends Bar, Baz {}
                """,
                formatted);
    }

    @Test
    public void methodWithAnnotationOnParameter()
    {
        String source =
                """
                class Foo
                {
                    int run(@Nullable String s)
                    {
                        return 1;
                    }
                }
                """;
        String formatted = format(source);
        assertEquals(
                """
                class Foo
                {
                    int run(@Nullable String s)
                    {
                        return 1;
                    }
                }
                """,
                formatted);
    }

    @Test
    public void methodAnnotationLinesDoNotMakeSingleParameterHeaderWrapped()
            throws Exception
    {
        String source =
                """
                class Test
                {
                    @Deprecated
                    // method-prefix comment
                    public Test setEnabled(boolean enabled)
                    {
                        return this;
                    }
                }
                """;

        assertFalse(hasWrappedParameters(source));
    }

    @Test
    public void singleParameterHeaderIsWrappedWhenParameterStartsOnNewLine()
            throws Exception
    {
        String source =
                """
                class Test
                {
                    public Test setEnabled(
                            boolean enabled)
                    {
                        return this;
                    }
                }
                """;

        assertTrue(hasWrappedParameters(source));
    }

    @Test
    public void nestedRecordWithWrappedParamsKeepsBracePosition()
    {
        // Nested record with wrapped params and original brace on own line.
        // Same KEEP_SIMPLE pattern as the top-level case, exercising the
        // deeper composite structure (the record sits inside a class body).
        String source =
                """
                class Outer
                {
                    private record CredentialFixture(
                            String clientId,
                            String clientSecret,
                            String serviceRole,
                            String userInfo)
                    {
                    }
                }
                """;
        String formatted = format(source);
        assertEquals(
                """
                class Outer
                {
                    private record CredentialFixture(
                            String clientId,
                            String clientSecret,
                            String serviceRole,
                            String userInfo) {}
                }
                """,
                formatted);
    }

    @Test
    public void classWithMultipleMethods()
    {
        String source =
                """
                class Foo
                {
                    int a()
                    {
                        return 1;
                    }

                    int b()
                    {
                        return 2;
                    }
                }
                """;
        String formatted = format(source);
        assertEquals(
                """
                class Foo
                {
                    int a()
                    {
                        return 1;
                    }

                    int b()
                    {
                        return 2;
                    }
                }
                """,
                formatted);
    }

    @Test
    public void ifStatementWithBlockBodies()
    {
        // Airlift BRACE_STYLE=END_OF_LINE for control-flow braces:
        // `if (x > 0) {` with `{` on the same line as the header.
        String source =
                """
                class Foo
                {
                    int check(int x)
                    {
                        if (x > 0) {
                            return 1;
                        }
                        return 0;
                    }
                }
                """;
        String formatted = format(source);
        assertEquals(source, formatted);
    }

    @Test
    public void tryCatchFinallyBlock()
    {
        // END_OF_LINE brace for try/catch/finally; keyword on its own line.
        String source =
                """
                class Foo
                {
                    void run()
                    {
                        try {
                            System.out.println("a");
                        }
                        catch (Exception e) {
                            return;
                        }
                        finally {
                            System.out.println("done");
                        }
                    }
                }
                """;
        String formatted = format(source);
        assertEquals(source, formatted);
    }

    @Test
    public void whileLoopWithBlockBody()
    {
        String source =
                """
                class Foo
                {
                    void run(int x)
                    {
                        while (isPositive(x)) {
                            decrement();
                        }
                    }
                }
                """;
        String formatted = format(source);
        assertEquals(source, formatted);
    }

    @Test
    public void whileLoopWithSimpleStatementBodyUsesStatementBuilder()
    {
        String source =
                """
                class Foo
                {
                    void run(boolean ready)
                    {
                        while (ready())
                            consume(
                                    first(),
                                    second());
                    }
                }
                """;
        String formatted = format(source);
        assertEquals(source, formatted);
    }

    @Test
    public void forLoopWithSimpleStatementBodyUsesStatementBuilder()
    {
        String source =
                """
                class Foo
                {
                    void run(boolean ready)
                    {
                        for (; ready(); advance())
                            consume(
                                    first(),
                                    second());
                    }
                }
                """;
        String formatted = format(source);
        assertEquals(source, formatted);
    }

    @Test
    public void doWhileLoopWithSimpleStatementBodyUsesStatementBuilder()
    {
        String source =
                """
                class Foo
                {
                    void run(boolean ready)
                    {
                        do
                            consume(
                                    first(),
                                    second());
                        while (ready());
                    }
                }
                """;
        String formatted = format(source);
        assertEquals(source, formatted);
    }

    @Test
    public void ternaryExpressionInReturn()
    {
        String source =
                """
                class Foo
                {
                    int sign(int x)
                    {
                        return x == 0 ? 0 : 1;
                    }
                }
                """;
        String formatted = format(source);
        assertEquals(source, formatted);
    }

    @Test
    public void lambdaExpressionInVariable()
    {
        String source =
                """
                class Foo
                {
                    void run()
                    {
                        Runnable r = () -> System.out.println("hi");
                    }
                }
                """;
        String formatted = format(source);
        assertEquals(source, formatted);
    }

    @Test
    public void chainWithExpressionLambdaArgument()
    {
        // Common Airlift pattern: chained builder with an inline lambda arg.
        String source =
                """
                class Foo
                {
                    Builder run(Builder b)
                    {
                        return b.map(x -> x.toString())
                                .toList();
                    }
                }
                """;
        String formatted = format(source);
        assertEquals(source, formatted);
    }

    @Test
    public void lambdaWithBlockBodyInVariable()
    {
        String source =
                """
                class Foo
                {
                    void run()
                    {
                        Runnable r = () -> {
                            System.out.println("a");
                            System.out.println("b");
                        };
                    }
                }
                """;
        String formatted = format(source);
        assertEquals(source, formatted);
    }

    @Test
    public void classInstanceCreationInVariable()
    {
        String source =
                """
                class Foo
                {
                    void run()
                    {
                        Object o = new Object();
                    }
                }
                """;
        String formatted = format(source);
        assertEquals(source, formatted);
    }

    @Test
    public void sealedInterfaceWithExtendsAndPermits()
    {
        // Empty body always renders as `{}` inline on the last header line —
        // even with wrapped extends/permits clauses.
        String source =
                """
                package p;

                public sealed interface Fruit
                        extends Plant
                        permits Apple, Banana
                {
                }
                """;
        String formatted = format(source);
        assertEquals(
                """
                package p;

                public sealed interface Fruit
                        extends Plant
                        permits Apple, Banana {}
                """,
                formatted);
    }

    @Test
    public void qualifiedInterfaceNameContainingPermitsIsNotAClause()
    {
        String source =
                """
                class Foo implements com.example.permits.Bar {}
                """;
        assertEquals(source, format(source));
    }

    @Test
    public void recordHeaderIgnoresCommentParenthesisBeforeComponents()
    {
        String source =
                """
                public record Tag /* marker ( */ (String key, String value) {}
                """;
        assertEquals(source, format(source));
    }

    @Test
    public void switchCaseIdentifierNamedWhenIsNotTreatedAsGuard()
    {
        String source =
                """
                class Test
                {
                    Object run(Object value)
                    {
                        return switch (value) {
                            case when,
                                 other -> value;
                            default -> value;
                        };
                    }
                }
                """;
        assertEquals(source, format(source));
    }

    @Test
    public void interfaceWithMethodDeclarations()
    {
        String source =
                """
                package p;

                public interface Holder
                {
                    String name();

                    int size();
                }
                """;
        String formatted = format(source);
        assertEquals(source, formatted);
    }

    @Test
    public void throwNewException()
    {
        String source =
                """
                class Foo
                {
                    void run()
                    {
                        throw new RuntimeException("oops");
                    }
                }
                """;
        String formatted = format(source);
        assertEquals(source, formatted);
    }

    @Test
    public void switchStatementWithCases()
    {
        String source =
                """
                class Foo
                {
                    int select(int x)
                    {
                        switch (x) {
                            case 1:
                                return 10;
                            case 2:
                                return 20;
                            default:
                                return 0;
                        }
                    }
                }
                """;
        String formatted = format(source);
        assertEquals(source, formatted);
    }

    @Test
    public void enhancedForLoopWithBlockBody()
    {
        String source =
                """
                class Foo
                {
                    void run(java.util.List<String> items)
                    {
                        for (String item : items) {
                            System.out.println(item);
                        }
                    }
                }
                """;
        String formatted = format(source);
        assertEquals(source, formatted);
    }

    @Test
    public void parameterizedTypeFieldGenerics()
    {
        String source =
                """
                class Foo
                {
                    private final java.util.List<String> items = new java.util.ArrayList<>();
                }
                """;
        String formatted = format(source);
        assertEquals(source, formatted);
    }

    @Test
    public void binaryOperatorsGetSpaceAround()
    {
        String source =
                """
                class Foo
                {
                    int compute(int a, int b)
                    {
                        int sum = a + b;
                        int diff = a - b;
                        int prod = a * b;
                        return sum;
                    }
                }
                """;
        String formatted = format(source);
        assertEquals(source, formatted);
    }

    @Test
    public void equalityOperatorWithSpace()
    {
        String source =
                """
                class Foo
                {
                    boolean same(Object a, Object b)
                    {
                        return a == b;
                    }
                }
                """;
        String formatted = format(source);
        assertEquals(source, formatted);
    }

    @Test
    public void mapOfGenericsSpacing()
    {
        String source =
                """
                class Foo
                {
                    private final java.util.Map<String, Integer> map = null;
                }
                """;
        String formatted = format(source);
        assertEquals(source, formatted);
    }

    @Test
    public void ifElseWithBlockBodies()
    {
        String source =
                """
                class Foo
                {
                    int check(int x)
                    {
                        if (x > 0) {
                            return 1;
                        }
                        else {
                            return 0;
                        }
                    }
                }
                """;
        String formatted = format(source);
        assertEquals(source, formatted);
    }

    @Test
    public void ifElseWithSimpleStatementBodiesUsesStatementBuilder()
    {
        String source =
                """
                class Foo
                {
                    void run(boolean ready)
                    {
                        if (ready())
                            consume(
                                    first(),
                                    second());
                        else
                            fallback(
                                    third(),
                                    fourth());
                    }
                }
                """;
        String formatted = format(source);
        assertEquals(source, formatted);
    }

    @Test
    public void realFileWithLicenseHeaderAndAnnotatedRecord()
    {
        // Full-file test: license header comment + package + imports +
        // class-level annotation + record with annotated wrapped components
        // + empty body with brace on its own line. Exercises: preamble
        // preservation, inter-block spacing, annotation-on-parameter
        // spacing, wrapped-record CONTINUATION indent, KEEP_SIMPLE empty
        // body collapse.
        String source =
                """
                /*
                 * Copyright 2024 Example Inc.
                 *
                 * Licensed under the Apache License, Version 2.0.
                 */
                package com.example.entities;

                import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
                import io.swagger.v3.oas.annotations.media.Schema;

                import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

                @JsonIgnoreProperties(ignoreUnknown = true)
                public record Tag(
                        @Schema(requiredMode = REQUIRED) String key,
                        @Schema(requiredMode = REQUIRED) String value) {}
                """;
        String formatted = format(source);
        assertEquals(source, formatted);
    }

    @Test
    public void realWorldStyleClass()
    {
        // Representative of real Airlift-style class. Validates end-to-end
        // that the engine produces reasonable structural output.
        String source =
                """
                package io.example;

                import java.util.List;

                public class Example
                {
                    private final String name;

                    public Example(String name)
                    {
                        this.name = name;
                    }

                    public String name()
                    {
                        return name;
                    }
                }
                """;
        String formatted = format(source);
        assertEquals(
                """
                package io.example;

                import java.util.List;

                public class Example
                {
                    private final String name;

                    public Example(String name)
                    {
                        this.name = name;
                    }

                    public String name()
                    {
                        return name;
                    }
                }
                """,
                formatted);
    }

    @Test
    public void complexClassWithManyConstructs()
    {
        // Representative of Airlift-style idiomatic code: package, imports,
        // class with field, constructor, multiple methods, generics, for-loop,
        // try-catch. Exercises most of the engine's current coverage together.
        String source =
                """
                package io.example;

                import java.util.ArrayList;
                import java.util.List;

                public class Processor
                {
                    private final List<String> items;

                    public Processor(List<String> items)
                    {
                        this.items = items;
                    }

                    public void run()
                    {
                        try {
                            process();
                        }
                        catch (RuntimeException e) {
                            report(e);
                        }
                    }

                    private void process()
                    {
                        int total = 0;
                        for (String item : items) {
                            total = total + item.length();
                        }
                    }

                    private void report(Exception e) {}
                }
                """;
        String formatted = format(source);
        assertEquals(source, formatted);
    }

    @Test
    public void simpleMethodChainOnOneLine()
    {
        // Inline chain — engine should round-trip without modification.
        String source =
                """
                class Foo
                {
                    String build(Builder b)
                    {
                        return b.name("x").value(1).build();
                    }
                }
                """;
        String formatted = format(source);
        assertEquals(source, formatted);
    }

    @Test
    public void wrappedMethodChainWithContinuationIndent()
    {
        // Each selector on its own line; engine preserves this via the
        // smart indent on selector blocks.
        String source =
                """
                class Foo
                {
                    String build(Builder b)
                    {
                        return b.name("x")
                                .value(1)
                                .build();
                    }
                }
                """;
        String formatted = format(source);
        assertEquals(source, formatted);
    }

    @Test
    public void chainInExpressionStatement()
    {
        String source =
                """
                class Foo
                {
                    void run(Builder b)
                    {
                        b.first()
                                .second();
                    }
                }
                """;
        String formatted = format(source);
        assertEquals(source, formatted);
    }

    @Test
    public void chainInVariableDeclaration()
    {
        String source =
                """
                class Foo
                {
                    void run(Builder b)
                    {
                        String result = b.name("x")
                                .value(1)
                                .build();
                    }
                }
                """;
        String formatted = format(source);
        assertEquals(source, formatted);
    }

    @Test
    public void shortFileWithLicenseHeaderAndSingleLineRecord()
    {
        String source =
                """
                /*
                 * Copyright 2024 Example Inc.
                 *
                 * Licensed under the Apache License, Version 2.0.
                 */
                package com.example.entities;

                public record CloudRef(String account, String region, String zone) {}
                """;
        String formatted = format(source);
        assertEquals(source, formatted);
    }

    // ------------ Canonicalization tests ------------
    // Verify the engine ACTIVELY FIXES formatting, not just preserves
    // correctly-formatted input.

    @Test
    public void classCanonicalizesToNextLineBrace()
    {
        String source = "class Foo{int x;}";
        String formatted = format(source);
        assertEquals(
                """
                class Foo
                {
                    int x;
                }""",
                formatted);
    }

    @Test
    public void methodBodyCanonicalizesIndent()
    {
        String source =
                """
                class Foo
                {
                    void run(){System.out.println("hi");}
                }
                """;
        String formatted = format(source);
        assertEquals(
                """
                class Foo
                {
                    void run()
                    {
                        System.out.println("hi");
                    }
                }
                """,
                formatted);
    }

    @Test
    public void missingIndentIsAdded()
    {
        String source =
                """
                class Foo
                {
                int x;
                int y;
                }
                """;
        String formatted = format(source);
        assertEquals(
                """
                class Foo
                {
                    int x;
                    int y;
                }
                """,
                formatted);
    }

    @Test
    public void commentBetweenStatementsInMethodBody()
    {
        // Engine preserves inter-statement comments at correct indent.
        String source =
                """
                class Test
                {
                    void run()
                    {
                        work();
                        // comment
                        done();
                    }
                }
                """;
        assertEquals(source, format(source));
    }

    @Test
    public void methodTypeParameterSpacePreserved()
    {
        // Space between keyword and `<` must be preserved for method type
        // parameters: `public <T>`, `static <K, V>`, not `public<T>`.
        String source =
                """
                class Foo
                {
                    public <T> T identity(T value)
                    {
                        return value;
                    }

                    private static <K, V> Map<K, V> wrap(Map<K, V> map)
                    {
                        return map;
                    }

                    <T extends Comparable<T>> T max(T a, T b)
                    {
                        return a.compareTo(b) >= 0 ? a : b;
                    }
                }
                """;
        assertEquals(source, format(source));
    }

    private static String format(String source)
    {
        var unit = SourceModel.create(source).compilationUnit();
        Block root = JavaBlockBuilder.build(unit, source);
        return new FormatProcessor().format(root, source);
    }

    private static boolean hasWrappedParameters(String source)
            throws Exception
    {
        var unit = SourceModel.create(source).compilationUnit();
        JavaBlockBuilder builder = JavaBlockBuilder.forTesting(unit, source);

        return builder.hasWrappedParametersForTesting(firstMethodDeclaration(unit));
    }

    private static MethodDeclaration firstMethodDeclaration(org.eclipse.jdt.core.dom.CompilationUnit unit)
    {
        List<MethodDeclaration> methods = new ArrayList<>();
        unit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(MethodDeclaration node)
            {
                methods.add(node);
                return false;
            }
        });
        return methods.getFirst();
    }
}
