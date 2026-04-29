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

public class TestSwitchFormatting
{
    @Test
    void testFormatterFixesSwitchRuleExpressionContinuation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(int input)
                    {
                        return switch (input) {
                            case 1 -> first()
                                 + second()
                                        + third();
                            default -> enabled
                                     ? choose(
                                             first(),
                                             second())
                                      : fallback();
                        };
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(int input)
                    {
                        return switch (input) {
                            case 1 -> first()
                                    + second()
                                    + third();
                            default -> enabled
                                    ? choose(
                                    first(),
                                    second())
                                    : fallback();
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesSwitchYieldExpressionContinuation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(int input)
                    {
                        return switch (input) {
                            case 1 -> {
                                yield enabled
                                     ? choose(
                                             first(),
                                             second())
                                      : fallback();
                            }
                            default -> {
                                yield first()
                                     + second()
                                            + third();
                            }
                        };
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(int input)
                    {
                        return switch (input) {
                            case 1 -> {
                                yield enabled
                                        ? choose(
                                        first(),
                                        second())
                                        : fallback();
                            }
                            default -> {
                                yield first()
                                        + second()
                                        + third();
                            }
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesSwitchRuleSingleLineExpressionPlacement()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(int value)
                    {
                        return switch (value) {
                            case 1 ->
                                    one();
                            default -> other();
                        };
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(int value)
                    {
                        return switch (value) {
                            case 1 -> one();
                            default -> other();
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesSwitchRuleSingleLineThrowPlacement()
    {
        String oldCode =
                """
                class Test
                {
                    void run(int state)
                    {
                        switch (state) {
                            case 1, 2, 3, 4 ->
                                    throw new IllegalArgumentException("bad " + state);
                            default -> work();
                        }
                    }

                    void work() {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(int state)
                    {
                        switch (state) {
                            case 1, 2, 3, 4 -> throw new IllegalArgumentException("bad " + state);
                            default -> work();
                        }
                    }

                    void work() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesSwitchStatementRuleSingleLineExpressionPlacement()
    {
        String oldCode =
                """
                class Test
                {
                    void run(int value)
                    {
                        switch (value) {
                            case 1 ->
                                    one();
                            default -> other();
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
                            case 1 -> one();
                            default -> other();
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesSwitchRuleBlockBodyIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    int run(int value)
                    {
                        return switch (value) {
                            case 1 -> {
                            if (enabled) {
                                yield 1;
                            }
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
                                if (enabled) {
                                    yield 1;
                                }
                            }
                            default -> 0;
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedSwitchRuleBlockBodyIndentation()
    {
        String oldCode =
                """
                record Pair(Object left, Object right) {}

                class Test
                {
                    boolean run(Object expression)
                    {
                        return switch (expression) {
                            case Pair(
                                    var left,
                                    var right) -> {
                            if (left == right) {
                                yield true;
                            }
                            yield false;
                        }
                            default -> false;
                        };
                    }
                }
                """;

        String newCode =
                """
                record Pair(Object left, Object right) {}

                class Test
                {
                    boolean run(Object expression)
                    {
                        return switch (expression) {
                            case Pair(
                                    var left,
                                    var right) -> {
                                if (left == right) {
                                    yield true;
                                }
                                yield false;
                            }
                            default -> false;
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesSwitchRuleCommentByMovingItBeforeCase()
    {
        String oldCode =
                """
                class Test
                {
                    void run(int value)
                    {
                        switch (value) {
                            case 1 ->
                                // keep schedule comment
                                execute(
                                        left,
                                        right);
                            default -> other();
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
                            // keep schedule comment
                            case 1 -> execute(
                                    left,
                                    right);
                            default -> other();
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesSwitchRuleMultilineCallPlacement()
    {
        String oldCode =
                """
                class Test
                {
                    void run(int value)
                    {
                        switch (value) {
                            case 1 ->
                                execute(
                                        left,
                                        right);
                            default -> other();
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
                            case 1 -> execute(
                                    left,
                                    right);
                            default -> other();
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedBooleanContinuationInsideSwitchWhenGuard()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object entry)
                    {
                        return switch (entry) {
                            case Item item
                                    when first() &&
                                                    second() &&
                                                    third() -> item;
                            default -> null;
                        };
                    }

                    record Item(Object value) {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object entry)
                    {
                        return switch (entry) {
                            case Item item
                                    when first() &&
                                    second() &&
                                    third() -> item;
                            default -> null;
                        };
                    }

                    record Item(Object value) {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedSwitchWhenGuardWhenPrefixFits()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object entry)
                    {
                        return switch (entry) {
                            case Field field when !field.fragments().isEmpty()
                                            && field.fragments().getFirst() instanceof Fragment fragment -> fragment.name();
                            default -> null;
                        };
                    }

                    interface Field
                    {
                        java.util.List<Object> fragments();
                    }

                    interface Fragment
                    {
                        Object name();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object entry)
                    {
                        return switch (entry) {
                            case Field field when !field.fragments().isEmpty()
                                    && field.fragments().getFirst() instanceof Fragment fragment -> fragment.name();
                            default -> null;
                        };
                    }

                    interface Field
                    {
                        java.util.List<Object> fragments();
                    }

                    interface Fragment
                    {
                        Object name();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsSwitchWhenGuardContinuationAfterWrappedWhen()
    {
        String code =
                """
                class Test
                {
                    Object run(Object entry)
                    {
                        return switch (entry) {
                            case Item item when
                                    first() &&
                                            second() &&
                                            third() -> item;
                            default -> null;
                        };
                    }

                    record Item(Object value) {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesOverIndentedSwitchWhenGuardContinuation()
    {
        String oldCode =
                """
                record Pair(Object left, Object right) {}

                class Test
                {
                    boolean run(Object expression)
                    {
                        return switch (expression) {
                            case Pair(var left, var right)
                                    when matches(left) &&
                                                    matches(right) -> true;
                            default -> false;
                        };
                    }
                }
                """;

        String newCode =
                """
                record Pair(Object left, Object right) {}

                class Test
                {
                    boolean run(Object expression)
                    {
                        return switch (expression) {
                            case Pair(var left, var right)
                                    when matches(left) &&
                                    matches(right) -> true;
                            default -> false;
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsQualifiedSwitchWhenGuardContinuation()
    {
        String code =
                """
                record Pair(Object left, Object right) {}

                class Test
                {
                    boolean run(Object expression)
                    {
                        return switch (expression) {
                            case Pair(var left, var right)
                                    when first(left) &&
                                    value(right).ready() &&
                                    value(right).allowed() -> true;
                            default -> false;
                        };
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsSameLineSwitchWhenGuardContinuation()
    {
        String code =
                """
                class Test
                {
                    Object run(Object expression, Entry entry, Aliases aliases)
                    {
                        return switch (expression) {
                            case In expected
                                    when entry.value() instanceof In actual &&
                                    matches(aliases, expected.value(), actual.value()) &&
                                    matches(aliases, expected.reference(), actual.reference()) -> entry;
                            default -> null;
                        };
                    }

                    boolean matches(Object aliases, Object expected, Object actual)
                    {
                        return true;
                    }

                    interface Entry
                    {
                        Object value();
                    }

                    interface In
                    {
                        Object value();

                        Object reference();
                    }

                    interface Aliases {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsSameLineSwitchWhenGuardContinuationAfterQualifiedFirstOperand()
    {
        String code =
                """
                class Test
                {
                    Object run(Object expression, Entry entry)
                    {
                        return switch (expression) {
                            case Match expected
                                    when entry.value() instanceof Match actual &&
                                    expected.operator().equals(actual.operator()) &&
                                    expected.quantifier().equals(actual.quantifier()) -> entry;
                            default -> null;
                        };
                    }

                    interface Entry
                    {
                        Object value();
                    }

                    interface Match
                    {
                        Object operator();

                        Object quantifier();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsQualifiedSwitchWhenGuardContinuationForQualifiedCallNullabilityChecks()
    {
        String code =
                """
                class Test
                {
                    Object run(Object expression)
                    {
                        return switch (expression) {
                            case Call inner when
                                    inner.function().deterministic() &&
                                            !inner.function().functionNullability().isReturnNullable() &&
                                            inner.function().functionNullability().getArgumentNullable().stream().allMatch(Boolean.TRUE::equals) -> false;
                            default -> null;
                        };
                    }

                    interface Call
                    {
                        Function function();
                    }

                    interface Function
                    {
                        boolean deterministic();

                        Nullability functionNullability();
                    }

                    interface Nullability
                    {
                        boolean isReturnNullable();

                        java.util.List<Boolean> getArgumentNullable();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsQualifiedSwitchWhenGuardContinuationAcrossMixedPriorCases()
    {
        String code =
                """
                class Test
                {
                    Object run(Object expression)
                    {
                        return switch (expression) {
                            case IsNull _ -> false;
                            case Row _ -> false;
                            case Call inner when
                                    inner.function().deterministic() &&
                                            !inner.function().functionNullability().isReturnNullable() &&
                                            inner.function().functionNullability().getArgumentNullable().stream().allMatch(Boolean.TRUE::equals) -> false;
                            default -> null;
                        };
                    }

                    record IsNull(Object value) {}

                    record Row(Object value) {}

                    interface Call
                    {
                        Function function();
                    }

                    interface Function
                    {
                        boolean deterministic();

                        Nullability functionNullability();
                    }

                    interface Nullability
                    {
                        boolean isReturnNullable();

                        java.util.List<Boolean> getArgumentNullable();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsQualifiedSwitchWhenGuardContinuationAfterMixedCallCases()
    {
        String code =
                """
                class Test
                {
                    Object run(Object inner)
                    {
                        return switch (inner) {
                            case IsNull _ -> false;
                            case Row _ -> false;
                            case Comparison comparison when comparison.operator() != IDENTICAL -> or(
                                    new IsNull(comparison.left()),
                                    new IsNull(comparison.right()));
                            case Comparison comparison when comparison.operator() == IDENTICAL -> false;
                            case Call call when call.function().name().equals(name("$not")) -> new IsNull(call.arguments().getFirst());
                            case Call call when
                                    call.function().deterministic() &&
                                            !call.function().functionNullability().isReturnNullable() &&
                                            call.function().functionNullability().getArgumentNullable().stream().allMatch(Boolean.TRUE::equals) -> false;
                            default -> null;
                        };
                    }

                    static final Object IDENTICAL = new Object();

                    record IsNull(Object value) {}

                    record Row(Object value) {}

                    record Comparison(Object left, Object right, Object operator) {}

                    interface Call
                    {
                        Function function();

                        java.util.List<Object> arguments();
                    }

                    interface Function
                    {
                        Object name();

                        boolean deterministic();

                        Nullability functionNullability();
                    }

                    interface Nullability
                    {
                        boolean isReturnNullable();

                        java.util.List<Boolean> getArgumentNullable();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterRemovesSpaceBeforeColonInCaseLabel()
    {
        String oldCode =
                """
                class Test
                {
                    String run(String kind)
                    {
                        switch (kind) {
                            case "double" :
                                return "d";
                            case "float" :
                                return "f";
                            default:
                                return "x";
                        }
                    }
                }
                """;
        String newCode =
                """
                class Test
                {
                    String run(String kind)
                    {
                        switch (kind) {
                            case "double":
                                return "d";
                            case "float":
                                return "f";
                            default:
                                return "x";
                        }
                    }
                }
                """;
        assertFormatsOldToNew(oldCode, newCode);
    }
}
