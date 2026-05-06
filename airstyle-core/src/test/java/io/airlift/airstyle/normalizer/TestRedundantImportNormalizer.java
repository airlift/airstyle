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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestRedundantImportNormalizer
{
    @Test
    void testFormatterFixesRedundantImports()
    {
        String oldCode =
                """
                package test;

                import java.lang.String;
                import java.util.List;
                import java.util.List;
                import test.Helper;

                class Helper {}

                class Test {
                    List<String> values;
                    Helper helper;
                }
                """;

        String newCode =
                """
                package test;

                import java.util.List;

                class Helper {}

                class Test
                {
                    List<String> values;
                    Helper helper;
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsJavaLangSubpackageImports()
    {
        String oldCode =
                """
                import java.lang.annotation.Retention;
                import java.lang.annotation.Target;

                import static java.lang.annotation.ElementType.FIELD;
                import static java.lang.annotation.RetentionPolicy.RUNTIME;

                @Target(FIELD)
                @Retention(RUNTIME)
                @interface Marker {}
                """;

        String newCode =
                """
                import java.lang.annotation.Retention;
                import java.lang.annotation.Target;

                import static java.lang.annotation.ElementType.FIELD;
                import static java.lang.annotation.RetentionPolicy.RUNTIME;

                @Target(FIELD)
                @Retention(RUNTIME)
                @interface Marker {}
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterKeepsCommentsAttachedToRemovedDuplicateImports()
    {
        String oldCode =
                """
                import java.util.List;
                // keep-import-comment
                import java.util.List;

                class Test {}
                """;

        String newCode =
                """
                import java.util.List;
                // keep-import-comment

                class Test {}
                """;

        assertEquals(newCode, RedundantImportNormalizer.normalize(oldCode));
        assertEquals(newCode, RedundantImportNormalizer.normalize(newCode));
    }

    @Test
    void testFormatterKeepsTrailingCommentsAttachedToRemovedDuplicateImports()
    {
        String oldCode =
                """
                import java.util.List; // first
                import java.util.Set;
                import java.util.List; // second
                import java.util.List;   // keep-inline-import-comment

                class Test {}
                """;

        String expectedSetThenList =
                """
                import java.util.Set;
                import java.util.List;   // keep-inline-import-comment

                class Test {}
                """;

        String expectedListThenSet =
                """
                import java.util.List;   // keep-inline-import-comment
                import java.util.Set;

                class Test {}
                """;

        String normalized = RedundantImportNormalizer.normalize(oldCode);

        assertTrue(
                normalized.equals(expectedSetThenList) || normalized.equals(expectedListThenSet),
                () -> "Unexpected normalized output:\n" + normalized);
        assertTrue(normalized.contains("import java.util.List;   // keep-inline-import-comment"));
        assertTrue(!normalized.contains("// first"));
        assertTrue(!normalized.contains("// second"));
        assertEquals(normalized, RedundantImportNormalizer.normalize(normalized));
    }
}
