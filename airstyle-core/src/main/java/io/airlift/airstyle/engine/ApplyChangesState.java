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
package io.airlift.airstyle.engine;

import java.util.List;

/// Produces the final formatted source string by splicing the computed
/// whitespace from each leaf in place of the original whitespace.
///
/// Ported from IntelliJ's `ApplyChangesState`, adapted to produce
/// a string rather than mutate a document. Includes the
/// `shiftIndentInsideRange` logic from IntelliJ's
/// `FormatProcessorUtils` that shifts internal lines of multi-line
/// tokens (block comments) when their indent changes.
public final class ApplyChangesState
{
    public String apply(CharSequence source, List<LeafBlockWrapper> leaves)
    {
        StringBuilder result = new StringBuilder(source.length());
        int cursor = 0;
        for (LeafBlockWrapper leaf : leaves) {
            WhiteSpace whitespace = leaf.whiteSpace();
            int wsStart = whitespace.startOffset();
            // Keep any source text between the last leaf and the whitespace start.
            if (cursor < wsStart) {
                result.append(source, cursor, wsStart);
            }
            // Append the leaf's own text, shifting internal lines for
            // multi-line tokens (block comments) when indent changed.
            String leafText = source.subSequence(leaf.startOffset(), leaf.endOffset()).toString();
            // For ragged block comments (internal content starts before the
            // opening `/*` column), preserve the source indent and content
            // verbatim — don't reindent or shift.
            boolean ragged = whitespace.containsLineFeeds() && leafText.contains("\n")
                    && leafText.startsWith("/*")
                    && isRaggedBlockComment(leafText, whitespace.initialIndentSpaces());
            if (ragged) {
                whitespace.setIndent(whitespace.initialIndentSpaces());
                result.append(whitespace.render());
                result.append(leafText);
                cursor = leaf.endOffset();
                continue;
            }
            if (leafText.startsWith("//")
                    && !leafText.startsWith("///")
                    && startsWithCommentedOutIndent(leafText)
                    && startsAtColumnZero(source, leaf.startOffset())) {
                whitespace.setIndent(0);
            }
            // Replace the whitespace range with the rendered whitespace.
            result.append(whitespace.render());
            // Shift internal lines for multi-line block comments when their
            // indent changed. Skip text blocks (""") — their content indent
            // is managed by the textBlockMargin POST_FORMAT phase.
            if (whitespace.containsLineFeeds() && leafText.contains("\n")
                    && !leafText.startsWith("\"\"\"")) {
                leafText = shiftInternalLines(leafText, whitespace);
            }
            if (leafText.startsWith("/**")) {
                leafText = normalizeJavadocLeadingAsterisks(leafText, whitespace.indentSpaces());
            }
            else if (leafText.startsWith("/*")) {
                leafText = normalizeBlockCommentClosingMarker(leafText, whitespace.indentSpaces());
            }
            result.append(leafText);
            cursor = leaf.endOffset();
        }
        // Trailing text (rare — usually nothing).
        if (cursor < source.length()) {
            result.append(source, cursor, source.length());
        }
        return result.toString();
    }

    private static String normalizeJavadocLeadingAsterisks(String text, int openingIndent)
    {
        String[] lines = text.split("\n", -1);
        if (lines.length <= 1) {
            return text;
        }

        StringBuilder normalized = new StringBuilder(text.length());
        normalized.append(lines[0]);
        for (int i = 1; i < lines.length; i++) {
            normalized.append('\n');
            String line = lines[i];
            String stripped = line.stripLeading();
            if (stripped.startsWith("*")) {
                normalized.repeat(' ', openingIndent + 1);
                normalized.append(stripped);
            }
            else {
                normalized.append(line);
            }
        }
        return normalized.toString();
    }

    private static String normalizeBlockCommentClosingMarker(String text, int openingIndent)
    {
        String[] lines = text.split("\n", -1);
        if (lines.length <= 1) {
            return text;
        }

        StringBuilder normalized = new StringBuilder(text.length());
        normalized.append(lines[0]);
        for (int i = 1; i < lines.length; i++) {
            normalized.append('\n');
            String line = lines[i];
            String stripped = line.stripLeading();
            if (stripped.startsWith("*/") && leadingSpaces(line) < openingIndent) {
                normalized.repeat(' ', openingIndent);
                normalized.append(stripped);
            }
            else {
                normalized.append(line);
            }
        }
        return normalized.toString();
    }

    /// Shift every line after the first by the delta between the original
    /// indent and the new computed indent. Mirrors IntelliJ's
    /// `shiftIndentInsideRange`.
    private static String shiftInternalLines(String text, WhiteSpace whitespace)
    {
        int oldIndent = whitespace.initialIndentSpaces();
        int newIndent = whitespace.indentSpaces();
        int shift = newIndent - oldIndent;
        if (shift == 0) {
            return text;
        }
        // Split into lines and shift all lines AFTER the first.
        String[] lines = text.split("\n", -1);
        if (lines.length <= 1) {
            return text;
        }
        StringBuilder shifted = new StringBuilder();
        shifted.append(lines[0]); // first line stays as-is (its indent is in the whitespace)
        for (int i = 1; i < lines.length; i++) {
            shifted.append('\n');
            String line = lines[i];
            if (shift > 0) {
                // Add spaces
                shifted.repeat(' ', shift);
                shifted.append(line);
            }
            else {
                // Remove spaces (but don't go below 0)
                int spacesToRemove = Math.min(-shift, leadingSpaces(line));
                shifted.append(line.substring(spacesToRemove));
            }
        }
        return shifted.toString();
    }

    /// A block comment is "ragged" when an internal line's content starts
    /// left of the opening `/*`. Ragged blocks keep their source indent —
    /// reindenting them would eat the overhang.
    private static boolean isRaggedBlockComment(String text, int openingIndent)
    {
        String[] lines = text.split("\n", -1);
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            int leading = leadingSpaces(line);
            // Skip empty lines.
            if (leading == line.length()) {
                continue;
            }
            if (line.substring(leading).startsWith("*/")) {
                continue;
            }
            if (leading < openingIndent) {
                return true;
            }
        }
        return false;
    }

    private static int leadingSpaces(String line)
    {
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == ' ') {
                count++;
            }
            else {
                break;
            }
        }
        return count;
    }

    private static boolean startsAtColumnZero(CharSequence source, int offset)
    {
        return offset == 0 || source.charAt(offset - 1) == '\n';
    }

    private static boolean startsWithCommentedOutIndent(String text)
    {
        return text.length() > 3 && text.charAt(2) == ' ' && text.charAt(3) == ' ';
    }
}
