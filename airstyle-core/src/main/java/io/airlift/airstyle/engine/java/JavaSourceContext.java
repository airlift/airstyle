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

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;
import static java.util.Objects.requireNonNull;

final class JavaSourceContext
{
    private final String source;
    private final List<JavaTokens.Token> tokens;

    JavaSourceContext(String source, List<JavaTokens.Token> tokens)
    {
        this.source = requireNonNull(source, "source is null");
        this.tokens = List.copyOf(requireNonNull(tokens, "tokens is null"));
    }

    String source()
    {
        return source;
    }

    List<JavaTokens.Token> tokens()
    {
        return tokens;
    }

    List<JavaTokens.Token> tokensIn(int start, int end)
    {
        List<JavaTokens.Token> result = new ArrayList<>();
        for (JavaTokens.Token token : tokens) {
            if (token.start() >= end) {
                break;
            }
            if (token.end() > start && token.start() >= start) {
                result.add(token);
            }
        }
        return result;
    }

    boolean containsLineBreak(int start, int end)
    {
        int limit = Math.min(end, source.length());
        for (int i = max(0, start); i < limit; i++) {
            if (source.charAt(i) == '\n') {
                return true;
            }
        }
        return false;
    }

    boolean lineBreakBetween(JavaTokens.Token previous, JavaTokens.Token next)
    {
        return previous.text().endsWith("\n")
                || containsLineBreak(previous.end(), next.start());
    }

    int firstTokenAfterLineBreak(int start, int end)
    {
        JavaTokens.Token previous = null;
        for (JavaTokens.Token token : tokensIn(start, end)) {
            if (previous != null && lineBreakBetween(previous, token)) {
                return token.start();
            }
            previous = token;
        }
        return -1;
    }
}
