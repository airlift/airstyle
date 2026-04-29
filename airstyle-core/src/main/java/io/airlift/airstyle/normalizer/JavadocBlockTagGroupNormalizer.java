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
import java.util.List;

/// Ensures a blank `*` line separates the description from the first block
/// tag group (`@param`, `@return`, `@throws`, etc.) in a Javadoc comment.
///
/// ### Example
///
/// Before:
/// ```java
/// /**
///  * Describe the thing.
///  * @param x the input
///  * @return the result
///  */
/// int compute(int x) { ... }
/// ```
///
/// After:
/// ```java
/// /**
///  * Describe the thing.
///  *
///  * @param x the input
///  * @return the result
///  */
/// int compute(int x) { ... }
/// ```
public final class JavadocBlockTagGroupNormalizer
{
    private JavadocBlockTagGroupNormalizer() {}

    public static String normalize(String source)
    {
        SourceModel sourceModel = SourceModel.create(source);
        List<Replacement> replacements = collectReplacements(sourceModel);
        return Replacement.applyAll(source, replacements);
    }

    private static List<Replacement> collectReplacements(SourceModel sourceModel)
    {
        List<Replacement> replacements = new ArrayList<>();
        List<Comment> comments = sourceModel.comments();
        if (comments == null || comments.isEmpty()) {
            return replacements;
        }

        for (Comment comment : comments) {
            if (!(comment instanceof Javadoc javadoc)) {
                continue;
            }

            replacements.addAll(createReplacements(sourceModel, javadoc));
        }
        return replacements;
    }

    private static List<Replacement> createReplacements(SourceModel sourceModel, Javadoc javadoc)
    {
        List<Replacement> replacements = new ArrayList<>();
        String source = sourceModel.source();
        List<TagElement> tags = sourceModel.topLevelJavadocTags(javadoc).stream()
                .filter(tag -> !sourceModel.isInsideJavadocPreBlock(javadoc, tag.getStartPosition()))
                .toList();
        if (tags.isEmpty()) {
            return replacements;
        }

        int firstBlockTagIndex = firstBlockTagIndex(tags);
        boolean markdown = javadoc.isMarkdown();
        String blankLinePrefix = markdown ? "///" : "*";

        if (firstBlockTagIndex > 0) {
            TagElement descriptionTag = tags.get(firstBlockTagIndex - 1);
            if (descriptionTag.getTagName() == null && hasDescription(source, descriptionTag)) {
                TagElement firstBlockTag = tags.get(firstBlockTagIndex);
                int tagLineStart = sourceModel.lineStart(firstBlockTag.getStartPosition());
                if (!hasBlankJavadocLineBefore(sourceModel, tagLineStart, javadoc.getStartPosition(), markdown)) {
                    replacements.add(new Replacement(tagLineStart, tagLineStart, sourceModel.lineIndent(tagLineStart) + blankLinePrefix + "\n"));
                }
            }
        }

        if (firstBlockTagIndex >= 0) {
            for (int index = firstBlockTagIndex + 1; index < tags.size(); index++) {
                TagElement tag = tags.get(index);
                int tagLineStart = sourceModel.lineStart(tag.getStartPosition());
                Replacement blankLineRemoval = blankLineRemovalBefore(sourceModel, tagLineStart, javadoc.getStartPosition(), markdown);
                if (blankLineRemoval != null) {
                    replacements.add(blankLineRemoval);
                }
            }
        }
        return replacements;
    }

    /// Drop blank `*` / `///` lines immediately before `tagLineStart`
    /// so consecutive block tags sit flush. Preserves lines inside `<pre>` by
    /// not running on tags that overlap them (filtered by caller).
    private static Replacement blankLineRemovalBefore(SourceModel sourceModel, int tagLineStart, int javadocStart, boolean markdown)
    {
        String source = sourceModel.source();
        int cursor = tagLineStart;
        int firstBlankLineStart = -1;
        while (cursor > javadocStart) {
            int previousLineEnd = cursor - 1;
            if (previousLineEnd >= 0 && source.charAt(previousLineEnd) == '\n') {
                // fall through
            }
            else {
                break;
            }
            int previousLineStart = sourceModel.lineStart(previousLineEnd);
            if (previousLineStart <= javadocStart) {
                break;
            }
            SourceModel.JavadocLineInfo lineInfo = sourceModel.javadocLineInfo(
                    sourceModel.lineNumber(previousLineStart), markdown);
            if (lineInfo == null || !lineInfo.content().isEmpty()) {
                break;
            }
            firstBlankLineStart = previousLineStart;
            cursor = previousLineStart;
        }
        if (firstBlankLineStart < 0) {
            return null;
        }
        return new Replacement(firstBlankLineStart, tagLineStart, "");
    }

    private static int firstBlockTagIndex(List<TagElement> tags)
    {
        for (int index = 0; index < tags.size(); index++) {
            if (tags.get(index).getTagName() != null) {
                return index;
            }
        }
        return -1;
    }

    private static boolean hasDescription(String source, TagElement descriptionTag)
    {
        int start = descriptionTag.getStartPosition();
        int end = start + descriptionTag.getLength();
        if (start < 0 || end > source.length() || start >= end) {
            return false;
        }
        return !source.substring(start, end).trim().isEmpty();
    }

    private static boolean hasBlankJavadocLineBefore(SourceModel sourceModel, int lineStart, int javadocStart, boolean markdown)
    {
        String source = sourceModel.source();
        int previousLineEnd = lineStart - 1;
        if (previousLineEnd >= 0 && source.charAt(previousLineEnd) == '\n') {
            previousLineEnd--;
        }
        if (previousLineEnd <= javadocStart) {
            return false;
        }

        SourceModel.JavadocLineInfo lineInfo = sourceModel.javadocLineInfo(sourceModel.lineNumber(previousLineEnd), markdown);
        return lineInfo != null && lineInfo.content().isEmpty();
    }
}
