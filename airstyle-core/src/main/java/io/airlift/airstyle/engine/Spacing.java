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

/// A [Spacing] describes the whitespace allowed/required between two
/// adjacent blocks in the document. Modeled after IntelliJ's
/// `com.intellij.formatting.Spacing`.
public final class Spacing
{
    private static final Spacing NONE = new Spacing(0, 0, 0, true, 0);
    private static final Spacing ONE_SPACE = new Spacing(1, 1, 0, true, 0);

    private final int minSpaces;
    private final int maxSpaces;
    private final int minLineFeeds;
    private final boolean keepLineBreaks;
    private final int keepBlankLines;

    private Spacing(int minSpaces, int maxSpaces, int minLineFeeds, boolean keepLineBreaks, int keepBlankLines)
    {
        this.minSpaces = minSpaces;
        this.maxSpaces = maxSpaces;
        this.minLineFeeds = minLineFeeds;
        this.keepLineBreaks = keepLineBreaks;
        this.keepBlankLines = keepBlankLines;
    }

    public int minSpaces()
    {
        return minSpaces;
    }

    public int maxSpaces()
    {
        return maxSpaces;
    }

    public int minLineFeeds()
    {
        return minLineFeeds;
    }

    public boolean keepLineBreaks()
    {
        return keepLineBreaks;
    }

    public int keepBlankLines()
    {
        return keepBlankLines;
    }

    public static Spacing none()
    {
        return NONE;
    }

    public static Spacing oneSpace()
    {
        return ONE_SPACE;
    }

    public static Spacing createSpacing(
            int minSpaces,
            int maxSpaces,
            int minLineFeeds,
            boolean keepLineBreaks,
            int keepBlankLines)
    {
        return new Spacing(minSpaces, maxSpaces, minLineFeeds, keepLineBreaks, keepBlankLines);
    }
}
