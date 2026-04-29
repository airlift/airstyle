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

public class TestRedundantModifierNormalizer
{
    @Test
    void testFormatterFixesRedundantInterfaceModifiers()
    {
        String oldCode =
                """
                interface Test {
                    public abstract void run();
                    public static final int VALUE = 1;
                }
                """;

        String newCode =
                """
                interface Test
                {
                    void run();

                    int VALUE = 1;
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }
}
