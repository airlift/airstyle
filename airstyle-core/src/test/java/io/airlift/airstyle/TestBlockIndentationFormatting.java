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

public class TestBlockIndentationFormatting
{
    @Test
    void testFormatterFixesUnderIndentedStatementRelativeToMethodBlock()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object first, Object second)
                    {
                return execute(
                                first,
                                second);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object first, Object second)
                    {
                        return execute(
                                first,
                                second);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedSelectorInMethodBlock()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object value)
                    {
                        return source.get(value)
                  .map(this::convert);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object value)
                    {
                        return source.get(value)
                                .map(this::convert);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedRecordCompactConstructorDeclaration()
    {
        String oldCode =
                """
                record Value()
                {
                            Value {}
                }
                """;

        String newCode =
                """
                record Value()
                {
                    Value {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesRecordCompactConstructorBlockIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    record Value(String value)
                    {
                                Value
                                {
                                if (true) {
                                use(value);
                            }
                                }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    record Value(String value)
                    {
                        Value
                        {
                            if (true) {
                                use(value);
                            }
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesTryWithResourcesBlockIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        try (Object value = builder()
                                .a()
                                .b()) {
                                    x();
                                    return y();
                                }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        try (Object value = builder()
                                .a()
                                .b()) {
                            x();
                            return y();
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsWrappedQualifiedCallNonFirstLambdaBlockIndentation()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        tester().assertStatsFor(
                                        enabled,
                                        value -> {
                                            Object result = transform(value);
                                            return finish(result);
                                        })
                                .check();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsSelectorLineInvocationBlockLambdaBodyIndentation()
    {
        String code =
                """
                class Test
                {
                    void run(java.util.List<Object> values)
                    {
                        values.stream()
                                .filter(value -> value != null)
                                .findFirst().ifPresent(found -> {
                                    throw failure(found);
                                });
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesSelectorLineInvocationBlockLambdaBodyIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        builder()
                                .withAction(
                                        (a, b, c) -> {
                                    if (a != null) {
                                        return b;
                                    }
                                    return c;
                                })
                                .done();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        builder()
                                .withAction(
                                        (a, b, c) -> {
                                            if (a != null) {
                                                return b;
                                            }
                                            return c;
                                        })
                                .done();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedAnnotatedLocalVariableDeclaration()
    {
        String oldCode =
                """
                class Test
                {
                    void run(java.util.Map<Integer, Object> values, int index)
                    {
                        if (values != null) {
                            @SuppressWarnings("unchecked")
                             java.util.List<String> value = (java.util.List<String>) values.get(index);
                            consume(value);
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(java.util.Map<Integer, Object> values, int index)
                    {
                        if (values != null) {
                            @SuppressWarnings("unchecked")
                            java.util.List<String> value = (java.util.List<String>) values.get(index);
                            consume(value);
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedSwitchClosingBrace()
    {
        String oldCode =
                """
                class Test
                {
                    int run(int value)
                    {
                        switch (value) {
                            case 1:
                                return 1;
                            default:
                                throw new IllegalStateException("bad");
                         }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    int run(int value)
                    {
                        switch (value) {
                            case 1:
                                return 1;
                            default:
                                throw new IllegalStateException("bad");
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedNestedSwitchClosingBrace()
    {
        String oldCode =
                """
                class Test
                {
                    void run(int value)
                    {
                        if (value >= 0) {
                            switch (value) {
                                case 1:
                                    break;
                                default:
                                    throw new IllegalStateException("bad");
                        }
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(int value)
                    {
                        if (value >= 0) {
                            switch (value) {
                                case 1:
                                    break;
                                default:
                                    throw new IllegalStateException("bad");
                            }
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsNestedColonSwitchClosingBraceInsideOuterSwitchCase()
    {
        String code =
                """
                class Test
                {
                    void run(int outer, int inner)
                    {
                        switch (outer) {
                            case 1:
                                if (inner > 0) {
                                    switch (inner) {
                                        case 1:
                                            break;
                                        default:
                                            throw new IllegalStateException("bad");
                                    }
                                }
                                else if (inner < 0) {
                                    work();
                                }
                                break;
                            default:
                                break;
                        }
                    }

                    void work() {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesUnderIndentedSwitchClosingBraceInsideLambdaBlock()
    {
        String oldCode =
                """
                class Test
                {
                    void run(int value)
                    {
                        execute(() -> {
                            switch (value) {
                                case 1:
                                    break;
                                default:
                                    throw new IllegalStateException("bad");
                        }
                        });
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(int value)
                    {
                        execute(() -> {
                            switch (value) {
                                case 1:
                                    break;
                                default:
                                    throw new IllegalStateException("bad");
                            }
                        });
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsArrowSwitchClosingBraceInsideLambdaBlockBeforeReturn()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return execute(() -> {
                            switch (value()) {
                                case 1 -> work();
                                default -> throw new IllegalStateException("bad");
                            }

                            return result();
                        });
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsArrowSwitchClosingBraceInsideLambdaBlockBeforeFollowingStatement()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        execute(state -> {
                            switch (state) {
                                case 1 -> first();
                                case 2 -> second();
                                default -> fail();
                            }
                            after();
                        });
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedArrowSwitchThrowIndentation()
    {
        String code =
                """
                class Test
                {
                    Object run(Object operator, Object quantifier)
                    {
                        return switch (operator) {
                            case VALUE -> // Cannot be used with quantified comparison
                                    throw new IllegalArgumentException("Unexpected quantified comparison: %s".formatted(quantifier));
                            default -> quantifier;
                        };
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesNestedStandaloneBlockIndentation()
    {
        String oldCode =
                """
                class Test
                {
                static void main()
                {
                {
                System.out.println("hello");
                }
                }
                }
                """;

        String newCode =
                """
                class Test
                {
                    static void main()
                    {
                        {
                            System.out.println("hello");
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesNestedStandaloneBlockIndentationInsideLambdaBlock()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        call(() -> {
                {
                x();
                }
                        });
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        call(() -> {
                            {
                                x();
                            }
                        });
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesNestedStandaloneBlockIndentationInsideSwitchArrowBlock()
    {
        String oldCode =
                """
                class Test
                {
                    int run(int value)
                    {
                        return switch (value) {
                            case 1 -> {
                {
                x();
                }
                yield 1;
                            }
                            default -> 0;
                        };
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    int run(int value)
                    {
                        return switch (value) {
                            case 1 -> {
                                {
                                    x();
                                }
                                yield 1;
                            }
                            default -> 0;
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsBlankLineBeforeStandaloneBlockStatement()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        a();

                        {
                            b();
                        }
                    }
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterKeepsBlankLineBetweenStandaloneBlockStatements()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        {
                            a();
                        }

                        {
                            b();
                        }
                    }
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterKeepsBlankLineBeforeStandaloneBlockStatementInsideLambdaBlock()
    {
        String oldCode =
                """
                class Test
                {
                    Runnable run()
                    {
                        return () -> {
                            a();

                            {
                                b();
                            }
                        };
                    }
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterFixesLabeledLoopBodyIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        retry:
                                for (int attempt = 0; attempt < 10; attempt++) {
                                    continue retry;
                                }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        retry:
                        for (int attempt = 0; attempt < 10; attempt++) {
                            continue retry;
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedQualifiedCallLambdaBlockIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object cache, Object key)
                    {
                        return cache.computeIfAbsent(
                                        key,
                                        value -> {
                                                Object created = create(value);
                                                return created;
                                            })
                                .toString();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object cache, Object key)
                    {
                        return cache.computeIfAbsent(
                                        key,
                                        value -> {
                                            Object created = create(value);
                                            return created;
                                        })
                                .toString();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedBuilderLambdaBlockIndentationBeforeTrailingSelector()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return call(
                                factory()
                                        .withAction(
                                                        (a, b, c) -> {
                                                            work();
                                                            return done();
                                                        })
                                        .build(),
                                tail());
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
                                factory()
                                        .withAction(
                                                (_, _, _) -> {
                                                    work();
                                                    return done();
                                                })
                                        .build(),
                                tail());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedOptionalMapBlockLambdaBeforeTrailingSelector()
    {
        String oldCode =
                """
                import java.util.Optional;

                class Test
                {
                    boolean run(Optional<String> location)
                    {
                        return location.map(path -> {
                                    try {
                                        return exists(path);
                                    }
                                    catch (RuntimeException e) {
                                        return fallback();
                                    }
                                }).orElse(false);
                    }

                    boolean exists(String path)
                    {
                        return true;
                    }

                    boolean fallback()
                    {
                        return false;
                    }
                }
                """;

        String newCode =
                """
                import java.util.Optional;

                class Test
                {
                    boolean run(Optional<String> location)
                    {
                        return location.map(path -> {
                            try {
                                return exists(path);
                            }
                            catch (RuntimeException e) {
                                return fallback();
                            }
                        }).orElse(false);
                    }

                    boolean exists(String path)
                    {
                        return true;
                    }

                    boolean fallback()
                    {
                        return false;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesDirectBuilderBlockLambdaIndentationBeforeTrailingSelector()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        factory()
                                .withAction(
                                                (a, b, c, d, e) -> {
                                                    if (a != null) {
                                                        use(b);
                                                    }
                                                })
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
                        factory()
                                .withAction(
                                        (a, b, _, _, _) -> {
                                            if (a != null) {
                                                use(b);
                                            }
                                        })
                                .build();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedInvocationBlockLambdaBodyIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        assertThatThrownBy(() -> {
                                        doWork();
                                    })
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
                        assertThatThrownBy(() -> {
                            doWork();
                        })
                                .isInstanceOf(RuntimeException.class);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedBuilderBlockLambdaIndentationBeforeTrailingSelector()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return factory()
                                .withApply(
                                                (a, b, c) -> {
                                                    if (handler() != null) {
                                                        return handler().apply(a, b, c);
                                                    }
                                                    return empty();
                                                })
                                .build();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return factory()
                                .withApply(
                                        (a, b, c) -> {
                                            if (handler() != null) {
                                                return handler().apply(a, b, c);
                                            }
                                            return empty();
                                        })
                                .build();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesInlineBlockLambdaBodyIndentationBeforeTrailingSelectors()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object leftName, Object rightName, Object joinType, Object leftStats, Object rightStats)
                    {
                        return tester().assertStatsFor(pb -> {
                            Object left = pb.symbol(leftName, type());
                            Object right = pb.symbol(rightName, type());
                            return pb
                                    .join(joinType,
                                            pb.values(left),
                                            pb.values(right),
                                            relation());
                        }).withSourceStats(leftStats)
                                .withSourceStats(rightStats)
                                .check();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object leftName, Object rightName, Object joinType, Object leftStats, Object rightStats)
                    {
                        return tester().assertStatsFor(pb -> {
                                    Object left = pb.symbol(leftName, type());
                                    Object right = pb.symbol(rightName, type());
                                    return pb
                                            .join(joinType,
                                                    pb.values(left),
                                                    pb.values(right),
                                                    relation());
                                }).withSourceStats(leftStats)
                                .withSourceStats(rightStats)
                                .check();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesSwitchCaseBreakIndentationAfterNestedBlock()
    {
        String oldCode =
                """
                class Test
                {
                    void run(int value)
                    {
                        switch (value) {
                            case 1:
                                if (ready()) {
                                    work();
                                }
                                    break;
                            default:
                                break;
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(int value)
                    {
                        switch (value) {
                            case 1:
                                if (ready()) {
                                    work();
                                }
                                break;
                            default:
                                break;
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesSwitchCaseBreakIndentationAfterNestedBlockAndLeadingStatements()
    {
        String oldCode =
                """
                class Test
                {
                    void run(int value)
                    {
                        switch (value) {
                            case 1:
                                Object cursor = start();
                                if (ready(cursor)) {
                                    work(cursor);
                                }
                                    break;
                            default:
                                break;
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(int value)
                    {
                        switch (value) {
                            case 1:
                                Object cursor = start();
                                if (ready(cursor)) {
                                    work(cursor);
                                }
                                break;
                            default:
                                break;
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesSwitchCaseBreakIndentationAfterBracedCaseBody()
    {
        String oldCode =
                """
                import java.util.ArrayList;
                import java.util.List;

                class Test
                {
                    Object redisCursor;
                    Object scanParams;
                    List<Object> keys;
                    long totalValues;
                    int kind;

                    void run()
                    {
                        switch (kind) {
                            case 1: {
                                String cursor = start();
                                if (redisCursor != null) {
                                    cursor = nextCursor();
                                }
                                log(cursor, totalValues);
                                redisCursor = scan(cursor, scanParams);
                                keys = new ArrayList<>(result(redisCursor));
                            }
                                    break;
                            case 2:
                                keys = new ArrayList<>(range());
                                break;
                            default:
                                warn(kind);
                        }
                    }
                }
                """;

        String newCode =
                """
                import java.util.ArrayList;
                import java.util.List;

                class Test
                {
                    Object redisCursor;
                    Object scanParams;
                    List<Object> keys;
                    long totalValues;
                    int kind;

                    void run()
                    {
                        switch (kind) {
                            case 1: {
                                String cursor = start();
                                if (redisCursor != null) {
                                    cursor = nextCursor();
                                }
                                log(cursor, totalValues);
                                redisCursor = scan(cursor, scanParams);
                                keys = new ArrayList<>(result(redisCursor));
                            }
                            break;
                            case 2:
                                keys = new ArrayList<>(range());
                                break;
                            default:
                                warn(kind);
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesBlockLambdaIndentationInsideWrappedTraversalCall()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return stream(
                                traverse(value -> {
                                            if (isNested(value)) {
                                                        return nested(value);
                                                    }
                                            if (isPrimitive(value)) {
                                                        return empty();
                                                    }
                                            throw new IllegalArgumentException();
                                        })
                                        .walk(root()));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return stream(
                                traverse(value -> {
                                    if (isNested(value)) {
                                        return nested(value);
                                    }
                                    if (isPrimitive(value)) {
                                        return empty();
                                    }
                                    throw new IllegalArgumentException();
                                })
                                        .walk(root()));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsBlockLambdaIndentationInsideWrappedTraverserCall()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return root(
                                Traverser.forTree(node -> {
                                            Object value = map(node);
                                            return value;
                                        })
                                        .depthFirst(items()));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesOverIndentedBlockLambdaInsideMapCollectChain()
    {
        String oldCode =
                """
                import java.util.List;

                import static java.util.stream.Collectors.joining;

                class Test
                {
                    static String run(List<Object> values)
                    {
                        return values.stream().map(value -> {
                                    String result = "";
                                    if (first(value)) {
                                                List<Object> columns = columns(value);
                                                if (columns.size() == 1) {
                                                    result = format(getOnly(columns));
                                                }
                                                else {
                                                    result = formatGroup(columns);
                                                }
                                            }
                                    else if (second(value)) {
                                        result = "AUTO";
                                    }
                                    return result;
                                }).collect(joining(", "));
                    }
                }
                """;

        String newCode =
                """
                import java.util.List;

                import static java.util.stream.Collectors.joining;

                class Test
                {
                    static String run(List<Object> values)
                    {
                        return values.stream().map(value -> {
                            String result = "";
                            if (first(value)) {
                                List<Object> columns = columns(value);
                                if (columns.size() == 1) {
                                    result = format(getOnly(columns));
                                }
                                else {
                                    result = formatGroup(columns);
                                }
                            }
                            else if (second(value)) {
                                result = "AUTO";
                            }
                            return result;
                        }).collect(joining(", "));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesSwitchCaseBreakIndentationAfterWhileLoop()
    {
        String oldCode =
                """
                class Test
                {
                    void run(int kind)
                    {
                        switch (kind) {
                            case 1:
                                while (ready()) {
                                    work();
                                }
                                    break;
                            default:
                                work();
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(int kind)
                    {
                        switch (kind) {
                            case 1:
                                while (ready()) {
                                    work();
                                }
                                break;
                            default:
                                work();
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesBlockLambdaIndentationInsideMapCollectorChain()
    {
        String oldCode =
                """
                import java.util.List;

                import static java.util.stream.Collectors.joining;

                class Test
                {
                    static String formatGroupBy(List<Object> groupingElements)
                    {
                        return groupingElements.stream().map(groupingElement -> {
                                    String result = "";
                                    if (first(groupingElement)) {
                                                List<Object> columns = columns(groupingElement);
                                                if (columns.size() == 1) {
                                                    result = format(getOnly(columns));
                                                }
                                                else {
                                                    result = formatGroupingSet(columns);
                                                }
                                            }
                                    else if (second(groupingElement)) {
                                        result = "AUTO";
                                    }
                                    return result;
                                }).collect(joining(", "));
                    }
                }
                """;

        String newCode =
                """
                import java.util.List;

                import static java.util.stream.Collectors.joining;

                class Test
                {
                    static String formatGroupBy(List<Object> groupingElements)
                    {
                        return groupingElements.stream().map(groupingElement -> {
                            String result = "";
                            if (first(groupingElement)) {
                                List<Object> columns = columns(groupingElement);
                                if (columns.size() == 1) {
                                    result = format(getOnly(columns));
                                }
                                else {
                                    result = formatGroupingSet(columns);
                                }
                            }
                            else if (second(groupingElement)) {
                                result = "AUTO";
                            }
                            return result;
                        }).collect(joining(", "));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedPatternMatchingBlockLambdaInsideMapCollectorChain()
    {
        String oldCode =
                """
                class Test
                {
                    static String run(List<GroupingElement> groupingElements)
                    {
                        return groupingElements.stream().map(groupingElement -> {
                                    String result = "";
                                    if (groupingElement instanceof SimpleGroupBy) {
                                                List<Expression> columns = groupingElement.getExpressions();
                                                if (columns.size() == 1) {
                                                    result = formatExpression(getOnlyElement(columns));
                                                }
                                                else {
                                                    result = formatGroupingSet(columns);
                                                }
                                            }
                                    else if (groupingElement instanceof AutoGroupBy) {
                                        result = "AUTO";
                                    }
                                    return result;
                                }).collect(java.util.stream.Collectors.joining(", "));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    static String run(List<GroupingElement> groupingElements)
                    {
                        return groupingElements.stream().map(groupingElement -> {
                            String result = "";
                            if (groupingElement instanceof SimpleGroupBy) {
                                List<Expression> columns = groupingElement.getExpressions();
                                if (columns.size() == 1) {
                                    result = formatExpression(getOnlyElement(columns));
                                }
                                else {
                                    result = formatGroupingSet(columns);
                                }
                            }
                            else if (groupingElement instanceof AutoGroupBy) {
                                result = "AUTO";
                            }
                            return result;
                        }).collect(java.util.stream.Collectors.joining(", "));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedTypedBlockLambdaInsideWrappedTraverserCall()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return wrap(
                                Traverser.forTree((Node node) -> {
                                            Object value = map(node);
                                            if (first(value)) {
                                                        return nested(value);
                                                    }
                                            return empty();
                                        })
                                        .depthFirst(items()));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return wrap(
                                Traverser.forTree((Node node) -> {
                __I28__Object value = map(node);
                __I28__if (first(value)) {
                __I32__return nested(value);
                __I28__}
                __I28__return empty();
                __I24__})
                __I24__.depthFirst(items()));
                    }
                }
                """.replace("__I32__", "                                ")
                        .replace("__I28__", "                            ")
                        .replace("__I24__", "                        ");

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedTypedBlockLambdaInsideTraverserForTreeWithNestedTypeBranches()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return Traverser.forTree((NestedField nestedField) -> {
                                    Type type = nestedField.type();
                                    if (type instanceof NestedType nestedType) {
                                                return nestedType.fields();
                                            }
                                    if (type instanceof VariantType) {
                                                return ImmutableList.of();
                                            }
                                    if (type instanceof PrimitiveType) {
                                                return ImmutableList.of();
                                            }
                                    throw new IllegalArgumentException("x");
                                })
                                .depthFirstPreOrder(schema().columns());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return Traverser.forTree((NestedField nestedField) -> {
                                    Type type = nestedField.type();
                                    if (type instanceof NestedType nestedType) {
                                        return nestedType.fields();
                                    }
                                    if (type instanceof VariantType) {
                                        return ImmutableList.of();
                                    }
                                    if (type instanceof PrimitiveType) {
                                        return ImmutableList.of();
                                    }
                                    throw new IllegalArgumentException("x");
                                })
                                .depthFirstPreOrder(schema().columns());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsEnumInsideAnnotationIndentation()
    {
        String code =
                """
                @interface ThriftException
                {
                    Retryable retryable() default Retryable.UNKNOWN;

                    enum Retryable
                    {
                        UNKNOWN, FALSE, TRUE
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testBlockLambdaBodyInsideWrappedArgInheritsContinuation()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        jdbi.useExtension(
                                TestingDao.class, dao -> {
                                    dao.clearA();
                                    dao.clearB();
                                });
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testChainOnBuilderAsWrappedArgKeepsContinuationOnSelectors()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        ImmutableMap.of(
                                KEY, Args.builder()
                                        .name("X")
                                        .value(1)
                                        .build());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testIfConditionWithWrappedCallKeepsArgContinuation()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        if (!verifySaltedHash(
                                state,
                                new SaltedHash(
                                        getA(),
                                        getB()))) {
                            log();
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testCastLambdaMethodReferenceAsArgWithWrappedInnerArgs()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        assertThatThrownBy(((Runnable) () -> serviceAccountApi.createServiceAccount(
                                user.user(), "Name", "Desc", Optional.of("X"), Optional.empty()))::run)
                                .isInstanceOf(BadRequestException.class);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testChainAsSoleArgOfSelectorAfterWrappedCtor()
    {
        String code =
                """
                class Test
                {
                    Bootstrap run()
                    {
                        return new Bootstrap(
                                new AwsSdkModule(),
                                new DatabaseModule())
                                .setRequiredConfigurationProperties(ImmutableMap.<String, String>builder()
                                        .putAll(requiredProperties)
                                        .put("log.path", "tcp://localhost:4500")
                                        .buildOrThrow());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsDoWhileAlignedInsideLabeledLoop()
    {
        String code =
                """
                class Test
                {
                    void run(boolean skip, boolean more)
                    {
                        outer:
                        while (true) {
                            do {
                                if (skip) {
                                    continue outer;
                                }
                            }
                            while (more);
                            return;
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesTryWithResourcesContinuationIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                            throws Exception
                    {
                        try (var in = source();
                              var out = sink()) {
                            use(in, out);
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                            throws Exception
                    {
                        try (var in = source();
                                var out = sink()) {
                            use(in, out);
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesBlankLineBetweenWrappedTryResources()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                            throws Exception
                    {
                        try (var in = source();

                                var out = sink()) {
                            use(in, out);
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                            throws Exception
                    {
                        try (var in = source();
                                var out = sink()) {
                            use(in, out);
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsMultilineTryWithResourcesWrapped()
    {
        String code =
                """
                class Test
                {
                    void copy()
                    {
                        try (
                                InputStream input = Files.newInputStream(originalFile);
                                OutputStream output = Files.newOutputStream(targetFile)) {
                            input.transferTo(output);
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesMixedTryResourcesWithSynchronizedBlock()
    {
        String oldCode =
                """
                class Test{int run(int[]values)throws Exception{int total=0;AutoCloseable existing=()->close("existing");try(existing;AutoCloseable created=()->close("created")){synchronized(this){total+=values.length;}}catch(IllegalArgumentException|IllegalStateException e){total=-1;}finally{total++;}

                assert total>=0:"negative total "+total;return total;}private static void close(String name){if(name.isBlank()){throw new IllegalStateException();}}}
                """;

        String newCode =
                """
                class Test
                {
                    int run(int[] values)
                            throws Exception
                    {
                        int total = 0;
                        AutoCloseable existing = () -> close("existing");
                        try (existing; AutoCloseable created = () -> close("created")) {
                            synchronized (this) {
                                total += values.length;
                            }
                        }
                        catch (IllegalArgumentException | IllegalStateException e) {
                            total = -1;
                        }
                        finally {
                            total++;
                        }

                        assert total >= 0 : "negative total " + total;
                        return total;
                    }

                    private static void close(String name)
                    {
                        if (name.isBlank()) {
                            throw new IllegalStateException();
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }
}
