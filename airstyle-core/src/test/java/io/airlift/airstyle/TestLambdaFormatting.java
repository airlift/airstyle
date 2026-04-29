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

public class TestLambdaFormatting
{
    @Test
    void testLambdaArrowOnNewLineGetsContinuationIndent()
    {
        // When source places `->` on its own line after the parameter list,
        // the arrow is a continuation sibling of the params list and lands at
        // the parameter-list column plus continuation indent.
        String code =
                """
                class Test
                {
                    Object run(java.util.stream.Stream<Object> stream)
                    {
                        return stream
                                .mapMulti((logEntryStream, builder)
                                        -> builder.accept(logEntryStream));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterIndentsLambdaArrowWhenOnItsOwnLine()
    {
        String oldCode =
                """
                import java.util.stream.Stream;

                class Test
                {
                    Object run(Stream<Object> stream)
                    {
                        return stream
                                .mapMulti((one, builder)
                                -> builder.accept(one));
                    }
                }
                """;
        String newCode =
                """
                import java.util.stream.Stream;

                class Test
                {
                    Object run(Stream<Object> stream)
                    {
                        return stream
                                .mapMulti((one, builder)
                                        -> builder.accept(one));
                    }
                }
                """;
        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesParenthesizedBlockLambdaArgumentIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return call((
                        (_, _, value) -> {
                            consume(value);
                            return true;
                        }));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return call((
                                (_, _, value) -> {
                                    consume(value);
                                    return true;
                                }));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesParenthesizedExpressionLambdaArgumentIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return call((
                        (_, _, value) ->
                                transform(value)));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return call((
                                (_, _, value) ->
                                        transform(value)));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }
}
