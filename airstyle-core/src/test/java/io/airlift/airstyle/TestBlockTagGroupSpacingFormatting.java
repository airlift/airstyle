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

public class TestBlockTagGroupSpacingFormatting
{
    @Test
    void testFormatterFixesMissingEmptyLineBeforeBlockTagGroup()
    {
        String oldCode =
                """
                class Test
                {
                    /**
                     * Returns value.
                     * @param value input value
                     * @return value
                     */
                    String run(String value)
                    {
                        return value;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    /**
                     * Returns value.
                     *
                     * @param value input value
                     * @return value
                     */
                    String run(String value)
                    {
                        return value;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsPreCodeBlockAtLinesWithoutInjectingBlankLine()
    {
        String code =
                """
                class Test
                {
                    /**
                     * Example usage:
                     * <pre>{@code
                     * @ExampleTest
                     * class MyTest {}
                     * }</pre>
                     */
                    void run() {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesMissingEmptyLineBeforeBlockTagGroupAfterPreCodeBlock()
    {
        String oldCode =
                """
                class Test
                {
                    /**
                     * Example usage:
                     * <pre>{@code
                     * @ExampleTest
                     * class MyTest {}
                     * }</pre>
                     * @param value input value
                     */
                    String run(String value)
                    {
                        return value;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    /**
                     * Example usage:
                     * <pre>{@code
                     * @ExampleTest
                     * class MyTest {}
                     * }</pre>
                     *
                     * @param value input value
                     */
                    String run(String value)
                    {
                        return value;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsBlockTagOnlyJavadocUnchanged()
    {
        String code =
                """
                class Test
                {
                    /**
                     * @param value input value
                     * @return value
                     */
                    String run(String value) {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsPreCodeBlockContainingAtSignsUnchanged()
    {
        String code =
                """
                class Test
                {
                    /**
                     * Example usage:
                     * <pre>{@code
                     * @Sample
                     * class Example {
                     *     @Field
                     *     Object value;
                     * }
                     * }</pre>
                     */
                    void run() {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsMarkdownDocCommentBlankLineBeforeBlockTags()
    {
        String code =
                """
                class Test
                {
                    /// Returns value.
                    ///
                    /// @param value input value
                    /// @return value
                    String run(String value)
                    {
                        return value;
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesMissingBlankLineBeforeMarkdownDocCommentBlockTags()
    {
        String oldCode =
                """
                class Test
                {
                    /// Returns value.
                    /// @param value input value
                    /// @return value
                    String run(String value)
                    {
                        return value;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    /// Returns value.
                    ///
                    /// @param value input value
                    /// @return value
                    String run(String value)
                    {
                        return value;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsSpacesInsideInlineJavadocCodeTags()
    {
        String code =
                """
                class Test
                {
                    /**
                     * Returns a token.
                     *
                     * @return {@code a  b}
                     */
                    String run()
                    {
                        return "";
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }
}
