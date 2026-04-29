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

public class TestNestedArrayInitializerFormatting
{
    @Test
    void testFormatterKeepsNestedArrayInitializersOnSeparateLines()
    {
        String code =
                """
                class Test
                {
                    double[][] values = {
                            {
                                    1.0, 2.0,
                            },
                            {
                                    3.0, 4.0,
                            },
                    };
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsInlineNestedArrayInitializersInline()
    {
        String code =
                """
                class Test
                {
                    double[][] values = {{1.0, 2.0}, {3.0, 4.0}};
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsMixedNestedArrayInitializerOnSeparateLine()
    {
        String code =
                """
                class Test
                {
                    byte[][] values = new byte[][] {
                            new byte[0],
                            {(byte) 1},
                            new byte[1],
                    };
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesMixedNestedArrayInitializerOnSeparateLine()
    {
        String oldCode =
                """
                class Test
                {
                    byte[][] values = new byte[][] {
                            new byte[0], {(byte) 1},
                            new byte[1],
                    };
                }
                """;

        String newCode =
                """
                class Test
                {
                    byte[][] values = new byte[][] {
                            new byte[0],
                            {(byte) 1},
                            new byte[1],
                    };
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsGroupedNestedArrayInitializersPerLine()
    {
        String code =
                """
                class Test
                {
                    String[][] resources = {
                            {"server-01"}, {"server-02"}, {"server-03"}, {"server-04"}, {"server-05"},
                            {"database-01"}, {"database-02"},
                            {"firewall-01"}, {"firewall-02"},
                            {"loadbalancer-01"},
                            {"server-01", "server-02"}, {"database-01", "database-02"}, {"server-03", "firewall-01"},
                    };
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterDoesNotCrashWhenEarlierArrayFormattingShiftsNestedArrayOffsets()
    {
        String oldCode =
                """
                class Test
                {
                    String[] names = {
                            "a",
                            "b"
                    };

                    double[][] values = {
                            {
                                    1.0, 2.0,
                            },
                            {
                                    3.0, 4.0,
                            }
                    };
                }
                """;

        String newCode =
                """
                class Test
                {
                    String[] names = {
                            "a",
                            "b",
                    };

                    double[][] values = {
                            {
                                    1.0, 2.0,
                            },
                            {
                                    3.0, 4.0,
                            },
                    };
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }
}
