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

public class TestJavadocCommentFormatting
{
    @Test
    void testFormatterFixesOneLineJavadocComment()
    {
        String oldCode =
                """
                class Test
                {
                    /** Summary. */
                    void run() {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    /**
                     * Summary.
                     */
                    void run() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOneLineJavadocCommentBeforeDeclarationOnSameLine()
    {
        String oldCode =
                """
                class Test
                {
                    /** Summary. */ void run() {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    /**
                     * Summary.
                     */
                    void run() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOneLineJavadocBlockTag()
    {
        String oldCode =
                """
                class Test
                {
                    /** @return value */
                    int read()
                    {
                        return 1;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    /**
                     * @return value
                     */
                    int read()
                    {
                        return 1;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsMultilineJavadocComment()
    {
        String code =
                """
                class Test
                {
                    /**
                     * Summary.
                     */
                    void run() {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsJavadocSnippetBodyIndentation()
    {
        String code =
                """
                class Test
                {
                    /**
                     * @snippet :
                     * class Example {
                     *     void run() {}
                     * }
                     */
                    void run() {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsOneLineMarkdownJavadocComment()
    {
        String code =
                """
                class Test
                {
                    /// Summary.
                    void run() {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesJavadocBetweenAnnotationAndDeclaration()
    {
        String oldCode =
                """
                @Deprecated
                /** Class docs. */
                class Test
                {
                    @Deprecated
                    /**
                     * Method docs.
                     */
                    void run() {}
                }
                """;

        String newCode =
                """
                /**
                 * Class docs.
                 */
                @Deprecated
                class Test
                {
                    /**
                     * Method docs.
                     */
                    @Deprecated
                    void run() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }
}
