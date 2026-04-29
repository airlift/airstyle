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

public class TestNoWhitespaceAfterFormatting
{
    @Test
    void testFormatterFixesNoWhitespaceAfterTokens()
    {
        String oldCode =
                """
                class Test {
                    void run(String value)
                    {
                        String result = value. trim();
                        int count = 1;
                        ++ count;
                        -- count;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(String value)
                    {
                        String result = value.trim();
                        int count = 1;
                        ++count;
                        --count;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testUnaryMinusAfterOpenParenHasNoSpace()
    {
        String oldCode =
                """
                class Test
                {
                    long run(long offset)
                    {
                        return adjust(- 1 + (int) offset);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    long run(long offset)
                    {
                        return adjust(-1 + (int) offset);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testUnaryMinusInArrayInitializerHasNoSpace()
    {
        String oldCode =
                """
                class Test
                {
                    Object[] run()
                    {
                        return new Object[] { -1L};
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object[] run()
                    {
                        return new Object[] {-1L};
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }
}
