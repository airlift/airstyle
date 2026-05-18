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

public class TestComplexExpressionFormatting
{
    @Test
    void testFormatterKeepsWrappedSelectorChain()
    {
        assertCanonicalFormatting(
                """
                class Test
                {
                    Object run(Object client)
                    {
                        return client.execute(Request.builder()
                                        .setA("a")
                                        .setB("b")
                                        .build())
                                .stream()
                                .map(this::convert);
                    }
                }
                """);
    }

    @Test
    void testFormatterKeepsTopLevelQualifiedCallWithTrailingSelector()
    {
        assertCanonicalFormatting(
                """
                class Test
                {
                    void run(Object first, Object second)
                    {
                        Output<List<Object>> value = Output.format(
                                        "value %s %s",
                                        first,
                                        second)
                                .applyValue(List::of);
                    }
                }
                """);
    }

    @Test
    void testFormatterKeepsTopLevelQualifiedChainWithWrappedBuilderArgument()
    {
        assertCanonicalFormatting(
                """
                class Test
                {
                    void run(Object id)
                    {
                        Output<String> value = Client.get(Request.builder()
                                .setId(id)
                                .build()).map(Result::id);
                    }
                }
                """);
    }

    @Test
    void testFormatterKeepsCompactWrappedConstructorArguments()
    {
        assertCanonicalFormatting(
                """
                class Test
                {
                    Object run(Object first, Object second, Object third, Object fourth)
                    {
                        return new Value(
                                first, second, third, fourth);
                    }
                }
                """);
    }

    @Test
    void testFormatterKeepsCompactWrappedCallWithTrailingBlockLambda()
    {
        assertCanonicalFormatting(
                """
                class Test
                {
                    boolean run()
                    {
                        return runIfAllowed(
                                principal, targetItem, combinationRule, () -> {
                                    work();
                                    return true;
                                })
                                .orElse(false);
                    }
                }
                """);
    }

