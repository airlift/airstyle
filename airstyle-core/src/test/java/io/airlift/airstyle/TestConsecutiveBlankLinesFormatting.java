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

public class TestConsecutiveBlankLinesFormatting
{
    @Test
    void testFormatterFixesMultipleConsecutiveBlankLinesBetweenMembers()
    {
        String oldCode =
                """
                class Test {
                    void first()
                    {
                    }

                    void second()
                    {
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void first() {}

                    void second() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testBlankLineBeforeElseIsPreserved()
    {
        String code =
                """
                class Test
                {
                    void run(int x)
                    {
                        if (x == 1) {
                            doOne();
                        }

                        else if (x == 2) {
                            doTwo();
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testBlankLineBetweenJavadocAndAnnotationPreserved()
    {
        String code =
                """
                class Test
                {
                    /**
                     * doc
                     **/

                    @JsonProperty
                    public Discriminator getDiscriminator()
                    {
                        return null;
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFileStartingWithBlankLineBeforePackageNotExpanded()
    {
        // Source starts with a single blank line followed by `package`.
        // Engine should not add more blank lines before package.
        String code = "\npackage foo.bar;\n\nclass Test {}\n";

        assertCanonicalFormatting(code);
    }

    @Test
    void testConsecutiveWrappedAnnotationsOnClassStaySingleBlankLine()
    {
        String code =
                """
                @JsonTypeInfo(
                        use = JsonTypeInfo.Id.NAME,
                        property = "@type")
                @JsonSubTypes({
                        @JsonSubTypes.Type(value = Car.class, name = "car"),
                        @JsonSubTypes.Type(value = Truck.class, name = "truck"),
                })
                public interface Vehicle {}
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testBlankLineBeforeSectionMarkerCommentBetweenEnumConstants()
    {
        String code =
                """
                public enum JsonRpcErrorCode
                {
                    CONNECTION_CLOSED(-32000),
                    REQUEST_TIMEOUT(-32001),

                    // Standard codes
                    PARSE_ERROR(-32700),
                    INVALID_REQUEST(-32600);

                    JsonRpcErrorCode(int code) {}
                }
                """;

        assertCanonicalFormatting(code);
    }
}
