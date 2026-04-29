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

public class TestThrowsClauseLineFormatting
{
    @Test
    void testFormatterFixesThrowsClauseLinePlacement()
    {
        String oldCode =
                """
                class Test {
                    void run() throws Exception {
                        work();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                            throws Exception
                    {
                        work();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesThrowsClauseIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                        throws IOException
                    {
                        work();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                            throws IOException
                    {
                        work();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedWrappedThrowsItems()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                            throws IOException,
                                            SQLException,
                                            OtherException
                    {
                        work();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                            throws IOException,
                            SQLException,
                            OtherException
                    {
                        work();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedWrappedThrowsItems()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                            throws IOException,
                        SQLException,
                        OtherException
                    {
                        work();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                            throws IOException,
                            SQLException,
                            OtherException
                    {
                        work();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsInterfaceMethodThrowsKeywordAtContinuationIndent()
    {
        String code =
                """
                interface Foo
                {
                    T apply(Connection c)
                            throws Exception;
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsNestedInterfaceMethodThrowsKeywordAtContinuationIndent()
    {
        String code =
                """
                class Outer
                {
                    interface Nested
                    {
                        T apply(Connection c)
                                throws Exception;
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesOverIndentedWrappedThrowsItemsInInterfaceMethod()
    {
        String oldCode =
                """
                interface Foo
                {
                    List<String> runTyped()
                            throws IOException,
                                    SQLException;
                }
                """;

        String newCode =
                """
                interface Foo
                {
                    List<String> runTyped()
                            throws IOException,
                            SQLException;
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testMethodDeclWithWrappedParamsAndThrowsClauseNoBlankLine()
    {
        String code =
                """
                class Test
                {
                    public Test(
                            String name,
                            int value,
                            boolean flag)
                            throws IOException
                    {
                        this.name = name;
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testInterfaceMethodWithAnnotatedThrowsItemsKeepsContinuationIndent()
    {
        String code =
                """
                interface Scribe
                {
                    Result log(List<Entry> messages)
                            throws
                            @Id(1) DataException,
                            @Id(2) TransportException;
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testMethodWithBodyAndAnnotatedThrowsItemsKeepsContinuationIndent()
    {
        String code =
                """
                class Test
                {
                    void fail(boolean retryable)
                            throws
                            @Id(1) @Retryable(true) RetryableException,
                            @Id(2) @Retryable(false) NonRetryableException
                    {
                        throw new RetryableException();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsTrailingCommentAfterLastThrowsItem()
    {
        String code =
                """
                class Test
                {
                    void run()
                            throws java.io.IOException,
                            java.sql.SQLException // keep-throws-comment
                    {
                        work();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }
}
