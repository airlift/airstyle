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

public class TestOuterTypeSemicolonNormalizer
{
    @Test
    void testFormatterFixesUnnecessarySemicolonAfterOuterClass()
    {
        String oldCode =
                """
                class Test
                {
                };
                """;

        String newCode =
                """
                class Test {}
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnnecessarySemicolonAfterOuterInterface()
    {
        String oldCode =
                """
                interface Test
                {
                };
                """;

        String newCode =
                """
                interface Test {}
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }
}
