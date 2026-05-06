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

import io.airlift.airstyle.engine.Block;
import io.airlift.airstyle.engine.Indent;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;

import java.util.List;

final class JavaCommentBlocks
{
    private JavaCommentBlocks() {}

    static int renderEnd(JavaTokens.Token token)
    {
        return token.text().endsWith("\n") ? token.end() - 1 : token.end();
    }

    static Block leaf(JavaTokens.Token token)
    {
        int commentEnd = token.end();
        String text = token.text();
        if (hasTrailingNewline(token) && text.endsWith("\n")) {
            commentEnd--;
            text = text.substring(0, text.length() - 1);
        }
        return JavaBlock.leaf(token.start(), commentEnd, text);
    }

    static Indent indent(String source, JavaTokens.Token token, Indent defaultIndent)
    {
        if (token.type() == ITerminalSymbols.TokenNameCOMMENT_MARKDOWN) {
            return defaultIndent;
        }
        return columnOf(source, token.start()) == 0 ? Indent.absoluteNoneIndent() : defaultIndent;
    }

    static JavaTokens.Token trailingInlineComment(List<JavaTokens.Token> tokens, String source, int itemEnd, int rangeEnd)
    {
        for (JavaTokens.Token token : tokens) {
            if (token.start() >= rangeEnd) {
                break;
            }
            if (token.start() >= itemEnd && token.isComment()
                    && (token.type() == ITerminalSymbols.TokenNameCOMMENT_LINE
                    || token.type() == ITerminalSymbols.TokenNameCOMMENT_BLOCK)) {
                if (!containsLineBreak(source, itemEnd, token.start())) {
                    return token;
                }
                break;
            }
            if (token.start() >= itemEnd && !token.isComment()) {
                break;
            }
        }
        return null;
    }

    static boolean containsLineBreak(String source, int start, int end)
    {
        int limit = Math.min(end, source.length());
        for (int index = Math.max(0, start); index < limit; index++) {
            if (source.charAt(index) == '\n') {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTrailingNewline(JavaTokens.Token token)
    {
        return token.type() == ITerminalSymbols.TokenNameCOMMENT_LINE
                || token.type() == ITerminalSymbols.TokenNameCOMMENT_MARKDOWN;
    }

    private static int columnOf(String source, int offset)
    {
        int column = 0;
        for (int index = offset - 1; index >= 0; index--) {
            if (source.charAt(index) == '\n') {
                break;
            }
            column++;
        }
        return column;
    }
}
