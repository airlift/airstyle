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

public class TestDeclarationStructureFormatting
{
    @Test
    void testPermitsClauseRemovesBlankLinesBetweenPermittedTypes()
    {
        String oldCode =
                """
                sealed interface Fruit
                        permits Apple,

                                Orange
                {
                    String name();
                }

                final class Apple implements Fruit
                {
                    public String name()
                    {
                        return "apple";
                    }
                }

                final class Orange implements Fruit
                {
                    public String name()
                    {
                        return "orange";
                    }
                }
                """;

        String newCode =
                """
                sealed interface Fruit
                        permits Apple,
                                Orange
                {
                    String name();
                }

                final class Apple
                        implements Fruit
                {
                    public String name()
                    {
                        return "apple";
                    }
                }

                final class Orange
                        implements Fruit
                {
                    public String name()
                    {
                        return "orange";
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testExtendsClauseRemovesBlankLineBeforeKeyword()
    {
        String oldCode =
                """
                interface Parent
                {
                    void parent();
                }

                interface Foo

                        extends Parent
                {
                    @Override
                    void parent();
                }
                """;

        String newCode =
                """
                interface Parent
                {
                    void parent();
                }

                interface Foo
                        extends Parent
                {
                    @Override
                    void parent();
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testImplementsClauseRemovesBlankLineBeforeKeyword()
    {
        String oldCode =
                """
                interface Parent
                {
                    void parent();
                }

                class Test

                        implements Parent
                {
                    @Override
                    public void parent() {}
                }
                """;

        String newCode =
                """
                interface Parent
                {
                    void parent();
                }

                class Test
                        implements Parent
                {
                    @Override
                    public void parent() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFieldDeclarationAndInitializerBlockStayOnSeparateLines()
    {
        String code =
                """
                class Test
                {
                    private final Object value;

                    {
                        value = new Object();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testAnonymousClassFieldDeclarationAndInitializerBlockStayOnSeparateLines()
    {
        String code =
                """
                class Test
                {
                    Runnable runnable = new Runnable()
                    {
                        private final Object value;

                        {
                            value = new Object();
                        }

                        @Override
                        public void run() {}
                    };
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesWrappedGenericWildcardInPatternMatch()
    {
        String oldCode =
                """
                class Test
                {
                    void run(Object argumentValue)
                    {
                        if (argumentValue instanceof Map<?,
                                        ?> argumentMap) {
                            use(argumentMap);
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(Object argumentValue)
                    {
                        if (argumentValue instanceof Map<?, ?> argumentMap) {
                            use(argumentMap);
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesMisindentedAnnotationArrayValueOnRecordParameter()
    {
        String oldCode =
                """
                class Test
                {
                    public record Foo(
                            @Schema(allowableValues =
                    {"ok", "error"}) String status)
                    {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    public record Foo(
                            @Schema(allowableValues =
                                    {"ok", "error"}) String status) {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testWrappedAnnotationDecomposesEvenWhenTypeHeaderHasClauses()
    {
        // Type headers with javadocs and extends/implements/permits clauses
        // still decompose wrapped annotations so annotation arguments receive
        // continuation indent.
        String code =
                """
                package io.example;

                import com.fasterxml.jackson.annotation.JsonTypeInfo;

                /**
                 * Type description.
                 */
                @JsonTypeInfo(
                        use = JsonTypeInfo.Id.NAME,
                        property = "@type")
                public sealed interface T
                        permits X {}
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testLocalEnumInsideMethodIndentsConstants()
    {
        // Local type declarations inside method bodies still decompose their
        // bodies, so enum constants receive normal member indentation.
        String code =
                """
                class Outer
                {
                    void run()
                    {
                        enum Kind
                        {
                            A,
                            B,
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsThrowsClauseAfterTripleNestedGenericParam()
    {
        String code =
                """
                class Test
                {
                    private static Object lookupSchemas(URI metadataUri, JsonCodec<Map<String, List<Object>>> catalogCodec)
                            throws IOException
                    {
                        return null;
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterSplitsModifierAnnotationOntoItsOwnLine()
    {
        String oldCode =
                """
                class Test
                {
                    @VisibleForTesting final int splitBatchSize = 1;

                    @Deprecated public String name;

                    @SuppressWarnings("unchecked") void run() {}
                }

                @interface VisibleForTesting {}
                """;
        String newCode =
                """
                class Test
                {
                    @VisibleForTesting
                    final int splitBatchSize = 1;

                    @Deprecated
                    public String name;

                    @SuppressWarnings("unchecked")
                    void run() {}
                }

                @interface VisibleForTesting {}
                """;
        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterEmptyBodyAfterWrappedThrowsOnOwnLine()
    {
        String oldCode =
                """
                class Test
                {
                    public void redirectTo(String uri)
                            throws Exception {}

                    public Test(String uri)
                            throws Exception {}

                    void multiThrow()
                            throws IOException, SQLException {}
                }
                """;
        String newCode =
                """
                class Test
                {
                    public void redirectTo(String uri)
                            throws Exception
                    {}

                    public Test(String uri)
                            throws Exception
                    {}

                    void multiThrow()
                            throws IOException, SQLException
                    {}
                }
                """;
        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesEmptyAnonymousClassMethodBodyAfterWrappedParametersOnOwnLine()
    {
        String oldCode =
                """
                class Test
                {
                    Object listener()
                    {
                        return new Listener()
                        {
                            @Override
                            public void onToolExecutionFinished(
                                    String toolCallId,
                                    String toolName,
                                    boolean success,
                                    long durationMs,
                                    String errorCode,
                                    String errorMessage) {}
                        };
                    }
                }
                """;
        String newCode =
                """
                class Test
                {
                    Object listener()
                    {
                        return new Listener()
                        {
                            @Override
                            public void onToolExecutionFinished(
                                    String toolCallId,
                                    String toolName,
                                    boolean success,
                                    long durationMs,
                                    String errorCode,
                                    String errorMessage)
                            {}
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }
}
