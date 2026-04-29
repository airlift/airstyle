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
package io.airlift.airstyle.engine.java;

import io.airlift.airstyle.JavaLanguageSupport;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;

/// Tokenizes a Java source string via the JDT scanner, producing the list of
/// non-whitespace tokens. The engine's block builder needs per-token leaf
/// granularity — the AST alone doesn't expose token boundaries.
public final class JavaTokens
{
    private JavaTokens() {}

    public record Token(int start, int end, int type, String text)
    {
        public boolean isComment()
        {
            return type == ITerminalSymbols.TokenNameCOMMENT_LINE
                    || type == ITerminalSymbols.TokenNameCOMMENT_BLOCK
                    || type == ITerminalSymbols.TokenNameCOMMENT_JAVADOC
                    || type == ITerminalSymbols.TokenNameCOMMENT_MARKDOWN;
        }
    }

    public static List<Token> scan(String source)
    {
        IScanner scanner = newScanner();
        scanner.setSource(source.toCharArray());
        List<Token> tokens = new ArrayList<>();
        try {
            while (true) {
                int type = scanner.getNextToken();
                if (type == ITerminalSymbols.TokenNameEOF) {
                    break;
                }
                int start = scanner.getCurrentTokenStartPosition();
                int end = scanner.getCurrentTokenEndPosition() + 1;
                String text = source.substring(max(0, start), Math.min(end, source.length()));
                // Markdown javadoc (`///`) is emitted as a single multi-line
                // token by the JDT scanner. Split it into one token per line
                // so each `///` line can be re-indented independently.
                if (type == ITerminalSymbols.TokenNameCOMMENT_MARKDOWN && text.contains("\n")) {
                    splitMarkdownToken(start, text, tokens, source);
                }
                else {
                    tokens.add(new Token(start, end, type, text));
                }
            }
        }
        catch (InvalidInputException e) {
            // Scanner hit a malformed token — stop here; callers fall back.
        }
        return tokens;
    }

    private static void splitMarkdownToken(int tokenStart, String text, List<Token> tokens, String source)
    {
        int cursor = 0;
        int len = text.length();
        while (cursor < len) {
            int newlineAt = text.indexOf('\n', cursor);
            int lineEnd = newlineAt < 0 ? len : newlineAt + 1; // include trailing \n
            // Skip leading spaces/tabs on the line so the token starts at
            // the first `/` character; the engine's indent computation will
            // place the line at the right column.
            int lineStart = cursor;
            while (lineStart < lineEnd && (text.charAt(lineStart) == ' ' || text.charAt(lineStart) == '\t')) {
                lineStart++;
            }
            if (lineStart < lineEnd) {
                int absStart = tokenStart + lineStart;
                int absEnd = tokenStart + lineEnd;
                tokens.add(new Token(
                        absStart,
                        absEnd,
                        ITerminalSymbols.TokenNameCOMMENT_MARKDOWN,
                        source.substring(absStart, absEnd)));
            }
            cursor = lineEnd;
        }
    }

    private static IScanner newScanner()
    {
        // Tokenize comments (tokenizeComments=true) so the block builder can
        // emit them as leaves and preserve copyright headers / doc comments.
        return ToolFactory.createScanner(
                true,
                false,
                false,
                JavaLanguageSupport.latestJavaVersion(),
                JavaLanguageSupport.latestJavaVersion(),
                true);
    }
}
