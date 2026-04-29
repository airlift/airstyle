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

public class TestMarkdownJavadocComment
{
    @Test
    void testMarkdownJavadocCommentsPreserveLineWrapping()
    {
        String input =
                """
                class Test
                {
                    /// First paragraph line one
                    /// line two should not be joined.
                    ///
                    /// - first item
                    /// - second item
                    void method() {}
                }
                """;

        assertCanonicalFormatting(input);
    }

    @Test
    void testFormatterFixesMarkdownJavadocParamContinuationIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    /// Summary.
                    ///
                    /// @param value A value used by this operation.
                    /// Defaults to the current value if none is provided.
                    void run(String value) {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    /// Summary.
                    ///
                    /// @param value A value used by this operation.
                    ///         Defaults to the current value if none is provided.
                    void run(String value) {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedMarkdownJavadocParamContinuationIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    /// Summary.
                    ///
                    /// @param value A value used by this operation.
                    /// Defaults to the current value if none is provided.
                    void run(String value) {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    /// Summary.
                    ///
                    /// @param value A value used by this operation.
                    ///         Defaults to the current value if none is provided.
                    void run(String value) {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedMarkdownJavadocParamContinuationIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    /// Summary.
                    ///
                    /// @param value A value used by this operation.
                    ///                 Defaults to the current value if none is provided.
                    void run(String value) {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    /// Summary.
                    ///
                    /// @param value A value used by this operation.
                    ///         Defaults to the current value if none is provided.
                    void run(String value) {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesMarkdownJavadocThrowsContinuationIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    /// Summary.
                    ///
                    /// @throws IllegalStateException if no value is available.
                    ///                              Use withDefaultValue() to avoid this.
                    String run() {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    /// Summary.
                    ///
                    /// @throws IllegalStateException if no value is available.
                    ///         Use withDefaultValue() to avoid this.
                    String run() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsMarkdownJavadocBlockTagContinuationIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    /// Summary.
                    ///
                    /// @throws IllegalStateException if no value is available.
                    ///         Use withDefaultValue() to avoid this.
                    String run() {}
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterKeepsMarkdownJavadocListIndentation()
    {
        String code =
                """
                class Test
                {
                    /// Header:
                    ///   * first
                    ///   * second
                    void run() {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsMarkdownJavadocFencedCodeBlockIndentation()
    {
        String code =
                """
                class Test
                {
                    /// ```text
                    /// Header:
                    ///   int32  magic
                    ///   int64  checksum
                    /// ```
                    void run() {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesMarkdownJavadocBracketRangeSpacing()
    {
        String oldCode =
                """
                class Test
                {
                    /// Queries a metric summed over [from, to] grouped by the specified tags.
                    void run() {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    /// Queries a metric summed over [from,to] grouped by the specified tags.
                    void run() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesMarkdownJavadocNumericRangeSpacing()
    {
        String oldCode =
                """
                class Test
                {
                    /// [1.125, 1.25] overhead that scales between 10,000 and 1,000,000 distinct hashes.
                    void run() {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    /// [1.125,1.25] overhead that scales between 10,000 and 1,000,000 distinct hashes.
                    void run() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsMarkdownJavadocEscapedBracketSpacing()
    {
        String code =
                """
                class Test
                {
                    /// Escaped bracket \\[from, to] remains prose.
                    void run() {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsMarkdownJavadocInlineCodeBracketSpacing()
    {
        String code =
                """
                class Test
                {
                    /// Inline code `[from, to]` remains literal.
                    void run() {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsMarkdownJavadocMultiBacktickInlineCodeBracketSpacing()
    {
        String code =
                """
                class Test
                {
                    /// Inline code ``[from, to]`` remains literal.
                    void run() {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsMarkdownJavadocReferenceLinkBracketSpacing()
    {
        String code =
                """
                class Test
                {
                    /// See [from, to][range].
                    /// [range]: https://example.com
                    void run() {}
                }
                """;

        assertCanonicalFormatting(code);
    }
}
