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

public class TestCommentFormatting
{
    @Test
    void testFormatterFixesContinuationCommentIndentInsideReturnExpression()
    {
        String oldCode =
                """
                class Test
                {
                    boolean run(Object left, Object right)
                    {
                        return equals(left, right)
                        // remoteTableName is not compared here
                                /**/;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    boolean run(Object left, Object right)
                    {
                        return equals(left, right)
                                // remoteTableName is not compared here
                                /**/;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesBinaryContinuationWithTrailingComments()
    {
        String oldCode =
                """
                class Test
                {
                    int run()
                    {
                        return first()
                             + second() // two
                                    + third(); // three
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    int run()
                    {
                        return first()
                                + second() // two
                                + third(); // three
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesTernaryContinuationWithTrailingComments()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return enabled
                             ? choose(left, right) // then
                              : choose(fallbackLeft, fallbackRight); // else
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return enabled
                                ? choose(left, right) // then
                                : choose(fallbackLeft, fallbackRight); // else
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesBinaryContinuationWithBlockCommentBetweenOperatorAndOperand()
    {
        String oldCode =
                """
                class Test
                {
                    int run()
                    {
                        return first()
                             + /* mid */ second()
                                    + third();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    int run()
                    {
                        return first()
                                + /* mid */ second()
                                + third();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesTernaryContinuationWithInlineCommentsBetweenOperatorAndOperand()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return enabled
                             ? /* then */ choose(left, right)
                              : /* else */ choose(fallbackLeft, fallbackRight);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return enabled
                                ? /* then */ choose(left, right)
                                : /* else */ choose(fallbackLeft, fallbackRight);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsBlankLineBeforeCommentsBetweenWrappedMethodArguments()
    {
        String oldCode =
                """
                class Test{void run(){call(
                first,

                                // Group
                second,
                third,

                                // Other group
                fourth);}}
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        call(
                                first,

                                // Group
                                second,
                                third,

                                // Other group
                                fourth);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesRepeatedBlankLinesBeforeCommentsBetweenWrappedMethodArguments()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        call(
                                first,


                                // Group
                                second);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        call(
                                first,

                                // Group
                                second);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testColumnZeroLineCommentsArePreservedAtColumnZero()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        URI redirect = UriBuilder.fromUri(redirectUri)
                                .queryParam("code", code)
                //                .queryParam("error", error)
                //                .queryParam("error_description", description)
                                .build();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testAddingTrailingCommaPreservesInlineCommentAlignment()
    {
        // When the last element of a multiline array has an inline
        // trailing comment and no trailing comma, add the comma while
        // preserving the author's alignment spacing:
        //   0 or 1 spaces  ->  ", " (comma + 1 space)
        //   N >= 2 spaces  ->  replace the first space with the comma,
        //                      keeping the remaining (N-1) spaces.
        String input =
                """
                class Test
                {
                    void run()
                    {
                        String[] xs = {
                                "2025-3-3",    // without leading zeros
                                "2025-03-03"   // with leading zeros
                        };
                    }
                }
                """;
        String expected =
                """
                class Test
                {
                    void run()
                    {
                        String[] xs = {
                                "2025-3-3",    // without leading zeros
                                "2025-03-03",  // with leading zeros
                        };
                    }
                }
                """;
        assertFormatsOldToNew(input, expected);
    }

    @Test
    void testAddingTrailingCommaWithNoSpaceBeforeComment()
    {
        String input =
                """
                class Test
                {
                    void run()
                    {
                        int[] xs = {
                                1,
                                123// zero spaces
                        };
                    }
                }
                """;
        String expected =
                """
                class Test
                {
                    void run()
                    {
                        int[] xs = {
                                1,
                                123, // zero spaces
                        };
                    }
                }
                """;
        assertFormatsOldToNew(input, expected);
    }

    @Test
    void testAddingTrailingCommaWithOneSpaceBeforeComment()
    {
        String input =
                """
                class Test
                {
                    void run()
                    {
                        int[] xs = {
                                1,
                                123 // one space
                        };
                    }
                }
                """;
        String expected =
                """
                class Test
                {
                    void run()
                    {
                        int[] xs = {
                                1,
                                123, // one space
                        };
                    }
                }
                """;
        assertFormatsOldToNew(input, expected);
    }

    @Test
    void testAddingTrailingCommaWithFourSpacesBeforeComment()
    {
        String input =
                """
                class Test
                {
                    void run()
                    {
                        int[] xs = {
                                1,
                                123    // four spaces
                        };
                    }
                }
                """;
        String expected =
                """
                class Test
                {
                    void run()
                    {
                        int[] xs = {
                                1,
                                123,   // four spaces
                        };
                    }
                }
                """;
        assertFormatsOldToNew(input, expected);
    }

    @Test
    void testInlineTrailingCommentOnLastArrayElementStaysInline()
    {
        // A same-line trailing comment belongs to the array element even when
        // the formatter adds the trailing comma. Keep the comment inline with
        // the element.
        String code =
                """
                class Test
                {
                    void run()
                    {
                        String[] xs = {
                                "a",    // first
                                "b",    // last (with trailing comma)
                        };
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testStandaloneCommentBeforeClosingBraceInArrayKeepsSiblingIndent()
    {
        // Variant: the comment sits on its own line between the last
        // element and the `}`. Preserve the comment's source indent so it
        // aligns with the element siblings it was written next to.
        String code =
                """
                class Test
                {
                    void run()
                    {
                        String[] xs = {
                                "a",
                                "b"
                                // sibling comment
                        };
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterCollapsesPaddingInJavadocTagLine()
    {
        String oldCode =
                """
                class Test
                {
                    /**
                     * @param path      input directory
                     * @param fileEntry file entry in the directory
                     * @throws Exception      description
                     * @return the path
                     */
                    Object run(String path, Object fileEntry) throws Exception
                    {
                        return null;
                    }
                }
                """;
        String newCode =
                """
                class Test
                {
                    /**
                     * @param path input directory
                     * @param fileEntry file entry in the directory
                     * @throws Exception description
                     * @return the path
                     */
                    Object run(String path, Object fileEntry)
                            throws Exception
                    {
                        return null;
                    }
                }
                """;
        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterJavadocTagContinuationUsesFixedContinuationIndent()
    {
        String oldCode =
                """
                class Test
                {
                    /**
                     * @deprecated Use bar() instead. This method is long and
                     *             will wrap onto the next line.
                     */
                    @Deprecated
                    void foo() {}
                }
                """;
        String newCode =
                """
                class Test
                {
                    /**
                     * @deprecated Use bar() instead. This method is long and
                     *         will wrap onto the next line.
                     */
                    @Deprecated
                    void foo() {}
                }
                """;
        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterInsertsBlankLineBetweenLineCommentAndFollowingJavadoc()
    {
        String oldCode =
                """
                // copy of https://example.com/Foo.java
                /**
                 * Utility.
                 */
                class Foo {}
                """;
        String newCode =
                """
                // copy of https://example.com/Foo.java

                /**
                 * Utility.
                 */
                class Foo {}
                """;
        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterRemovesBlankLineBetweenConsecutiveJavadocTags()
    {
        String oldCode =
                """
                class Test
                {
                    /**
                     * Do the thing.
                     *
                     * @param lineBuffer input
                     * @param builder output
                     *
                     * @throws IOException when broken
                     * @throws RuntimeException when really broken
                     */
                    void run(String lineBuffer, Object builder)
                    {}
                }
                """;
        String newCode =
                """
                class Test
                {
                    /**
                     * Do the thing.
                     *
                     * @param lineBuffer input
                     * @param builder output
                     * @throws IOException when broken
                     * @throws RuntimeException when really broken
                     */
                    void run(String lineBuffer, Object builder) {}
                }
                """;
        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsDeepIndentOfJavadocPreCodeContent()
    {
        String code =
                """
                class Test
                {
                    /**
                     * @return x
                     *         For example:
                     *         <pre>{@code
                     *                 SELECT count(*) FROM nation
                     *                 }</pre>
                     *         Trailing paragraph.
                     */
                    int run()
                    {
                        return 0;
                    }
                }
                """;
        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsShallowBulletContinuationInJavadocDescription()
    {
        String code =
                """
                /**
                 * Notes:
                 * - first item
                 *   inline continuation
                 * - second item
                 */
                class Test {}
                """;
        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsDeepIndentInsideDescriptionSnippet()
    {
        // Description that contains a multi-line `@Tag(\n        value)` call
        // should NOT have its wide indent collapsed — 8+ spaces carries meaning
        // (e.g. nested tag continuation).
        String code =
                """
                class Test
                {
                    /**
                     * Example:
                     * @Demo(
                     *         value)
                     */
                    void run() {}
                }
                """;
        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsExistingJavadocListIndentation()
    {
        String code =
                """
                class Test
                {
                    /**
                     * Notes:
                     * <ul>
                     *   <li>first</li>
                     *   <li>second</li>
                     * </ul>
                     */
                    void run() {}
                }
                """;
        assertCanonicalFormatting(code);
    }
}
