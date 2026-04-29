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

/// Tests spacing behavior around empty and non-empty brace blocks.
public class TestEmptyBraceSpacing
{
    @Test
    void testFormatterFixesEmptyAnonymousClassSpacing()
    {
        String oldCode =
                """
                class Test {
                    void method()
                    {
                        Object o = new TypeReference<String>(){};
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void method()
                    {
                        Object o = new TypeReference<String>() {};
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsEmptyAnonymousClassSpacing()
    {
        String oldCode =
                """
                class Test {
                    void method()
                    {
                        Object o = new TypeReference<String>() {};
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void method()
                    {
                        Object o = new TypeReference<String>() {};
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testEmptyArrayInitializerNoSpaceNormalized()
    {
        String oldCode =
                """
                class Test {
                    void method()
                    {
                        String[] arr = new String[]{};
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void method()
                    {
                        String[] arr = new String[] {};
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testNonEmptyArrayInitializerNoSpaceNormalized()
    {
        String oldCode =
                """
                class Test {
                    void method()
                    {
                        String[] arr = new String[]{"value"};
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void method()
                    {
                        String[] arr = new String[] {"value"};
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testNonEmptyArrayInitializerWithSpaceRemainsCanonical()
    {
        String oldCode =
                """
                class Test {
                    void method()
                    {
                        String[] arr = new String[] {"value"};
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void method()
                    {
                        String[] arr = new String[] {"value"};
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesEmptyAnonymousClassSpacingInInvocationArgument()
    {
        String oldCode =
                """
                class Test {
                    void method()
                    {
                        OBJECT_MAPPER.convertValue(node.get("type"), new TypeReference<>(){});
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void method()
                    {
                        OBJECT_MAPPER.convertValue(node.get("type"), new TypeReference<>() {});
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testEmptyArrowBodyStaysInline()
    {
        String code =
                """
                class Test
                {
                    void run(int x)
                    {
                        switch (x) {
                            case 1 -> doOne();
                            default -> {}
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testEmptyPatternArrowBodyWithTrailingCommentStaysInline()
    {
        String code =
                """
                class Test
                {
                    void run(Object o)
                    {
                        switch (o) {
                            case String s -> handle(s);
                            default -> {} // do nothing
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testSwitchCaseEmptyBlockCollapsesWhenSourceIsInline()
    {
        String code =
                """
                class Test
                {
                    void run(int x)
                    {
                        switch (x) {
                            case 1 -> doOne();
                            case 2 -> {}
                            default -> {}
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testSwitchCaseEmptyBlockStaysExpandedWhenSourceIsExpanded()
    {
        String code =
                """
                class Test
                {
                    void run(State state)
                    {
                        switch (state) {
                            case FOO -> {
                                // ignore
                            }
                            default -> {
                            }
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }
}
