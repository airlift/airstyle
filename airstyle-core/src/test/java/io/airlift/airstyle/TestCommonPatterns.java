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
package io.airlift.airstyle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.airlift.airstyle.FormatterAssertions.assertCanonicalFormatting;
import static io.airlift.airstyle.FormatterAssertions.assertFormatsOldToNew;

/// Tests for common coding patterns that require careful formatting preservation.
///
/// These tests verify representative formatting patterns used by the style.
@DisplayName("Common Formatting Patterns")
public class TestCommonPatterns
{
    // =========================================================================
    // Builder Pattern
    // =========================================================================

    @Nested
    @DisplayName("Builder Pattern")
    class BuilderPatternTests
    {
        @Test
        @DisplayName("Fluent builder chain - preserve line breaks")
        void testFluentBuilderChain()
        {
            String input =
                    """
                    class Test
                    {
                        void method()
                        {
                            Foo result = Foo.builder()
                                    .name("test")
                                    .value(42)
                                    .enabled(true)
                                    .build();
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        void method()
                        {
                            Foo result = Foo.builder()
                                    .name("test")
                                    .value(42)
                                    .enabled(true)
                                    .build();
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("Builder with long arguments")
        void testBuilderWithLongArguments()
        {
            String input =
                    """
                    class Test
                    {
                        void method()
                        {
                            Config config = Config.builder()
                                    .connectionString("jdbc:postgresql://localhost:5432/database")
                                    .timeout(Duration.ofSeconds(30))
                                    .retryPolicy(RetryPolicy.exponentialBackoff())
                                    .build();
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        void method()
                        {
                            Config config = Config.builder()
                                    .connectionString("jdbc:postgresql://localhost:5432/database")
                                    .timeout(Duration.ofSeconds(30))
                                    .retryPolicy(RetryPolicy.exponentialBackoff())
                                    .build();
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("Inline builder - preserve compact form")
        void testInlineBuilder()
        {
            String input =
                    """
                    class Test
                    {
                        void method()
                        {
                            Foo result = Foo.builder().name("x").build();
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        void method()
                        {
                            Foo result = Foo.builder().name("x").build();
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }
    }

    // =========================================================================
    // Method Chaining with Lambdas
    // =========================================================================

    @Nested
    @DisplayName("Method Chaining with Lambdas")
    class MethodChainingTests
    {
        @Test
        @DisplayName("Stream with lambdas - preserve line breaks")
        void testStreamWithLambdas()
        {
            String input =
                    """
                    class Test
                    {
                        void method(List<String> items)
                        {
                            List<String> result = items.stream()
                                    .filter(s -> !s.isEmpty())
                                    .map(String::toUpperCase)
                                    .sorted()
                                    .collect(toList());
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        void method(List<String> items)
                        {
                            List<String> result = items.stream()
                                    .filter(s -> !s.isEmpty())
                                    .map(String::toUpperCase)
                                    .sorted()
                                    .collect(toList());
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("Stream with multi-line lambdas")
        void testStreamWithMultiLineLambdas()
        {
            String input =
                    """
                    class Test
                    {
                        void method(List<String> items)
                        {
                            items.stream()
                                    .filter(s -> {
                                        if (s.isEmpty()) {
                                            return false;
                                        }
                                        return s.startsWith("A");
                                    })
                                    .forEach(System.out::println);
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        void method(List<String> items)
                        {
                            items.stream()
                                    .filter(s -> {
                                        if (s.isEmpty()) {
                                            return false;
                                        }
                                        return s.startsWith("A");
                                    })
                                    .forEach(System.out::println);
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("CompletableFuture chain")
        void testCompletableFutureChain()
        {
            String input =
                    """
                    class Test
                    {
                        CompletableFuture<String> method()
                        {
                            return CompletableFuture.supplyAsync(() -> fetchData())
                                    .thenApply(this::transform)
                                    .thenCompose(this::asyncOperation)
                                    .exceptionally(ex -> {
                                        log.error("Error", ex);
                                        return defaultValue();
                                    });
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        CompletableFuture<String> method()
                        {
                            return CompletableFuture.supplyAsync(() -> fetchData())
                                    .thenApply(this::transform)
                                    .thenCompose(this::asyncOperation)
                                    .exceptionally(ex -> {
                                        log.error("Error", ex);
                                        return defaultValue();
                                    });
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }
    }

    // =========================================================================
    // Ternary Expressions
    // =========================================================================

    @Nested
    @DisplayName("Ternary Expressions")
    class TernaryExpressionTests
    {
        @Test
        @DisplayName("Simple ternary on one line")
        void testSimpleTernaryOneLine()
        {
            String input =
                    """
                    class Test
                    {
                        String method(boolean flag)
                        {
                            return flag ? "yes" : "no";
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        String method(boolean flag)
                        {
                            return flag ? "yes" : "no";
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("Multi-line ternary with method calls")
        void testMultiLineTernary()
        {
            String input =
                    """
                    class Test
                    {
                        String method(boolean flag)
                        {
                            return flag
                                    ? computeYes()
                                    : computeNo();
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        String method(boolean flag)
                        {
                            return flag
                                    ? computeYes()
                                    : computeNo();
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("Nested ternary expressions")
        void testNestedTernary()
        {
            String input =
                    """
                    class Test
                    {
                        String method(int value)
                        {
                            return value < 0
                                    ? "negative"
                                    : value == 0
                                            ? "zero"
                                            : "positive";
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        String method(int value)
                        {
                            return value < 0
                                    ? "negative"
                                    : value == 0
                                      ? "zero"
                                      : "positive";
                        }
                    }
                    """;

            assertFormatsOldToNew(input, expected);
        }

        @Test
        @DisplayName("Nested ternary initializer")
        void testFormatterFixesNestedTernaryInitializerIndent()
        {
            String input =
                    """
                    class Test
                    {
                        Object method(Node firstNode, Node secondNode, Node thirdNode)
                        {
                            Object selectedNode = !firstNode.isMissingNode() ? firstNode
                                    : !secondNode.isMissingNode() ? secondNode
                            : !thirdNode.isMissingNode() ? thirdNode
                            : firstNode;
                            return selectedNode;
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        Object method(Node firstNode, Node secondNode, Node thirdNode)
                        {
                            Object selectedNode = !firstNode.isMissingNode() ? firstNode
                                    : !secondNode.isMissingNode() ? secondNode
                                      : !thirdNode.isMissingNode() ? thirdNode
                                        : firstNode;
                            return selectedNode;
                        }
                    }
                    """;

            assertFormatsOldToNew(input, expected);
        }
    }

    // =========================================================================
    // Array Initializers
    // =========================================================================

    @Nested
    @DisplayName("Array Initializers")
    class ArrayInitializerTests
    {
        @Test
        @DisplayName("Multi-line array initializer")
        void testMultiLineArrayInitializer()
        {
            String input =
                    """
                    class Test
                    {
                        int[] data = {
                                1, 2, 3,
                                4, 5, 6,
                                7, 8, 9,
                        };
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        int[] data = {
                                1, 2, 3,
                                4, 5, 6,
                                7, 8, 9,
                        };
                    }
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("Inline array initializer")
        void testInlineArrayInitializer()
        {
            String input =
                    """
                    class Test
                    {
                        int[] data = {1, 2, 3, 4, 5};
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        int[] data = {1, 2, 3, 4, 5};
                    }
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("Object array with new expressions")
        void testObjectArrayInitializer()
        {
            String input =
                    """
                    class Test
                    {
                        Point[] points = {
                                new Point(0, 0),
                                new Point(1, 0),
                                new Point(0, 1),
                        };
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        Point[] points = {
                                new Point(0, 0),
                                new Point(1, 0),
                                new Point(0, 1),
                        };
                    }
                    """;

            assertCanonicalFormatting(input);
        }
    }

    // =========================================================================
    // Annotation Stacking
    // =========================================================================

    @Nested
    @DisplayName("Annotation Stacking")
    class AnnotationStackingTests
    {
        @Test
        @DisplayName("Multiple annotations on method")
        void testMultipleAnnotationsOnMethod()
        {
            String input =
                    """
                    class Test
                    {
                        @Nullable
                        @Override
                        @SuppressWarnings("unchecked")
                        public String method()
                        {
                            return null;
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        @Nullable
                        @Override
                        @SuppressWarnings("unchecked")
                        public String method()
                        {
                            return null;
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("Inline annotations on parameters")
        void testInlineAnnotationsOnParameters()
        {
            String input =
                    """
                    class Test
                    {
                        void method(@Nullable String name, @NotNull String value)
                        {
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        void method(@Nullable String name, @NotNull String value) {}
                    }
                    """;

            assertFormatsOldToNew(input, expected);
        }

        @Test
        @DisplayName("Annotation with parameters")
        void testAnnotationWithParameters()
        {
            String input =
                    """
                    class Test
                    {
                        @JsonProperty(value = "user_name", required = true)
                        @NotNull
                        private String userName;
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        @JsonProperty(value = "user_name", required = true)
                        @NotNull
                        private String userName;
                    }
                    """;

            assertCanonicalFormatting(input);
        }
    }

    // =========================================================================
    // Complex Generics
    // =========================================================================

    @Nested
    @DisplayName("Complex Generics")
    class ComplexGenericsTests
    {
        @Test
        @DisplayName("Deeply nested generics")
        void testDeeplyNestedGenerics()
        {
            String input =
                    """
                    class Test
                    {
                        Map<String, List<Function<Integer, Optional<String>>>> map;
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        Map<String, List<Function<Integer, Optional<String>>>> map;
                    }
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("Wildcards with bounds")
        void testWildcardsWithBounds()
        {
            String input =
                    """
                    class Test
                    {
                        void method(List<? extends Number> numbers, List<? super Integer> integers)
                        {
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        void method(List<? extends Number> numbers, List<? super Integer> integers) {}
                    }
                    """;

            assertFormatsOldToNew(input, expected);
        }

        @Test
        @DisplayName("Generic method with multiple type parameters")
        void testGenericMethodMultipleTypes()
        {
            String input =
                    """
                    class Test
                    {
                        <K, V extends Comparable<V>> Map<K, V> sort(Map<K, V> input)
                        {
                            return input;
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        <K, V extends Comparable<V>> Map<K, V> sort(Map<K, V> input)
                        {
                            return input;
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }
    }

    // =========================================================================
    // Try-With-Resources
    // =========================================================================

    @Nested
    @DisplayName("Try-With-Resources")
    class TryWithResourcesTests
    {
        @Test
        @DisplayName("Single resource")
        void testSingleResource()
        {
            String input =
                    """
                    class Test
                    {
                        void method()
                        {
                            try (InputStream is = new FileInputStream("file.txt")) {
                                process(is);
                            }
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        void method()
                        {
                            try (InputStream is = new FileInputStream("file.txt")) {
                                process(is);
                            }
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("Multiple resources")
        void testMultipleResources()
        {
            String input =
                    """
                    class Test
                    {
                        void method()
                        {
                            try (InputStream is = new FileInputStream("in.txt");
                                    OutputStream os = new FileOutputStream("out.txt")) {
                                transfer(is, os);
                            }
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        void method()
                        {
                            try (InputStream is = new FileInputStream("in.txt");
                                    OutputStream os = new FileOutputStream("out.txt")) {
                                transfer(is, os);
                            }
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("Effectively final resources")
        void testEffectivelyFinalResources()
        {
            String input =
                    """
                    class Test
                    {
                        void method(InputStream in, OutputStream out)
                        {
                            try (in; out) {
                                transfer(in, out);
                            }
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        void method(InputStream in, OutputStream out)
                        {
                            try (in; out) {
                                transfer(in, out);
                            }
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }
    }

    // =========================================================================
    // Long Method Signatures
    // =========================================================================

    @Nested
    @DisplayName("Long Method Signatures")
    class LongMethodSignatureTests
    {
        @Test
        @DisplayName("Long parameter list - wrapped")
        void testLongParameterListWrapped()
        {
            String input =
                    """
                    class Test
                    {
                        void method(
                                String firstName,
                                String lastName,
                                String email,
                                String phone,
                                String address)
                        {
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        void method(
                                String firstName,
                                String lastName,
                                String email,
                                String phone,
                                String address)
                        {}
                    }
                    """;

            assertFormatsOldToNew(input, expected);
        }

        @Test
        @DisplayName("Method with throws clause")
        void testMethodWithThrowsClause()
        {
            String input =
                    """
                    class Test
                    {
                        void method(String input)
                                throws IOException, SQLException, IllegalStateException
                        {
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        void method(String input)
                                throws IOException, SQLException, IllegalStateException
                        {}
                    }
                    """;

            assertFormatsOldToNew(input, expected);
        }
    }

    // =========================================================================
    // Comment Preservation
    // =========================================================================

    @Nested
    @DisplayName("Comment Preservation")
    class CommentPreservationTests
    {
        @Test
        @DisplayName("Trailing comments on same line")
        void testTrailingComments()
        {
            String input =
                    """
                    class Test
                    {
                        int x = 1; // first value
                        int y = 2; // second value
                        int z = 3; // third value
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        int x = 1; // first value
                        int y = 2; // second value
                        int z = 3; // third value
                    }
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("Block comment before method")
        void testBlockCommentBeforeMethod()
        {
            String input =
                    """
                    class Test
                    {
                        /* This is a legacy method
                         * that should not be used
                         * in new code */
                        @Deprecated
                        void oldMethod()
                        {
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        /* This is a legacy method
                         * that should not be used
                         * in new code */
                        @Deprecated
                        void oldMethod() {}
                    }
                    """;

            assertFormatsOldToNew(input, expected);
        }

        @Test
        @DisplayName("Inline comments in code")
        void testInlineComments()
        {
            String input =
                    """
                    class Test
                    {
                        void method()
                        {
                            int result = compute() // get initial value
                                    + adjustment   // apply adjustment
                                    - offset;      // remove offset
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        void method()
                        {
                            int result = compute() // get initial value
                                    + adjustment   // apply adjustment
                                    - offset;      // remove offset
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }
    }

    // =========================================================================
    // Lambda Expressions
    // =========================================================================

    @Nested
    @DisplayName("Lambda Expressions")
    class LambdaExpressionTests
    {
        @Test
        @DisplayName("Simple expression lambda")
        void testSimpleExpressionLambda()
        {
            String input =
                    """
                    class Test
                    {
                        void method(List<String> list)
                        {
                            list.forEach(s -> System.out.println(s));
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        void method(List<String> list)
                        {
                            list.forEach(s -> System.out.println(s));
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("Block lambda with multiple statements")
        void testBlockLambda()
        {
            String input =
                    """
                    class Test
                    {
                        void method(List<String> list)
                        {
                            list.forEach(s -> {
                                log.info("Processing: {}", s);
                                process(s);
                            });
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        void method(List<String> list)
                        {
                            list.forEach(s -> {
                                log.info("Processing: {}", s);
                                process(s);
                            });
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("Lambda with explicit types")
        void testLambdaWithExplicitTypes()
        {
            String input =
                    """
                    class Test
                    {
                        BiFunction<String, Integer, String> fn = (String s, Integer i) -> s.repeat(i);
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        BiFunction<String, Integer, String> fn = (String s, Integer i) -> s.repeat(i);
                    }
                    """;

            assertCanonicalFormatting(input);
        }
    }

    @Nested
    @DisplayName("Exact Output Coverage")
    class ExactOutputCoverageTests
    {
        @Test
        @DisplayName("Builder chain")
        void testFormatterFormatsBuilderChainExactly()
        {
            String oldCode =
                    """
                    class Test {
                        Foo build() {
                            return Foo.builder()
                                    .name("test")
                                    .value(42)
                                    .enabled(true)
                                    .build();
                        }
                    }
                    """;

            String newCode =
                    """
                    class Test
                    {
                        Foo build()
                        {
                            return Foo.builder()
                                    .name("test")
                                    .value(42)
                                    .enabled(true)
                                    .build();
                        }
                    }
                    """;

            assertFormatsOldToNew(oldCode, newCode);
        }

        @Test
        @DisplayName("Method chain with block lambda")
        void testFormatterKeepsMethodChainWithBlockLambdaExactly()
        {
            String code =
                    """
                    class Test
                    {
                        void method(List<String> items)
                        {
                            items.stream()
                                    .filter(s -> {
                                        if (s.isEmpty()) {
                                            return false;
                                        }
                                        return s.startsWith("A");
                                    })
                                    .forEach(System.out::println);
                        }
                    }
                    """;

            assertCanonicalFormatting(code);
        }

        @Test
        @DisplayName("Multiple try-with-resources")
        void testFormatterKeepsMultipleTryResourcesExactly()
        {
            String code =
                    """
                    class Test
                    {
                        void method()
                        {
                            try (InputStream is = new FileInputStream("in.txt");
                                    OutputStream os = new FileOutputStream("out.txt")) {
                                transfer(is, os);
                            }
                        }
                    }
                    """;

            assertCanonicalFormatting(code);
        }

        @Test
        @DisplayName("Wrapped throws clause")
        void testFormatterKeepsWrappedThrowsClauseExactly()
        {
            String code =
                    """
                    class Test
                    {
                        void method(String input)
                                throws IOException, SQLException, IllegalStateException
                        {}
                    }
                    """;

            assertCanonicalFormatting(code);
        }

        @Test
        @DisplayName("Inline comments in expression")
        void testFormatterKeepsInlineCommentsInExpressionExactly()
        {
            String code =
                    """
                    class Test
                    {
                        void method()
                        {
                            int result = compute() // get initial value
                                    + adjustment // apply adjustment
                                    - offset; // remove offset
                        }
                    }
                    """;

            assertCanonicalFormatting(code);
        }
    }

    @Test
    void testFormatterKeepsNestedArrayGroupingAndTrailingComma()
    {
        String code =
                """
                class Test
                {
                    String[][] values = {
                            {"a"}, {"b"}, {"c"},
                            {"d", "e"},
                    };
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsSimpleBinaryOperatorChain()
    {
        String code =
                """
                class Test
                {
                    int sum(int a, int b, int c, int d)
                    {
                        return a + b + c + d;
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsSimpleTernaryExpression()
    {
        String code =
                """
                class Test
                {
                    String choose(boolean flag)
                    {
                        return flag ? "yes" : "no";
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }
}
