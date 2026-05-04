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

public class TestEnumSemicolonNormalizer
{
    @Test
    void testFormatterFixesUnnecessarySemicolonInEnumWithOnlyConstants()
    {
        String oldCode =
                """
                enum State
                {
                    UNINITIALIZED,
                    INITIALIZED;
                }
                """;

        String newCode =
                """
                enum State
                {
                    UNINITIALIZED,
                    INITIALIZED,
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnnecessarySemicolonInEnumWithSingleConstantWithoutTrailingComma()
    {
        String oldCode =
                """
                enum State
                {
                    INITIALIZED;
                }
                """;

        String newCode =
                """
                enum State
                {
                    INITIALIZED
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsSingleConstantEnumWithoutTrailingComma()
    {
        String code =
                """
                enum State
                {
                    INITIALIZED
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesTrailingCommaInEnumWithSingleConstant()
    {
        String oldCode =
                """
                enum State
                {
                    INITIALIZED,
                }
                """;

        String newCode =
                """
                enum State
                {
                    INITIALIZED
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesTrailingCommaInEnumWithSingleConstantAndTrailingComment()
    {
        String oldCode =
                """
                enum State
                {
                    INITIALIZED, // trailing
                }
                """;

        String newCode =
                """
                enum State
                {
                    INITIALIZED // trailing
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsSemicolonInEnumWithMembers()
    {
        String oldCode =
                """
                enum State
                {
                    UNINITIALIZED;

                    int value()
                    {
                        return 0;
                    }
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterFixesUnnecessarySemicolonInEnumWithTrailingComment()
    {
        String oldCode =
                """
                enum State
                {
                    UNINITIALIZED,
                    INITIALIZED; // trailing
                }
                """;

        String newCode =
                """
                enum State
                {
                    UNINITIALIZED,
                    INITIALIZED, // trailing
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnnecessarySemicolonInEnumWithSingleConstantAndTrailingComment()
    {
        String oldCode =
                """
                enum State
                {
                    INITIALIZED; // trailing
                }
                """;

        String newCode =
                """
                enum State
                {
                    INITIALIZED // trailing
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnnecessarySemicolonInEnumWithTrailingBlockComment()
    {
        String oldCode =
                """
                enum State
                {
                    UNINITIALIZED,
                    INITIALIZED; /* trailing ; marker */
                }
                """;

        String newCode =
                """
                enum State
                {
                    UNINITIALIZED,
                    INITIALIZED, /* trailing ; marker */
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnnecessarySemicolonInEnumWithSingleConstantAndTrailingBlockComment()
    {
        String oldCode =
                """
                enum State
                {
                    INITIALIZED; /* trailing ; marker */
                }
                """;

        String newCode =
                """
                enum State
                {
                    INITIALIZED /* trailing ; marker */
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnnecessarySemicolonAfterEnumConstantClassBody()
    {
        String oldCode =
                """
                enum State
                {
                    INITIALIZED {
                        int value()
                        {
                            return 1;
                        }
                    };
                }
                """;

        String newCode =
                """
                enum State
                {
                    INITIALIZED {
                        int value()
                        {
                            return 1;
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsSimpleEnumWithCommentUnchanged()
    {
        String code =
                """
                enum State
                {
                    UNINITIALIZED, // comment
                    INITIALIZED,
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedSimpleEnumUnchanged()
    {
        String code =
                """
                class Test
                {
                    private enum JettyAsyncHttpState
                    {
                        WAITING_FOR_CONNECTION,
                        PROCESSING_RESPONSE,
                        DONE,
                        FAILED,
                        CANCELED,
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedSimpleEnumUnchangedAfterWrappedGenericHeader()
    {
        String code =
                """
                class Test<T, E
                        extends Base<T>>
                        implements Runnable
                {
                    private enum JettyAsyncHttpState
                    {
                        WAITING_FOR_CONNECTION,
                        PROCESSING_RESPONSE,
                        DONE,
                        FAILED,
                        CANCELED,
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }
}
