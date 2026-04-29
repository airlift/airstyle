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

public class TestTypeBraceFormatting
{
    @Test
    void testEnumBracePlacement()
    {
        String oldCode =
                """
                public enum ContentSortBy {
                    NAME,
                    DATE_MODIFIED;
                }
                """;

        String newCode =
                """
                public enum ContentSortBy
                {
                    NAME,
                    DATE_MODIFIED,
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testClassAndInterfaceBracePlacement()
    {
        String oldCode =
                """
                class Sample {
                }

                interface Contract {
                    void run();
                }
                """;

        String newCode =
                """
                class Sample {}

                interface Contract
                {
                    void run();
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesSingleLineNestedInterfaceDeclaration()
    {
        String oldCode =
                """
                class Test
                {
                    interface Action { void run(); }
                }
                """;

        String newCode =
                """
                class Test
                {
                    interface Action
                    {
                        void run();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsInlineSuppressWarningsOnNestedType()
    {
        String code =
                """
                class Test
                {
                    @SuppressWarnings({"a", "b"})
                    interface Inner
                    {
                        void run();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesWrappedSuppressWarningsValuesOnNestedType()
    {
        String oldCode =
                """
                class Test
                {
                    @SuppressWarnings({
                            "a", "b"})
                    interface Inner
                    {
                        void run();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    @SuppressWarnings({
                            "a", "b",
                    })
                    interface Inner
                    {
                        void run();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsInlineMultiArgumentAnnotationOnNestedType()
    {
        String code =
                """
                @interface Demo
                {
                    String[] value();

                    boolean enabled();
                }

                class Test
                {
                    @Demo(value = {"a", "b"}, enabled = true)
                    interface Inner
                    {
                        void run();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedArrayValuesInsideMultiArgumentAnnotationOnNestedType()
    {
        String code =
                """
                @interface Demo
                {
                    String[] value();

                    boolean enabled();
                }

                class Test
                {
                    @Demo(
                            value = {
                                    "a", "b",
                            },
                            enabled = true)
                    interface Inner
                    {
                        void run();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testRecordBracePlacement()
    {
        String oldCode =
                """
                public record Point(int x, int y) {
                    int sum()
                    {
                        return x + y;
                    }
                }
                """;

        String newCode =
                """
                public record Point(int x, int y)
                {
                    int sum()
                    {
                        return x + y;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testEmptyAnnotationTypeBracePlacement()
    {
        String oldCode =
                """
                public @interface ForSensitiveData
                {
                }
                """;

        String newCode =
                """
                public @interface ForSensitiveData {}
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testEmptyMethodBracePlacement()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testEmptyLambdaBracePlacement()
    {
        String oldCode =
                """
                class Test
                {
                    Runnable action = () -> {
                    };
                }
                """;

        String newCode =
                """
                class Test
                {
                    Runnable action = () -> {};
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesEnumConstantsOnSameLineAsOpeningBrace()
    {
        String oldCode =
                """
                class Test
                {
                    private enum State
                    { UNINITIALIZED, CONFIGURED, INITIALIZED }
                }
                """;

        String newCode =
                """
                class Test
                {
                    private enum State
                    {
                        UNINITIALIZED, CONFIGURED, INITIALIZED
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesSingleLineEnumDeclaration()
    {
        String oldCode =
                """
                class Test
                {
                    private enum State { UNINITIALIZED, CONFIGURED, INITIALIZED }
                }
                """;

        String newCode =
                """
                class Test
                {
                    private enum State
                    {
                        UNINITIALIZED, CONFIGURED, INITIALIZED
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesMixedWrappedEnumConstantsAndRemovesTrailingSemicolon()
    {
        String oldCode =
                """
                class Test
                {
                    private enum State {
                        UNINITIALIZED, CONFIGURED,
                        INITIALIZED;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    private enum State
                    {
                        UNINITIALIZED,
                        CONFIGURED,
                        INITIALIZED,
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesMixedWrappedEnumConstantsWithArgumentsAndMembers()
    {
        String oldCode =
                """
                class Test
                {
                    private enum State
                    {
                        GET("GET"),
                        LIST("GET"), CREATE("POST"),
                        DELETE("DELETE"),
                        UPDATE("PUT", "PATCH");

                        private final String value;

                        State(String value)
                        {
                            this.value = value;
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    private enum State
                    {
                        GET("GET"),
                        LIST("GET"),
                        CREATE("POST"),
                        DELETE("DELETE"),
                        UPDATE("PUT", "PATCH");

                        private final String value;

                        State(String value)
                        {
                            this.value = value;
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOwnLineInterfaceBraceIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    interface ItemInput
                       {
                        String name();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    interface ItemInput
                    {
                        String name();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOwnLineRecordBraceIndentationAfterWrappedImplements()
    {
        String oldCode =
                """
                class Test
                {
                    record Item(String value)
                            implements java.io.Serializable
                      {
                        Item
                        {
                            value = value.trim();
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    record Item(String value)
                            implements java.io.Serializable
                    {
                        Item
                        {
                            value = value.trim();
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesBlankLineBetweenWrappedEnumConstants()
    {
        String oldCode =
                """
                class Test
                {
                    enum RetryMode
                    {
                        NONE,
                        ABORT,

                        RETRY,
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    enum RetryMode
                    {
                        NONE,
                        ABORT,
                        RETRY,
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesBlankLineBetweenWrappedEnumConstantsWithTrailingComment()
    {
        String oldCode =
                """
                class Test
                {
                    enum RetryMode
                    {
                        NONE,
                        ABORT,

                        RETRY,
                        /**/
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    enum RetryMode
                    {
                        NONE,
                        ABORT,
                        RETRY,
                        /**/
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesMixedWrappedEnumConstantsWithArgumentsWithoutMembers()
    {
        String oldCode =
                """
                class Test
                {
                    private enum ErrorCode
                    {
                        FIRST(1),
                        SECOND(2), THIRD(3),
                        FOURTH(4);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    private enum ErrorCode
                    {
                        FIRST(1),
                        SECOND(2),
                        THIRD(3),
                        FOURTH(4),
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesEnumConstantClosingBraceFusion()
    {
        String oldCode =
                """
                class Test
                {
                    private enum State
                    {
                        FALSE,
                        TRUE,}
                }
                """;

        String newCode =
                """
                class Test
                {
                    private enum State
                    {
                        FALSE,
                        TRUE,
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsTwoItemMultilineEnumInline()
    {
        String code =
                """
                class Test
                {
                    private enum StoreType
                    {
                        POSTGRESQL, MEMORY,
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesAnonymousClassOpeningBraceToLineStart()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        CacheLoader<String, String> loader = new CacheLoader<String, String>()
                                {
                                    @Override
                                    public String load(String key)
                                    {
                                        return key;
                                    }
                                };
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        CacheLoader<String, String> loader = new CacheLoader<String, String>()
                        {
                            @Override
                            public String load(String key)
                            {
                                return key;
                            }
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesAnonymousClassOpeningBraceInReturnExpression()
    {
        String oldCode =
                """
                class Test
                {
                    CloseableIterator<String> run()
                    {
                        return new CloseableIterator<String>()
                                {
                                    @Override
                                    public boolean hasNext()
                                    {
                                        return false;
                                    }
                                };
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    CloseableIterator<String> run()
                    {
                        return new CloseableIterator<String>()
                        {
                            @Override
                            public boolean hasNext()
                            {
                                return false;
                            }
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testNoBlankLinesImmediatelyInsideTypeBraces()
    {
        String oldCode =
                """
                class Foo
                {

                    String abc;

                }
                """;

        String newCode =
                """
                class Foo
                {
                    String abc;
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testNoBlankLinesImmediatelyInsideIfBlockBraces()
    {
        String oldCode =
                """
                class Foo
                {
                    void run()
                    {
                        if (abc) {

                            exec();

                        }
                    }
                }
                """;

        String newCode =
                """
                class Foo
                {
                    void run()
                    {
                        if (abc) {
                            exec();
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testNoBlankLinesImmediatelyInsideLambdaAndSwitchExpressionBraces()
    {
        String oldCode =
                """
                class Foo
                {
                    Runnable create(int value)
                    {
                        return () -> {
                            if (value > 0) {

                                int result = switch (value) {

                                    case 1 -> 1;
                                    default -> 2;

                                };
                            }

                        };
                    }
                }
                """;

        String newCode =
                """
                class Foo
                {
                    Runnable create(int value)
                    {
                        return () -> {
                            if (value > 0) {
                                int result = switch (value) {
                                    case 1 -> 1;
                                    default -> 2;
                                };
                            }
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testCompressMultipleBlankLinesBetweenStatementsToSingle()
    {
        String oldCode =
                """
                class Foo
                {
                    void run(boolean first, boolean second)
                    {

                        if (first) {
                            executeFirst();
                        }

                        if (second) {
                            executeSecond();
                        }

                    }
                }
                """;

        String newCode =
                """
                class Foo
                {
                    void run(boolean first, boolean second)
                    {
                        if (first) {
                            executeFirst();
                        }

                        if (second) {
                            executeSecond();
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testAnonymousClassBodyAfterBlockLambdaCtorArgKeepsMemberIndent()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        tryInvoke(
                                policy,
                                new Invoker(() -> {
                                    count.getAndIncrement();
                                    return done();
                                })
                                {
                                    @Override
                                    public synchronized void delay(Duration duration)
                                    {
                                        super.delay(duration);
                                    }
                                },
                                selector);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testAnonymousClassBodyAfterWrappedCtorArgsKeepsMemberIndent()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        Foo builder = new Foo(
                                arg1,
                                arg2,
                                arg3)
                        {
                            @Override
                            void method(int x)
                                    throws IOException
                            {
                                doWork();
                                throw new IOException("boom");
                            }
                        };
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsAnonymousClassMembersAtParentIndentPlusNormal()
    {
        // Assignment-wrapped anon class in interface body: the engine should
        // place `new Foo()` at continuation and `{ ... }` members at the
        // brace's column + NORMAL. Earlier the AssignmentBlankLine normalizer
        // would cascade a +8 shift into the anon body, putting members at
        // column 24 instead of 16.
        String code =
                """
                public interface WarningCollector
                {
                    WarningCollector NOOP =
                            new WarningCollector()
                            {
                                @Override
                                public void add(StyleWarning warning) {}
                            };
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsSimpleClassWithMethod()
    {
        String code =
                """
                class Test
                {
                    int run()
                    {
                        return 1;
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesEnumWithConstants()
    {
        String oldCode =
                """
                enum Color
                {
                    RED,
                    GREEN,
                    BLUE
                }
                """;

        String newCode =
                """
                enum Color
                {
                    RED,
                    GREEN,
                    BLUE,
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsEmptyClassBodyInline()
    {
        String code =
                """
                class Empty {}
                """;

        assertCanonicalFormatting(code);
    }
}
