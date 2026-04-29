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

public class TestLambdaBraceLineFormatting
{
    @Test
    void testFormatterFixesLambdaOpeningBracePlacement()
    {
        String oldCode =
                """
                class Test {
                    Runnable action = () ->
                    {
                        run();
                    };
                }
                """;

        String newCode =
                """
                class Test
                {
                    Runnable action = () -> {
                        run();
                    };
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsSingleLineThrowOnlyLambdaBlockInMethodArguments()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        var future = addTimeout(root, () -> { throw new RuntimeException("x"); }, duration, executorService);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedThrowOnlyLambdaBlockInMethodArguments()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        var future = addTimeout(root, () -> {
                            throw new RuntimeException("x");
                        }, duration, executorService);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesSingleLineReturnLambdaBlockInMethodArguments()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        var future = addTimeout(root, () -> { return value(); }, duration, executorService);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        var future = addTimeout(root, () -> {
                            return value();
                        }, duration, executorService);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesSingleLineMultiStatementLambdaBlockInMethodArguments()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        var future = addTimeout(root, () -> { before(); throw new RuntimeException("x"); }, duration, executorService);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        var future = addTimeout(root, () -> {
                            before();
                            throw new RuntimeException("x");
                        }, duration, executorService);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsWrappedListOfInlineSingleThrowLambdas()
    {
        // Each throw-only block lambda keeps its `{ throw ...; }` on one line
        // (FORMATTER_STYLE.md: "A block lambda with a body that is only a
        // single `throw` statement may stay inline if it is already inline").
        // This compact lambda body is handled before the general
        // brace-on-own-line rule for non-empty blocks.
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        exec(
                                () -> { throw new UnsupportedOperationException(); },
                                () -> { throw new UnsupportedOperationException(); },
                                () -> { throw new UnsupportedOperationException(); });
                    }
                }
                """;

        // WLN moves the first short lambda inline with `exec(` — that's a
        // separate WLN decision, orthogonal to the throw-lambda-inline fix.
        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        exec(() -> { throw new UnsupportedOperationException(); },
                                () -> { throw new UnsupportedOperationException(); },
                                () -> { throw new UnsupportedOperationException(); });
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testSwitchExpressionAsLambdaBodyOnNewLineGetsContinuationIndent()
    {
        String code =
                """
                class Test
                {
                    Object run(int kind)
                    {
                        return Optional.of(kind).map(k ->
                                switch (k) {
                                    case 1 -> "one";
                                    default -> "other";
                                });
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }
}
