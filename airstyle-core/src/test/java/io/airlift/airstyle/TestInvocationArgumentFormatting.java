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

public class TestInvocationArgumentFormatting
{
    @Test
    void testFormatterFixesWrappedShortSuperCallFirstArgument()
    {
        String oldCode =
                """
                class Parent
                {
                    Parent(Object config, boolean enabled) {}
                }

                class Test
                        extends Parent
                {
                    Test(Object name, Object value, boolean enabled)
                    {
                        super(
                                createConfig(
                                        name,
                                        value),
                                enabled);
                    }

                    static Object createConfig(Object name, Object value)
                    {
                        return null;
                    }
                }
                """;

        String newCode =
                """
                class Parent
                {
                    Parent(Object config, boolean enabled) {}
                }

                class Test
                        extends Parent
                {
                    Test(Object name, Object value, boolean enabled)
                    {
                        super(createConfig(
                                        name,
                                        value),
                                enabled);
                    }

                    static Object createConfig(Object name, Object value)
                    {
                        return null;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedFirstArgumentExpressionContinuationInMultiArgumentCall()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return call(
                                values.stream()
                                .filter(Test::matches)
                                .toList(),
                                other());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return call(
                                values.stream()
                                        .filter(Test::matches)
                                        .toList(),
                                other());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesBinaryContinuationInsideWrappedInvocationArguments()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return execute(
                                first,
                                left()
                             + middle()
                                    + right(),
                                fallback);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return execute(
                                first,
                                left()
                                        + middle()
                                        + right(),
                                fallback);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesTernaryContinuationInsideWrappedInvocationArguments()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return execute(
                                first,
                                enabled
                             ? choose(left, right)
                              : choose(fallbackLeft, fallbackRight),
                                fallback);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return execute(
                                first,
                                enabled
                                        ? choose(left, right)
                                        : choose(fallbackLeft, fallbackRight),
                                fallback);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedTernaryContinuationInsideOnlyInvocationArgument()
    {
        String oldCode =
                """
                class Test
                {
                    void run(boolean transactionalCreateTable, Session session, int index)
                    {
                        missingFileNamesBuilder.add(transactionalCreateTable
                                            ? computeTransactionalBucketedFilename(index)
                                            : computeNonTransactionalBucketedFilename(session.getQueryId(), index));
                    }

                    Builder missingFileNamesBuilder;

                    Object computeTransactionalBucketedFilename(int index)
                    {
                        return null;
                    }

                    Object computeNonTransactionalBucketedFilename(Object queryId, int index)
                    {
                        return null;
                    }

                    interface Builder
                    {
                        void add(Object value);
                    }

                    interface Session
                    {
                        Object getQueryId();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(boolean transactionalCreateTable, Session session, int index)
                    {
                        missingFileNamesBuilder.add(transactionalCreateTable
                                ? computeTransactionalBucketedFilename(index)
                                : computeNonTransactionalBucketedFilename(session.getQueryId(), index));
                    }

                    Builder missingFileNamesBuilder;

                    Object computeTransactionalBucketedFilename(int index)
                    {
                        return null;
                    }

                    Object computeNonTransactionalBucketedFilename(Object queryId, int index)
                    {
                        return null;
                    }

                    interface Builder
                    {
                        void add(Object value);
                    }

                    interface Session
                    {
                        Object getQueryId();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsBlankLineBetweenWrappedParameters()
    {
        // Match the argument-list policy: one blank line between related
        // items stays as a grouping cue.
        String code =
                """
                class Test
                {
                    void run(
                            String first,

                            String second)
                    {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsBlankLineBetweenWrappedMethodArguments()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        call(
                                first,

                                second);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesRepeatedBlankLinesBetweenWrappedMethodArguments()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        call(
                                first,


                                second);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        call(
                                first,

                                second);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesRepeatedBlankLinesBetweenWrappedParameters()
    {
        String oldCode =
                """
                class Test
                {
                    void run(
                            String first,


                            String second)
                    {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(
                            String first,

                            String second)
                    {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsTrailingOperatorBooleanContinuationInWrappedInvocationArgument()
    {
        String code =
                """
                class Test
                {
                    void run(int parent, int child, int otherChild, boolean isLeft, int[] levels, long[] values, double[] counts)
                    {
                        checkState(first(parent, child) || second(parent, child), "first");

                        long branch = values[child] & (1L << (levels[parent] - 1));
                        checkState(branch == 0 && isLeft || branch != 0 && !isLeft, "second");

                        checkState(counts[parent] > 0 ||
                                        counts[child] > 0 || otherChild != -1,
                                "third");
                    }

                    boolean first(int left, int right)
                    {
                        return true;
                    }

                    boolean second(int left, int right)
                    {
                        return true;
                    }

                    void checkState(boolean value, String message) {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesNestedMethodCallArgumentDepthInChainSelectorContext()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        Object result = ImmutableMap.builder()
                                .put(col, Domain.create(SortedRangeSet.copyOf(BIGINT,
                                ImmutableList.of(
                                        Range.equal(BIGINT, 128L),
                                        Range.equal(BIGINT, 180L))),
                                false))
                                .build();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        Object result = ImmutableMap.builder()
                                .put(col, Domain.create(SortedRangeSet.copyOf(BIGINT,
                                                ImmutableList.of(
                                                        Range.equal(BIGINT, 128L),
                                                        Range.equal(BIGINT, 180L))),
                                        false))
                                .build();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testNestedCallArgumentsInChainSelector()
    {
        String input =
                """
                class Test
                {
                    void test()
                    {
                        assertThat(query())
                                .matches(results(column("x", INT))
                                        .addRow(List.of(List.of(
                                            List.of(1, "a"),
                                            List.of(2, "b"))))
                                        .build());
                    }
                }
                """;
        String expected =
                """
                class Test
                {
                    void test()
                    {
                        assertThat(query())
                                .matches(results(column("x", INT))
                                        .addRow(List.of(List.of(
                                                List.of(1, "a"),
                                                List.of(2, "b"))))
                                        .build());
                    }
                }
                """;
        assertFormatsOldToNew(input, expected);
    }

    @Test
    void testNestedCallArgumentsInChainSelectorThreeLevels()
    {
        String expected =
                """
                class Test
                {
                    void test()
                    {
                        assertThat(query())
                                .matches(results(
                                        column("mylist", new ArrayType(structType)))
                                        .addRow(List.of(List.of(
                                                List.of(1, "a"),
                                                List.of(2, "b"),
                                                List.of(3, "c"))))
                                        .build());
                    }
                }
                """;
        assertCanonicalFormatting(expected);
    }
}
