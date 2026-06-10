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

public class TestStaticImportRuleNormalizer
{
    @Test
    void testFormatterFixesBannedStaticImports()
    {
        String oldCode =
                """
                import static com.google.common.collect.ImmutableList.of;
                import static com.google.common.collect.ImmutableMap.builder;
                import static com.google.common.collect.ImmutableSet.copyOf;
                import static java.text.MessageFormat.format;
                import static java.math.BigInteger.valueOf;
                import static java.util.Optional.empty;
                import java.util.List;
                import java.util.Map;

                class Test {
                    Map<String, java.math.BigInteger> run(List<String> values)
                    {
                        String value = format("{0}", "x");
                        if (of(value).isEmpty()) {
                            return builder().put("x", valueOf(1L)).buildOrThrow();
                        }
                        return builder()
                                .put(copyOf(values).iterator().next(), valueOf(2L))
                                .put(empty().orElse("y"), valueOf(3L))
                                .buildOrThrow();
                    }
                }
                """;

        String newCode =
                """
                import com.google.common.collect.ImmutableList;
                import com.google.common.collect.ImmutableMap;
                import com.google.common.collect.ImmutableSet;

                import java.math.BigInteger;
                import java.text.MessageFormat;
                import java.util.List;
                import java.util.Map;
                import java.util.Optional;

                class Test
                {
                    Map<String, java.math.BigInteger> run(List<String> values)
                    {
                        String value = MessageFormat.format("{0}", "x");
                        if (ImmutableList.of(value).isEmpty()) {
                            return ImmutableMap.builder().put("x", BigInteger.valueOf(1L)).buildOrThrow();
                        }
                        return ImmutableMap.builder()
                                .put(ImmutableSet.copyOf(values).iterator().next(), BigInteger.valueOf(2L))
                                .put(Optional.empty().orElse("y"), BigInteger.valueOf(3L))
                                .buildOrThrow();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesBannedStaticImportsWithNonCanonicalImportSpacing()
    {
        String oldCode =
                """
                import  static   com.google.common.collect.ImmutableList.of;

                class Test {
                    Object run(String value)
                    {
                        return of(value);
                    }
                }
                """;

        String newCode =
                """
                import com.google.common.collect.ImmutableList;

                class Test
                {
                    Object run(String value)
                    {
                        return ImmutableList.of(value);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesRequiredStaticImports()
    {
        String oldCode =
                """
                import com.google.common.collect.ImmutableList;
                import com.google.common.collect.ImmutableMap;
                import com.google.common.collect.ImmutableSet;
                import java.util.List;
                import java.util.Map;
                import java.util.Objects;
                import java.util.Set;

                class Test {
                    String run(Map<String, Integer> values, List<String> names, Set<Integer> numbers)
                    {
                        String mapped = Objects.requireNonNull(values).entrySet().stream()
                                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, entry -> Math.toIntExact(entry.getValue())))
                                .toString();
                        String listed = ImmutableList.copyOf(names).stream()
                                .collect(ImmutableList.toImmutableList())
                                .toString();
                        String settled = ImmutableSet.copyOf(numbers).stream()
                                .collect(ImmutableSet.toImmutableSet())
                                .toString();
                        return mapped + listed + settled;
                    }
                }
                """;

        String newCode =
                """
                import com.google.common.collect.ImmutableList;
                import com.google.common.collect.ImmutableSet;

                import java.util.List;
                import java.util.Map;
                import java.util.Set;

                import static com.google.common.collect.ImmutableList.toImmutableList;
                import static com.google.common.collect.ImmutableMap.toImmutableMap;
                import static com.google.common.collect.ImmutableSet.toImmutableSet;
                import static java.lang.Math.toIntExact;
                import static java.util.Objects.requireNonNull;

                class Test
                {
                    String run(Map<String, Integer> values, List<String> names, Set<Integer> numbers)
                    {
                        String mapped = requireNonNull(values).entrySet().stream()
                                .collect(toImmutableMap(Map.Entry::getKey, entry -> toIntExact(entry.getValue())))
                                .toString();
                        String listed = ImmutableList.copyOf(names).stream()
                                .collect(toImmutableList())
                                .toString();
                        String settled = ImmutableSet.copyOf(numbers).stream()
                                .collect(toImmutableSet())
                                .toString();
                        return mapped + listed + settled;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsPlainImportWhenTypeStillUsedAfterStaticImportRewrite()
    {
        String oldCode =
                """
                import com.google.common.collect.ImmutableSet;

                class Test
                {
                    Object run()
                    {
                        ImmutableSet.of("5");
                        return ImmutableSet.toImmutableSet();
                    }
                }
                """;

        String newCode =
                """
                import com.google.common.collect.ImmutableSet;

                import static com.google.common.collect.ImmutableSet.toImmutableSet;

                class Test
                {
                    Object run()
                    {
                        ImmutableSet.of("5");
                        return toImmutableSet();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterRemovesPlainImportWhenTypeOnlyUsedForStaticImportRewrite()
    {
        String oldCode =
                """
                import com.google.common.collect.ImmutableSet;

                class Test
                {
                    Object run()
                    {
                        return ImmutableSet.toImmutableSet();
                    }
                }
                """;

        String newCode =
                """
                import static com.google.common.collect.ImmutableSet.toImmutableSet;

                class Test
                {
                    Object run()
                    {
                        return toImmutableSet();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesBannedPlainUtilityImports()
    {
        String oldCode =
                """
                import com.google.common.base.MoreObjects;
                import com.google.common.base.Preconditions;
                import com.google.common.base.Verify;
                import org.testng.Assert;

                class Test {
                    String run(String value, boolean flag)
                    {
                        Preconditions.checkArgument(!value.isEmpty(), "value is empty");
                        Verify.verify(flag, "flag is false");
                        Assert.assertEquals(value, "x");
                        return MoreObjects.toStringHelper(this).add("value", value).toString();
                    }
                }
                """;

        String newCode =
                """
                import static com.google.common.base.MoreObjects.toStringHelper;
                import static com.google.common.base.Preconditions.checkArgument;
                import static com.google.common.base.Verify.verify;
                import static org.testng.Assert.assertEquals;

                class Test
                {
                    String run(String value, boolean flag)
                    {
                        checkArgument(!value.isEmpty(), "value is empty");
                        verify(flag, "flag is false");
                        assertEquals(value, "x");
                        return toStringHelper(this).add("value", value).toString();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsUnsafeImportSectionWithCommentUnchanged()
    {
        String code =
                """
                import static java.text.MessageFormat.format;
                // keep this comment
                import java.util.List;

                class Test
                {
                    String run(List<String> values)
                    {
                        return format("{0}", values.getFirst());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesBannedStaticImportsWithExtraWhitespace()
    {
        String oldCode =
                """
                import  static  com.google.common.collect.ImmutableList.of;
                import  static  java.math.BigInteger.valueOf;
                import java.util.List;

                class Test {
                    List<java.math.BigInteger> run(String value)
                    {
                        return of(value).stream()
                                .map(v -> valueOf(v.length()))
                                .toList();
                    }
                }
                """;

        String newCode =
                """
                import com.google.common.collect.ImmutableList;

                import java.math.BigInteger;
                import java.util.List;

                class Test
                {
                    List<java.math.BigInteger> run(String value)
                    {
                        return ImmutableList.of(value).stream()
                                .map(v -> BigInteger.valueOf(v.length()))
                                .toList();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterAddsStaticImportsWhenNoImportBlockExists()
    {
        String oldCode =
                """
                package io.airlift.test;

                class Test {
                    String run(Object value)
                    {
                        return Objects.requireNonNull(value).toString();
                    }
                }
                """;

        String newCode =
                """
                package io.airlift.test;

                import static java.util.Objects.requireNonNull;

                class Test
                {
                    String run(Object value)
                    {
                        return requireNonNull(value).toString();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterAddsStaticImportsAfterLeadingFileCommentWhenNoImportBlockExists()
    {
        String oldCode =
                """
                // keep this header

                class Test {
                    String run(Object value)
                    {
                        return Objects.requireNonNull(value).toString();
                    }
                }
                """;

        String newCode =
                """
                // keep this header

                import static java.util.Objects.requireNonNull;

                class Test
                {
                    String run(Object value)
                    {
                        return requireNonNull(value).toString();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterAddsStaticImportsBeforeAttachedTypeJavadocWhenNoImportBlockExists()
    {
        String oldCode =
                """
                /**
                 * Test type.
                 */
                class Test {
                    String run(Object value)
                    {
                        return Objects.requireNonNull(value).toString();
                    }
                }
                """;

        String newCode =
                """
                import static java.util.Objects.requireNonNull;

                /**
                 * Test type.
                 */
                class Test
                {
                    String run(Object value)
                    {
                        return requireNonNull(value).toString();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }
}
