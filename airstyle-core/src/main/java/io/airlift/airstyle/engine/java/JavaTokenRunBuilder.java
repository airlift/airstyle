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
import io.airlift.airstyle.engine.Spacing;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;

import java.util.List;

final class JavaTokenRunBuilder
{
    private final JavaSourceContext sourceContext;
    private final JavaSpacingPolicy spacingPolicy;

    JavaTokenRunBuilder(JavaSourceContext sourceContext, JavaSpacingPolicy spacingPolicy)
    {
        this.sourceContext = sourceContext;
        this.spacingPolicy = spacingPolicy;
    }

    Block buildTokensRange(int start, int end, String debugName)
    {
        return buildTokensRange(start, end, debugName, true);
    }

    Block buildTokensRange(int start, int end, String debugName, boolean canUseFirstChildIndent)
    {
        return buildTokenRun(JavaSourceRange.leaf(debugName, start, end), debugName, canUseFirstChildIndent);
    }

    Block buildTokensRangeWithLineStartIndent(int start, int end, String debugName, Indent lineStartIndent)
    {
        return buildTokenRun(JavaSourceRange.leaf(debugName, start, end), debugName, true, lineStartIndent);
    }

    Block buildLeafTokenRun(JavaSourceRange range, String debugName)
    {
        return buildTokenRun(range, debugName, true);
    }

    Block buildLeafTokenRun(JavaSourceRange range, String debugName, boolean canUseFirstChildIndent)
    {
        return buildTokenRun(range, debugName, canUseFirstChildIndent);
    }

    private Block buildTokenRun(JavaSourceRange range, String debugName, boolean canUseFirstChildIndent)
    {
        return buildTokenRun(range, debugName, canUseFirstChildIndent, null);
    }

    private Block buildTokenRun(JavaSourceRange range, String debugName, boolean canUseFirstChildIndent, Indent lineStartIndent)
    {
        JavaBlock.Builder composite = JavaBlock.builder(range.start(), range.end(), debugName);
        if (!canUseFirstChildIndent) {
            composite.canUseFirstChildIndent(false);
        }
        List<JavaTokens.Token> tokens = sourceContext.tokensIn(range.start(), range.end());
        Block prev = null;
        JavaTokens.Token prevToken = null;
        int prevType = -1;
        int prevStart = -1;
        int prevEnd = -1;
        int prevNonCommentType = -1;
        int prevPrevNonCommentType = -1;
        int prevNonCommentStart = -1;
        int prevPrevNonCommentStart = -1;
        for (JavaTokens.Token token : tokens) {
            boolean startsLine = prevToken == null || sourceContext.lineBreakBetween(prevToken, token);
            Indent leafIndent = startsLine ? lineStartIndent : null;
            Block leaf = leafFor(token, leafIndent);
            if (prev != null) {
                Spacing spacing = spacingPolicy.between(
                        prevType,
                        prevStart,
                        prevEnd,
                        token,
                        prevPrevNonCommentType,
                        prevPrevNonCommentStart);
                composite.spacing(prev, leaf, spacing);
            }
            composite.child(leaf);
            prevToken = token;
            prev = leaf;
            prevType = token.type();
            prevStart = token.start();
            prevEnd = token.end();
            if (!token.isComment()) {
                prevPrevNonCommentType = prevNonCommentType;
                prevPrevNonCommentStart = prevNonCommentStart;
                prevNonCommentType = token.type();
                prevNonCommentStart = token.start();
            }
        }
        return composite.build();
    }

    private static Block leafFor(JavaTokens.Token token, Indent indent)
    {
        boolean trailingNewlineComment = token.isComment()
                && (token.type() == ITerminalSymbols.TokenNameCOMMENT_LINE
                || token.type() == ITerminalSymbols.TokenNameCOMMENT_MARKDOWN);
        if (trailingNewlineComment && token.text().endsWith("\n")) {
            return leaf(token.start(), token.end() - 1, token.text().substring(0, token.text().length() - 1), indent);
        }
        return leaf(token.start(), token.end(), token.text(), indent);
    }

    private static Block leaf(int start, int end, String debugName, Indent indent)
    {
        JavaBlock.Builder leaf = JavaBlock.builder(start, end, debugName);
        if (indent != null) {
            leaf.indent(indent);
        }
        return leaf.build();
    }
}
