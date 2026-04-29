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

public class TestLambdaParameterParenthesesNormalizer
{
    @Test
    void testFormatterFixesParenthesizedSingleUntypedLambdaParameter()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        apply((x) -> List.of(x));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        apply(x -> List.of(x));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsParenthesizedTypedSingleLambdaParameter()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        apply((String x) -> List.of(x));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsParenthesizedVarSingleLambdaParameter()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        apply((var x) -> List.of(x));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsParenthesizedMultipleLambdaParameters()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        apply((x, y) -> List.of(x, y));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }
}
