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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestReplacement
{
    @Test
    void testFormatterKeepsNonOverlappingReplacementsAppliedFromTheEnd()
    {
        List<Replacement> replacements = new ArrayList<>();
        replacements.add(new Replacement(1, 2, "X"));
        replacements.add(new Replacement(3, 4, "Y"));

        assertEquals("aXcY", Replacement.applyAll("abcd", replacements));
    }

    @Test
    void testFormatterFixesOverlappingReplacementsByRejectingThem()
    {
        List<Replacement> replacements = new ArrayList<>();
        replacements.add(new Replacement(1, 3, "X"));
        replacements.add(new Replacement(2, 4, "Y"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Replacement.applyAll("abcd", replacements));
        assertEquals("Overlapping replacements are not allowed: [2,4) overlaps [1,3)", exception.getMessage());
    }

    @Test
    void testFormatterFixesNegativeReplacementStartByRejectingIt()
    {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Replacement(-1, 0, "X"));
        assertEquals("Replacement start -1 < 0 for value [X]", exception.getMessage());
    }

    @Test
    void testFormatterFixesOutOfBoundsReplacementByRejectingIt()
    {
        List<Replacement> replacements = new ArrayList<>();
        replacements.add(new Replacement(1, 5, "X"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Replacement.applyAll("abcd", replacements));
        assertEquals("Replacement range [1,5) exceeds source length 4", exception.getMessage());
    }
}
