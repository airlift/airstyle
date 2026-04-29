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

public class TestArrayAndEnumFormatting
{
    @Test
    void testFormatterKeepsMultilineArrayCreationDimensionIndent()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        Object[] buffers = new Object[
                                (a.isPresent() ? 1 : 0) // decompression buffer
                                        + (b.isPresent() ? 1 : 0) // decryption buffer
                                        + 1 // input buffer
                                ];
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsInlineArrayDimensionWithWrappedInfixOperand()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        Object[] buf = new Object[thing.len + other.len
                                - first];
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsEnumConstantsSemicolonIndentedAtMemberColumn()
    {
        String code =
                """
                class Outer
                {
                    enum Kind
                    {
                        A {
                            @Override
                            String run()
                            {
                                return "a";
                            }
                        },
                        B {
                            @Override
                            String run()
                            {
                                return "b";
                            }
                        }
                        /**/;

                        abstract String run();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }
}
