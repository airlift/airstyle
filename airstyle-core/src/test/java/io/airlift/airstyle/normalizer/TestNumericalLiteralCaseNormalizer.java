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

public class TestNumericalLiteralCaseNormalizer
{
    @Test
    void testFormatterFixesUppercaseNumericalPrefixes()
    {
        String oldCode =
                """
                class Test
                {
                    int binary = 0B1010;
                    double hex = 0X1A.FP3;
                }
                """;

        String newCode =
                """
                class Test
                {
                    int binary = 0b1010;
                    double hex = 0x1A.Fp3;
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUppercaseNumericalInfixesAndSuffixes()
    {
        String oldCode =
                """
                class Test
                {
                    double decimal = 1E3D;
                    float hex = 0x1A.FP3F;
                }
                """;

        String newCode =
                """
                class Test
                {
                    double decimal = 1e3d;
                    float hex = 0x1A.Fp3f;
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }
}
