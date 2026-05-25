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

import org.junit.jupiter.api.Test;

import static io.airlift.airstyle.FormatterAssertions.assertCanonicalFormatting;
import static io.airlift.airstyle.FormatterAssertions.assertFormatsOldToNew;

public class TestChainExpressionFormatting
{
    @Test
    void testFormatterFixesTernaryContinuationWithChainedThenAndElseBranches()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return enabled
                             ? source.lookup(id)
                                   .map(this::convert)
                                   .orElse(null)
                              : fallback.lookup(id)
                                    .map(this::convert)
                                    .orElse(null);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return enabled
                                ? source.lookup(id)
                                  .map(this::convert)
                                  .orElse(null)
                                : fallback.lookup(id)
                                  .map(this::convert)
                                  .orElse(null);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsBooleanContinuationInsideSelectorLineAssertionShallow()
    {
        String code =
                """
                class Test
                {
                    void run(boolean first, boolean second, boolean third)
                    {
                        assertThat(first &&
                                second &&
                                third).isTrue();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesOverIndentedBooleanContinuationInsideSelectorLineAssertionWithComparisons()
    {
        String oldCode =
                """
                class Test
                {
                    void run(Object actual, Object expected)
                    {
                        assertThat(actual.first() == expected.first() &&
                                        actual.second() == expected.second() &&
                                        actual.third() == expected.third()).isTrue();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(Object actual, Object expected)
                    {
                        assertThat(actual.first() == expected.first() &&
                                actual.second() == expected.second() &&
                                actual.third() == expected.third()).isTrue();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedTernaryLambdaContinuationInsideSelectorLineFilter()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(boolean flag)
                    {
                        return values().stream()
                                .filter(flag
                        ? _ -> true
                                : value -> keep(value))
                                .toList();
                    }

                    static boolean keep(Object value)
                    {
                        return true;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(boolean flag)
                    {
                        return values().stream()
                                .filter(flag
                                        ? _ -> true
                                        : value -> keep(value))
                                .toList();
                    }

                    static boolean keep(Object value)
                    {
                        return true;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedConstructorSelectorIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object value)
                    {
                        Object provider = new Provider
                        .Builder(value)
                        .build();
                        return provider;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object value)
                    {
                        Object provider = new Provider
                                .Builder(value)
                                .build();
                        return provider;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesInconsistentWrappedConstructorSelectorIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Config config, Object provider)
                    {
                        provider = new LongQualifiedConstructorReceiverName
                        .Builder(config.value(), "session")
                                .withFirst(provider)
                                .withSecond(config.other())
                                .build();
                        return provider;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Config config, Object provider)
                    {
                        provider = new LongQualifiedConstructorReceiverName
                                .Builder(config.value(), "session")
                                .withFirst(provider)
                                .withSecond(config.other())
                                .build();
                        return provider;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsWrappedSelectorsInsideConstructorLambdaArgument()
    {
        String code =
                """
                class Test
                {
                    Object run(Object value)
                    {
                        return new Bootstrap(
                                binder -> httpClientBinder(binder)
                                        .bindClient("foo", Client.class)
                                        .withConfig(config -> config.setValue(value)))
                                .quiet()
                                .initialize();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedQualifiedConstructorSelectorWithLambdaArgumentChain()
    {
        String code =
                """
                class Test
                {
                    Object run(Object value)
                    {
                        return new Provider
                                .Builder(
                                        binder -> httpClientBinder(binder)
                                                .bindClient("foo", Client.class)
                                                .withConfig(config -> config.setValue(value)))
                                .build();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsStandaloneWrappedQualifiedConstructorSelectorWithLambdaArgumentChain()
    {
        String code =
                """
                class Test
                {
                    Object run(Object value)
                    {
                        Object provider = new Provider
                                .Builder(
                                        binder -> httpClientBinder(binder)
                                                .bindClient("foo", Client.class)
                                                .withConfig(config -> config.setValue(value)));
                        return provider;
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedQualifiedConstructorSelectorWithNestedArgumentChain()
    {
        String code =
                """
                class Test
                {
                    Object run(Object value)
                    {
                        return new Provider
                                .Builder(
                                        Module.builder()
                                                .withValue(value)
                                                .build())
                                .build();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsBlockLambdaInsideConstructorArgument()
    {
        String code =
                """
                class Test
                {
                    Object run(Object first, Object second)
                    {
                        return new Bootstrap(
                                binder -> {
                                    binder.bind(First.class).toInstance(first);
                                    binder.bind(Second.class).toInstance(second);
                                },
                                Module.builder()
                                        .withValue(first)
                                        .build())
                                .quiet()
                                .initialize();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesWrappedStaticGenericSelectorIndentation()
    {
        String oldCode =
                """
                import java.util.Comparator;
                import java.util.Map;

                class Test
                {
                    Comparator<Map.Entry<String, Long>> run()
                    {
                        Comparator<Map.Entry<String, Long>> comparator = Comparator
                        .<Map.Entry<String, Long>, Long>comparing(Map.Entry::getValue)
                        .thenComparing(Map.Entry::getKey);
                        return comparator;
                    }
                }
                """;

        String newCode =
                """
                import java.util.Comparator;
                import java.util.Map;

                class Test
                {
                    Comparator<Map.Entry<String, Long>> run()
                    {
                        Comparator<Map.Entry<String, Long>> comparator = Comparator
                                .<Map.Entry<String, Long>, Long>comparing(Map.Entry::getValue)
                                .thenComparing(Map.Entry::getKey);
                        return comparator;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedInlineBuilderChainInsideWrappedArgumentWithInlineArguments()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return assertThat(query())
                                .result().matches(resultBuilder(session(), first(), second())
                                .row(third())
                                .row(fourth())
                                .build());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return assertThat(query())
                                .result().matches(resultBuilder(session(), first(), second())
                                        .row(third())
                                        .row(fourth())
                                        .build());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedTernaryContinuationInsideWrappedSelectorInvocation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return values()
                                .filter(partitionConstraint.isAll()
                                ? value -> true
                                : value -> matches(value, partitionConstraint.getDomains().orElseThrow()));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return values()
                                .filter(partitionConstraint.isAll()
                                        ? _ -> true
                                        : value -> matches(value, partitionConstraint.getDomains().orElseThrow()));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsStandardBooleanContinuationIndentAfterQualifiedHead()
    {
        String code =
                """
                class Test
                {
                    Object run(Call inner)
                    {
                        return inner != null
                                && !inner.function().functionNullability().isReturnNullable() &&
                                inner.function().functionNullability().getArgumentNullable().stream().allMatch(Boolean.TRUE::equals);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testMethodInvocationOnParenCastPreservesChainInsideCast()
    {
        // A chain inside a parenthesized cast remains a chain: selectors
        // inside the cast keep continuation indent before the outer method
        // invocation is formatted.
        String code =
                """
                import com.google.common.collect.ImmutableList;
                import com.google.common.collect.ImmutableMap;

                import java.util.Map;

                class Test
                {
                    Object run()
                    {
                        return ImmutableList.copyOf(((Map<String, Object>) ImmutableMap.<String, Object>builder()
                                .put("a", 1)
                                .put("b", 2)
                                .buildOrThrow()).keySet());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testAnonymousClassAsChainReceiverIndentsBody()
    {
        // An anonymous class used as a method-chain receiver keeps its body
        // formatted as an anonymous-class body before the selector is applied.
        String code =
                """
                class Test
                {
                    Object run(Object tree)
                    {
                        return new Visitor()
                        {
                            @Override
                            public boolean foo()
                            {
                                return false;
                            }
                        }.visit(tree);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testMethodReferenceWithTypeWitnessHasNoSpacesAroundBrackets()
    {
        // Method-reference type witnesses are generic brackets, not binary
        // operators, so no spaces are inserted around `<` and `>`.
        String code =
                """
                import com.google.common.collect.ImmutableList;

                class Test
                {
                    Object run(Object target)
                    {
                        return target.stream().map(ImmutableList::<Object>of);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testConstructorReferenceWithTypeWitnessHasNoSpacesAroundBrackets()
    {
        // Method-reference type witnesses are generic brackets, not binary
        // operators, so no spaces are inserted around `<` and `>`.
        String code =
                """
                import java.util.ArrayList;

                class Test
                {
                    Object run(Object target)
                    {
                        return target.stream().map(ArrayList::<Object>new);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterIndentsMethodReferenceOnChainTail()
    {
        String code =
                """
                class Test
                {
                    Object run(java.util.Set<Object> values)
                    {
                        return values.stream()
                                .map(Object::toString)
                                .collect(java.util.stream.Collectors.toSet())
                                ::contains;
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsQualifiedTypeArgGenericMethodAdjacentToDot()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return ImmutableSet.<Outer.Inner>builder().build();
                    }

                    static class Outer
                    {
                        static class Inner {}
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsChainedGenericMethodInvocationsAdjacentToDot()
    {
        String code =
                """
                class Test
                {
                    Object run(Object node)
                    {
                        return node
                                .<Object>set("a", "b")
                                .<Object>set("c", "d");
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterIndentsInfixOperandsInsideChainHeadParens()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        execute(("" +
                        "CREATE TABLE " +
                        "AS SELECT ")
                                .replace("x", "y"));
                    }

                    void execute(String s) {}
                }
                """;
        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        execute(("" +
                                "CREATE TABLE " +
                                "AS SELECT ")
                                .replace("x", "y"));
                    }

                    void execute(String s) {}
                }
                """;
        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterAlignsChainSelectorsUnderReceiverInsideTernaryBranch()
    {
        String code =
                """
                class Test
                {
                    String run(boolean flag, String s)
                    {
                        return flag ?
                                s.trim()
                                .toLowerCase() :
                                s;
                    }
                }
                """;
        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterIndentsWrappedQualifiedConstructorTypeSelector()
    {
        String oldCode =
                """
                class Test
                {
                    Object create(Config config)
                    {
                        Object provider = new Factory
                        .Builder(config.value())
                                .build();
                        return provider;
                    }
                }
                """;
        String newCode =
                """
                class Test
                {
                    Object create(Config config)
                    {
                        Object provider = new Factory
                                .Builder(config.value())
                                .build();
                        return provider;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterIndentsWrappedGenericStaticCallSelector()
    {
        String oldCode =
                """
                class Test
                {
                    Object comparator()
                    {
                        Object comparator = Comparator
                        .<Entry, Long>comparing(Entry::value)
                                .thenComparing(Entry::id);
                        return comparator;
                    }
                }
                """;
        String newCode =
                """
                class Test
                {
                    Object comparator()
                    {
                        Object comparator = Comparator
                                .<Entry, Long>comparing(Entry::value)
                                .thenComparing(Entry::id);
                        return comparator;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterIndentsWrappedMethodReferenceTailAfterQualifierChain()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        assertThatThrownBy(new Config().setName("name").setValue("value")
                        ::checkConfig)
                                .isInstanceOf(RuntimeException.class);
                    }
                }
                """;
        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        assertThatThrownBy(new Config().setName("name").setValue("value")
                                ::checkConfig)
                                .isInstanceOf(RuntimeException.class);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsSingleBlankLineBetweenWrappedChainSelectors()
    {
        String code =
                """
                class Test
                {
                    Object values()
                    {
                        return builder()
                                .add("abc")
                                .add("xyz")

                                .add("123")
                                .build();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesRepeatedBlankLinesBetweenWrappedChainSelectors()
    {
        String oldCode =
                """
                class Test
                {
                    Object values()
                    {
                        return builder()
                                .add("abc")
                                .add("xyz")


                                .add("123")
                                .build();
                    }
                }
                """;
        String newCode =
                """
                class Test
                {
                    Object values()
                    {
                        return builder()
                                .add("abc")
                                .add("xyz")

                                .add("123")
                                .build();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsSingleBlankLineBetweenWrappedChainSelectorsInExpression()
    {
        String code =
                """
                class Test
                {
                    Object values(boolean flag)
                    {
                        return flag
                                ? builder()
                                  .add("abc")

                                  .build()
                                : fallback();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsParenthesizedWrappedChainArgumentCovered()
    {
        String code =
                """
                class Test
                {
                    Object run(Object dir, Object keys)
                    {
                        return transform(
                                toListenableFuture((listObjectsRecursively(dir)
                                        .subscribe(response -> response.contents().stream()
                                                .map(Object::toString)
                                                .forEach(keys::add)))),
                                _ -> keys.build(),
                                directExecutor());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesUnderIndentedSelectorAfterWrappedReceiverWithFieldAccess()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        blah
                        .x.y();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        blah
                                .x.y();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedSelectorAfterWrappedCallWithFieldAccess()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        foo()
                        .x.y();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        foo()
                                .x.y();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedArgInsideFieldAccessReceiverCall()
    {
        String oldCode =
                """
                class Test
                {
                    private void x()
                    {
                        blah
                                .x.y(a,
                                b);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    private void x()
                    {
                        blah
                                .x.y(a,
                                        b);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }
}
