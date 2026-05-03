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

import static io.airlift.airstyle.FormatterAssertions.assertFormatsOldToNew;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestEmptyLambdaBodySpacingFormatting
{
    @Test
    void testFormatterFixesWhitespaceInsideEmptyLambdaBody()
    {
        String oldCode =
                """
                class Test {
                    Runnable action = () -> {   };
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
    void testFormatterFixesUnusedLambdaParameterToUnnamed()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(java.util.function.Function<String, String> function)
                    {
                        return function.apply("value").transform(value -> "constant");
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(java.util.function.Function<String, String> function)
                    {
                        return function.apply("value").transform(_ -> "constant");
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsUsedLambdaParameter()
    {
        String code =
                """
                class Test
                {
                    Object run(java.util.function.Function<String, String> function)
                    {
                        return function.apply("value").transform(value -> value.trim());
                    }
                }
                """;

        assertFormatsOldToNew(code, code);
    }

    @Test
    void testFormatterKeepsUnusedLambdaParameterWhenRewriteDisabled()
    {
        String oldCode =
                """
                class Test {
                    Object run(java.util.function.Function<String, String> function) {
                        return function.apply("value").transform(value -> "constant");
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(java.util.function.Function<String, String> function)
                    {
                        return function.apply("value").transform(value -> "constant");
                    }
                }
                """;

        AirstyleFormatter formatter = new AirstyleFormatter(false);
        assertEquals(newCode, formatter.format(oldCode));
        assertEquals(newCode, formatter.format(newCode), "Formatting is not idempotent");
    }
}
