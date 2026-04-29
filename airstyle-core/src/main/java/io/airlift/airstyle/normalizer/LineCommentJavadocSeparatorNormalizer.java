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
import org.eclipse.jdt.core.dom.LineComment;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;

/// Inserts a blank line between a standalone `//` line comment and a Javadoc
/// block that immediately follows, so the descriptive comment does not appear
/// to be part of the API doc.
///
/// ### Example
///
/// Before:
/// ```java
/// // implementation note
/// /**
///  * Returns the value.
///  */
/// int value() { ... }
/// ```
///
/// After:
/// ```java
/// // implementation note
///
/// /**
///  * Returns the value.
///  */
/// int value() { ... }
/// ```
public final class LineCommentJavadocSeparatorNormalizer
{
    private LineCommentJavadocSeparatorNormalizer() {}

    public static String normalize(String source)
    {
        SourceModel sourceModel = SourceModel.create(source);
        List<Comment> comments = sourceModel.comments();
        if (comments.size() < 2) {
            return source;
        }

        List<Replacement> replacements = new ArrayList<>();
        for (int index = 1; index < comments.size(); index++) {
            if (!(comments.get(index) instanceof Javadoc javadoc) || !(comments.get(index - 1) instanceof LineComment lineComment)) {
                continue;
            }

            int javadocStart = javadoc.getStartPosition();
            int javadocStartLine = sourceModel.lineNumber(javadocStart);
            int javadocLineStart = sourceModel.lineStart(javadocStart);

            int lineCommentStart = lineComment.getStartPosition();
            int lineCommentLineStart = sourceModel.lineStart(lineCommentStart);
            int lineCommentLineEnd = sourceModel.lineEnd(lineCommentStart);
            int lineCommentFirstNonWhitespace = sourceModel.firstNonWhitespace(lineCommentLineStart, lineCommentLineEnd);
            if (lineCommentFirstNonWhitespace != lineCommentStart) {
                continue;
            }

            int lineCommentEnd = lineCommentStart + max(0, lineComment.getLength() - 1);
            if (sourceModel.lineNumber(lineCommentEnd) != javadocStartLine - 1) {
                continue;
            }

            replacements.add(new Replacement(javadocLineStart, javadocLineStart, "\n"));
        }
        return Replacement.applyAll(source, replacements);
    }
}
