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

public class TestGenericWhitespaceFormatting
{
    @Test
    void testFormatterFixesGenericWhitespace()
    {
        String oldCode =
                """
                import java.util.HashMap;
                import java.util.List;
                import java.util.Map;

                class Test {
                    Map < String , List < Integer > > values = new HashMap < > ();
                }
                """;

        String newCode =
                """
                import java.util.HashMap;
                import java.util.List;
                import java.util.Map;

                class Test
                {
                    Map<String, List<Integer>> values = new HashMap<>();
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testMethodLevelTypeParamsNoSpaceAfterOpenBracket()
    {
        String code =
                """
                class Test
                {
                    public static <T, R> void foo(T a, R b) {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testGenericReturnTypeKeepsSpaceBeforeVoidKeyword()
    {
        String code =
                """
                class Test
                {
                    public <T> void bindConfig(Class<T> configClass) {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testGenericReturnTypeKeepsSpaceBeforeBooleanKeyword()
    {
        String code =
                """
                class Test
                {
                    public <T> boolean setValue(Key<T> key, T value)
                    {
                        return true;
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testClassTypeParamsKeepInlineSpacing()
    {
        String code =
                """
                public final class ThreadLocalCache<K, V>
                {
                    private final ThreadLocal<Map<K, V>> cache = null;
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testJavadocWithPreTagDoesNotAffectMethodGenericSpacing()
    {
        String code =
                """
                class Test
                {
                    /**
                     * <pre>
                     *     Foo.bar(x);
                     * </pre>
                     */
                    public static <T> Void toVoid(T value)
                    {
                        return null;
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testJavadocWithHtmlTagsDoesNotAffectMethodGenericSpacing()
    {
        String code =
                """
                class Test
                {
                    /**
                     * Uses {@link Map} and {@code <T>}.
                     */
                    public <K, V> void putAll(Map<K, V> target) {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testNestedGenericCloseKeepsAdjacentBrackets()
    {
        String code =
                """
                class Test
                {
                    private final Map<Class<? extends ApiId<?, ?>>, Consumer<LinkedBindingBuilder<ApiIdLookup<? extends ApiId<?, ?>>>>> bindings = someValue();
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testTripleGenericCloseKeepsAdjacentBrackets()
    {
        String code =
                """
                class Test
                {
                    Map<K, List<Set<V>>> tripleNested = someValue();
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testVarargsAfterGenericKeepsAdjacentEllipsis()
    {
        String code =
                """
                class Test
                {
                    @SafeVarargs
                    public static <T> void assertDeprecatedEquivalence(Class<T> configClass, Map<String, String>... oldPropertiesList) {}
                }
                """;

        assertCanonicalFormatting(code);
    }
}
