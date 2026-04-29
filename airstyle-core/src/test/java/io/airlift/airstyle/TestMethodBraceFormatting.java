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

public class TestMethodBraceFormatting
{
    @Test
    void testFormatterFixesMethodAndConstructorBracePlacement()
    {
        String oldCode =
                """
                class Test {
                    Test() {
                    }

                    void run() {
                        call();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Test() {}

                    void run()
                    {
                        call();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesEmptyBodyWithInnerWhitespace()
    {
        String oldCode =
                """
                class Test {
                    Test() {   }

                    void foo() { }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Test() {}

                    void foo() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesSingleLineNonEmptyMethodBody()
    {
        String oldCode =
                """
                class Test {
                    void foo() { IO.println("what"); }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void foo()
                    {
                        IO.println("what");
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsCompactRecordConstructorEmptyBodyInlineWithWrappedAnnotation()
    {
        String code =
                """
                @interface JsonCreator
                {
                    boolean disabled() default false;
                }

                public record ApiResourceVersion(int major, int minor)
                {
                    @JsonCreator(disabled = true)
                    public ApiResourceVersion {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesBlankLineBetweenMethodSignatureAndOpeningBrace()
    {
        String oldCode =
                """
                class Test
                {
                    void run()

                    {
                        work();
                    }

                    void work() {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        work();
                    }

                    void work() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }
}
