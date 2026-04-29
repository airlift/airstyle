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

public class TestCommentPreservationFormatting
{
    @Test
    void testFormatterKeepsCommentsBeforeFirstWrappedArgument()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        execute(
                                // keep-first-argument-comment
                                first,
                                second);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesCommentBeforeWrappedFieldInitializer()
    {
        String oldCode =
                """
                class Test
                {
                    private static final Object VALUE =
                    // describe initializer
                    create()
                            .build();
                }
                """;

        String newCode =
                """
                class Test
                {
                    private static final Object VALUE =
                            // describe initializer
                            create()
                                    .build();
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesCommentBeforeWrappedLambdaBody()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        matcher.matching(node ->
                        // describe predicate
                        node.isReady()
                                && node.hasValue());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        matcher.matching(node ->
                                // describe predicate
                                node.isReady()
                                        && node.hasValue());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesInlineCommentBeforeWrappedLambdaBody()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        verify(
                                plan -> // describe assertion
                                assertThat(count(plan))
                                        .isEqualTo(2));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        verify(
                                plan -> // describe assertion
                                        assertThat(count(plan))
                                                .isEqualTo(2));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesCommentBeforeWrappedTernaryBranch()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        Object value = condition ?
                                first :
                        // describe fallback
                        second;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        Object value = condition ?
                                first :
                                // describe fallback
                                second;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsCommentAfterTernaryQuestion()
    {
        String code =
                """
                class Test
                {
                    Object run(boolean enabled)
                    {
                        return enabled ?
                                // describe then
                                value() :
                                fallback();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesInlineCommentAfterTernaryQuestion()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(int marker, String value)
                    {
                        return marker <= 0 ? // marker not found
                        new Result(value) :
                                new Result(value.substring(marker));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(int marker, String value)
                    {
                        return marker <= 0 ? // marker not found
                                new Result(value) :
                                new Result(value.substring(marker));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsCommentBeforeTernaryColon()
    {
        String code =
                """
                class Test
                {
                    Object run(Handle handle)
                    {
                        return handle.isReady()
                                ? handle.value()
                                // describe fallback
                                : fallback();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsCommentBeforeTernaryQuestion()
    {
        String code =
                """
                class Test
                {
                    Object run(boolean enabled)
                    {
                        return enabled
                                // describe then
                                ? value()
                                : fallback();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsCommentsBetweenWrappedArguments()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        execute(
                                first, // keep-argument-comment
                                second,
                                third);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        execute(first, // keep-argument-comment
                                second,
                                third);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesStandaloneCommentBetweenWrappedArguments()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        execute(
                                first,
                                    // keep-argument-comment
                                    second,
                                third);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        execute(
                                first,
                                // keep-argument-comment
                                second,
                                third);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsCommentsInNestedArrayInitializers()
    {
        String code =
                """
                class Test
                {
                    double[][] values = {
                            {
                                    1.0, 2.0,
                            }, // keep-row-one
                            {
                                    3.0, 4.0,
                            }, // keep-row-two
                    };
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsCommentsInMultilineAssignments()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        String value =
                                // keep-assignment-comment
                                service.call();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsStandaloneCommentBetweenChainedSelectorsAfterWrappedLambda()
    {
        String code =
                """
                class Test
                {
                    void run(Object requests)
                    {
                        assertThat(requests).hasSize(2)
                                // first predicate
                                .anyMatch(request -> request.isEnabled()
                                        && request.hasId()
                                        && request.hasType())
                                // second predicate
                                .anyMatch(request -> request.isReady()
                                        && request.hasParent());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsStandaloneCommentBeforeFirstWrappedSelector()
    {
        String code =
                """
                class Test
                {
                    Object run(Object service)
                    {
                        return service
                                // explain first selector
                                .execute()
                                .orElseThrow();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsStandaloneCommentBeforeLambdaTextBlockBody()
    {
        String code =
                """
                class Test
                {
                    Object run(Object service)
                    {
                        return service.apply(_ ->
                                // explain body
                                \"""
                                value
                                \""");
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsStandaloneCommentBeforeNestedLambdaTextBlockBody()
    {
        String code =
                """
                class Test
                {
                    Object run(Object service, Object first, Object second, Object third)
                    {
                        return service
                                .configure(first, second, third)
                                .applyValue(_ ->
                                        // explain body
                                        \"""
                                        value
                                        \""");
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsStandaloneCommentBeforeLambdaTextBlockBodyInWrappedArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object builder, Object first, Object second, Object third)
                    {
                        return builder
                                .name("example")
                                .definition(Tuple.of(
                                                first,
                                                second,
                                                third)
                                        .applyValue(key ->
                                        // explain body
                                                \"""
                                        value %s
                                        \""".formatted(key)))
                                .build();
                    }
                }
                """;
        String newCode =
                """
                class Test
                {
                    Object run(Object builder, Object first, Object second, Object third)
                    {
                        return builder
                                .name("example")
                                .definition(Tuple.of(
                                                first,
                                                second,
                                                third)
                                        .applyValue(key ->
                                                // explain body
                                                \"""
                                        value %s
                                        \""".formatted(key)))
                                .build();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsCommentOnlyMethodBodies()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        // keep-method-comment
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsBlockEndCommentsWhenMovingRightCurly()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        call(); /* keep-block-end-comment */}
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        call(); /* keep-block-end-comment */
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsCommentsInsideWrappedSwitchRuleInvocation()
    {
        String code =
                """
                class Test
                {
                    void run(String value)
                    {
                        switch (value) {
                            case "a" -> execute(
                                    // keep-switch-argument-comment
                                    first,
                                    second);
                            default -> {
                            }
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsTrailingLineCommentAfterOpeningBrace()
    {
        String code =
                """
                class Test
                {
                    void run(Object value)
                    {
                        if (value == null) {    // don't use computeIfAbsent
                            doSomething();
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsInlineBlockCommentBetweenArguments()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        verify(metadata, LegacyClass.class, null, false /* don't care */, attributes);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsBlockCommentInsideForLoopHeader()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        for (int i = 0; /* no test */ ; ++i) {
                            break;
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsTrailingCommentAlignmentAfterOperators()
    {
        String code =
                """
                class Test
                {
                    private static final int SIZE =
                            Short.BYTES +               // magic
                                    Short.BYTES +       // flags
                                    Integer.BYTES +     // sequenceId
                                    Short.BYTES;        // header size
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsConsecutiveBlankLinesInsideTextBlock()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        log.error(
                                \"""

                                !!!! Errors found !!!!

                                See log above for details.
                                \""");
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsClosingTextBlockDelimiterOnSameLineAsContent()
    {
        // The closing """ must stay on the same line as RETURNING\s
        // because moving it to a new line changes the string content
        String code =
                """
                interface Test
                {
                    @SqlQuery(
                            \"""
                            INSERT INTO items (name, value)
                            VALUES (:name, :value)
                            RETURNING\\s\""" + SELECT_COLUMNS)
                    Object create();

                    @SqlQuery(
                            \"""
                            UPDATE items SET name = :name
                            WHERE id = :id
                            RETURNING\\s\""" + SELECT_COLUMNS)
                    Object update();
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedAnnotationExampleInJavadoc()
    {
        String code =
                """
                class Test
                {
                    /**
                     * Example:
                     * @Demo(
                     *         value)
                     */
                    void run() {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsBlankLineBetweenBracesInsideTextBlock()
    {
        String code =
                """
                class Test
                {
                    String run()
                    {
                        return \"""
                               {

                               }
                               \""";
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsHierarchyKeywordContentInsideTextBlock()
    {
        String code =
                """
                class Test
                {
                    String run()
                    {
                        return \"""

                               extends Demo
                               \""";
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFirstColumnLineCommentInEnumBodyPreserved()
    {
        String code =
                """
                enum HttpStatus
                {
                    // 1xx
                    CONTINUE(100, "Continue"),
                    SWITCHING_PROTOCOLS(101, "Switching Protocols");

                    HttpStatus(int code, String reason) {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsEnumConstantLeadingCommentIndentation()
    {
        String oldCode =
                """
                enum RowsPerMatch
                {
                    // ONE option applies to the clause.
                    // Output one row per match.
                    ONE
                    {
                    },

                    // ALL option applies to the clause.
                    // Output all rows of every match.
                    // In the case of an empty match, output the starting row.
                    // Do not produce output for excluded rows.
                    ALL
                    {
                    },
                }
                """;

        String newCode =
                """
                enum RowsPerMatch
                {
                    // ONE option applies to the clause.
                    // Output one row per match.
                    ONE {},

                    // ALL option applies to the clause.
                    // Output all rows of every match.
                    // In the case of an empty match, output the starting row.
                    // Do not produce output for excluded rows.
                    ALL {},
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testStandaloneLineCommentInMethodBodyPreserved()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        // explain what follows
                        doSomething();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testRecordBodyCommentsPreserved()
    {
        String code =
                """
                class Test
                {
                    public record R(int foo)
                    {
                        // Shadow accessor comment
                        public String getFoo()
                        {
                            return "x";
                        }

                        // Second method comment
                        @JsonProperty
                        public String other()
                        {
                            return "y";
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testAnnotationTypeBodyCommentsPreserved()
    {
        String code =
                """
                public @interface Foo
                {
                    // Usage note 1
                    String name() default "";

                    // Usage note 2
                    String value() default "";
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testStandaloneLineCommentAttachedToFollowingMemberNoBlankLineInserted()
    {
        String code =
                """
                class Test
                {
                    // copied from WeakHashMap implementation
                    private static class Wrapper
                    {
                        Wrapper() {}
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testBlankLineBetweenStatementAndTrailingComment()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        doSomething();

                        // next phase
                        doNext();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testBlockCommentBetweenLastArgAndClosingParenIsPreserved()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        foo(
                                bar(x)
                                /* TODO next step
                                Args.builder()
                                .a("y")
                                .build()
                                 */);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        foo(bar(x)
                        /* TODO next step
                        Args.builder()
                        .a("y")
                        .build()
                         */);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testLineCommentBetweenLastArgAndClosingParenIsPreserved()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        List.of(
                                first,
                                second
                                // trailing note
                        );
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsLineCommentBetweenInfixOperands()
    {
        String code =
                """
                class Test
                {
                    void run(int blockSizeInBytes)
                    {
                        int bufferSize = blockSizeInBytes
                                // to guarantee a single long can always be read entirely
                                + Long.BYTES;
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsTrailingCommentOnRecordComponentLine()
    {
        String code =
                """
                class Outer
                {
                    record PartitionUpdate(
                            int partitionId,
                            ListMultimap<Integer, Split> splits, // sourcePartition -> splits
                            boolean noMoreSplits) {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsCommentInsideOtherwiseEmptyClassBody()
    {
        String code =
                """
                public class Dummy
                {
                    // intentionally empty
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsCommentInsideOtherwiseEmptyRecordBody()
    {
        String oldCode =
                """
                public record R() implements I
                {
                    // intentionally empty
                }
                """;
        String newCode =
                """
                public record R()
                        implements I
                {
                    // intentionally empty
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsCommentInsideOtherwiseEmptyAnonymousClassBody()
    {
        String code =
                """
                class Test
                {
                    Runnable r = new Runnable()
                    {
                        // intentionally empty
                    };
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsCommentBeforeFirstArrayInitializerElement()
    {
        String code =
                """
                class Test
                {
                    byte[] run()
                    {
                        return new byte[] {
                                // leading comment
                                (byte) 0x41, 0x41,
                                0x41, 0x41,
                        };
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsCommentBeforeFirstAnnotationArrayValueElement()
    {
        String code =
                """
                @interface JsonSubTypes
                {
                    Type[] value();

                    @interface Type
                    {
                        String name();
                    }
                }

                @JsonSubTypes({
                        // leading note
                        @JsonSubTypes.Type(name = "a"),
                        @JsonSubTypes.Type(name = "b"),
                })
                class Test {}
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsCommentBeforeFirstEnumConstantArgument()
    {
        String code =
                """
                enum Behavior
                {
                    SOME_CONSTANT(
                            // leading note
                            false);

                    Behavior(boolean supported) {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsCommentBeforeFirstThrownExceptionType()
    {
        String code =
                """
                interface Test
                {
                    void run()
                            throws
                            // leading note
                            java.io.IOException,
                            RuntimeException;
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsCommentBeforeArrayDimensionExpression()
    {
        String code =
                """
                class Test
                {
                    Object[] run()
                    {
                        return new Object[
                                // size note
                                5];
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsCommentOnUnusedImport()
    {
        String code =
                """
                // explanatory note
                import java.util.Map;

                class Test {}
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsTrailingCommentAfterLastRecordComponent()
    {
        String code =
                """
                public record R(
                        int x,
                        int y
                        // trailing note
                ) {}
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsCommentBeforeFirstTryResource()
    {
        String code =
                """
                class Test
                {
                    void run()
                            throws Exception
                    {
                        try (
                                // first resource
                                AutoCloseable a = open();
                                AutoCloseable b = open()) {
                            use();
                        }
                    }

                    AutoCloseable open()
                    {
                        return null;
                    }

                    void use() {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesCommentBeforeWrappedTryResource()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                            throws Exception
                    {
                        try (AutoCloseable first = open();
                        // describe resource
                        AutoCloseable second = open()) {
                            use();
                        }
                    }

                    AutoCloseable open()
                    {
                        return null;
                    }

                    void use() {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                            throws Exception
                    {
                        try (AutoCloseable first = open();
                                // describe resource
                                AutoCloseable second = open()) {
                            use();
                        }
                    }

                    AutoCloseable open()
                    {
                        return null;
                    }

                    void use() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsTrailingCommentAfterLastTryResource()
    {
        String code =
                """
                class Test
                {
                    void run()
                            throws Exception
                    {
                        try (
                                AutoCloseable left = open();
                                AutoCloseable right = open() // keep-resource-comment
                        ) {
                            use();
                        }
                    }

                    AutoCloseable open()
                    {
                        return null;
                    }

                    void use() {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsTrailingCommentAfterLastEnumConstantArgument()
    {
        String code =
                """
                enum Behavior
                {
                    SOME_CONSTANT(
                            false,
                            true // keep-enum-argument-comment
                    );

                    Behavior(boolean supported, boolean enabled) {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsCommentBetweenSwitchCaseMultiLabels()
    {
        String code =
                """
                class Test
                {
                    int run(int x)
                    {
                        return switch (x) {
                            case 1,
                                 // between labels
                                 2,
                                 3 -> 0;
                            default -> -1;
                        };
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsCommentAfterStatementLabel()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        outer:
                        // post-label note
                        for (int i = 0; i < 1; i++) {
                            break outer;
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsCommentBetweenIfHeaderAndThenBranchBody()
    {
        String oldCode =
                """
                class Test
                {
                    void run(boolean b)
                    {
                        if (b)
                            // explain
                            doit();
                    }

                    void doit() {}
                }
                """;
        String newCode =
                """
                class Test
                {
                    void run(boolean b)
                    {
                        if (b)
                        // explain
                        {
                            doit();
                        }
                    }

                    void doit() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsCommentBetweenElseAndElseBranchBody()
    {
        String oldCode =
                """
                class Test
                {
                    int run(boolean b)
                    {
                        if (b)
                            return 1;
                        else
                            // fallback
                            return 0;
                    }
                }
                """;
        String newCode =
                """
                class Test
                {
                    int run(boolean b)
                    {
                        if (b) {
                            return 1;
                        }
                        else
                        // fallback
                        {
                            return 0;
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsMarkdownJavadocBeforeLineCommentOnSeparateLines()
    {
        String code =
                """
                class Test
                {
                    /// doc line
                    // note line
                    public void run() {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsCommentBetweenWrappedAnnotationAndDeclarationAtMethodInternalIndent()
    {
        String oldCode =
                """
                interface Test
                {
                    @SqlQuery(
                            \"""
                            UPDATE items
                            SET name = :name
                            \""")
                        // The coalesce for database_id allows preserving the existing
                        // value if null is passed, but prevents overwriting with null.
                    Optional<Item> updateItem();
                }
                """;

        String newCode =
                """
                interface Test
                {
                    @SqlQuery(
                            \"""
                            UPDATE items
                            SET name = :name
                            \""")
                        // The coalesce for database_id allows preserving the existing
                        // value if null is passed, but prevents overwriting with null.
                    Optional<Item> updateItem();
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsCommentBetweenMarkerAnnotationAndDeclarationAtDeclarationIndent()
    {
        String code =
                """
                class Test
                {
                    @Override
                    // the listener is executed concurrently
                    public String toString()
                    {
                        return "test";
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsCommentsBetweenDeprecatedAnnotationAndDeclarationAtDeclarationIndent()
    {
        String oldCode =
                """
                class Test
                {
                    @Deprecated
                // temporary boolean based setter to provide binary compatibility
                // with code that calls the boolean setter.
                // Slated for removal in a couple releases
                    public Test setEnabled(boolean enabled)
                    {
                        return this;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    @Deprecated
                    // temporary boolean based setter to provide binary compatibility
                    // with code that calls the boolean setter.
                    // Slated for removal in a couple releases
                    public Test setEnabled(boolean enabled)
                    {
                        return this;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
        assertCanonicalFormatting(newCode);
    }

    @Test
    void testFormatterKeepsCommentBetweenSingleLineAnnotationsAndDeclarationAtDeclarationIndent()
    {
        String code =
                """
                class Test
                {
                    @GET
                    @Produces(APPLICATION_JSON)
                    @Path("/v2/rest/query")
                    // TODO properties are also supported
                    public Object query()
                    {
                        return new Object();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedArgumentInlineAndLeadingComments()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        execute(
                                // keep-first
                                first, // keep-inline
                                second);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsAnnotationArrayElementComments()
    {
        String code =
                """
                @Demo(values = {
                        "alpha", // keep-alpha
                        "beta", // keep-beta
                })
                class Test {}
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsJavadocPreBlockContent()
    {
        String code =
                """
                class Test
                {
                    /**
                     * Example:
                     * <pre>{@code
                     * @alpha
                     *   body();
                     * }</pre>
                     * <p>Keep the paragraph aligned here.
                     */
                    void run() {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsAlignedTrailingCommentsInArrays()
    {
        String code =
                """
                class Test
                {
                    Object value = new Object[] {
                            call("a"),          // alpha
                            call("b"),          // beta
                    };
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsIndentedBlockCommentBodyAligned()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        /*
                          note
                        */
                        call();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }
}
