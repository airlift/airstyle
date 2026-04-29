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

public class TestAssignmentFormatting
{
    @Test
    void testFormatterFixesBlankLineAfterVariableInitializerAssignment()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        String value =

                                "x";
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        String value =
                                "x";
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesBlankLineAfterAssignmentExpression()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        value =

                                service.call();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        value =
                                service.call();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesMisindentedMultilineAssignmentExpression()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        String value =
                             service.call();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        String value =
                                service.call();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesTextBlockInitializerWithCommentLikeContent()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        String value =
                             \"""
                             // literal text
                             value
                             \""";
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        String value =
                                \"""
                                // literal text
                                value
                                \""";
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsAnnotatedMultilineStringInitializerIndentation()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        @SuppressWarnings("unused") String expected =
                                "VALUES " +
                                        "('AFRICA',      'MOZAMBIQUE'), " +
                                        "('AMERICA',     'UNITED STATES'), " +
                                        "('ASIA',        'VIETNAM'), " +
                                        "('EUROPE',      'UNITED KINGDOM'), " +
                                        "('MIDDLE EAST', 'SAUDI ARABIA')";
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsAnnotatedMultilineStringInitializerIndentationAfterInlineBlockComment()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        @SuppressWarnings("unused") /* keep */ String expected =
                                "VALUES " +
                                        "('AFRICA',      'MOZAMBIQUE'), " +
                                        "('AMERICA',     'UNITED STATES'), " +
                                        "('ASIA',        'VIETNAM'), " +
                                        "('EUROPE',      'UNITED KINGDOM'), " +
                                        "('MIDDLE EAST', 'SAUDI ARABIA')";
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }
}
