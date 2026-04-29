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

import java.util.Comparator;
import java.util.List;

/// A text edit emitted by a normalizer: replace the half-open range
/// `[start, end)` in the source with `value`.
///
/// Normalizers collect these into a list and hand them to [#applyAll] to
/// produce the edited source. Edits are applied highest-offset first so
/// earlier edits don't shift the offsets of later ones.
record Replacement(int start, int end, String value)
{
    Replacement
    {
        if (start < 0) {
            throw new IllegalArgumentException("Replacement start %s < 0 for value [%s]".formatted(start, value));
        }
        if (start > end) {
            throw new IllegalArgumentException("Replacement start %s > end %s for value [%s]".formatted(start, end, value));
        }
    }

    /// Apply all `replacements` to `source` and return the edited result.
    /// Returns `source` unchanged when `replacements` is empty.
    static String applyAll(String source, List<Replacement> replacements)
    {
        if (replacements.isEmpty()) {
            return source;
        }

        List<Replacement> ordered = replacements.stream()
                .sorted(Comparator.comparingInt(Replacement::start)
                        .thenComparingInt(Replacement::end)
                        .reversed())
                .toList();

        Replacement previous = ordered.getFirst();
        for (int index = 1; index < ordered.size(); index++) {
            Replacement current = ordered.get(index);
            if (current.end() > previous.start()) {
                throw new IllegalArgumentException("Overlapping replacements are not allowed: [%s,%s) overlaps [%s,%s)"
                        .formatted(previous.start(), previous.end(), current.start(), current.end()));
            }
            previous = current;
        }

        StringBuilder result = new StringBuilder(source);
        for (Replacement replacement : ordered) {
            if (replacement.end() > source.length()) {
                throw new IllegalArgumentException("Replacement range [%s,%s) exceeds source length %s"
                        .formatted(replacement.start(), replacement.end(), source.length()));
            }
            result.replace(replacement.start(), replacement.end(), replacement.value());
        }
        return result.toString();
    }
}
