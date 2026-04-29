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

import static io.airlift.airstyle.AirstyleFormatter.CONTINUATION_INDENT_SIZE;
import static java.lang.Math.max;

/// Normalizes continuation lines inside Javadoc block tags so they are
/// indented the configured continuation width past the `*`, making long tag
/// descriptions visually nested under the tag. The continuation column is a
/// fixed continuation indent past the `*`, regardless of the tag name's
/// length (no alignment-under-first-word padding).
///
/// ### Example
///
/// Before:
/// ```java
/// /**
///  * @deprecated Use foo() instead.
///  * Also see bar().
///  */
/// ```
///
/// After:
/// ```java
/// /**
///  * @deprecated Use foo() instead.
///  *         Also see bar().
///  */
/// ```
public final class JavadocContinuationIndentationNormalizer
{
    private JavadocContinuationIndentationNormalizer() {}

    public static String normalize(String source)
    {
        SourceModel sourceModel = SourceModel.create(source);
        List<Replacement> replacements = collectReplacements(sourceModel);
        return Replacement.applyAll(source, replacements);
    }

    private static List<Replacement> collectReplacements(SourceModel sourceModel)
    {
        List<Replacement> replacements = new ArrayList<>();
        for (Comment comment : sourceModel.comments()) {
            if (!(comment instanceof Javadoc javadoc)) {
                continue;
            }
            replacements.addAll(javadocContinuationReplacements(sourceModel, javadoc));
        }
        return replacements;
    }

    private static List<Replacement> javadocContinuationReplacements(SourceModel sourceModel, Javadoc javadoc)
    {
        List<TagElement> tags = sourceModel.topLevelJavadocTags(javadoc);
        List<Replacement> replacements = new ArrayList<>();
        if (!tags.isEmpty()) {
            for (TagElement tag : tags) {
                if (sourceModel.isInsideJavadocPreBlock(javadoc, tag.getStartPosition())) {
                    continue;
                }
                if (tag.getTagName() == null) {
                    continue;
                }
                if ("@snippet".equals(tag.getTagName())) {
                    continue;
                }
                replacements.addAll(tagContinuationReplacements(sourceModel, javadoc, tag));
            }
        }
        replacements.addAll(paragraphAfterPreReplacements(sourceModel, javadoc.isMarkdown(), javadoc));
        return replacements;
    }

    private static List<Replacement> tagContinuationReplacements(SourceModel sourceModel, Javadoc javadoc, TagElement tag)
    {
        boolean markdown = javadoc.isMarkdown();
        int tagLine = sourceModel.lineNumber(tag.getStartPosition());
        int endLine = sourceModel.lineNumber(max(tag.getStartPosition(), tag.getStartPosition() + tag.getLength() - 1));
        if (endLine <= tagLine) {
            return List.of();
        }

        SourceModel.JavadocLineInfo tagLineInfo = sourceModel.javadocLineInfo(tagLine, markdown);
        if (tagLineInfo == null) {
            return List.of();
        }

        String lineIndent = sourceModel.leadingWhitespace(tagLineInfo.lineStart(), tagLineInfo.firstNonWhitespace());
        String continuationPrefix = lineIndent + (markdown ? "/// " : "* ") + " ".repeat(CONTINUATION_INDENT_SIZE);

        List<Replacement> replacements = new ArrayList<>();
        for (int line = tagLine + 1; line <= endLine; line++) {
            int lineStart = sourceModel.lineStartForLine(line);
            if (lineStart < 0) {
                continue;
            }
            // Preserve lines inside <pre> blocks verbatim — they're code
            // samples whose indentation carries meaning.
            if (sourceModel.isInsideJavadocPreBlock(javadoc, lineStart)) {
                continue;
            }
            int lineEnd = sourceModel.lineEnd(lineStart);
            int lineFirstNonWhitespace = sourceModel.firstNonWhitespace(lineStart, lineEnd);
            if (lineFirstNonWhitespace >= lineEnd) {
                continue;
            }

            SourceModel.JavadocLineInfo lineInfo = sourceModel.javadocLineInfo(line, markdown);
            int contentStart = (lineInfo == null) ? lineFirstNonWhitespace : lineInfo.contentStart();
            if (contentStart >= lineEnd) {
                continue;
            }

            if (sourceModel.source().substring(lineStart, contentStart).equals(continuationPrefix)) {
                continue;
            }
            replacements.add(new Replacement(lineStart, contentStart, continuationPrefix));
        }
        return replacements;
    }

    private static List<Replacement> paragraphAfterPreReplacements(SourceModel sourceModel, boolean markdown, Javadoc javadoc)
    {
        List<Replacement> replacements = new ArrayList<>();
        int startLine = sourceModel.lineNumber(javadoc.getStartPosition());
        int endLine = sourceModel.lineNumber(max(javadoc.getStartPosition(), javadoc.getStartPosition() + javadoc.getLength() - 1));
        boolean inPre = false;

        for (int line = startLine; line <= endLine; line++) {
            SourceModel.JavadocLineInfo lineInfo = sourceModel.javadocLineInfo(line, markdown);
            if (lineInfo == null || lineInfo.content().isEmpty()) {
                continue;
            }

            String trimmed = lineInfo.content().trim();
            if (trimmed.startsWith("<pre")) {
                inPre = true;
            }

            if (!inPre && trimmed.startsWith("<p>")) {
                String desiredPrefix = sourceModel.leadingWhitespace(lineInfo.lineStart(), lineInfo.firstNonWhitespace()) + (markdown ? "/// " : "* ");
                if (!sourceModel.source().substring(lineInfo.lineStart(), lineInfo.contentStart()).equals(desiredPrefix)) {
                    replacements.add(new Replacement(lineInfo.lineStart(), lineInfo.contentStart(), desiredPrefix));
                }
            }

            if (trimmed.contains("</pre>")) {
                inPre = false;
            }
        }
        return replacements;
    }
}
