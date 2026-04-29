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

public class TestControlFlowBraceFormatting
{
    @Test
    void testFormatterFixesControlFlowBracePlacement()
    {
        String oldCode =
                """
                class Test {
                    void run(boolean flag)
                    {
                        if (flag)
                        {
                            execute();
                        } else
                        {
                            fallback();
                        }

                        while (flag)
                        {
                            break;
                        }

                        do
                        {
                            execute();
                        } while (flag);

                        try
                        {
                            execute();
                        } catch (RuntimeException e)
                        {
                            fallback();
                        } finally
                        {
                            execute();
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(boolean flag)
                    {
                        if (flag) {
                            execute();
                        }
                        else {
                            fallback();
                        }

                        while (flag) {
                            break;
                        }

                        do {
                            execute();
                        }
                        while (flag);

                        try {
                            execute();
                        }
                        catch (RuntimeException e) {
                            fallback();
                        }
                        finally {
                            execute();
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesRightCurlyPlacement()
    {
        String oldCode =
                """
                class Test {
                    int run(boolean flag)
                    {
                        if (flag) { return 1; } else { return 2; }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    int run(boolean flag)
                    {
                        if (flag) {
                            return 1;
                        }
                        else {
                            return 2;
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesElseIfIndentationDrift()
    {
        String oldCode =
                """
                class Test
                {
                    void run(boolean first, boolean second)
                    {
                        if (first) {
                            execute();
                        }
                           else if (second) {
                               fallback();
                           }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(boolean first, boolean second)
                    {
                        if (first) {
                            execute();
                        }
                        else if (second) {
                            fallback();
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesFinallyIndentationDrift()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        try {
                            execute();
                        }
                           finally {
                               cleanup();
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
                        try {
                            execute();
                        }
                        finally {
                            cleanup();
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsCommentBeforeFinallyKeyword()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        try {
                            execute();
                        }
                        /* finally marker */
                        finally {
                            cleanup();
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testEmptyCatchBlockPreservesMultilineBraces()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        try {
                            doSomething();
                        }
                        catch (Exception e) {
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testEmptyFinallyBlockPreservesMultilineBraces()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        try {
                            doSomething();
                        }
                        finally {
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testBlankLineBeforeFinallyClausePreserved()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        try {
                            doWork();
                        }

                        finally {
                            cleanup();
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsIfElseWithMethodCall()
    {
        String code =
                """
                class Test
                {
                    void run(int x)
                    {
                        if (x > 0) {
                            doIt();
                        }
                        else {
                            skip();
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsTryCatchFinally()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        try {
                            work();
                        }
                        catch (Exception e) {
                            log(e);
                        }
                        finally {
                            cleanup();
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsForLoopWithContinue()
    {
        String code =
                """
                class Test
                {
                    void run(List<String> values)
                    {
                        for (String value : values) {
                            if (value.isEmpty()) {
                                continue;
                            }
                            process(value);
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }
}
