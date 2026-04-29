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

public class TestMemberSeparatorFormatting
{
    @Test
    void testFormatterFixesMissingEmptyLineBetweenFieldAndConstructor()
    {
        String oldCode =
                """
                class Test {
                    private final String value;
                    Test(String value)
                    {
                        this.value = value;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    private final String value;

                    Test(String value)
                    {
                        this.value = value;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesMissingEmptyLineBetweenConstructorAndMethod()
    {
        String oldCode =
                """
                class Test {
                    Test()
                    {
                    }
                    void run()
                    {
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Test() {}

                    void run() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesMissingEmptyLineBetweenMethodAndMemberType()
    {
        String oldCode =
                """
                class Test {
                    void run()
                    {
                    }
                    static class Nested
                    {
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run() {}

                    static class Nested {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsAdjacentFieldsGroupedWithoutBlankLine()
    {
        String code =
                """
                class Test
                {
                    private final String first;
                    private final String second;
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesBlankLineBeforeFirstField()
    {
        String oldCode =
                """
                class Test
                {

                    private final String value;
                }
                """;

        String newCode =
                """
                class Test
                {
                    private final String value;
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesBlankLineAfterLastField()
    {
        String oldCode =
                """
                class Test
                {
                    private final String value;

                }
                """;

        String newCode =
                """
                class Test
                {
                    private final String value;
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverindentedFieldDeclaration()
    {
        String oldCode =
                """
                class Test
                {
                       private final String value;
                }
                """;

        String newCode =
                """
                class Test
                {
                    private final String value;
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverindentedJavadocAndMethodDeclaration()
    {
        String oldCode =
                """
                class Test
                {
                       /**
                        * @param value A value used by this operation.
                        *         Defaults to the current value if none is provided.
                        */
                       void run(String value) {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    /**
                     * @param value A value used by this operation.
                     *         Defaults to the current value if none is provided.
                     */
                    void run(String value) {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterSplitsInlineNestedClassOntoItsOwnLine()
    {
        String oldCode =
                """
                class Outer
                {
                    static class Inner { int x; }
                }
                """;

        String newCode =
                """
                class Outer
                {
                    static class Inner
                    {
                        int x;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsRecordWithCompactConstructorAsLastMember()
    {
        String code =
                """
                class Outer
                {
                    void run() {}

                    public record Inner(String a, String b)
                    {
                        public Inner
                        {
                            check(a);
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsRecordWithCompactConstructorFollowedByMoreMembers()
    {
        String code =
                """
                class Outer
                {
                    public record Inner(String a, String b)
                    {
                        public Inner
                        {
                            check(a);
                        }
                    }

                    void run() {}
                }
                """;

        assertCanonicalFormatting(code);
    }
}
