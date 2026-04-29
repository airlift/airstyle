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

import io.airlift.airstyle.JavaLanguageSupport;
import io.airlift.airstyle.model.SourceModel;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/// Normalizes spacing around trailing line and block comments on a source
/// line. Ensures the comment is separated from the preceding code by a
/// single space, preserves multi-space column alignment when one is already
/// present, and leaves `// noinspection` suppressor comments untouched.
///
/// ### Example
///
/// Before:
/// ```java
/// int a = 1;// first
/// int b = 2;  //second
/// int c = 3;/*trailing*/
/// ```
///
/// After:
/// ```java
/// int a = 1; // first
/// int b = 2;  // second
/// int c = 3; /*trailing*/
/// ```
public final class TrailingCommentSpacingNormalizer
{
    private TrailingCommentSpacingNormalizer() {}

    private static final String NOINSPECTION_DIRECTIVE = "noinspection";

    public static String normalize(String source)
    {
        SourceModel sourceModel = SourceModel.create(source);
        List<Replacement> replacements = new ArrayList<>();
        IScanner scanner = ToolFactory.createScanner(
                true,
                false,
                false,
                JavaLanguageSupport.latestJavaVersion(),
                JavaLanguageSupport.latestJavaVersion(),
                true);
        scanner.setSource(source.toCharArray());

        try {
            while (true) {
                int token = scanner.getNextToken();
                if (token == ITerminalSymbols.TokenNameEOF) {
                    break;
                }
                if (token != ITerminalSymbols.TokenNameCOMMENT_LINE) {
                    continue;
                }

                int commentStart = scanner.getCurrentTokenStartPosition();
                int commentEnd = scanner.getCurrentTokenEndPosition();
                boolean trailingComment = isTrailingComment(sourceModel, commentStart);
                boolean firstColumnComment = isFirstColumnComment(sourceModel, commentStart);
                if (!trailingComment && firstColumnComment) {
                    continue;
                }

                if (trailingComment && commentStart > 0 && !isHorizontalWhitespace(source.charAt(commentStart - 1))) {
                    replacements.add(new Replacement(commentStart, commentStart, " "));
                }

                int firstCommentChar = commentStart + 2;
                if (firstCommentChar <= commentEnd) {
                    char first = source.charAt(firstCommentChar);
                    if (!isHorizontalWhitespace(first)
                            && !isLineBreak(first)
                            && first != '/'
                            && !startsWithNoinspectionDirective(source, firstCommentChar, commentEnd)) {
                        replacements.add(new Replacement(firstCommentChar, firstCommentChar, " "));
                    }
                }
            }
        }
        catch (InvalidInputException _) {
            return source;
        }

        if (replacements.isEmpty()) {
            return source;
        }
        replacements.sort(Comparator.comparingInt(Replacement::start).reversed());
        return applyReplacements(source, replacements);
    }

    private static boolean isTrailingComment(SourceModel sourceModel, int commentStart)
    {
        String source = sourceModel.source();
        int lineStart = sourceModel.lineStart(commentStart);
        for (int i = lineStart; i < commentStart; i++) {
            if (!isHorizontalWhitespace(source.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean startsWithNoinspectionDirective(String source, int contentStart, int commentEnd)
    {
        if (contentStart < 0 || contentStart >= source.length()) {
            return false;
        }
        if (contentStart + NOINSPECTION_DIRECTIVE.length() - 1 > commentEnd) {
            return false;
        }
        for (int i = 0; i < NOINSPECTION_DIRECTIVE.length(); i++) {
            if (source.charAt(contentStart + i) != NOINSPECTION_DIRECTIVE.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isFirstColumnComment(SourceModel sourceModel, int commentStart)
    {
        return sourceModel.lineStart(commentStart) == commentStart;
    }

    private static boolean isHorizontalWhitespace(char ch)
    {
        return ch == ' ' || ch == '\t';
    }

    private static boolean isLineBreak(char ch)
    {
        return ch == '\n' || ch == '\r';
    }

    private static String applyReplacements(String source, List<Replacement> replacements)
    {
        StringBuilder builder = new StringBuilder(source);
        for (Replacement replacement : replacements) {
            builder.replace(replacement.start(), replacement.end(), replacement.value());
        }
        return builder.toString();
    }
}
