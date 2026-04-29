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

import static io.airlift.airstyle.FormatterAssertions.assertFormatsOldToNew;

public class TestNeedBracesNormalizer
{
    @Test
    void testFormatterFixesIfElseWithoutBraces()
    {
        String oldCode =
                """
                class Test {
                    int adjust(int value)
                    {
                        if (value > 0)
                            value++;
                        else
                            value--;
                        return value;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    int adjust(int value)
                    {
                        if (value > 0) {
                            value++;
                        }
                        else {
                            value--;
                        }
                        return value;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesElseIfChainWithoutBraces()
    {
        String oldCode =
                """
                class Test {
                    int normalize(int value)
                    {
                        if (value > 0)
                            return 1;
                        else if (value < 0)
                            return -1;
                        else
                            return 0;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    int normalize(int value)
                    {
                        if (value > 0) {
                            return 1;
                        }
                        else if (value < 0) {
                            return -1;
                        }
                        else {
                            return 0;
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesLoopBodiesWithoutBraces()
    {
        String oldCode =
                """
                import java.util.List;

                class Test {
                    int run(int value, List<String> values)
                    {
                        for (int i = 0; i < value; i++)
                            value += i;
                        while (value > 100)
                            value--;
                        do
                            value++;
                        while (value < 5);
                        for (String entry : values)
                            value += entry.length();
                        return value;
                    }
                }
                """;

        String newCode =
                """
                import java.util.List;

                class Test
                {
                    int run(int value, List<String> values)
                    {
                        for (int i = 0; i < value; i++) {
                            value += i;
                        }
                        while (value > 100) {
                            value--;
                        }
                        do {
                            value++;
                        }
                        while (value < 5);
                        for (String entry : values) {
                            value += entry.length();
                        }
                        return value;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsElseCommentWhenAddingBraces()
    {
        String oldCode =
                """
                class Test {
                    int adjust(int value)
                    {
                        if (value > 0)
                            value++;
                        /* else marker */
                        else
                            value--;
                        return value;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    int adjust(int value)
                    {
                        if (value > 0) {
                            value++;
                        }
                        /* else marker */
                        else {
                            value--;
                        }
                        return value;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }
}
