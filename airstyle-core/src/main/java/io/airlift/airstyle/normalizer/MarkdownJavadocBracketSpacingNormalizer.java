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

import static java.lang.Math.max;

/// Normalizes bracketed comma-separated spans inside markdown doc comments to
/// IntelliJ's compact form: `[from, to]` becomes `[from,to]`.
///
/// Applies only to markdown doc comment description lines and skips
/// protected markdown content such as escaped brackets, inline code, and
/// fenced code blocks.
public final class MarkdownJavadocBracketSpacingNormalizer
{
    private MarkdownJavadocBracketSpacingNormalizer() {}

    public static String normalize(String source)
    {
        SourceModel sourceModel = SourceModel.create(source);
        List<Replacement> replacements = new ArrayList<>();
        for (Comment comment : sourceModel.comments()) {
            if (!(comment instanceof Javadoc javadoc) || !javadoc.isMarkdown()) {
                continue;
            }
            replacements.addAll(bracketSpacingReplacements(sourceModel, javadoc));
        }
        return Replacement.applyAll(source, replacements);
    }

    private static List<Replacement> bracketSpacingReplacements(SourceModel sourceModel, Javadoc javadoc)
    {
        List<Replacement> replacements = new ArrayList<>();
        int startLine = sourceModel.lineNumber(javadoc.getStartPosition());
        int endLine = sourceModel.lineNumber(max(javadoc.getStartPosition(), javadoc.getStartPosition() + javadoc.getLength() - 1));
        for (int line = startLine; line <= endLine; line++) {
            SourceModel.JavadocLineInfo lineInfo = sourceModel.javadocLineInfo(line, true);
            if (lineInfo == null || lineInfo.content().isEmpty()) {
                continue;
            }
            if (sourceModel.isInsideJavadocPreBlock(javadoc, lineInfo.contentStart())) {
                continue;
            }

            String updated = compactBracketSpacing(lineInfo.content());
            if (!updated.equals(lineInfo.content())) {
                replacements.add(new Replacement(
                        lineInfo.contentStart(),
                        lineInfo.contentStart() + lineInfo.content().length(),
                        updated));
            }
        }
        return replacements;
    }

    private static String compactBracketSpacing(String content)
    {
        StringBuilder result = new StringBuilder(content.length());
        int index = 0;
        while (index < content.length()) {
            char current = content.charAt(index);
            if (current == '\\' && index + 1 < content.length()) {
                result.append(current);
                result.append(content.charAt(index + 1));
                index += 2;
                continue;
            }
            if (current == '`') {
                int fenceLength = countBackticks(content, index);
                int close = findClosingInlineCodeFence(content, index + fenceLength, fenceLength);
                if (close >= 0) {
                    result.append(content, index, close + fenceLength);
                    index = close + fenceLength;
                    continue;
                }
            }
            if (current != '[') {
                result.append(current);
                index++;
                continue;
            }

            int close = findClosingBracket(content, index + 1);
            if (close < 0 || isProtectedMarkdownBracket(content, close)) {
                result.append(current);
                index++;
                continue;
            }

            result.append('[');
            appendCompactBracketContent(result, content, index + 1, close);
            result.append(']');
            index = close + 1;
        }
        return result.toString();
    }

    private static int countBackticks(String content, int start)
    {
        int index = start;
        while (index < content.length() && content.charAt(index) == '`') {
            index++;
        }
        return index - start;
    }

    private static boolean isProtectedMarkdownBracket(String content, int close)
    {
        if (close + 1 >= content.length()) {
            return false;
        }
        char next = content.charAt(close + 1);
        return next == '(' || next == '[' || next == ':';
    }

    private static int findClosingBracket(String content, int start)
    {
        for (int index = start; index < content.length(); index++) {
            char current = content.charAt(index);
            if (current == '\\' && index + 1 < content.length()) {
                index++;
                continue;
            }
            if (current == ']') {
                return index;
            }
            if (current == '[') {
                return -1;
            }
        }
        return -1;
    }

    private static int findClosingInlineCodeFence(String content, int start, int fenceLength)
    {
        for (int index = start; index < content.length(); index++) {
            if (content.charAt(index) == '\\' && index + 1 < content.length()) {
                index++;
                continue;
            }
            if (matchesBacktickFence(content, index, fenceLength)) {
                return index;
            }
        }
        return -1;
    }

    private static boolean matchesBacktickFence(String content, int start, int fenceLength)
    {
        if (start + fenceLength > content.length()) {
            return false;
        }
        for (int index = start; index < start + fenceLength; index++) {
            if (content.charAt(index) != '`') {
                return false;
            }
        }
        return true;
    }

    private static void appendCompactBracketContent(StringBuilder result, String content, int start, int end)
    {
        int index = start;
        while (index < end) {
            char current = content.charAt(index);
            result.append(current);
            if (current == ',') {
                index++;
                while (index < end && (content.charAt(index) == ' ' || content.charAt(index) == '\t')) {
                    index++;
                }
                continue;
            }
            index++;
        }
    }
}
