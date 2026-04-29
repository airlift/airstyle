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

/// Test for Pattern 3: Block comment aligned spacing preservation.
/// The formatter should preserve original spacing after block comments.
/// For example, if original has 2 spaces after the comment, the output should also have 2 spaces.
public class TestBlockCommentSpacing
{
    @Test
    void testBlockCommentWithMultipleSpacesAfter()
    {
        String input =
                """
                class Test {
                    void method() {
                        call(
                                /* spill enabled */  true,
                                /* other param */    false);
                    }
                }
                """;

        String expected =
                """
                class Test
                {
                    void method()
                    {
                        call(
                                /* spill enabled */  true,
                                /* other param */    false);
                    }
                }
                """;

        assertFormatsOldToNew(input, expected);
    }

    @Test
    void testBlockCommentWithSingleSpaceAfter()
    {
        String input =
                """
                class Test {
                    void method() {
                        call(
                                /* spill enabled */ true,
                                /* other param */ false);
                    }
                }
                """;

        String expected =
                """
                class Test
                {
                    void method()
                    {
                        call(
                                /* spill enabled */ true,
                                /* other param */ false);
                    }
                }
                """;

        assertFormatsOldToNew(input, expected);
    }

    @Test
    void testAlignedBlockComments()
    {
        String input =
                """
                class Test {
                    void method() {
                        method(
                                '\\\\',                       /* esc */
                                INEFFECTIVE_META_CHAR,          /* anychar '.' */
                                INEFFECTIVE_META_CHAR);         /* anytime '*' */
                    }
                }
                """;

        String expected =
                """
                class Test
                {
                    void method()
                    {
                        method('\\\\',                       /* esc */
                                INEFFECTIVE_META_CHAR,          /* anychar '.' */
                                INEFFECTIVE_META_CHAR);         /* anytime '*' */
                    }
                }
                """;

        assertFormatsOldToNew(input, expected);
    }
}
