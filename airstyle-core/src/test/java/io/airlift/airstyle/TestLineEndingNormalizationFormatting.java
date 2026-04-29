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

import static io.airlift.airstyle.FormatterAssertions.assertFormatsOldToNew;

public class TestLineEndingNormalizationFormatting
{
    @Test
    void testFormatterFixesCarriageReturnLineEndings()
    {
        String oldCode = withLineEnding(
                """
                class Test {
                    void run() {
                        value();
                    }
                }
                """,
                "\r\n");

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        value();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesCrOnlyLineEndings()
    {
        String oldCode = withLineEnding(
                """
                class Test {
                    Object run() {
                        return first()
                             + second()
                                    + third();
                    }
                }
                """,
                "\r");

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
    void testFormatterFixesCrlfLineEndingsForWrappedListContinuation()
    {
        String oldCode = withLineEnding(
                """
                class Test {
                    Object run(Object first, Object second) {
                        return execute(
                                first,
                                      Policy.builder()
                                              .setFirst(first)
                                              .setSecond(second)
                                              .build(),
                                "done");
                    }
                }
                """,
                "\r\n");

        String newCode =
                """
                class Test
                {
                    Object run(Object first, Object second)
                    {
                        return execute(
                                first,
                                Policy.builder()
                                        .setFirst(first)
                                        .setSecond(second)
                                        .build(),
                                "done");
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    private static String withLineEnding(String source, String lineEnding)
    {
        return source.replace("\n", lineEnding);
    }
}
