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

public class TestModifierAnnotationLineBreakNormalizer
{
    @Test
    void testFormatterSplitsAnnotationBeforeModifierOnField()
    {
        String oldCode =
                """
                class Test
                {
                    @VisibleForTesting final int value = 1;
                }

                @interface VisibleForTesting {}
                """;

        String newCode =
                """
                class Test
                {
                    @VisibleForTesting
                    final int value = 1;
                }

                @interface VisibleForTesting {}
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterSplitsAnnotationBeforeInlineMethod()
    {
        String oldCode =
                """
                class Test
                {
                    @SuppressWarnings("unchecked") void run() {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    @SuppressWarnings("unchecked")
                    void run() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterSplitsAnnotationBeforeTypeDeclaration()
    {
        String oldCode =
                """
                @Deprecated class Holder {}
                """;

        String newCode =
                """
                @Deprecated
                class Holder {}
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsTrailingLineCommentInlineWithAnnotation()
    {
        String code =
                """
                class Test
                {
                    @Override // keep the note
                    void run() {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterSplitsAnnotationFollowedByModifierEvenWhenAnnotationHasArguments()
    {
        String oldCode =
                """
                class Test
                {
                    @Path("/dummy") public String name;
                }

                @interface Path
                {
                    String value();
                }
                """;

        String newCode =
                """
                class Test
                {
                    @Path("/dummy")
                    public String name;
                }

                @interface Path
                {
                    String value();
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsAnnotationAlreadyOnItsOwnLine()
    {
        String code =
                """
                class Test
                {
                    @Deprecated
                    public String name;

                    @Override
                    void run() {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsTypeUseAnnotationInlineAfterKeywordModifiers()
    {
        String code =
                """
                class Test
                {
                    private final @Nullable String configPrefix = "x";

                    public static @Nullable Object result = "y";
                }

                @interface Nullable {}
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterLeavesParameterAnnotationInline()
    {
        String code =
                """
                class Test
                {
                    void run(@Nullable String name, @Deprecated int value) {}
                }

                @interface Nullable {}
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterSplitsAnnotationOnEnumAndRecord()
    {
        String oldCode =
                """
                @Deprecated enum Kind { A }

                @Deprecated record Pair(int x, int y) {}
                """;

        String newCode =
                """
                @Deprecated
                enum Kind
                {
                    A
                }

                @Deprecated
                record Pair(int x, int y) {}
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }
}
