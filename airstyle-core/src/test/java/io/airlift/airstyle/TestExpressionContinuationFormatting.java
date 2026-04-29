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

public class TestExpressionContinuationFormatting
{
    @Test
    void testFormatterFixesBooleanContinuationIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    boolean run(boolean a, boolean b, boolean c)
                    {
                        return a &&
                               b &&
                               c;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    boolean run(boolean a, boolean b, boolean c)
                    {
                        return a &&
                                b &&
                                c;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedBinaryExpressionContinuation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return first()
                             + second()
                                    + third();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return first()
                                + second()
                                + third();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedBinaryExpressionContinuation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return first()
                                      + second()
                                      + third();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return first()
                                + second()
                                + third();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesBinaryExpressionContinuationInExpressionLambda()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return withSupplier(() ->
                                first()
                              + second()
                                     + third());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return withSupplier(() ->
                                first()
                                        + second()
                                        + third());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesBinaryExpressionContinuationInBlockLambda()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return withSupplier(() -> {
                            return first()
                                + second()
                                   + third();
                        });
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return withSupplier(() -> {
                            return first()
                                    + second()
                                    + third();
                        });
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedTernaryContinuationWithWrappedInvocation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return enabled
                             ? choose(
                                      first(),
                                      second())
                              : fallback();
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
                                ? choose(
                                first(),
                                second())
                                : fallback();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedTernaryContinuationWithWrappedInvocation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return enabled
                                      ? choose(
                                                    first(),
                                                    second())
                                      : fallback();
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
                                ? choose(
                                first(),
                                second())
                                : fallback();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesTernaryContinuationInExpressionLambda()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return withSupplier(() -> enabled
                              ? choose(
                                       first(),
                                       second())
                               : fallback());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return withSupplier(() -> enabled
                                ? choose(
                                first(),
                                second())
                                : fallback());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesTernaryContinuationInBlockLambda()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return withSupplier(() -> {
                            return enabled
                                ? choose(
                                         first(),
                                         second())
                                 : fallback();
                        });
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return withSupplier(() -> {
                            return enabled
                                    ? choose(
                                    first(),
                                    second())
                                    : fallback();
                        });
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedLogicalBinaryExpressionContinuation()
    {
        String oldCode =
                """
                class Test
                {
                    boolean run()
                    {
                        return first()
                             && second()
                                    && third();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    boolean run()
                    {
                        return first()
                                && second()
                                && third();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesParenthesizedPrefixExpressionContinuationInLogicalExpression()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return values.stream()
                                .filter(value -> value.ready() || (
                        !hasType(value)) &&
                                        !hasName(value) &&
                                        !hasSize(value))
                                .toList();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return values.stream()
                                .filter(value -> value.ready() || (
                                        !hasType(value)) &&
                                        !hasName(value) &&
                                        !hasSize(value))
                                .toList();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedLogicalBinaryExpressionContinuationAfterTrailingOperator()
    {
        String oldCode =
                """
                class Test
                {
                    boolean run(Holder node)
                    {
                        return node.first() &&
                               node.second() == 1 &&
                               node.third();
                    }

                    interface Holder
                    {
                        boolean first();

                        int second();

                        boolean third();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    boolean run(Holder node)
                    {
                        return node.first() &&
                                node.second() == 1 &&
                                node.third();
                    }

                    interface Holder
                    {
                        boolean first();

                        int second();

                        boolean third();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedMultiplicativeBinaryExpressionContinuation()
    {
        String oldCode =
                """
                class Test
                {
                    int run()
                    {
                        return first()
                                      * second()
                                      / third();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    int run()
                    {
                        return first()
                                * second()
                                / third();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsBinaryExpressionWithTrailingOperators()
    {
        String code =
                """
                class Test
                {
                    int run()
                    {
                        return first() +
                                second() +
                                third();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesTernaryConstructorBranchIndentationWithWrappedArguments()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(boolean enabled, int maxBufferSize, long maxHeapMemory, long maxOffHeapMemory)
                    {
                        Object value = enabled ?
                                    new BufferPool(
                                        0,
                                        maxBufferSize,
                                        Integer.MAX_VALUE,
                                        maxHeapMemory,
                                        maxOffHeapMemory) :
                                    new BufferPool(
                                        0,
                                        maxBufferSize,
                                        Integer.MAX_VALUE,
                                        maxHeapMemory,
                                        maxOffHeapMemory);
                        return value;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(boolean enabled, int maxBufferSize, long maxHeapMemory, long maxOffHeapMemory)
                    {
                        Object value = enabled ?
                                new BufferPool(
                                        0,
                                        maxBufferSize,
                                        Integer.MAX_VALUE,
                                        maxHeapMemory,
                                        maxOffHeapMemory) :
                                new BufferPool(
                                        0,
                                        maxBufferSize,
                                        Integer.MAX_VALUE,
                                        maxHeapMemory,
                                        maxOffHeapMemory);
                        return value;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedBooleanContinuationInsideCheckArgument()
    {
        String oldCode =
                """
                class Test
                {
                    void run(boolean left, boolean right, java.util.Optional<Object> planNode)
                    {
                        checkArgument(left && planNode.isEmpty()
                                || right && planNode.isPresent(),
                                "plan");
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(boolean left, boolean right, java.util.Optional<Object> planNode)
                    {
                        checkArgument(left && planNode.isEmpty()
                                        || right && planNode.isPresent(),
                                "plan");
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsBooleanContinuationInsideCheckArgumentWithInlineTrailingArgument()
    {
        String code =
                """
                class Test
                {
                    void run(boolean first, boolean second, boolean third, boolean fourth)
                    {
                        checkArgument(first
                                || second
                                || third
                                || fourth, "value must satisfy at least one source");
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesOverIndentedBooleanContinuationInsideCheckArgumentWithInlineTrailingArgument()
    {
        String oldCode =
                """
                class Test
                {
                    void run(boolean first, boolean second, boolean third, boolean fourth)
                    {
                        checkArgument(first
                                        || second
                                        || third
                                        || fourth, "value must satisfy at least one source");
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(boolean first, boolean second, boolean third, boolean fourth)
                    {
                        checkArgument(first
                                || second
                                || third
                                || fourth, "value must satisfy at least one source");
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedNestedInvocationInTernaryAssignment()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(boolean flag, Name name)
                    {
                        Name base = flag ? name : schemaTableName(
                        name.schema(),
                        name.value());
                        return base;
                    }

                    Name schemaTableName(String schema, String value)
                    {
                        return new Name(schema, value);
                    }

                    record Name(String schema, String value) {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(boolean flag, Name name)
                    {
                        Name base = flag ? name : schemaTableName(
                                name.schema(),
                                name.value());
                        return base;
                    }

                    Name schemaTableName(String schema, String value)
                    {
                        return new Name(schema, value);
                    }

                    record Name(String schema, String value) {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedBooleanContinuationInsideParenthesizedRightOperand()
    {
        String oldCode =
                """
                class Test
                {
                    boolean run(Object expression)
                    {
                        return predicate(expression)
                                && (expression instanceof Variable ||
                                        (expression instanceof FieldDereference fieldDereference
                                                        && accepts(fieldDereference.target())));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    boolean run(Object expression)
                    {
                        return predicate(expression)
                                && (expression instanceof Variable ||
                                (expression instanceof FieldDereference fieldDereference
                                        && accepts(fieldDereference.target())));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsBooleanContinuationInsideParenthesizedRightOperandWithPlainWrappedOperand()
    {
        // IntelliJ places secondCheck() at the same column as the `&&` line
        // start: inside `(firstCheck() || secondCheck())`, the `||` wrap
        // gets a single CONTINUATION from the parenthesized infix's line,
        // which is the `&&` line. No extra indent from the paren layer.
        String code =
                """
                class Test
                {
                    boolean run()
                    {
                        return allowed()
                                && (firstCheck() ||
                                secondCheck());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsBooleanComparisonInsideParenthesizedRightOperand()
    {
        String code =
                """
                class Test
                {
                    void run(Object keyState, Object keyBlock)
                    {
                        if (!present() ||
                                (isNull(keyState) && !isNull(keyBlock)) ||
                                (!isNull(keyState) && !isNull(keyBlock) &&
                                        ((long) compare(keyBlock, keyState)) > 0)) {
                            done();
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesUnderIndentedQualifiedBooleanContinuationInsideParenthesizedRightOperand()
    {
        String oldCode =
                """
                class Test
                {
                    boolean run(Object composite)
                    {
                        return outer() && (
                                composite.getPrecision().isPresent() ||
                                (composite.getTo() instanceof Second value && value.ok()));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    boolean run(Object composite)
                    {
                        return outer() && (
                                composite.getPrecision().isPresent() ||
                                        (composite.getTo() instanceof Second value && value.ok()));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterPreservesMultilinePrefixExpressionContinuation()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        boolean result = !
                                (first instanceof Foo
                                        || second instanceof Bar);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterPreservesNestedTernaryFinalValueAlignment()
    {
        String code =
                """
                class Test
                {
                    int run(int a, int b, int c, int d, int e, int f)
                    {
                        return a < c ||
                                (a == c && b < d) ? -1 :
                                a > e ||
                                        (a == e && b > f) ? 1 :
                                0;
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testTernaryColonAdjacentToIncrementDecrementKeepsSpace()
    {
        // Ternary separators keep their required spaces even next to prefix or
        // postfix increment/decrement operators.
        String code =
                """
                class Test
                {
                    int f(boolean flag, int channel)
                    {
                        return flag ? channel++ : -1;
                    }

                    int g(int b)
                    {
                        return b == 0 ? 1 : ++b;
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsMultilineInfixOnAssignmentRhs()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        double factor =
                                // first
                                termA +
                                        // second
                                        termB * termC +
                                        // third
                                        termD;
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterAlignsParenthesizedTernaryClosingParenWithOpening()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(String format)
                    {
                        return query(format(
                                "VALUES " +
                                        "('regionkey', NULL, '0', '4'), " +
                                        "(NULL, NULL, NULL, NULL)") +
                                (format.equals("ORC") ?
                                        "  CAST(NULL AS ROW(min uuid, max uuid)) " :
                                        "  CAST(ROW(UUID 'abc', UUID 'abc') AS ROW(min uuid, max uuid)) "
                ) +
                                ")");
                    }

                    static Object query(String s) { return null; }
                    static String format(String fmt, Object... args) { return ""; }
                }
                """;
        String newCode =
                """
                class Test
                {
                    Object run(String format)
                    {
                        return query(format(
                                "VALUES " +
                                        "('regionkey', NULL, '0', '4'), " +
                                        "(NULL, NULL, NULL, NULL)") +
                                (format.equals("ORC") ?
                                        "  CAST(NULL AS ROW(min uuid, max uuid)) " :
                                        "  CAST(ROW(UUID 'abc', UUID 'abc') AS ROW(min uuid, max uuid)) "
                                ) +
                                ")");
                    }

                    static Object query(String s)
                    {
                        return null;
                    }

                    static String format(String fmt, Object... args)
                    {
                        return "";
                    }
                }
                """;
        assertFormatsOldToNew(oldCode, newCode);
    }
}