    @Test
    void testFormatterFixesSpaceBeforeCommaAfterWrappedParenthesizedExpression()
    {
        String oldCode =
                """
                class Test
                {
                    void run(boolean first, boolean second)
                    {
                        check(
                                (first ||
                                        second) ,
                                "message");
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(boolean first, boolean second)
                    {
                        check(
                                (first ||
                                        second),
                                "message");
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesSpaceBetweenNestedClosingParentheses()
    {
        String oldCode =
                """
                class Test
                {
                    double run(long[] values, int min, int max)
                    {
                        return ((double) (values[max]
                                - values[min]) )
                                / values[max];
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    double run(long[] values, int min, int max)
                    {
                        return ((double) (values[max]
                                - values[min]))
                                / values[max];
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsExpressionLambdaWrappedArguments()
    {
        assertCanonicalFormatting(
                """
                class Test
                {
                    void run(Object body)
                    {
                        client.call(
                                value -> value
                                        .setA("a")
                                        .setB("b"),
                                body);
                    }
                }
                """);
    }

    @Test
    void testFormatterKeepsMethodChainInsideTernaryBranchAtReceiverColumn()
    {
        assertCanonicalFormatting(
                """
                class Test
                {
                    Object run(boolean enabled, Object input)
                    {
                        Object value = enabled
                                ? Builder.create(input)
                                  .indent()
                                  .child(input)
                                  .build()
                                : input;
                    }
                }
                """);
    }

    @Test
    void testFormatterFixesMethodChainInsideTernaryBranchToReceiverColumn()
    {
        assertFormatsOldToNew(
                """
                class Test
                {
                    Object run(boolean anonymous, Object values)
                    {
                        Object output = anonymous
                                ? values.stream().map(this::map).toList()
                                : values.stream()
                                        .map(this::coerce)
                                        .toList();
                    }
                }
                """,
                """
                class Test
                {
                    Object run(boolean anonymous, Object values)
                    {
                        Object output = anonymous
                                ? values.stream().map(this::map).toList()
                                : values.stream()
                                  .map(this::coerce)
                                  .toList();
                    }
                }
                """);
    }

    @Test
    void testFormatterFixesNestedMethodChainInsideTernaryBranchToReceiverColumn()
    {
        assertFormatsOldToNew(
                """
                class Test
                {
                    Object run(boolean enabled, Object values, Object key)
                    {
                        Object output = enabled
                                ? values.computeIfAbsent(key, _ -> new Object())
                                        .computeIfAbsent(key, _ -> new Object())
                                : null;
                    }
                }
                """,
                """
                class Test
                {
                    Object run(boolean enabled, Object values, Object key)
                    {
                        Object output = enabled
                                ? values.computeIfAbsent(key, _ -> new Object())
                                  .computeIfAbsent(key, _ -> new Object())
                                : null;
                    }
                }
                """);
    }

    @Test
    void testFormatterKeepsMapFactoryTextBlockValueLayout()
    {
        assertCanonicalFormatting(
                """
                import java.util.Map;

                class Test
                {
                    Map<String, String> run(String name)
                    {
                        return Map.of(
                                "hello",
                                \"""
                                hello %s
                                \""".formatted(name));
                    }
                }
                """);
    }

    @Test
    void testFormatterKeepsNestedConstructorInConstructorArguments()
    {
        assertCanonicalFormatting(
                """
                class Test
                {
                    void run(Object request, Throwable cause)
                    {
                        throw new Failure(new Detail(
                                "INVALID_ARGUMENTS",
                                "Invalid arguments for '%s'".formatted(request)), cause);
                    }
                }
                """);
    }

    @Test
    void testFormatterKeepsNestedInvocationInConstructorArguments()
    {
        assertCanonicalFormatting(
                """
                class Test
                {
                    Object run(Object left, Object right, Object metadata)
                    {
                        return new Response(build(
                                left,
                                right), metadata);
                    }
                }
                """);
    }

    @Test
    void testFormatterKeepsNestedInvocationInMethodArguments()
    {
        assertCanonicalFormatting(
                """
                class Test
                {
                    Object run(Object left, Object right, Object other)
                    {
                        return wrap(build(
                                        left,
                                        right),
                                other);
                    }
                }
                """);
    }

    @Test
    void testFormatterFixesWrappedLambdaPredicateInsideConditionalChain()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(boolean enabled, Object columns, Object field)
                    {
                        return enabled ?
                                columns.stream()
                                .filter(column -> column.name().equals(field) &&
                                        column.type() instanceof RowType rowType &&
                                        rowType.fields().stream().map(Field::name).anyMatch(field::equals))
                                // The field may be absent
                                .collect(toOptional())
                                : Optional.empty();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(boolean enabled, Object columns, Object field)
                    {
                        return enabled ?
                                columns.stream()
                                .filter(column -> column.name().equals(field) &&
                                                  column.type() instanceof RowType rowType &&
                                        rowType.fields().stream().map(Field::name).anyMatch(field::equals))
                                        // The field may be absent
                                .collect(toOptional())
                                : Optional.empty();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsQualifiedPaginatorChainWithTrailingSelectors()
    {
        assertCanonicalFormatting(
                """
                class Test
                {
                    Object run(Object client)
                    {
                        return client.listTypesPaginator(ListTypesRequest.builder()
                                        .type(type)
                                        .visibility(visibility)
                                        .build())
                                .typeSummaries().stream()
                                .toList();
                    }
                }
                """);
    }

    @Test
    void testFormatterKeepsWrappedMultilineLambdaArgumentWithTrailingSelector()
    {
        assertCanonicalFormatting(
                """
                class Test
                {
                    Object run(Object api, Object x)
                    {
                        return api.call(x,
                                        (_, _) -> {
                                            run();
                                        })
                                .done();
                    }
                }
                """);
    }

    @Test
    void testFormatterKeepsCommentedBlockLambdaInWrappedQualifiedCallArguments()
    {
        assertCanonicalFormatting(
                """
                class Test
                {
                    Object run(Object factories, Object partitionSpec, Object partitionData, Object structType)
                    {
                        return factories.computeIfAbsent(
                                        partitionSpec,
                                        key -> {
                                            // creating the template wrapper is expensive, reuse it for all partitions of the same spec
                                            // reuse is only safe because we only use the copyFor method which is thread safe
                                            Object templateWrapper = createWrapper(structType);
                                            return createPartitionKey(key, templateWrapper, partitionData);
                                        })
                                .apply(partitionData);
                    }
                }
                """);
    }

    @Test
    void testFormatterKeepsAssignmentPrefixedSelectorChainInLambdaBlock()
    {
        assertCanonicalFormatting(
                """
                class Test
                {
                    Object userInfo;
                    Object targetItem;
                    Object ANY;
                    Object filter;

                    Object run(Object workflowApi)
                    {
                        return workflowApi.runWithTransactionAndFilter(
                                userInfo,
                                targetItem,
                                ANY,
                                (_, _) -> {
                                    Set<ItemType> itemTypeFilter = filter.typeFilter().stream()
                                            .map(ItemTypeFilter::toItemType)
                                            .collect(Collectors.toUnmodifiableSet());

                                    Set<RuleCategory> ruleCategoryFilter = filter.typeFilter().stream()
                                            .map(ItemTypeFilter::toRuleCategory)
                                            .filter(Objects::nonNull)
                                            .collect(Collectors.toUnmodifiableSet());

                                    String orderByClause = buildOrderByClause(filter.sortBy(), filter.sortOrder());

                                    return orderByClause;
                                });
                    }
                }
                """);
    }

    @Test
    void testFormatterKeepsTextBlockArgumentInQualifiedInvocationInsideConstructorArgument()
    {
        assertCanonicalFormatting(
                """
                class Test
                {
                    void run(Object systemdUnitsBuilder)
                    {
                        systemdUnitsBuilder.add(new SystemdUnit("setup-ephemeral-disk.service", true, Optional.of(
                                \"\"\"
                                [Unit]
                                Before=mnt-ephemeral.mount
                                \"\"\")));
                    }
                }
                """);
    }

    @Test
    void testFormatterFixesUncommonExpressionForms()
    {
        String oldCode =
                """
                import java.io.Serializable;
                import java.util.List;
                import java.util.function.Function;
                import java.util.function.IntFunction;
                import java.util.function.Supplier;

                class Test{Object run(List<String> values){Function<String,String> trim=String::trim;IntFunction<String[]> arrayFactory=String[]::new;Supplier<Test> constructor=Test::new;Runnable serializableRunnable=(Runnable&Serializable)()->values.forEach(System.out::println);Object anonymous=new Object(){@Override public String toString(){return values.isEmpty()?"empty":trim.apply(values.getFirst());}};int[][] matrix=new int[values.size()][];matrix[0]=new int[]{1,2,3};serializableRunnable.run();return values.isEmpty()
                ?constructor.get()
                :(arrayFactory.apply(values.size())[0]=anonymous.toString());}}
                """;

        String newCode =
                """
                import java.io.Serializable;
                import java.util.List;
                import java.util.function.Function;
                import java.util.function.IntFunction;
                import java.util.function.Supplier;

                class Test
                {
                    Object run(List<String> values)
                    {
                        Function<String, String> trim = String::trim;
                        IntFunction<String[]> arrayFactory = String[]::new;
                        Supplier<Test> constructor = Test::new;
                        Runnable serializableRunnable = (Runnable & Serializable) () -> values.forEach(System.out::println);
                        Object anonymous = new Object()
                        {
                            @Override
                            public String toString()
                            {
                                return values.isEmpty() ? "empty" : trim.apply(values.getFirst());
                            }
                        };
                        int[][] matrix = new int[values.size()][];
                        matrix[0] = new int[] {1, 2, 3};
                        serializableRunnable.run();
                        return values.isEmpty()
                                ? constructor.get()
                                : (arrayFactory.apply(values.size())[0] = anonymous.toString());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesTernaryInsideCallInsideOuterTernaryBranch()
    {
        // Inner ternary is the argument to a method call that is itself inside
        // an outer ternary else-branch. The inner branches must be at
        // double-continuation (col 24), not at the outer branch level (col 16).
        assertFormatsOldToNew(
                """
                class Test
                {
                    void run()
                    {
                        Object x = cond1 ?
                                first() :
                                of(cond2 ?
                                second() :
                                third());
                    }
                }
                """,
                """
                class Test
                {
                    void run()
                    {
                        Object x = cond1 ?
                                first() :
                                of(cond2 ?
                                        second() :
                                        third());
                    }
                }
                """);
    }
}
