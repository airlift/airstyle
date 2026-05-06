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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public final class FormatterAssertions
{
    private static final AirstyleFormatter FORMATTER = new AirstyleFormatter();

    private FormatterAssertions() {}

    public static void assertFormatsOldToNew(String oldCode, String newCode)
    {
        assertNotEquals(oldCode, newCode, "assertFormatsOldToNew requires different input and expected output; use assertCanonicalFormatting for already-canonical code");
        assertFormatsTo(oldCode, newCode);
    }

    public static void assertCanonicalFormatting(String source)
    {
        assertFormatsTo(source, source);
    }

    private static void assertFormatsTo(String oldCode, String newCode)
    {
        assertEquals(newCode, FORMATTER.format(oldCode));

        // Second-pass idempotence: formatting the expected output must produce
        // itself, guarding against non-stable transformations.
        assertEquals(newCode, FORMATTER.format(newCode), "Formatting is not idempotent");
    }

    /// Asserts the formatter returns unparseable input unchanged — i.e. that
    /// the syntax-error fallback path preserves the source. Use for tests
    /// whose input is deliberately unparseable (malformed syntax, preview
    /// features the runtime doesn't recognize). [#assertFormatsOldToNew]
    /// should not be used here because these cases do not expect formatting
    /// changes.
    public static void assertUnparseableInputUnchanged(String source)
    {
        assertEquals(source, FORMATTER.format(source));
    }
}
