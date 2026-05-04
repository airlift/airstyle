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

import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TextBlock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.concurrent.atomic.AtomicReference;

import static io.airlift.airstyle.FormatterAssertions.assertCanonicalFormatting;
import static io.airlift.airstyle.FormatterAssertions.assertFormatsOldToNew;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@TestInstance(PER_CLASS)
public class TestTextBlockFormatting
{
    private AirstyleFormatter formatter;

    @BeforeAll
    void setup()
    {
        formatter = new AirstyleFormatter();
    }

    @Test
    void testAnnotationTextBlockStartsOnNewLine()
    {
        String oldCode =
                """
                @interface Query
                {
                    String value();
                }

                class Test
                {
                    @Query(\"""
                            SELECT true
                            \""")
                    boolean isEnabled()
                    {
                        return true;
                    }
                }
                """;

        String newCode =
                """
                @interface Query
                {
                    String value();
                }

                class Test
                {
                    @Query(
                            \"""
                            SELECT true
                            \""")
                    boolean isEnabled()
                    {
                        return true;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testSingleMemberAnnotationTextBlockStartsOnNewLine()
    {
        String oldCode =
                """
                @interface SqlQuery
                {
                    String value();
                }

                interface Test
                {
                    @SqlQuery(\"""
                            SELECT 123
                            \""")
                    int getNames();
                }
                """;

        String newCode =
                """
                @interface SqlQuery
                {
                    String value();
                }

                interface Test
                {
                    @SqlQuery(
                            \"""
                            SELECT 123
                            \""")
                    int getNames();
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testStaticImportedAnnotationMemberTextBlockStartsAtContinuationIndent()
    {
        String oldCode =
                """
                import static example.CommandLine.Option;

                class Test
                {
                    @Option(names = "--image-tag", description =
                    \"""
                            Image tag to deploy, defaults to current version (999-SNAPSHOT).
                            \""")
                    String imageTag;
                }
                """;

        String newCode =
                """
                import static example.CommandLine.Option;

                class Test
                {
                    @Option(names = "--image-tag", description =
                            \"""
                            Image tag to deploy, defaults to current version (999-SNAPSHOT).
                            \""")
                    String imageTag;
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
        assertEquals(firstTextBlockLiteralValue(oldCode), firstTextBlockLiteralValue(newCode));
    }

    @Test
    void testStaticImportedAnnotationMemberTextBlockPreservesMultilineContentIndent()
    {
        String oldCode =
                """
                import static example.CommandLine.Option;

                class Test
                {
                    @Option(names = "--pull-through-secret-arn", description =
                    \"""
                            ARN of the ASM secret to use in a ECR pull-through rule. When not set,\\
                            the Docker image will be tagged and pushed directly to the ECR repository
                            \""")
                    String pullThroughSecretArn;
                }
                """;

        String newCode =
                """
                import static example.CommandLine.Option;

                class Test
                {
                    @Option(names = "--pull-through-secret-arn", description =
                            \"""
                            ARN of the ASM secret to use in a ECR pull-through rule. When not set,\\
                            the Docker image will be tagged and pushed directly to the ECR repository
                            \""")
                    String pullThroughSecretArn;
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
        assertEquals(firstTextBlockLiteralValue(oldCode), firstTextBlockLiteralValue(newCode));
    }

    @Test
    void testLongSingleMemberAnnotationTextBlockAlignsWithOpeningDelimiter()
    {
        String oldCode =
                """
                @interface SqlQuery
                {
                    String value();
                }

                interface Test
                {
                    @SqlQuery(\"""
                              SELECT names
                              FROM abc
                              WHERE id = 123
                                AND key = 'foo'
                              \""")
                    int getNames();
                }
                """;

        String newCode =
                """
                @interface SqlQuery
                {
                    String value();
                }

                interface Test
                {
                    @SqlQuery(
                            \"""
                            SELECT names
                            FROM abc
                            WHERE id = 123
                              AND key = 'foo'
                            \""")
                    int getNames();
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testAlreadyWrappedAnnotationTextBlockRealignsContent()
    {
        String oldCode =
                """
                @interface SqlQuery
                {
                    String value();
                }

                interface Test
                {
                    @SqlQuery(
                            \"""
                                      SELECT names
                                      FROM abc
                                      WHERE id = 123
                                        AND key = 'foo'
                                      \""")
                    int getNames();
                }
                """;

        String newCode =
                """
                @interface SqlQuery
                {
                    String value();
                }

                interface Test
                {
                    @SqlQuery(
                            \"""
                            SELECT names
                            FROM abc
                            WHERE id = 123
                              AND key = 'foo'
                            \""")
                    int getNames();
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testAlreadyWrappedAnnotationTextBlockIndentsOpeningDelimiter()
    {
        String oldCode =
                """
                @interface SqlQuery
                {
                    String value();
                }

                interface Test
                {
                    @SqlQuery(
                    \"""
                    SELECT names
                    FROM abc
                    \""")
                    int getNames();
                }
                """;

        String newCode =
                """
                @interface SqlQuery
                {
                    String value();
                }

                interface Test
                {
                    @SqlQuery(
                            \"""
                            SELECT names
                            FROM abc
                            \""")
                    int getNames();
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testAnnotationTextBlockIndentationPreservesLiteralValue()
    {
        String oldCode =
                """
                @interface SqlQuery
                {
                    String value();
                }

                interface Test
                {
                    @SqlQuery(
                    \"""
                            SELECT name
                        WHERE active = true
                    \""")
                    String query();
                }
                """;

        String expectedLiteralValue =
                """
                        SELECT name
                    WHERE active = true
                """;

        String formattedCode = formatter.format(oldCode);
        assertEquals(expectedLiteralValue, firstTextBlockLiteralValue(oldCode));
        assertEquals(expectedLiteralValue, firstTextBlockLiteralValue(formattedCode));
    }

    @Test
    void testUnderindentedAnnotationTextBlockContentIsRealigned()
    {
        String oldCode =
                """
                @interface SqlQuery
                {
                    String value();
                }

                interface Test
                {
                    @SqlQuery(
                            \"""
                      SELECT values
                      FROM xyz
                      \""")
                    int getValues();
                }
                """;

        String newCode =
                """
                @interface SqlQuery
                {
                    String value();
                }

                interface Test
                {
                    @SqlQuery(
                            \"""
                            SELECT values
                            FROM xyz
                            \""")
                    int getValues();
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testMethodArgumentTextBlockStartsOnNewLine()
    {
        String oldCode =
                """
                import java.util.Optional;

                class Test
                {
                    String value()
                    {
                        return Optional.of(\"""
                                [Unit]
                                WantedBy=multi-user.target
                                \""").orElseThrow();
                    }
                }
                """;

        String newCode =
                """
                import java.util.Optional;

                class Test
                {
                    String value()
                    {
                        return Optional.of(
                                \"""
                                [Unit]
                                WantedBy=multi-user.target
                                \""").orElseThrow();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesNestedMethodArgumentTextBlockToStartOnNextLine()
    {
        String oldCode =
                """
                class Test
                {
                    void run(ToolExecutor toolExecutor)
                    {
                        Object result = toolExecutor.execute(
                                Fixtures.toolRequest("tool-call-id", ToolExecutor.VALIDATE_QUERY_TOOL_NAME, \"""
                                        {"query":"StormEvents |","databaseName":"testdb"}
                                        \"""),
                                identityContext());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(ToolExecutor toolExecutor)
                    {
                        Object result = toolExecutor.execute(
                                Fixtures.toolRequest("tool-call-id", ToolExecutor.VALIDATE_QUERY_TOOL_NAME,
                                        \"""
                                        {"query":"StormEvents |","databaseName":"testdb"}
                                        \"""),
                                identityContext());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsTextBlockClosingDelimiterAlignedToHostInWrappedArgument()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        check(
                                \"""
                                SELECT *
                                FROM t
                                WHERE x = 1
                                \""");
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsTextBlockHostIndentInLocalFormattedInitializer()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        String sql =
                                \"""
                                INSERT INTO %s
                                SELECT *
                                FROM src
                                \"""
                                        .formatted(name());
                    }

                    String name()
                    {
                        return "";
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testAssignmentTextBlockStartsOnNewLine()
    {
        String oldCode =
                """
                class Test
                {
                    String query()
                    {
                        String sql = \"""
                        SELECT 1
                        \""";
                        return sql;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    String query()
                    {
                        String sql =
                                \"""
                                SELECT 1
                                \""";
                        return sql;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testAssignmentTextBlockOpenerUsesContinuationIndent()
    {
        String oldCode =
                """
                class Test
                {
                    String policy()
                    {
                        var policy = \"""
                        allow all
                        \""";
                        return policy;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    String policy()
                    {
                        var policy =
                                \"""
                                allow all
                                \""";
                        return policy;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testExpressionAssignmentTextBlockStartsOnNewLine()
    {
        String oldCode =
                """
                class Test
                {
                    void run(boolean first)
                    {
                        String responseBody;
                        if (first) {
                            responseBody = \"""
                                           {"results": [{"id": "user-1"}]}\""";
                        }
                        else {
                            responseBody = \"""
                                           {"results": [{"id": "user-2"}]}\""";
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(boolean first)
                    {
                        String responseBody;
                        if (first) {
                            responseBody =
                                    \"""
                                    {"results": [{"id": "user-1"}]}\""";
                        }
                        else {
                            responseBody =
                                    \"""
                                    {"results": [{"id": "user-2"}]}\""";
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testNestedAssignmentTextBlockStartsOnNewLine()
    {
        String oldCode =
                """
                class Test
                {
                    String assignAndReturn()
                    {
                        String responseBody;
                        return responseBody = \"""
                                              {"results": []}\""";
                    }

                    void assignInArgument()
                    {
                        String responseBody;
                        consume(responseBody = \"""
                                               {"results": []}\""");
                    }

                    void consume(String value) {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    String assignAndReturn()
                    {
                        String responseBody;
                        return responseBody =
                                \"""
                                {"results": []}\""";
                    }

                    void assignInArgument()
                    {
                        String responseBody;
                        consume(responseBody =
                                \"""
                                {"results": []}\""");
                    }

                    void consume(String value) {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testReturnTextBlockCanStayOnSameLine()
    {
        String code =
                """
                class Test
                {
                    String query()
                    {
                        return \"""
                               SELECT 1
                               \""";
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesInlineReturnTextBlockContentToOpeningDelimiter()
    {
        String oldCode =
                """
                class Test
                {
                    String value()
                    {
                        return \"""
                                [
                                  { "column": "raw_data", "path": "$.", "transform": "DropMappedFields" }
                                ]
                                \""";
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    String value()
                    {
                        return \"""
                               [
                                 { "column": "raw_data", "path": "$.", "transform": "DropMappedFields" }
                               ]
                               \""";
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
        assertEquals(firstTextBlockLiteralValue(oldCode), firstTextBlockLiteralValue(newCode));
    }

    @Test
    void testReturnTextBlockWithIndentedContentStaysOnSameLine()
    {
        String code =
                """
                class Test
                {
                    String query()
                    {
                        return \"""
                               Resources with required tags:
                               - tag_a
                               \""";
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testReturnFormattedTextBlockStaysOnSameLine()
    {
        String code =
                """
                class Test
                {
                    String query(String tags)
                    {
                        return \"""
                               Resources with required tags:

                               %s
                               \""".formatted(tags);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testThrowTextBlockArgumentStartsOnNewLine()
    {
        String oldCode =
                """
                class Test
                {
                    void fail()
                    {
                        throw new IllegalStateException(\"""
                                boom
                                \""");
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void fail()
                    {
                        throw new IllegalStateException(
                                \"""
                                boom
                                \""");
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testYieldTextBlockCanStayOnSameLine()
    {
        String code =
                """
                class Test
                {
                    String value(int input)
                    {
                        return switch (input) {
                            default -> {
                                yield \"""
                                      value
                                      \""";
                            }
                        };
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testTernaryTextBlocksUseSeparateQuestionAndColonLines()
    {
        String oldCode =
                """
                class Test
                {
                    String value(boolean cond)
                    {
                        var formattedPolicyText = cond ? \"""
                                       xxx
                                       \""" : \"""
                                               yyy
                                               \""";
                        return formattedPolicyText;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    String value(boolean cond)
                    {
                        var formattedPolicyText = cond
                                ? \"""
                                  xxx
                                  \"""
                                : \"""
                                  yyy
                                  \""";
                        return formattedPolicyText;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testParenthesizedTextBlockStartsOnNewLine()
    {
        String oldCode =
                """
                class Test
                {
                    String value()
                    {
                        var formattedPolicyText = (\"""
                                abc
                                \""");
                        return formattedPolicyText;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    String value()
                    {
                        var formattedPolicyText = (
                                \"""
                                abc
                                \""");
                        return formattedPolicyText;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testLambdaTextBlockStartsOnNewLine()
    {
        String oldCode =
                """
                class Test
                {
                    Runnable action()
                    {
                        return () -> \"""
                                lambda
                                \""";
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Runnable action()
                    {
                        return () ->
                                \"""
                                lambda
                                \""";
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testSwitchRuleTextBlockCanStayOnSameLine()
    {
        String code =
                """
                class Test
                {
                    String value(int input)
                    {
                        return switch (input) {
                            case 1 -> \"""
                                      one
                                      \""";
                            default -> \"""
                                       other
                                       \""";
                        };
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testBinaryExpressionTextBlockStartsOnNewLine()
    {
        String oldCode =
                """
                class Test
                {
                    String value()
                    {
                        var formattedPolicyText = \"prefix \" + \"""
                                value
                                \""";
                        return formattedPolicyText;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    String value()
                    {
                        var formattedPolicyText = \"prefix \" +
                                \"""
                                value
                                \""";
                        return formattedPolicyText;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testCastExpressionTextBlockStartsOnNewLine()
    {
        String oldCode =
                """
                class Test
                {
                    String value()
                    {
                        var formattedPolicyText = (String) \"""
                                value
                                \""";
                        return formattedPolicyText;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    String value()
                    {
                        var formattedPolicyText = (String)
                                \"""
                                value
                                \""";
                        return formattedPolicyText;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesAssignmentTextBlockHostIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    String run()
                    {
                        String expected =
                                    \"""
                                    # TYPE metric_name counter
                                    # HELP metric_name metric_help
                                    metric_name 0
                                    \""";
                        return expected;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    String run()
                    {
                        String expected =
                                \"""
                                # TYPE metric_name counter
                                # HELP metric_name metric_help
                                metric_name 0
                                \""";
                        return expected;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
        assertEquals(firstTextBlockLiteralValue(oldCode), firstTextBlockLiteralValue(newCode));
    }

    @Test
    void testFormatterKeepsAssignmentTextBlockHostIndentationWithIndentedContent()
    {
        String code =
                """
                class Test
                {
                    String run()
                    {
                        String expected =
                                \"""
                                        VALUES
                                          ('a', 1),
                                          ('b', 2)
                                        \""";
                        return expected;
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsLeadingWhitespaceOnFinalTextBlockContentLine()
    {
        String code =
                """
                class Test
                {
                    String run()
                    {
                        return \"""
                               first
                                   second\""";
                    }
                }
                """;

        assertCanonicalFormatting(code);
        assertEquals(firstTextBlockLiteralValue(code), firstTextBlockLiteralValue(formatter.format(code)));
    }

    @Test
    void testFormatterFixesAssignmentTextBlockHostIndentationWithEscapedClosingLine()
    {
        String oldCode =
                """
                class Test
                {
                    String run()
                    {
                        String json =
                                    \"""
                                    {
                                      "foo" : "my value"
                                    }\\
                                    \""";
                        return json;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    String run()
                    {
                        String json =
                                \"""
                                {
                                  "foo" : "my value"
                                }\\
                                \""";
                        return json;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
        assertEquals(firstTextBlockLiteralValue(oldCode), firstTextBlockLiteralValue(newCode));
    }

    @Test
    void testFormatterFixesConcatenatedTextBlockHostIndentationInMethodArgument()
    {
        String oldCode =
                """
                class Test
                {
                    void run(String tableName)
                    {
                        assertQuery(
                                "SELECT * FROM " + tableName,
                                "" +
                                \"""
                                    VALUES
                                        ('url1', 1),
                                        ('url2', 2)
                                \""");
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(String tableName)
                    {
                        assertQuery(
                                "SELECT * FROM " + tableName,
                                "" +
                                        \"""
                                            VALUES
                                                ('url1', 1),
                                                ('url2', 2)
                                        \""");
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
        assertEquals(firstTextBlockLiteralValue(oldCode), firstTextBlockLiteralValue(newCode));
    }

    @Test
    void testAssertMessageTextBlockStartsOnNewLine()
    {
        String oldCode =
                """
                class Test
                {
                    void verify(boolean ok)
                    {
                        assert ok : \"""
                                bad
                                \""";
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void verify(boolean ok)
                    {
                        assert ok :
                                \"""
                                bad
                                \""";
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testArrayInitializerElementTextBlockStartsOnNewLine()
    {
        String oldCode =
                """
                class Test
                {
                    String[] values()
                    {
                        return new String[] { \"""
                                value
                                \""" };
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    String[] values()
                    {
                        return new String[] {
                                \"""
                                value
                                \""",
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFieldInitializerTextBlockStartsOnNewLine()
    {
        String oldCode =
                """
                class Test
                {
                    String policy = \"""
                            field
                    \""";
                }
                """;

        String newCode =
                """
                class Test
                {
                    String policy =
                            \"""
                            field
                            \""";
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesTextBlockLeftMarginForPipePrefixedContent()
    {
        String oldCode =
                """
                class Test
                {
                    String run()
                    {
                        return \"""
                                %s
                                | where value == 1
                                | sort by createdAt desc;
                                \""".formatted(table);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    String run()
                    {
                        return \"""
                               %s
                               | where value == 1
                               | sort by createdAt desc;
                               \""".formatted(table);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
        assertEquals(firstTextBlockLiteralValue(oldCode), firstTextBlockLiteralValue(newCode));
    }

    @Test
    void testFormatterFixesTextBlockLeftMarginForIndentedMultilineContent()
    {
        String oldCode =
                """
                class Test
                {
                    String run()
                    {
                        return \"""
                                let recent = %s
                                    | summarize max(createdAt);
                                recent
                                \""".formatted(table);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    String run()
                    {
                        return \"""
                               let recent = %s
                                   | summarize max(createdAt);
                               recent
                               \""".formatted(table);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
        assertEquals(firstTextBlockLiteralValue(oldCode), firstTextBlockLiteralValue(newCode));
    }

    @Test
    void testFormatterFixesReturnTextBlockHostIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    String run()
                    {
                        return
                               \"""
                               VALUES
                               (1)
                               \""";
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    String run()
                    {
                        return
                                \"""
                                VALUES
                                (1)
                                \""";
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
        assertEquals(firstTextBlockLiteralValue(oldCode), firstTextBlockLiteralValue(newCode));
    }

    @Test
    void testFormatterFixesTextBlockFormattedSelectorHostIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    void run(String tableName)
                    {
                        insert(
                                \"""
                                       INSERT INTO %s VALUES
                                           (1000,
                                            'a')\\
                                       \""".formatted(tableName),
                                1);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(String tableName)
                    {
                        insert(
                                \"""
                                INSERT INTO %s VALUES
                                    (1000,
                                     'a')\\
                                \""".formatted(tableName),
                                1);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
        assertEquals(firstTextBlockLiteralValue(oldCode), firstTextBlockLiteralValue(newCode));
    }

    @Test
    void testFormatterKeepsReturnTextBlockIndentation()
    {
        String code =
                """
                class Test
                {
                    String run()
                    {
                        return
                                \"""
                                alpha
                                beta
                                \""";
                    }
                }
                """;

        assertCanonicalFormatting(code);
        assertEquals(firstTextBlockLiteralValue(code), firstTextBlockLiteralValue(formatter.format(code)));
    }

    @Test
    void testFormatterKeepsTextBlockFormattedSelectorIndentInTernaryBranch()
    {
        String code =
                """
                class Test
                {
                    String run(boolean enabled)
                    {
                        return enabled
                                ? \"""
                                  first
                                  second
                                  \"""
                                .formatted(value())
                                : \"""
                                  third
                                  fourth
                                  \"""
                                .formatted(value());
                    }
                }
                """;

        assertCanonicalFormatting(code);
        assertEquals(firstTextBlockLiteralValue(code), firstTextBlockLiteralValue(formatter.format(code)));
    }

    @Test
    void testFormatterFixesTextBlockFormattedSelectorIndentInWrappedTernaryArgument()
    {
        String oldCode =
                """
                class Test
                {
                    void run(boolean enabled)
                    {
                        assertValue(enabled ?
                                \"""
                                first
                                second
                                \"""
                                        .formatted(value())
                                :
                                \"""
                                third
                                fourth
                                \"""
                                        .formatted(value()));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(boolean enabled)
                    {
                        assertValue(enabled ?
                                \"""
                                first
                                second
                                \"""
                                .formatted(value())
                                :
                                \"""
                                third
                                fourth
                                \"""
                                .formatted(value()));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
        assertEquals(firstTextBlockLiteralValue(oldCode), firstTextBlockLiteralValue(newCode));
    }

    @Test
    void testFormatterKeepsTextBlockClosingIndentInsideWrappedMatchesCall()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(query(
                                \"""
                                SELECT left_value, right_value
                                FROM source_table
                                \"""))
                                .matches(
                                        \"""
                                        VALUES
                                            ('left', 1)
                                            ('right', 2)
                                        \""");
                    }
                }
                """;

        assertCanonicalFormatting(code);
        assertEquals(firstTextBlockLiteralValue(code), firstTextBlockLiteralValue(formatter.format(code)));
    }

    @Test
    void testFormatterFixesTextBlockClosingIndentInWrappedInvocationArgument()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        query(
                                \"""
                                first
                                second
                                  \""",
                                name());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        query(
                                \"""
                                first
                                second
                                \""",
                                name());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
        assertEquals(firstTextBlockLiteralValue(oldCode), firstTextBlockLiteralValue(newCode));
    }

    @Test
    void testFormatterFixesClosingIndentInAssignedTextBlockWithExplicitLeadingMargin()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        String expected =
                                \"""
                                first\\
                                 second\\
                                \"""
                                ;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        String expected =
                                \"""
                                first\\
                                 second\\
                                 \"""
                                ;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
        assertEquals(firstTextBlockLiteralValue(oldCode), firstTextBlockLiteralValue(newCode));
    }

    @Test
    void testInlineTextBlockArgWithSelectorAlignsContentUnderOpeningTripleQuote()
    {
        String oldCode =
                """
                class Test
                {
                    String run(long id)
                    {
                        return OBJECT_MAPPER.readValue(\"""
                                {
                                  "id": "%s"
                                }
                                \""".formatted(id), String.class);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    String run(long id)
                    {
                        return OBJECT_MAPPER.readValue(
                                \"""
                                {
                                  "id": "%s"
                                }
                                \""".formatted(id),
                                String.class);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFieldTextBlockWithSelectorWrapsOpeningTripleQuoteToNextLine()
    {
        String oldCode =
                """
                class Test
                {
                    public static final String JSON = \"""
                            {"client_id":"id"}
                            \""".trim();
                }
                """;

        String newCode =
                """
                class Test
                {
                    public static final String JSON =
                            \"""
                            {"client_id":"id"}
                            \""".trim();
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testLocalTextBlockWithSelectorWrapsOpeningTripleQuoteToNextLine()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        String q = \"""
                                SELECT id
                                \""".formatted(x);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        String q =
                                \"""
                                SELECT id
                                \""".formatted(x);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testLocalTextBlockWithChainedSelectorsWrapsOpeningTripleQuoteToNextLine()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        String query = \"""
                                      detections
                                      | where %s
                                      | take %s
                                      \""".formatted(whereClause, limit).strip();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        String query =
                                \"""
                                detections
                                | where %s
                                | take %s
                                \""".formatted(whereClause, limit).strip();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    private static String firstTextBlockLiteralValue(String source)
    {
        ASTParser parser = ASTParser.newParser(JavaLanguageSupport.latestAstLevel());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(source.toCharArray());
        parser.setCompilerOptions(JavaLanguageSupport.compilerOptions());

        CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null);
        AtomicReference<String> literalValue = new AtomicReference<>();
        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(TextBlock node)
            {
                if (literalValue.get() == null) {
                    literalValue.set(node.getLiteralValue());
                }
                return false;
            }
        });

        return literalValue.get();
    }
}
