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

import io.airlift.airstyle.model.SourceModel;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.TagElement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.max;

/// Collapses runs of two or more spaces in Javadoc block-tag lines to a
/// single space so `@param` / `@throws` descriptions are not aligned into a
/// common column — `@param path      input directory` becomes
/// `@param path input directory`. Tag-continuation lines are left untouched
/// (their leading indent is rewritten by
/// [JavadocContinuationIndentationNormalizer]). Lines inside `<pre>`
/// blocks are skipped.
public final class JavadocTagSpacingNormalizer
{
    private JavadocTagSpacingNormalizer() {}

    public static String normalize(String source)
    {
        SourceModel sourceModel = SourceModel.create(source);
        List<Replacement> replacements = new ArrayList<>();
        for (Comment comment : sourceModel.comments()) {
            if (!(comment instanceof Javadoc javadoc)) {
                continue;
            }
            collect(sourceModel, javadoc, replacements);
        }
        return Replacement.applyAll(source, replacements);
    }

    private static void collect(SourceModel sourceModel, Javadoc javadoc, List<Replacement> replacements)
    {
        boolean markdown = javadoc.isMarkdown();
        for (TagElement tag : sourceModel.topLevelJavadocTags(javadoc)) {
            if (tag.getTagName() == null) {
                continue;
            }
            int tagStart = tag.getStartPosition();
            if (sourceModel.isInsideJavadocPreBlock(javadoc, tagStart)) {
                continue;
            }
            int tagLine = sourceModel.lineNumber(tagStart);
            int tagEnd = max(tagStart, tagStart + tag.getLength() - 1);
            SourceModel.JavadocLineInfo info = sourceModel.javadocLineInfo(tagLine, markdown);
            if (info == null) {
                continue;
            }
            int lineEnd = sourceModel.lineEnd(info.lineStart());
            // The tag may end mid-line (another tag follows on the same
            // physical line is not possible in Javadoc), so the collapse
            // range is [contentStart, min(lineEnd, tagEnd+1)).
            int collapseEnd = Math.min(lineEnd, tagEnd + 1);
            int contentStart = info.contentStart();
            if (contentStart >= collapseEnd) {
                continue;
            }
            String original = sourceModel.source().substring(contentStart, collapseEnd);
            String collapsed = collapseRunsPreservingInlineTags(sourceModel.source(), contentStart, collapseEnd, collectProtectedRanges(tag));
            if (!collapsed.equals(original)) {
                replacements.add(new Replacement(contentStart, collapseEnd, collapsed));
            }
        }
    }

    private static List<ProtectedRange> collectProtectedRanges(TagElement tag)
    {
        List<ProtectedRange> ranges = new ArrayList<>();
        collectProtectedRanges(tag, ranges);
        if (ranges.isEmpty()) {
            return List.of();
        }

        ranges.sort(Comparator.comparingInt(ProtectedRange::start));
        List<ProtectedRange> merged = new ArrayList<>();
        ProtectedRange current = ranges.getFirst();
        for (int index = 1; index < ranges.size(); index++) {
            ProtectedRange next = ranges.get(index);
            if (next.start() <= current.end()) {
                current = new ProtectedRange(current.start(), Math.max(current.end(), next.end()));
                continue;
            }
            merged.add(current);
            current = next;
        }
        merged.add(current);
        return List.copyOf(merged);
    }

    private static void collectProtectedRanges(TagElement tag, List<ProtectedRange> ranges)
    {
        for (Object fragment : tag.fragments()) {
            if (fragment instanceof TagElement nestedTag) {
                int start = nestedTag.getStartPosition();
                int end = start + nestedTag.getLength();
                if (start >= 0 && start < end) {
                    ranges.add(new ProtectedRange(start, end));
                }
                collectProtectedRanges(nestedTag, ranges);
            }
        }
    }

    private static String collapseRunsPreservingInlineTags(String source, int start, int end, List<ProtectedRange> protectedRanges)
    {
        if (protectedRanges.isEmpty()) {
            return collapseRuns(source.substring(start, end));
        }

        List<ProtectedRange> clipped = new ArrayList<>();
        for (ProtectedRange protectedRange : protectedRanges) {
            int clippedStart = Math.max(start, protectedRange.start());
            int clippedEnd = Math.min(end, protectedRange.end());
            if (clippedStart < clippedEnd) {
                clipped.add(new ProtectedRange(clippedStart, clippedEnd));
            }
        }
        if (clipped.isEmpty()) {
            return collapseRuns(source.substring(start, end));
        }

        StringBuilder out = new StringBuilder(end - start);
        int cursor = start;
        int protectedIndex = 0;
        boolean previousSpace = false;
        while (cursor < end) {
            if (protectedIndex < clipped.size() && cursor == clipped.get(protectedIndex).start()) {
                ProtectedRange protectedRange = clipped.get(protectedIndex++);
                out.append(source, protectedRange.start(), protectedRange.end());
                previousSpace = endsWithCollapsibleWhitespace(source, protectedRange.start(), protectedRange.end());
                cursor = protectedRange.end();
                continue;
            }

            char current = source.charAt(cursor);
            if (current == ' ' || current == '\t') {
                if (!previousSpace) {
                    out.append(' ');
                }
                previousSpace = true;
            }
            else {
                out.append(current);
                previousSpace = false;
            }
            cursor++;
        }
        return out.toString();
    }

    private static boolean endsWithCollapsibleWhitespace(String source, int start, int end)
    {
        if (start >= end) {
            return false;
        }
        char last = source.charAt(end - 1);
        return last == ' ' || last == '\t';
    }

    private static String collapseRuns(String input)
    {
        StringBuilder out = new StringBuilder(input.length());
        boolean prevSpace = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == ' ') {
                if (!prevSpace) {
                    out.append(' ');
                }
                prevSpace = true;
            }
            else {
                out.append(c);
                prevSpace = false;
            }
        }
        return out.toString();
    }

    private record ProtectedRange(int start, int end) {}
}
