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
package io.airlift.airstyle.normalizer;

import org.junit.jupiter.api.Test;

import static io.airlift.airstyle.FormatterAssertions.assertCanonicalFormatting;
import static io.airlift.airstyle.FormatterAssertions.assertFormatsOldToNew;

/// Test for Pattern 2: Trailing line comment spacing preservation.
/// The formatter should preserve spacing between code and trailing comments:
/// ); // comment  should stay as  ); // comment
/// NOT become:  );// comment
public class TestTrailingCommentSpacingNormalizer
{
    @Test
    void testFormatterFixesMissingSpacesAroundTrailingLineComment()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        bar();//hello
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        bar(); // hello
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesMissingSpaceAfterLineCommentMarker()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        bar(); //hello
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        bar(); // hello
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesIndentedStandaloneLineCommentWithoutSpaceAfterMarker()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        //hello
                        bar();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        // hello
                        bar();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsIndentedNoinspectionCommentWithoutInsertedSpaceAfterMarker()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        //noinspection UnusedAssignment
                        bar();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesMissingSpaceBeforeNoinspectionTrailingCommentButKeepsMarkerTight()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        bar();//noinspection UnusedAssignment
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        bar(); //noinspection UnusedAssignment
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsFirstColumnStandaloneLineCommentWithoutInsertedSpaceAfterMarker()
    {
        String code =
                """
                //hello
                class Test
                {
                    void run()
                    {
                        bar();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsIndentedEmptyLineCommentWithoutTrailingWhitespace()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        //
                        bar();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesMissingSpaceBeforeTrailingEmptyCommentWithoutAddingTrailingWhitespace()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        bar();//
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        bar(); //
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testTrailingCommentSpacingPreserved()
    {
        String input =
                """
                class Test
                {
                    void method()
                    {
                        int x = getValue(); // comment
                    }
                }
                """;

        assertCanonicalFormatting(input);
    }

    @Test
    void testTrailingCommentWithMultipleSpaces()
    {
        String input =
                """
                class Test
                {
                    void method()
                    {
                        int x = getValue();  // comment with 2 spaces before
                    }
                }
                """;

        assertCanonicalFormatting(input);
    }

    @Test
    void testTrailingCommentSpacingInLongInvocation()
    {
        String input =
                """
                class Test
                {
                    void method()
                    {
                        BigintType.BIGINT.writeLong(blockBuilder, r.nextLong() >>> 1); // Only positives
                    }
                }
                """;

        assertCanonicalFormatting(input);
    }

    @Test
    void testAlignedCommentSpacing()
    {
        String code =
                """
                class Test
                {
                    void method()
                    {
                        doWork(
                                /*  spill enabled  */  true,
                                other);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsTrailingCommentInEnumLambda()
    {
        String oldCode =
                """
                enum ColumnType
                {
                    BIGINT(LongWriter.LONG, (blockBuilder, positionCount, seed) -> {
                        Random r = new Random(seed);
                        for (int i = 0; i < positionCount; i++) {
                            LongWriter.LONG.writeLong(blockBuilder, r.nextLong() >>> 1); // Only positives
                        }
                    });
                }
                """;

        String newCode =
                """
                enum ColumnType
                {
                    BIGINT(LongWriter.LONG, (blockBuilder, positionCount, seed) -> {
                        Random r = new Random(seed);
                        for (int i = 0; i < positionCount; i++) {
                            LongWriter.LONG.writeLong(blockBuilder, r.nextLong() >>> 1); // Only positives
                        }
                    })
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testCommentInForLoop()
    {
        String oldCode =
                """
                class Test {
                    void method() {
                        for (int i = 0; i < 10; i++) {
                            doSomething(i >>> 1); // comment
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void method()
                    {
                        for (int i = 0; i < 10; i++) {
                            doSomething(i >>> 1); // comment
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testLambdaOnly()
    {
        String oldCode =
                """
                class Test {
                    Runnable r = () -> {
                        doSomething(); // comment
                    };
                }
                """;

        String newCode =
                """
                class Test
                {
                    Runnable r = () -> {
                        doSomething(); // comment
                    };
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testEnumMethod()
    {
        String oldCode =
                """
                enum Test {
                    VALUE;
                    void method() {
                        doSomething(); // comment
                    }
                }
                """;

        String newCode =
                """
                enum Test
                {
                    VALUE;

                    void method()
                    {
                        doSomething(); // comment
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testEnumConstructorWithLambda()
    {
        String oldCode =
                """
                enum Test {
                    VALUE(() -> {
                        doSomething(); // comment
                    });
                    Test(Runnable r) {}
                }
                """;

        String newCode =
                """
                enum Test
                {
                    VALUE(() -> {
                        doSomething(); // comment
                    });

                    Test(Runnable r) {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testEnumConstructorWithLambdaMinimal()
    {
        // Test: lambda inside enum constant (no space before comment in input)
        String input1 = "enum E{V(()->{x();// c\n});}";
        // Test: lambda inside class (no space before comment in input)
        String input2 = "class C{Runnable r=()->{x();// c\n};}";
        // Test: lambda inside enum constant (WITH space before comment in input)
        String input3 = "enum E{V(()->{x(); // c\n});}";
        // Test: lambda inside class (WITH space before comment in input)
        String input4 = "class C{Runnable r=()->{x(); // c\n};}";

        // Matches IntelliJ-style formatting: lambda body expanded, outer
        // class/enum body braces on their own lines. For the enum, Airstyle's
        // EnumSemicolonNormalizer removes the redundant `;` between the
        // constant and the closing brace (IntelliJ does this via inspection,
        // not the formatter itself).
        String enumExpected =
                """
                enum E
                {
                    V(() -> {
                        x(); // c
                    })
                }
                """;
        String classExpected =
                """
                class C
                {
                    Runnable r = () -> {
                        x(); // c
                    };
                }
                """;

        assertFormatsOldToNew(input1, enumExpected);
        assertFormatsOldToNew(input2, classExpected);
        assertFormatsOldToNew(input3, enumExpected);
        assertFormatsOldToNew(input4, classExpected);
    }

    @Test
    void testForLoopInEnumMethod()
    {
        // For loop inside enum method
        String oldCode =
                """
                enum Test {
                    VALUE;
                    void method() {
                        for (int i = 0; i < 10; i++) {
                            doSomething(i); // comment
                        }
                    }
                }
                """;

        String newCode =
                """
                enum Test
                {
                    VALUE;

                    void method()
                    {
                        for (int i = 0; i < 10; i++) {
                            doSomething(i); // comment
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testForLoopInLambda()
    {
        // For loop inside lambda - simpler than full enum case
        String oldCode =
                """
                class Test {
                    Runnable r = () -> {
                        for (int i = 0; i < 10; i++) {
                            doSomething(i); // comment
                        }
                    };
                }
                """;

        String newCode =
                """
                class Test
                {
                    Runnable r = () -> {
                        for (int i = 0; i < 10; i++) {
                            doSomething(i); // comment
                        }
                    };
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testSimpleTrailingComment()
    {
        // Simplest possible case - verify the basic case works
        String oldCode =
                """
                class Test {
                    void method() {
                        x = 1; // comment
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void method()
                    {
                        x = 1; // comment
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testTrailingLineCommentOnBuilderChainStaysOnSameLine()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        builder
                                .put("a", "1")
                                .put("b", "2") // lowercase
                                .put("c", "3");
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testTrailingLineCommentOnCaseLabelStaysOnSameLine()
    {
        String code =
                """
                class Test
                {
                    void run(int x)
                    {
                        switch (x) {
                            case 1: // FIRST
                                break;
                            case 2: // SECOND
                                break;
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testTrailingLineCommentOnSwitchArrowStaysOnSameLine()
    {
        String code =
                """
                class Test
                {
                    String name(int x)
                    {
                        return switch (x) {
                            case 1 -> "one"; // first case
                            case 2 -> "two"; // second case
                            default -> "other";
                        };
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testSwitchExpressionTrailingCommentOnLastArrowCaseInline()
    {
        String code =
                """
                class Test
                {
                    String version(int v)
                    {
                        return switch (v) {
                            case 0 -> "0.9"; // rfc1945
                            case 1 -> "1.1"; // rfc2616
                            case 2 -> "2"; // rfc9113
                        };
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testChainTrailingCommentWithTwoSpaceAlignmentPreserved()
    {
        String code =
                """
                class Test
                {
                    void build()
                    {
                        b
                                .appendByte(3)  // format tag
                                .appendByte(12) // p
                                .appendByte(0)  // baseline
                                .build();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFieldInitializerChainPreservesTrailingCommentOnSameLine()
    {
        String code =
                """
                class Test
                {
                    private final Map<String, String> properties = ImmutableMap.<String, String>builder()
                            .put("a", "1")
                            .put("b", "2") // lowercase
                            .put("c", "3")
                            .build();
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testTrailingCommentPreservesSourceSpacesOnEmptyArrowBody()
    {
        String code =
                """
                class Test
                {
                    void run(Object o)
                    {
                        switch (o) {
                            case String s -> handle(s);
                            case Integer i -> {}   // note
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testTrailingLineCommentOnFieldPreservesSingleSpace()
    {
        String code =
                """
                class Test
                {
                    int counter = 1; // count occurrences
                }
                """;

        assertCanonicalFormatting(code);
    }
}
