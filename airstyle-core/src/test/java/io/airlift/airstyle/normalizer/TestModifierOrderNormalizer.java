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

public class TestModifierOrderNormalizer
{
    @Test
    void testFormatterFixesModifierOrder()
    {
        String oldCode =
                """
                class Test {
                    static public final int VALUE = 1;

                    synchronized public void run()
                    {
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    public static final int VALUE = 1;

                    public synchronized void run() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }
}
