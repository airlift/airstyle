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
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import java.util.ArrayList;
import java.util.List;

/// Rewrites the hex digits `A`–`F` inside hexadecimal numeric literals to
/// uppercase so `0x` literals look uniform. The `0x` prefix itself and any
/// type suffix (`L`, `l`) are left alone.
///
/// ### Example
///
/// Before:
/// ```java
/// int mask = 0xff;
/// long sig = 0xcafebabel;
/// ```
///
/// After:
/// ```java
/// int mask = 0xFF;
/// long sig = 0xCAFEBABEl;
/// ```
public final class HexLiteralCaseNormalizer
{
    private HexLiteralCaseNormalizer() {}

    public static String normalize(String source)
    {
        CompilationUnit compilationUnit = SourceModel.create(source).compilationUnit();
        ASTRewrite rewrite = ASTRewrite.create(compilationUnit.getAST());
        List<NumberLiteral> hexLiterals = collectHexLiterals(compilationUnit);

        if (hexLiterals.isEmpty()) {
            return source;
        }

        for (NumberLiteral literal : hexLiterals) {
            String token = literal.getToken();
            String normalized = normalizeHexDigits(token);
            if (!normalized.equals(token)) {
                rewrite.set(literal, NumberLiteral.TOKEN_PROPERTY, normalized, null);
            }
        }

        return AstRewrites.apply(source, rewrite);
    }

    private static List<NumberLiteral> collectHexLiterals(CompilationUnit compilationUnit)
    {
        List<NumberLiteral> results = new ArrayList<>();
        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(NumberLiteral node)
            {
                String token = node.getToken();
                if (token.startsWith("0x") || token.startsWith("0X")) {
                    results.add(node);
                }
                return true;
            }
        });
        return results;
    }

    private static String normalizeHexDigits(String token)
    {
        int limit = token.length();
        char last = token.charAt(token.length() - 1);
        if ((last == 'f' || last == 'F' || last == 'd' || last == 'D') && isHexFloatingLiteral(token)) {
            limit--;
        }

        char[] chars = token.toCharArray();
        for (int i = 0; i < limit; i++) {
            char current = chars[i];
            if (current >= 'a' && current <= 'f') {
                chars[i] = Character.toUpperCase(current);
            }
        }
        return new String(chars);
    }

    private static boolean isHexFloatingLiteral(String token)
    {
        return token.indexOf('p') >= 0 || token.indexOf('P') >= 0;
    }
}
