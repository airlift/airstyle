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
package io.airlift.airstyle.engine.java;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestJavaSourceRange
{
    @Test
    void testFormatterFixesNegativeSourceRangeByRejectingIt()
    {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> JavaSourceRange.leaf("Tokens", -1, 0));

        assertEquals("start is negative: -1", exception.getMessage());
    }

    @Test
    void testFormatterFixesInvertedSourceRangeByRejectingIt()
    {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> JavaSourceRange.leaf("Tokens", 3, 2));

        assertEquals("end is before start: [3,2)", exception.getMessage());
    }

    @Test
    void testFormatterKeepsOwnedSourceRangeMetadata()
    {
        JavaSourceRange range = JavaSourceRange.leaf("Tokens", 1, 3);

        assertEquals("Tokens", range.owner());
        assertEquals(1, range.start());
        assertEquals(3, range.end());
    }
}
