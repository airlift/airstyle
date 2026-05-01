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

public class TestCommentIndentationFormatting
{
    @Test
    void testFormatterFixesSingleLineCommentIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                      // comment
                        execute();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        // comment
                        execute();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesIndentedLineCommentWithoutChangingCommentText()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                         // keep exact comment text
                        process();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        // keep exact comment text
                        process();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsEndOfLineCommentSpacing()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        process();        // aligned on purpose
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesBlockCommentIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                      /* comment */
                        execute();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        /* comment */
                        execute();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedJavadocIndentation()
    {
        String oldCode =
                """
                class Test
                {
                  /**
                   * comment
                   */
                    void run() {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    /**
                     * comment
                     */
                    void run() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedJavadocIndentation()
    {
        String oldCode =
                """
                class Test
                {
                            /**
                             * comment
                             */
                    void run() {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    /**
                     * comment
                     */
                    void run() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesFirstColumnJavadocIndentationForMethod()
    {
        String oldCode =
                """
                class Test
                {
                /**
                 * comment
                 */
                    void run() {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    /**
                     * comment
                     */
                    void run() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesJavadocLeadingAsteriskIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    /**
                    * comment
                    */
                    void run() {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    /**
                     * comment
                     */
                    void run() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesJavadocReturnContinuationIndentationWithoutLinePrefix()
    {
        String oldCode =
                """
                class Test
                {
                    /**
                     * @return a validated value
                               in the allowed range
                     */
                    String run() {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    /**
                     * @return a validated value
                     *         in the allowed range
                     */
                    String run() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesJavadocParamContinuationIndentationWithoutLinePrefix()
    {
        String oldCode =
                """
                class Test
                {
                    /**
                     * @param value A value used by this operation.
                               Defaults to the current value if none is provided.
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
    void testFormatterFixesUnderIndentedJavadocParamContinuationIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    /**
                     * @param value A value used by this operation.
                     * Defaults to the current value if none is provided.
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
    void testFormatterFixesOverIndentedJavadocParamContinuationIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    /**
                     * @param value A value used by this operation.
                     *                 Defaults to the current value if none is provided.
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
    void testFormatterFixesJavadocThrowsContinuationIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    /**
                     * @throws IllegalStateException if no value is available.
                     * Use {@link #withDefaultValue(String)} to avoid this.
                     */
                    String run() {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    /**
                     * @throws IllegalStateException if no value is available.
                     *         Use {@link #withDefaultValue(String)} to avoid this.
                     */
                    String run() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedJavadocThrowsContinuationIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    /**
                     * @throws IllegalStateException if no value is available.
                     *                              Use {@link #withDefaultValue(String)} to avoid this.
                     */
                    String run() {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    /**
                     * @throws IllegalStateException if no value is available.
                     *         Use {@link #withDefaultValue(String)} to avoid this.
                     */
                    String run() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsJavadocBlockTagContinuationIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    /**
                     * @throws IllegalStateException if no value is available.
                     *         Use {@link #withDefaultValue(String)} to avoid this.
                     */
                    String run() {}
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterKeepsJavadocParagraphContinuationInsideReturnTag()
    {
        String code =
                """
                class Test
                {
                    /**
                     * @return whether this node should produce default output in case of no input pages.
                     *         For example:
                     *         <p>
                     *         SELECT count(*) FROM nation
                     *         <p>
                     *         A default output is expected.
                     */
                    boolean hasDefaultOutput()
                    {
                        return true;
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsCommentIndentedInsideCatchBlock()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        try {
                            call();
                        }
                        catch (RuntimeException e) {
                            // ignore
                        }
                    }
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterKeepsFirstColumnLineCommentUnchanged()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        try {
                            call();
                        }
                        catch (RuntimeException e) {
                // ignore
                        }
                    }
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterKeepsFirstColumnBlockCommentUnchanged()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        try {
                            call();
                        }
                        catch (RuntimeException e) {
                /*
                if (x) {
                    y();
                }
                */
                        }
                    }
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterKeepsFirstColumnBlockCommentUnchangedInSwitchArrowBlock()
    {
        String oldCode =
                """
                class Test
                {
                    int run(State state)
                    {
                        return switch (state) {
                            case FOO -> {
                /*
                commented out
                code
                */
                                yield 1;
                            }
                            default -> 0;
                        };
                    }
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterFixesMultilineBlockCommentByShiftingEntireBlock()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                      /*
                        xxx
                      */
                        abc();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        /*
                          xxx
                        */
                        abc();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesMultilineStarBlockCommentByShiftingEntireBlock()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                      /*
                       * one
                       * two
                       */
                        abc();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        /*
                         * one
                         * two
                         */
                        abc();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesCommentedOutCodeBlockCommentByShiftingEntireBlock()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                      /*
                        if (x) {
                            y();
                        }
                      */
                        abc();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        /*
                          if (x) {
                              y();
                          }
                        */
                        abc();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsRaggedBlockCommentWhenContentStartsBeforeCommentColumn()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                      /*
                  xxx
                */
                        abc();
                    }
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterKeepsBlockCommentWhenContentStartsBeforeOpeningMarkerEvenWithAlignedClosingMarker()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                      /*
                    xxx
                      */
                        abc();
                    }
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterFixesExtraIndentedRawBlockCommentWithBlankInteriorLines()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        call();

                            /*
                                Generate a dynamic pivot to output one column per key.
                                For example, a table with two keys.

                                SELECT
                                  partition_number
                                FROM ...

                                The query is then wrapped.
                            */

                        call();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        call();

                        /*
                            Generate a dynamic pivot to output one column per key.
                            For example, a table with two keys.

                            SELECT
                              partition_number
                            FROM ...

                            The query is then wrapped.
                        */

                        call();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedRawBlockCommentWithBlankInteriorLinesInsideNestedBlock()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        if (value) {
                            call();
                        }

                    /*
                        Generate a dynamic pivot to output one column per key.
                        For example, a table with two keys.

                        SELECT
                          partition_number
                        FROM ...

                        The query is then wrapped.
                    */

                        call();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        if (value) {
                            call();
                        }

                        /*
                            Generate a dynamic pivot to output one column per key.
                            For example, a table with two keys.

                            SELECT
                              partition_number
                            FROM ...

                            The query is then wrapped.
                        */

                        call();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesBlockCommentWhenClosingMarkerStartsBeforeCommentColumn()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                      /*
                        xxx
                    */
                        abc();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        /*
                          xxx
                        */
                        abc();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsLineCommentsAlignedWithSwitchCaseLabels()
    {
        String code =
                """
                class Test
                {
                    String run(int value)
                    {
                        switch (value) {
                            case 1:
                                return "a";
                            case 2:
                                return "b";
                            // TODO handle legacy type
                            // TODO remove when migrated
                            case 3:
                                return "c";
                            default:
                                return "z";
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsMethodLevelCommentsBeforeClosingBrace()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(value)
                                .usingRecursiveComparison()
                                .isEqualTo(other);

                        // Stuff that doesn't work
                        // TODO: Keep this comment aligned with the method body.
                //      assertThat(value).isEqualTo(other);
                    }
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterKeepsStandaloneLineCommentBeforeBlockComment()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        // TODO: compile as
                        /*
                            block
                         */
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsSwitchExpressionArrowBlockCommentIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    int run(State state)
                    {
                        return switch (state) {
                            case FOO -> {
                                // ignore
                                yield 1;
                            }
                            default -> 0;
                        };
                    }
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterKeepsSwitchArrowBlockCommentIndentationAfterNestedBlock()
    {
        String oldCode =
                """
                class Test
                {
                    int run(int value)
                    {
                        return switch (value) {
                            case 1 -> {
                                {
                                    x();
                                }
                                // keep at block indent
                                yield 1;
                            }
                            default -> 0;
                        };
                    }
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterFixesSwitchExpressionArrowBlockUnderIndentedLineComment()
    {
        String oldCode =
                """
                class Test
                {
                    int run(State state)
                    {
                        return switch (state) {
                            case FOO -> {
                              // ignore
                                yield 1;
                            }
                            default -> 0;
                        };
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    int run(State state)
                    {
                        return switch (state) {
                            case FOO -> {
                                // ignore
                                yield 1;
                            }
                            default -> 0;
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesSwitchExpressionArrowBlockOverIndentedLineComment()
    {
        String oldCode =
                """
                class Test
                {
                    int run(State state)
                    {
                        return switch (state) {
                            case FOO -> {
                                    // ignore
                                yield 1;
                            }
                            default -> 0;
                        };
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    int run(State state)
                    {
                        return switch (state) {
                            case FOO -> {
                                // ignore
                                yield 1;
                            }
                            default -> 0;
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsFirstColumnLineCommentInSwitchExpressionArrowBlock()
    {
        String oldCode =
                """
                class Test
                {
                    int run(State state)
                    {
                        return switch (state) {
                            case FOO -> {
                // ignore
                                yield 1;
                            }
                            default -> 0;
                        };
                    }
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterKeepsSwitchExpressionArrowBlockCommentBeforeClosingBrace()
    {
        String oldCode =
                """
                class Test
                {
                    int run(State state)
                    {
                        return switch (state) {
                            case FOO -> {
                                assertThat(value)
                                        .isEqualTo(other);
                                // keep at block indent
                            }
                            default -> 0;
                        };
                    }
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterFixesSwitchExpressionArrowBlockCommentBeforeClosingBraceOverIndentedToContinuation()
    {
        String oldCode =
                """
                class Test
                {
                    int run(State state)
                    {
                        return switch (state) {
                            case FOO -> {
                                assertThat(value)
                                        .isEqualTo(other);
                                        // keep at block indent
                            }
                            default -> 0;
                        };
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    int run(State state)
                    {
                        return switch (state) {
                            case FOO -> {
                                assertThat(value)
                                        .isEqualTo(other);
                                // keep at block indent
                            }
                            default -> 0;
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesSwitchExpressionArrowBlockUnderIndentedBlockComment()
    {
        String oldCode =
                """
                class Test
                {
                    int run(State state)
                    {
                        return switch (state) {
                            case FOO -> {
                              /* ignore */
                                yield 1;
                            }
                            default -> 0;
                        };
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    int run(State state)
                    {
                        return switch (state) {
                            case FOO -> {
                                /* ignore */
                                yield 1;
                            }
                            default -> 0;
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesClassicSwitchCaseCommentIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    int run(State state)
                    {
                        switch (state) {
                            case FOO:
                              // ignore
                                return 1;
                            default:
                                return 0;
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    int run(State state)
                    {
                        switch (state) {
                            case FOO:
                                // ignore
                                return 1;
                            default:
                                return 0;
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsGroupedClassicSwitchCaseCommentIndentation()
    {
        String code =
                """
                class Test
                {
                    void run(int state)
                    {
                        switch (state) {
                            case 1:
                            case 2:
                                // ignore
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesGroupedClassicSwitchCaseCommentIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    void run(int state)
                    {
                        switch (state) {
                            case 1:
                            case 2:
                            // ignore
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(int state)
                    {
                        switch (state) {
                            case 1:
                            case 2:
                                // ignore
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsClassicSwitchFallThroughCommentIndentation()
    {
        String code =
                """
                class Test
                {
                    void run(int state)
                    {
                        switch (state) {
                            case 1:
                                work();
                                // fall through
                            case 2:
                                stop();
                                break;
                            default:
                                break;
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsSwitchExpressionArrowBlockCommentWhenBodyContainsOnlyComment()
    {
        String oldCode =
                """
                class Test
                {
                    void run(State state)
                    {
                        switch (state) {
                            case FOO -> {
                                // ignore
                            }
                            default -> {
                            }
                        }
                    }
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterKeepsCommentBeforeClosingBraceAfterWrappedMethodChain()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(value)
                                .usingRecursiveComparison()
                                .isEqualTo(other);
                        // keep at block indent
                    }
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterFixesCommentBeforeClosingBraceAfterWrappedMethodChain()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(value)
                                .usingRecursiveComparison()
                                .isEqualTo(other);
                                // keep at block indent
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(value)
                                .usingRecursiveComparison()
                                .isEqualTo(other);
                        // keep at block indent
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesCommentBeforeClosingBraceAfterWrappedConstructorCall()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        var x = new Result(
                                left,
                                right);
                                // keep at block indent
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        var x = new Result(
                                left,
                                right);
                        // keep at block indent
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesCommentBeforeClosingBraceAfterWrappedBinaryExpression()
    {
        String oldCode =
                """
                class Test
                {
                    String run()
                    {
                        return first()
                                + second()
                                + third();
                                // keep at block indent
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    String run()
                    {
                        return first()
                                + second()
                                + third();
                        // keep at block indent
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsRecordComponentLineCommentIndented()
    {
        String oldCode =
                """
                class Test
                {
                    record Item(
                        // a basic property
                        String value,
                            // another property
                            boolean enabled)
                    {
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    record Item(
                            // a basic property
                            String value,
                            // another property
                            boolean enabled) {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesCommentBeforeClosingBraceAfterWrappedTernaryExpression()
    {
        String oldCode =
                """
                class Test
                {
                    int run(boolean flag)
                    {
                        return flag
                                ? one()
                                : two();
                                // keep at block indent
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    int run(boolean flag)
                    {
                        return flag
                                ? one()
                                : two();
                        // keep at block indent
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesCommentBeforeClosingBraceAfterWrappedInvocationArguments()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        execute(
                                one,
                                two,
                                three);
                                // keep at block indent
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        execute(one,
                                two,
                                three);
                        // keep at block indent
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesCommentBeforeClosingBraceInLambdaBlockAfterWrappedExpression()
    {
        String oldCode =
                """
                class Test
                {
                    Runnable run()
                    {
                        return () -> {
                            var x = compute(
                                    left,
                                    right);
                                    // keep at block indent
                        };
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Runnable run()
                    {
                        return () -> {
                            var x = compute(
                                    left,
                                    right);
                            // keep at block indent
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesCommentBeforeClosingBraceInCatchBlockAfterWrappedExpression()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        try {
                            call();
                        }
                        catch (RuntimeException e) {
                            var msg = format(
                                    one,
                                    two);
                                    // ignore
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        try {
                            call();
                        }
                        catch (RuntimeException e) {
                            var msg = format(
                                    one,
                                    two);
                            // ignore
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsFirstColumnCommentInLambdaBlock()
    {
        String oldCode =
                """
                class Test
                {
                    Runnable run()
                    {
                        return () -> {
                // commented out line
                            call();
                        };
                    }
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterKeepsSimpleCatchBlockCommentIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        try {
                            call();
                        }
                        catch (RuntimeException e) {
                            // ignore
                        }
                    }
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterKeepsJavadocPreBlockContentAndFixesFollowingParagraphIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    /**
                     * Example:
                     * <pre>
                     * if (enabled) {
                     *     run();
                     * }
                     * </pre>
                     *
                     *         <p>Next paragraph.
                     */
                    void run() {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    /**
                     * Example:
                     * <pre>
                     * if (enabled) {
                     *     run();
                     * }
                     * </pre>
                     *
                     * <p>Next paragraph.
                     */
                    void run() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsJavadocPreCodeBlockAndFollowingParagraphUnchanged()
    {
        String code =
                """
                class Test
                {
                    /**
                     * Example usage:
                     * <pre>{@code
                     * @Sample
                     * class Example {
                     *     @Field
                     *     Object value;
                     * }
                     * }</pre>
                     *
                     * <p>This is equivalent to using {@code @ExtendWith(SampleExtension.class)} directly.
                     *
                     * @see SampleExtension
                     */
                    void run() {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesLineCommentIndentBetweenReturnAndNextCase()
    {
        String oldCode =
                """
                class Test
                {
                    int run(int value)
                    {
                        switch (value) {
                            case 1:
                                return first();
                                        // keep note
                            default:
                                return second();
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    int run(int value)
                    {
                        switch (value) {
                            case 1:
                                return first();
                            // keep note
                            default:
                                return second();
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesLineCommentIndentBeforeNextCaseAfterMultilineReturn()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(int value)
                    {
                        switch (value) {
                            case 1:
                                return build(
                                        first(),
                                        second());
                                        // keep note
                            case 2:
                                return other();
                            default:
                                return last();
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(int value)
                    {
                        switch (value) {
                            case 1:
                                return build(
                                        first(),
                                        second());
                            // keep note
                            case 2:
                                return other();
                            default:
                                return last();
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesLineCommentIndentBeforeNextCaseAfterSingleLineReturn()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(int value)
                    {
                        switch (value) {
                            case 1:
                                return first();
                                // keep note
                            case 2:
                                return second();
                            default:
                                return last();
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(int value)
                    {
                        switch (value) {
                            case 1:
                                return first();
                            // keep note
                            case 2:
                                return second();
                            default:
                                return last();
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesLineCommentIndentBeforeNextCaseAfterBreak()
    {
        String oldCode =
                """
                class Test
                {
                    void run(int value)
                    {
                        switch (value) {
                            case 1:
                                work();
                                break;
                                // keep note
                            case 2:
                                stop();
                                break;
                            default:
                                break;
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(int value)
                    {
                        switch (value) {
                            case 1:
                                work();
                                break;
                            // keep note
                            case 2:
                                stop();
                                break;
                            default:
                                break;
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesLineCommentIndentInsideArrayInitializer()
    {
        String oldCode =
                """
                class Test
                {
                    Object[][] run()
                    {
                        return new Object[][] {
                                {first(), second()},
                            // keep note
                        };
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object[][] run()
                    {
                        return new Object[][] {
                                {first(), second()},
                                // keep note
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsCommentIndentInsideLabeledLoopIfBlock()
    {
        String code =
                """
                class Test
                {
                    void run(int[] positionEpoch, int epoch)
                    {
                        seedSearch:
                        for (int seed = 0; seed < 256; seed++) {
                            for (int i = 0; i < 10; i++) {
                                int position = reduce(i, seed);
                                if (positionEpoch[position] == epoch) {
                                    // Collision for this seed
                                    continue seedSearch;
                                }
                                positionEpoch[position] = epoch;
                            }
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }
}
