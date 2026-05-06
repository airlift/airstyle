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
package io.airlift.airstyle.format;

import io.airlift.airstyle.JavaLanguageSupport;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/// Utility methods for source text cleanup used in the format cleanup phase.
public final class TokenSpacingFormatter
{
    private TokenSpacingFormatter() {}

    /// Remove trailing whitespace from each line.
    public static String format(String source)
    {
        return removeTrailingWhitespace(source);
    }

    /// Replace every tab character in inter-token whitespace with `replacement`.
    /// Tabs inside string literals, text blocks, and comments are preserved —
    /// the scanner treats those as token content, not whitespace.
    public static String replaceTabsInWhitespace(String source, String replacement)
    {
        if (source.indexOf('\t') < 0) {
            return source;
        }
        IScanner scanner = createCommentAwareScanner();
        scanner.setSource(source.toCharArray());
        List<SpacingEdit> edits = new ArrayList<>();
        try {
            int prevEnd = 0;
            while (true) {
                int token = scanner.getNextToken();
                if (token == ITerminalSymbols.TokenNameEOF) {
                    addTabEdits(source, prevEnd, source.length(), replacement, edits);
                    break;
                }
                int start = scanner.getCurrentTokenStartPosition();
                addTabEdits(source, prevEnd, start, replacement, edits);
                prevEnd = scanner.getCurrentTokenEndPosition() + 1;
            }
        }
        catch (InvalidInputException _) {
        }
        return applyEdits(source, edits);
    }

    /// Normalize multiple spaces between tokens to single space, using the
    /// scanner to correctly skip string literals and text blocks. Preserves
    /// comment alignment (multiple spaces before // or /\*).
    public static String normalizeMultipleSpaces(String source)
    {
        IScanner scanner = createCommentAwareScanner();
        scanner.setSource(source.toCharArray());
        List<SpacingEdit> edits = new ArrayList<>();
        try {
            int prevEnd = 0;
            boolean commentBetween = false;
            while (true) {
                int token = scanner.getNextToken();
                if (token == ITerminalSymbols.TokenNameEOF) {
                    break;
                }
                if (isCommentToken(token)) {
                    commentBetween = true;
                    continue;
                }
                int start = scanner.getCurrentTokenStartPosition();

                if (prevEnd > 0 && start > prevEnd && !commentBetween) {
                    String whitespace = source.substring(prevEnd, start);
                    if (!whitespace.contains("\n") && whitespace.length() > 1) {
                        edits.add(new SpacingEdit(prevEnd, start, " "));
                    }
                }
                prevEnd = scanner.getCurrentTokenEndPosition() + 1;
                commentBetween = false;
            }
        }
        catch (InvalidInputException _) {
        }
        return applyEdits(source, edits);
    }

    private static String applyEdits(String source, List<SpacingEdit> edits)
    {
        if (edits.isEmpty()) {
            return source;
        }
        edits.sort(Comparator.comparingInt(SpacingEdit::start).reversed());
        StringBuilder result = new StringBuilder(source);
        for (SpacingEdit edit : edits) {
            result.replace(edit.start(), edit.end(), edit.replacement());
        }
        return result.toString();
    }

    private static void addTabEdits(String source, int start, int end, String replacement, List<SpacingEdit> edits)
    {
        int safeEnd = Math.min(end, source.length());
        for (int i = start; i < safeEnd; i++) {
            if (source.charAt(i) == '\t') {
                edits.add(new SpacingEdit(i, i + 1, replacement));
            }
        }
    }

    /// Strip trailing spaces/tabs from scanner gaps only, preserving comment
    /// and literal token contents.
    private static String removeTrailingWhitespace(String source)
    {
        IScanner scanner = createCommentAwareScanner();
        scanner.setSource(source.toCharArray());
        List<SpacingEdit> edits = new ArrayList<>();
        try {
            int prevEnd = 0;
            while (true) {
                int token = scanner.getNextToken();
                if (token == ITerminalSymbols.TokenNameEOF) {
                    addTrailingWhitespaceEdits(source, prevEnd, source.length(), true, edits);
                    break;
                }
                int start = scanner.getCurrentTokenStartPosition();
                addTrailingWhitespaceEdits(source, prevEnd, start, false, edits);
                prevEnd = scanner.getCurrentTokenEndPosition() + 1;
            }
        }
        catch (InvalidInputException _) {
            return source;
        }
        return applyEdits(source, edits);
    }

    private static void addTrailingWhitespaceEdits(String source, int start, int end, boolean trimEnd, List<SpacingEdit> edits)
    {
        int safeEnd = Math.min(end, source.length());
        int segmentStart = Math.max(0, start);
        for (int i = segmentStart; i < safeEnd; i++) {
            if (source.charAt(i) == '\n') {
                int contentEnd = i;
                while (contentEnd > segmentStart && isHorizontalWhitespace(source.charAt(contentEnd - 1))) {
                    contentEnd--;
                }
                if (contentEnd < i) {
                    edits.add(new SpacingEdit(contentEnd, i, ""));
                }
                segmentStart = i + 1;
            }
        }
        if (trimEnd) {
            int contentEnd = safeEnd;
            while (contentEnd > segmentStart && isHorizontalWhitespace(source.charAt(contentEnd - 1))) {
                contentEnd--;
            }
            if (contentEnd < safeEnd) {
                edits.add(new SpacingEdit(contentEnd, safeEnd, ""));
            }
        }
    }

    private static boolean isHorizontalWhitespace(char ch)
    {
        return ch == ' ' || ch == '\t';
    }

    private static IScanner createCommentAwareScanner()
    {
        return ToolFactory.createScanner(
                true, // tokenizeComments — comments are visible tokens
                false,
                false,
                JavaLanguageSupport.latestJavaVersion(),
                JavaLanguageSupport.latestJavaVersion(),
                true);
    }

    private static boolean isCommentToken(int token)
    {
        return token == ITerminalSymbols.TokenNameCOMMENT_LINE
                || token == ITerminalSymbols.TokenNameCOMMENT_BLOCK
                || token == ITerminalSymbols.TokenNameCOMMENT_JAVADOC
                || token == ITerminalSymbols.TokenNameCOMMENT_MARKDOWN;
    }

    private record SpacingEdit(int start, int end, String replacement) {}
}
