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

public class TestEnumConstantAnnotationFormatting
{
    @Test
    void testAnnotationAndEnumConstantStayOnSameLine()
    {
        String input =
                """
                import com.fasterxml.jackson.annotation.JsonProperty;

                enum SortOrder
                {
                    @JsonProperty("asc") ASC,
                    @JsonProperty("desc") DESC
                }
                """;

        String oldCode =
                """
                import com.fasterxml.jackson.annotation.JsonProperty;

                enum SortOrder
                {
                    @JsonProperty("asc") ASC,
                    @JsonProperty("desc") DESC;
                }
                """;

        assertFormatsOldToNew(oldCode, input);
    }

    @Test
    void testFormatterKeepsSingleBlankLineBetweenAnnotatedEnumConstants()
    {
        String code =
                """
                import com.fasterxml.jackson.annotation.JsonProperty;

                enum Fruit
                {
                    @JsonProperty("apple")
                    APPLE,

                    @JsonProperty("banana")
                    BANANA,

                    @JsonProperty("cherry")
                    CHERRY,
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesRepeatedBlankLinesBetweenAnnotatedEnumConstants()
    {
        String oldCode =
                """
                import com.fasterxml.jackson.annotation.JsonProperty;

                enum Fruit
                {
                    @JsonProperty("apple")
                    APPLE,

                    @JsonProperty("banana")
                    BANANA,

                    @JsonProperty("cherry")
                    CHERRY,
                }
                """;

        String newCode =
                """
                import com.fasterxml.jackson.annotation.JsonProperty;

                enum Fruit
                {
                    @JsonProperty("apple")
                    APPLE,

                    @JsonProperty("banana")
                    BANANA,

                    @JsonProperty("cherry")
                    CHERRY,
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testEnumConstantWithAnonymousBodyIndentedCorrectly()
    {
        String code =
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

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsCommentsBetweenEnumConstantsWithAnonymousBodies()
    {
        String code =
                """
                enum State
                {
                    FIRST {
                        @Override
                        boolean enabled()
                        {
                            return true;
                        }
                    },

                    // Explains the second state.
                    SECOND {
                        @Override
                        boolean enabled()
                        {
                            return false;
                        }
                    },
                }
                """;

        assertCanonicalFormatting(code);
    }
}
