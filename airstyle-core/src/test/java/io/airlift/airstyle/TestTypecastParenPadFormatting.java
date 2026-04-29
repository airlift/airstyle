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

public class TestTypecastParenPadFormatting
{
    @Test
    void testFormatterFixesTypecastParenthesisPadding()
    {
        String oldCode =
                """
                class Test {
                    Object run(Object value)
                    {
                        return ( String ) value;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object value)
                    {
                        return (String) value;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testUnaryMinusAfterCastKeepsAdjacency()
    {
        String code =
                """
                class Test
                {
                    byte neg = (byte) -1;
                    short negShort = (short) -1;
                }
                """;

        assertCanonicalFormatting(code);
    }
}
