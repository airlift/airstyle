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

import static java.lang.Math.max;

/// The whitespace between two leaf blocks. Carries both the original content
/// (to preserve comments) and the computed output (line feeds + indent spaces).
///
/// Simplified port of IntelliJ's `com.intellij.formatting.WhiteSpace`.
/// Airstyle's whitespace contains no comments — comments are AST nodes, not
/// whitespace — so we only track the final whitespace text.
public final class WhiteSpace
{
    private final int startOffset;

    private int lineFeeds;
    private int indentSpaces;
    private int spaces;

    private boolean initialLineFeeds;
    private int initialIndentSpaces;
    private final boolean isFirst;

    public WhiteSpace(int startOffset, boolean isFirst)
    {
        this.startOffset = startOffset;
        this.isFirst = isFirst;
    }

    public int startOffset()
    {
        return startOffset;
    }

    public void setEndOffset(int newEnd, CharSequence source)
    {
        this.lineFeeds = 0;
        this.spaces = 0;
        this.indentSpaces = 0;
        boolean sawLineFeed = false;
        int columnSincePreviousLineFeed = 0;
        for (int i = startOffset; i < newEnd && i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '\n') {
                lineFeeds++;
                sawLineFeed = true;
                columnSincePreviousLineFeed = 0;
            }
            else if (c == ' ' || c == '\t') {
                columnSincePreviousLineFeed++;
            }
        }
        if (sawLineFeed) {
            indentSpaces = columnSincePreviousLineFeed;
        }
        else {
            spaces = columnSincePreviousLineFeed;
        }
        initialLineFeeds = sawLineFeed;
        initialIndentSpaces = sawLineFeed ? columnSincePreviousLineFeed : 0;
    }

    /// The indent from the original source, before any formatting.
    public int initialIndentSpaces()
    {
        return initialIndentSpaces;
    }

    public boolean containsLineFeeds()
    {
        return lineFeeds > 0;
    }

    public int indentSpaces()
    {
        return indentSpaces;
    }

    public int spaces()
    {
        return spaces;
    }

    public int totalSpaces()
    {
        return containsLineFeeds() ? indentSpaces : spaces;
    }

    public boolean isFirstWhitespace()
    {
        return isFirst;
    }

    /// Set the line-feed and indent values based on the given [Spacing].
    public void applySpacing(Spacing spacing)
    {
        if (spacing == null) {
            return;
        }
        int desiredLineFeeds;
        if (spacing.keepLineBreaks() && initialLineFeeds) {
            int cap = max(spacing.minLineFeeds(), Math.min(lineFeeds, spacing.keepBlankLines() + 1));
            desiredLineFeeds = max(cap, spacing.minLineFeeds());
        }
        else {
            desiredLineFeeds = spacing.minLineFeeds();
        }
        if (desiredLineFeeds > 0) {
            lineFeeds = desiredLineFeeds;
            indentSpaces = 0;
            spaces = 0;
        }
        else {
            lineFeeds = 0;
            indentSpaces = 0;
            spaces = clampSpaces(spacing);
        }
    }

    /// Set the indent explicitly (used by indent resolution for new-line whitespace).
    public void setIndent(int newIndent)
    {
        indentSpaces = max(0, newIndent);
    }

    private int clampSpaces(Spacing spacing)
    {
        int minS = max(0, spacing.minSpaces());
        int maxS = max(minS, spacing.maxSpaces());
        return Math.clamp(spaces, minS, maxS);
    }

    /// Render the whitespace to the target buffer.
    public String render()
    {
        StringBuilder builder = new StringBuilder();
        builder.repeat('\n', lineFeeds);
        int indentCount = containsLineFeeds() ? indentSpaces : spaces;
        builder.repeat(' ', indentCount);
        return builder.toString();
    }
}
