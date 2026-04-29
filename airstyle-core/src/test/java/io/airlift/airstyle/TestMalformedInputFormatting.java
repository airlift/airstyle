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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static io.airlift.airstyle.FormatterAssertions.assertUnparseableInputUnchanged;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@TestInstance(PER_CLASS)
public class TestMalformedInputFormatting
{
    private AirstyleFormatter formatter;

    @BeforeAll
    void setup()
    {
        formatter = new AirstyleFormatter();
    }

    @Test
    void testFormatterKeepsMalformedImportSectionUnchanged()
    {
        String code =
                """
                package io.airlift.test;

                import java.util.Map;
                import java.util.List
                import com.google.common.collect.ImmutableList;

                public class Test {}
                """;

        assertUnparseableInputUnchanged(code);
    }

    @Test
    void testFormatterKeepsMalformedMethodBodyUnchanged()
    {
        String code =
                """
                class Test {
                    void run() {
                        if (ready) {
                            work();
                    }
                }
                """;

        assertUnparseableInputUnchanged(code);
    }

    @Test
    void testFormatterKeepsMalformedWrappedInvocationUnchanged()
    {
        String code =
                """
                class Test {
                    void run() {
                        call(
                                first,
                                second;
                    }
                }
                """;

        assertUnparseableInputUnchanged(code);
    }

    @Test
    void testFormatterKeepsMalformedCompilationUnitUnchanged()
    {
        String code =
                """
                class Test {
                    void run() {
                        execute(
                                first,
                                second;
                    }
                }
                """;

        assertUnparseableInputUnchanged(code);
    }

    @Test
    void testFormatterReturnsOriginalWhenDefaultFormattingWouldCreateSyntaxErrors()
    {
        String oldCode =
                """
                class Test { void run(){ int value=1; } }
                """;

        String formattedCode = new AirstyleFormatter().format(oldCode);
        AirstyleFormatter syntaxFailingFormatter = new SyntaxFailingFormatter(formattedCode);

        assertEquals(oldCode, syntaxFailingFormatter.format(oldCode));
    }

    @Test
    void testFormatterDoesNotCrashOnUnterminatedTypeDeclaration()
    {
        String code =
                """
                class Test {
                    void run() {
                        int value = 42;
                """;

        assertDoesNotThrow(() -> formatter.format(code));
    }

    private static class SyntaxFailingFormatter
            extends AirstyleFormatter
    {
        private final String invalidFormattedOutput;

        private SyntaxFailingFormatter(String invalidFormattedOutput)
        {
            this.invalidFormattedOutput = invalidFormattedOutput;
        }

        @Override
        boolean hasSyntaxErrors(String source)
        {
            if (source.equals(invalidFormattedOutput)) {
                return true;
            }
            return super.hasSyntaxErrors(source);
        }
    }
}
