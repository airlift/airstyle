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
package io.airlift.airstyle.normalizer;

import org.junit.jupiter.api.Test;

import static io.airlift.airstyle.FormatterAssertions.assertCanonicalFormatting;
import static io.airlift.airstyle.FormatterAssertions.assertFormatsOldToNew;

public class TestArrayInitializerBlankLineNormalizer
{
    @Test
    void testFormatterFixesLeadingBlankLineInArrayInitializer()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        String[] values = {

                                "a",
                                "b",
                        };
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        String[] values = {
                                "a",
                                "b",
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesTrailingBlankLineInArrayInitializer()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        String[] values = {
                                "a",
                                "b",

                        };
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        String[] values = {
                                "a",
                                "b",
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterAddsTrailingCommaToMultilineArrayInitializer()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        String[] values = {
                                "a",
                                "b"
                        };
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        String[] values = {
                                "a",
                                "b",
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesMixedMultilineArrayInitializer()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        String[] values = {
                                "a", "b",
                                "c",
                        };
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsInlineArrayInitializerInline()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        String[] values = {"a", "b", "c"};
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesBlankLineBeforeObjectArrayInitializerBrace()
    {
        String oldCode =
                """
                class Test
                {
                    Object[] values()
                    {
                        return new Object[]

                                {
                                        "a",
                                        "b",
                                };
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object[] values()
                    {
                        return new Object[] {
                                "a",
                                "b",
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesLineBreakBeforeObjectArrayInitializerBrace()
    {
        String oldCode =
                """
                class Test
                {
                    Object[] values()
                    {
                        return new Object[]
                        {
                                "a",
                                "b",
                        };
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object[] values()
                    {
                        return new Object[] {
                                "a",
                                "b",
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesBlankLineBeforeNestedObjectArrayInitializerBrace()
    {
        String oldCode =
                """
                class Test
                {
                    Object[][] values()
                    {
                        return new Object[][]

                                {
                                        {"a"},
                                        {"b"},
                                };
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object[][] values()
                    {
                        return new Object[][] {
                                {"a"},
                                {"b"},
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesLineBreakBeforeNestedObjectArrayInitializerBrace()
    {
        String oldCode =
                """
                class Test
                {
                    Object[][] values()
                    {
                        return new Object[][]
                        {
                                {"a"},
                                {"b"},
                        };
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object[][] values()
                    {
                        return new Object[][] {
                                {"a"},
                                {"b"},
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedCallInsideArrayInitializer()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        create(
                                new Row[] {row(
                                        cast(a()),
                                        cast(b()))},
                                value());
                    }

                    class Row {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        create(
                                new Row[] {
                                        row(cast(a()),
                                                cast(b())),
                                },
                                value());
                    }

                    class Row {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesLeadingBlankLineInAnnotationArrayLiteral()
    {
        String oldCode =
                """
                @Demo({

                        "a",
                        "b"
                })
                class Test {}
                """;

        String newCode =
                """
                @Demo({
                        "a",
                        "b",
                })
                class Test {}
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesLeadingBlankLineInAnnotationArrayDefault()
    {
        String oldCode =
                """
                @interface Demo
                {
                    String[] value() default {

                            "a",
                            "b"
                    };
                }
                """;

        String newCode =
                """
                @interface Demo
                {
                    String[] value() default {
                            "a",
                            "b",
                    };
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesMixedMultilineAnnotationArrayLiteral()
    {
        String code =
                """
                @Demo({
                        "a", "b",
                })
                class Test {}
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsAnnotationArrayOfAnnotationsAligned()
    {
        String code =
                """
                class Test
                {
                    @Demo(values = {
                            @Ann(key = "a", value = "1"),
                            @Ann(key = "b", value = "2"),
                    })
                    void run() {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesAnnotationArrayOfAnnotationsIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    @Demo(values = {
                            @Ann(key = "a", value = "1"),
                    @Ann(key = "b", value = "2"),
                    })
                    void run() {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    @Demo(values = {
                            @Ann(key = "a", value = "1"),
                            @Ann(key = "b", value = "2"),
                    })
                    void run() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsAnnotationArrayCommentsAttached()
    {
        String oldCode =
                """
                class Test
                {
                    @Tool(app = @App(
                            connectDomains = {
                                    "a", // first
                                    "b"}, // last
                            resourceDomains = {
                                    "c", // third
                                    "d"}))
                    void run() {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    @Tool(app = @App(
                            connectDomains = {
                                    "a", // first
                                    "b", // last
                            },
                            resourceDomains = {
                                    "c", // third
                                    "d",
                            }))
                    void run() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsAlignedTrailingCommentOnLastArrayElement()
    {
        String code =
                """
                class Test
                {
                    Object[] patterns = {
                            compile("a"),  // a
                            compile("b"),                    // b
                            compile("c"),          // c
                            compile("d"),                           // d
                    };
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsInlineAnnotationArrayLiteralInline()
    {
        String code =
                """
                @Demo({"a", "b"})
                class Test {}
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesTrailingBlankLineInAnnotationArrayLiteral()
    {
        String oldCode =
                """
                @Demo({
                        "a",
                        "b",

                })
                class Test {}
                """;

        String newCode =
                """
                @Demo({
                        "a",
                        "b",
                })
                class Test {}
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterMovesClosingBraceToOwnLineForWrappedArrayInitializer()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        String[] values = {
                                "a",
                                "b"};
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        String[] values = {
                                "a",
                                "b",
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsReturnObjectArrayInitializerWithFirstElementOnNextLine()
    {
        String code =
                """
                class Test
                {
                    Object[] row()
                    {
                        return new Object[] {
                                id,
                                name,
                                enabled ? "yes" : "no",
                                message.orElse("-"),
                        };
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesNestedReturnObjectArrayInitializerWithClosingBraceAfterLastElement()
    {
        String oldCode =
                """
                class Test
                {
                    static class Result
                    {
                        Object[] row()
                        {
                            return new Object[] {
                                id,
                                name,
                                enabled ? "yes" : "no",
                                message.orElse("-")};
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    static class Result
                    {
                        Object[] row()
                        {
                            return new Object[] {
                                    id,
                                    name,
                                    enabled ? "yes" : "no",
                                    message.orElse("-"),
                            };
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsArrayInitializerCommentsAttached()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        String[] values = {
                                "a", // first
                                "b"}; // last
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        String[] values = {
                                "a", // first
                                "b", // last
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }
}
