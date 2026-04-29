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

import java.util.ArrayList;
import java.util.List;

/// Expands classic one-line Javadoc comments when IntelliJ's
/// `JD_DO_NOT_WRAP_ONE_LINE_COMMENTS` style option is disabled.
public final class OneLineJavadocNormalizer
{
    private OneLineJavadocNormalizer() {}

    public static String normalize(String source)
    {
        SourceModel sourceModel = SourceModel.create(source);
        List<Replacement> replacements = new ArrayList<>();
        for (Comment comment : sourceModel.comments()) {
            if (comment instanceof Javadoc javadoc) {
                Replacement replacement = replacement(sourceModel, javadoc);
                if (replacement != null) {
                    replacements.add(replacement);
                }
            }
        }
        return Replacement.applyAll(source, replacements);
    }

    private static Replacement replacement(SourceModel sourceModel, Javadoc javadoc)
    {
        if (javadoc.isMarkdown()) {
            return null;
        }

        int start = javadoc.getStartPosition();
        int end = start + javadoc.getLength();
        String source = sourceModel.source();
        if (start < 0 || end > source.length() || sourceModel.containsLineBreak(start, end)) {
            return null;
        }
        if (!sourceModel.startsAtLineIndent(start)) {
            return null;
        }

        String text = source.substring(start, end);
        if (!text.startsWith("/**") || !text.endsWith("*/")) {
            return null;
        }

        String body = text.substring(3, text.length() - 2).trim();
        if (body.isEmpty()) {
            return null;
        }

        int lineStart = sourceModel.lineStart(start);
        int lineEnd = sourceModel.lineEnd(start);
        int nextTokenStart = sourceModel.firstNonWhitespace(end, lineEnd);
        boolean codeAfterComment = nextTokenStart < lineEnd;
        String indent = sourceModel.lineIndent(lineStart);
        String replacement = indent + "/**\n"
                + indent + " * " + body + "\n"
                + indent + " */"
                + (codeAfterComment ? "\n" : "");
        return new Replacement(lineStart, codeAfterComment ? nextTokenStart : end, replacement);
    }
}
