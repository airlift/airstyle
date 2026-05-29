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

public class TestAnnotationUseStyleFormatting
{
    @Test
    void testFormatterPreservesAnnotationArrayTrailingCommaStyle()
    {
        String code =
                """
                @interface Tags
                {
                    String[] value();
                }

                class Test
                {
                    @Tags({"a", "b",})
                    void run() {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterPreservesAnnotationArrayWithoutTrailingComma()
    {
        String code =
                """
                @interface Tags
                {
                    String[] value();
                }

                class Test
                {
                    @Tags({"a", "b"})
                    void run() {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testMultiLineAnnotationArgsKeepContinuationIndent()
    {
        String code =
                """
                class Test
                {
                    @McpTool(
                            name = "debug-tool",
                            description = "Comprehensive debug tool",
                            app = @McpApp(
                                    resourceUri = "ui://debug-tool/mcp-app.html",
                                    sourcePath = "debug-app.html"))
                    void run() {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testMultiLineAnnotationArgsKeepContinuationIndentWithArrayBlock()
    {
        String code =
                """
                class Test
                {
                    @McpTool(
                            name = "debug-tool",
                            description = "Comprehensive debug tool",
                            app = {
                                    @McpApp(resourceUri = "ui://debug-tool/mcp-app.html",
                                            sourcePath = "debug-app.html"),
                            })
                    void run() {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testMethodDeclWithWrappedAnnotationAndWrappedParamsKeepsAnnotationContinuation()
    {
        String code =
                """
                class Test
                {
                    @McpTool(
                            name = "debug-tool",
                            description = "Comprehensive debug tool",
                            app = @McpApp(
                                    resourceUri = "ui://debug-tool/mcp-app.html",
                                    sourcePath = "debug-app.html"))
                    public CallToolResult debugApp(
                            @McpDefaultValue("text") ContentType contentType,
                            boolean multipleBlocks,
                            Optional<String> largeInput,
                            boolean simulateError,
                            Optional<Integer> delayMs)
                    {
                        return null;
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testClassDeclWithWrappedAnnotationAndWrappedParamsKeepsAnnotationContinuation()
    {
        String code =
                """
                @McpTool(
                        name = "debug-tool",
                        description = "Comprehensive debug tool",
                        app = @McpApp(
                                resourceUri = "ui://debug-tool/mcp-app.html",
                                sourcePath = "debug-app.html"))
                class Test {}
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testAnnotationDeclWithWrappedAnnotationAndWrappedParamsKeepsAnnotationContinuation()
    {
        String code =
                """
                @McpTool(
                        name = "debug-tool",
                        description = "Comprehensive debug tool",
                        app = @McpApp(
                                resourceUri = "ui://debug-tool/mcp-app.html",
                                sourcePath = "debug-app.html"))
                @interface Test {}
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testRecordDeclWithWrappedAnnotationAndWrappedParamsKeepsAnnotationContinuation()
    {
        String code =
                """
                @McpTool(
                        name = "debug-tool",
                        description = "Comprehensive debug tool",
                        app = @McpApp(
                                resourceUri = "ui://debug-tool/mcp-app.html",
                                sourcePath = "debug-app.html"))
                record Test() {}
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testEnumDeclWithWrappedAnnotationAndWrappedParamsKeepsAnnotationContinuation()
    {
        String code =
                """
                @McpTool(
                        name = "debug-tool",
                        description = "Comprehensive debug tool",
                        app = @McpApp(
                                resourceUri = "ui://debug-tool/mcp-app.html",
                                sourcePath = "debug-app.html"))
                enum Test {}
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testNestedAnnotationInMemberValuePairNotDuplicated()
    {
        String code =
                """
                class Test
                {
                    @Tool(app = @App(
                            connectDomains = {
                                    "a",
                                    "b",
                            },
                            resourceDomains = {
                                    "c",
                                    "d",
                            }))
                    void run() {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testWrappedAnnotationOnMethodParameterKeepsContinuationIndent()
    {
        String oldCode =
                """
                class Test
                {
                    public static void apply(
                            @OperatorDependency(
                            operator = OperatorType.COMPARISON,
                            argumentTypes = {"K", "K"},
                            convention = @Convention(arguments = {VALUE_BLOCK_POSITION_NOT_NULL, IN_OUT}, result = FAIL_ON_NULL))
                            MethodHandle compare,
                            int x) {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    public static void apply(
                            @OperatorDependency(
                                    operator = OperatorType.COMPARISON,
                                    argumentTypes = {"K", "K"},
                                    convention = @Convention(arguments = {VALUE_BLOCK_POSITION_NOT_NULL, IN_OUT}, result = FAIL_ON_NULL))
                            MethodHandle compare,
                            int x)
                    {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testWrappedAnnotationOnUninitializedFieldKeepsContinuationIndent()
    {
        String code =
                """
                class Holder
                {
                    @ThriftField(
                            value = 1,
                            requiredness = Requiredness.OPTIONAL,
                            idlAnnotations = @ThriftIdlAnnotation(key = "k", value = "v"))
                    public Holder child;
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsTextBlockInAnnotationMarginSafe()
    {
        String code =
                """
                @Command(description = {
                        \"\"\"
                        alpha
                          beta
                        \"\"\",
                })
                class Test {}
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesSingleMemberAnnotationArrayInitializerIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    @Rows({
                    // name, value
                    "a, 1",
                    "b, 2",
                    })
                    void run() {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    @Rows({
                            // name, value
                            "a, 1",
                            "b, 2",
                    })
                    void run() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesSingleMemberAnnotationArrayInitializerAfterSiblingAnnotation()
    {
        String oldCode =
                """
                class Test
                {
                    @ParameterizedTest(name = "value={0}")
                    @CsvSource({
                    // name, value
                    "a, 1",
                    "b, 2",
                    })
                    void run(int value) {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    @ParameterizedTest(name = "value={0}")
                    @CsvSource({
                            // name, value
                            "a, 1",
                            "b, 2",
                    })
                    void run(int value) {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesRepeatableAnnotationWithNestedValues()
    {
        String oldCode =
                """
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Repeatable;
                import java.lang.annotation.Target;

                @CaseLabel(name=
                \"\"\"
                alpha
                beta
                \"\"\",mode=Mode.FAST,
                nested=@NestedCase("nested"),aliases={"a","b"})
                @CaseLabel(name="secondary",mode=Mode.SLOW,nested=@NestedCase("fallback"))class Test{@CaseLabel(name="method",mode=Mode.FAST,nested=@NestedCase("method-nested"),aliases={"method","runner"})void run(@NestedCase("parameter")String value){}enum Mode{FAST,SLOW,}
                @Repeatable(CaseLabels.class)
                @Target({ElementType.TYPE,ElementType.METHOD})
                @interface CaseLabel{String name();Mode mode();NestedCase nested();String[]aliases()default{};}
                @Target(ElementType.TYPE)
                @interface CaseLabels{CaseLabel[]value();}
                @Target({ElementType.ANNOTATION_TYPE,ElementType.PARAMETER})
                @interface NestedCase{String value();}}
                """;

        String newCode =
                """
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Repeatable;
                import java.lang.annotation.Target;

                @CaseLabel(
                        name =
                                \"\"\"
                                alpha
                                beta
                                \"\"\",
                        mode = Mode.FAST,
                        nested = @NestedCase("nested"),
                        aliases = {"a", "b"})
                @CaseLabel(name = "secondary", mode = Mode.SLOW, nested = @NestedCase("fallback"))
                class Test
                {
                    @CaseLabel(name = "method", mode = Mode.FAST, nested = @NestedCase("method-nested"), aliases = {"method", "runner"})
                    void run(@NestedCase("parameter") String value) {}

                    enum Mode
                    {
                        FAST, SLOW,
                    }

                    @Repeatable(CaseLabels.class)
                    @Target({ElementType.TYPE, ElementType.METHOD})
                    @interface CaseLabel
                    {
                        String name();

                        Mode mode();

                        NestedCase nested();

                        String[] aliases() default {};
                    }

                    @Target(ElementType.TYPE)
                    @interface CaseLabels
                    {
                        CaseLabel[] value();
                    }

                    @Target({ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
                    @interface NestedCase
                    {
                        String value();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }
}
