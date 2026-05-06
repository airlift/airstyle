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

public class TestUnusedImportNormalizer
{
    @Test
    void testFormatterFixesUnusedImports()
    {
        String oldCode =
                """
                import static java.util.Objects.isNull;
                import static java.util.Objects.requireNonNull;
                import java.util.List;
                import java.util.Map;

                class Test {
                    List<String> values;

                    void run(Object value)
                    {
                        requireNonNull(value);
                    }
                }
                """;

        String newCode =
                """
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
    void testFormatterKeepsUsedAnnotationImports()
    {
        String oldCode =
                """
                import com.google.inject.BindingAnnotation;

                import java.lang.annotation.Retention;
                import java.lang.annotation.Target;

                import static java.lang.annotation.ElementType.FIELD;
                import static java.lang.annotation.ElementType.METHOD;
                import static java.lang.annotation.ElementType.PARAMETER;
                import static java.lang.annotation.RetentionPolicy.RUNTIME;

                @BindingAnnotation
                @Target({FIELD, PARAMETER, METHOD})
                @Retention(RUNTIME)
                public @interface Actor {}
                """;

        String newCode =
                """
                import com.google.inject.BindingAnnotation;

                import java.lang.annotation.Retention;
                import java.lang.annotation.Target;

                import static java.lang.annotation.ElementType.FIELD;
                import static java.lang.annotation.ElementType.METHOD;
                import static java.lang.annotation.ElementType.PARAMETER;
                import static java.lang.annotation.RetentionPolicy.RUNTIME;

                @BindingAnnotation
                @Target({FIELD, PARAMETER, METHOD})
                @Retention(RUNTIME)
                public @interface Actor {}
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterKeepsImportsUsedInJavadocLinks()
    {
        String oldCode =
                """
                import java.util.function.Consumer;

                class Test
                {
                    /**
                     * Registers a callback via {@link Registry#register(String, Consumer)}.
                     */
                    void run()
                    {
                    }
                }
                """;

        String newCode =
                """
                import java.util.function.Consumer;

                class Test
                {
                    /**
                     * Registers a callback via {@link Registry#register(String, Consumer)}.
                     */
                    void run() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsImportsUsedInJavadocThrows()
    {
        String oldCode =
                """
                import jakarta.ws.rs.WebApplicationException;

                class Test
                {
                    /**
                     * @throws WebApplicationException with 401 status if no user is authenticated
                     */
                    void run()
                    {
                    }
                }
                """;

        String newCode =
                """
                import jakarta.ws.rs.WebApplicationException;

                class Test
                {
                    /**
                     * @throws WebApplicationException with 401 status if no user is authenticated
                     */
                    void run() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesImportUsedOnlyByFullyQualifiedTypeReference()
    {
        String oldCode =
                """
                import java.util.List;

                class Test
                {
                    java.util.List<String> values;
                }
                """;

        String newCode =
                """
                class Test
                {
                    java.util.List<String> values;
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsImportUsedAsQualifier()
    {
        String oldCode =
                """
                import io.airlift.drift.protocol.TType;

                class Test
                {
                    byte value = TType.STOP;
                }
                """;

        String newCode =
                """
                import io.airlift.drift.protocol.TType;

                class Test
                {
                    byte value = TType.STOP;
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterKeepsStaticNestedTypeImportUsedAsQualifier()
    {
        String oldCode =
                """
                import static io.airlift.drift.annotations.ThriftField.Requiredness;

                class Test
                {
                    Object value = Requiredness.OPTIONAL;
                }
                """;

        String newCode =
                """
                import static io.airlift.drift.annotations.ThriftField.Requiredness;

                class Test
                {
                    Object value = Requiredness.OPTIONAL;
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterFixesImportUsedOnlyByFullyQualifiedJavadocLink()
    {
        String oldCode =
                """
                import java.util.List;

                class Test
                {
                    /**
                     * Uses {@link java.util.List}.
                     */
                    void run() {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    /**
                     * Uses {@link java.util.List}.
                     */
                    void run() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }
}
