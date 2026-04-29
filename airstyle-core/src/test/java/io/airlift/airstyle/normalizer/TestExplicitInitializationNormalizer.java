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

public class TestExplicitInitializationNormalizer
{
    @Test
    void testFormatterFixesExplicitDefaultFieldInitialization()
    {
        String oldCode =
                """
                class Test {
                    int count = 0;
                    long id = 0L;
                    boolean enabled = false;
                    char marker = '\\0';
                    Object value = null;
                    String text = null;
                    double ratio = 0.0;
                    float percent = 0.0f;
                    int nonDefault = 1;
                }
                """;

        String newCode =
                """
                class Test
                {
                    int count;
                    long id;
                    boolean enabled;
                    char marker;
                    Object value;
                    String text;
                    double ratio;
                    float percent;
                    int nonDefault = 1;
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsInterfaceFieldInitializer()
    {
        // Interface fields are implicitly public static final (JLS §9.3);
        // stripping the initializer produces uncompilable code.
        String code =
                """
                interface Foo
                {
                    int SINGLE_SOURCE_PARTITION_ID = 0;
                    boolean DEFAULT_FLAG = false;
                    String MARKER = null;
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsAnnotationTypeFieldInitializer()
    {
        String code =
                """
                @interface Foo
                {
                    int VERSION = 0;
                }
                """;

        assertCanonicalFormatting(code);
    }
}
