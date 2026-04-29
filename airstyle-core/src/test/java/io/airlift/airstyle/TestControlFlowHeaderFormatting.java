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

public class TestControlFlowHeaderFormatting
{
    @Test
    void testFormatterKeepsWrappedBooleanAssignmentContinuationInCatchLoop()
    {
        String code =
                """
                class Test
                {
                    void run(Method method, Exception failure)
                            throws Exception
                    {
                        try {
                            throw failure;
                        }
                        catch (Exception e) {
                            boolean canThrowChecked = false;
                            for (Class<?> exceptionType : method.getExceptionTypes()) {
                                if (exceptionType.isAssignableFrom(e.getClass())) {
                                    throw e;
                                }
                                canThrowChecked = canThrowChecked ||
                                        exceptionType == Exception.class;
                            }
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testMultiLineForHeaderIndentsClausesAtContinuation()
    {
        // Each semicolon-separated for-header clause gets continuation indent
        // when it starts on its own line.
        String code =
                """
                class Test
                {
                    void run()
                    {
                        for (State s = init();
                                s != FAILED;
                                s = next()) {
                            nap();
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testIfConditionWithStringContainingOpenParenDoesNotBreakBodyIndent()
    {
        // Parenthesis matching for an if condition ignores parentheses inside
        // string literals so the body keeps normal block indentation.
        String code =
                """
                class Test
                {
                    void run(String x)
                    {
                        if (x.startsWith("(")) {
                            doit();
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedCallInsideForEachIterable()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        for (Object x : Sets.intersection(
                                ImmutableSet.copyOf(a),
                                ImmutableSet.copyOf(b))) {
                            use(x);
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedChainInsideForEachIterable()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        for (Object x : ClassPath.from(getClass().getClassLoader())
                                .getResources()) {
                            use(x);
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedCallInsideWhileCondition()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        while (matches(
                                left,
                                right)) {
                            work();
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedCallInsideSynchronizedCondition()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        synchronized (monitor(
                                left,
                                right)) {
                            work();
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedCallsInForInitializerAndUpdaterClauses()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        for (State state = init(
                                left,
                                right);
                                ready(state);
                                state = advance(
                                        state,
                                        right)) {
                            work();
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsEnhancedForIterableOnContinuationLineAfterColon()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        for (Object value :
                                values) {
                            work();
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterIndentsInfixOperandsInForConditionClause()
    {
        String code =
                """
                class Test
                {
                    void run(java.util.Queue<Object> exchangeClient)
                    {
                        for (Object state = null;
                                (state != this) &&
                                        !exchangeClient.isEmpty() &&
                                        !(exchangeClient.peek() == null);
                                state = this) {
                            use();
                        }
                    }

                    void use() {}
                }
                """;

        assertCanonicalFormatting(code);
    }
}
