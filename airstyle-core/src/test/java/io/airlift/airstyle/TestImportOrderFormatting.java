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

public class TestImportOrderFormatting
{
    @Test
    void testFormatterFixesImportOrder()
    {
        String oldCode =
                """
                import java.util.Map;
                import com.google.common.collect.ImmutableList;
                import static java.util.Objects.requireNonNull;
                import javax.annotation.Nullable;
                import java.util.List;

                class Test {
                    @Nullable
                    List<String> values;
                    Map<String, String> mapping;
                    ImmutableList<String> items = ImmutableList.of();

                    void run(Object value)
                    {
                        requireNonNull(value);
                    }
                }
                """;

        String newCode =
                """
                import com.google.common.collect.ImmutableList;

                import javax.annotation.Nullable;

                import java.util.List;
                import java.util.Map;

                import static java.util.Objects.requireNonNull;

                class Test
                {
                    @Nullable
                    List<String> values;
                    Map<String, String> mapping;
                    ImmutableList<String> items = ImmutableList.of();

                    void run(Object value)
                    {
                        requireNonNull(value);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesLineWrappedImports()
    {
        String oldCode =
                """
                package io.airlift.
                        test;

                import java.util.
                        List;
                import static java.util.Objects.
                        requireNonNull;

                class Test
                {
                    List<String> values;

                    void run(Object value)
                    {
                        requireNonNull(value);
                    }
                }
                """;

        String newCode =
                """
                package io.airlift.test;

                import java.util.List;

                import static java.util.Objects.requireNonNull;

                class Test
                {
                    List<String> values;

                    void run(Object value)
                    {
                        requireNonNull(value);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesModuleImportWithStaticImport()
    {
        String oldCode =
                """
                import module java.base;
                import static java.util.Map.entry;

                class Test
                {
                    Map.Entry<String, Integer> makeEntry()
                    {
                        return entry("one", 1);
                    }

                    List<String> names()
                    {
                        return List.of("alpha", "beta", "gamma");
                    }
                }
                """;

        String newCode =
                """
                import module java.base;

                import static java.util.Map.entry;

                class Test
                {
                    Map.Entry<String, Integer> makeEntry()
                    {
                        return entry("one", 1);
                    }

                    List<String> names()
                    {
                        return List.of("alpha", "beta", "gamma");
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterRestoresScrambledImports()
    {
        String oldCode =
                """
                import java.util.Map;
                import io.airlift.http.client.HttpClient;
                import java.security.PublicKey;
                import java.util.concurrent.atomic.AtomicReference;
                import io.airlift.units.Duration;
                import io.airlift.http.client.Response;
                import java.net.URI;
                import org.junit.jupiter.api.Test;
                import static org.assertj.core.api.Assertions.assertThat;

                class Thing
                {
                    HttpClient client;
                    Response response;
                    Duration timeout;
                    @Test
                    void run(URI endpoint, PublicKey key)
                    {
                        AtomicReference<Map<String, String>> ref = new AtomicReference<>();
                        assertThat(ref).isNotNull();
                    }
                }
                """;

        String newCode =
                """
                import io.airlift.http.client.HttpClient;
                import io.airlift.http.client.Response;
                import io.airlift.units.Duration;
                import org.junit.jupiter.api.Test;

                import java.net.URI;
                import java.security.PublicKey;
                import java.util.Map;
                import java.util.concurrent.atomic.AtomicReference;

                import static org.assertj.core.api.Assertions.assertThat;

                class Thing
                {
                    HttpClient client;
                    Response response;
                    Duration timeout;

                    @Test
                    void run(URI endpoint, PublicKey key)
                    {
                        AtomicReference<Map<String, String>> ref = new AtomicReference<>();
                        assertThat(ref).isNotNull();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterSortsMultipleStaticImportsAtBottom()
    {
        String oldCode =
                """
                import static java.util.Objects.requireNonNull;
                import java.util.List;
                import static com.google.common.base.Preconditions.checkState;
                import static com.google.common.base.Preconditions.checkArgument;

                class Thing
                {
                    List<String> values;
                    void run(Object value, boolean ready)
                    {
                        requireNonNull(value);
                        checkArgument(ready);
                        checkState(ready);
                    }
                }
                """;

        String newCode =
                """
                import java.util.List;

                import static com.google.common.base.Preconditions.checkArgument;
                import static com.google.common.base.Preconditions.checkState;
                import static java.util.Objects.requireNonNull;

                class Thing
                {
                    List<String> values;

                    void run(Object value, boolean ready)
                    {
                        requireNonNull(value);
                        checkArgument(ready);
                        checkState(ready);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterGroupsIoAirliftWithOthers()
    {
        String oldCode =
                """
                import io.airlift.units.Duration;
                import java.util.List;
                import com.google.common.collect.ImmutableList;

                class Thing
                {
                    Duration timeout;
                    List<String> values = ImmutableList.of();
                }
                """;

        String newCode =
                """
                import com.google.common.collect.ImmutableList;
                import io.airlift.units.Duration;

                import java.util.List;

                class Thing
                {
                    Duration timeout;
                    List<String> values = ImmutableList.of();
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsImportHeaderAndJavadocLayout()
    {
        String code =
                """
                package io.airlift.test;

                import java.util.List;
                import java.util.Map;

                import static java.util.Map.entry;

                /**
                 * Demo.
                 *
                 * @throws IllegalStateException if the values are invalid
                 */
                class Test
                {
                    Map<String, Integer> run(List<String> values)
                    {
                        if (values.isEmpty()) {
                            throw new IllegalStateException("values is empty");
                        }
                        return Map.ofEntries(entry("a", 1), entry("b", values.size()));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }
}
