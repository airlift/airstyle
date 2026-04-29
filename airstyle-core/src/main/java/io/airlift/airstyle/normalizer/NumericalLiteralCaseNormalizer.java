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

/// Rewrites the non-digit parts of a numeric literal to lowercase form —
/// the `0x`/`0b` prefix, the exponent marker `e`/`p`, and the type suffix
/// `d`/`f`. Hex digits themselves are left to [HexLiteralCaseNormalizer],
/// and the `l` long suffix is left to [UpperEllNormalizer].
///
/// ### Example
///
/// Before:
/// ```java
/// double a = 1.5E10D;
/// int    b = 0X1F;
/// float  c = 3.14F;
/// ```
///
/// After:
/// ```java
/// double a = 1.5e10d;
/// int    b = 0x1F;
/// float  c = 3.14f;
/// ```
public final class NumericalLiteralCaseNormalizer
{
    private NumericalLiteralCaseNormalizer() {}

    public static String normalize(String source)
    {
        CompilationUnit compilationUnit = SourceModel.create(source).compilationUnit();
        ASTRewrite rewrite = ASTRewrite.create(compilationUnit.getAST());
        List<NumberLiteral> literals = collectNumericLiterals(compilationUnit);

        if (literals.isEmpty()) {
            return source;
        }

        for (NumberLiteral literal : literals) {
            String token = literal.getToken();
            String normalized = normalizeToken(token);
            if (!normalized.equals(token)) {
                rewrite.set(literal, NumberLiteral.TOKEN_PROPERTY, normalized, null);
            }
        }

        return AstRewrites.apply(source, rewrite);
    }

    private static List<NumberLiteral> collectNumericLiterals(CompilationUnit compilationUnit)
    {
        List<NumberLiteral> results = new ArrayList<>();
        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(NumberLiteral node)
            {
                results.add(node);
                return true;
            }
        });
        return results;
    }

    private static String normalizeToken(String token)
    {
        StringBuilder result = new StringBuilder(token);
        boolean isHex = token.startsWith("0x") || token.startsWith("0X");

        if (token.startsWith("0X")) {
            result.setCharAt(1, 'x');
        }
        else if (token.startsWith("0B")) {
            result.setCharAt(1, 'b');
        }

        if (isHex) {
            for (int i = 0; i < result.length(); i++) {
                if (result.charAt(i) == 'P') {
                    result.setCharAt(i, 'p');
                }
            }
        }
        else {
            for (int i = 0; i < result.length(); i++) {
                if (result.charAt(i) == 'E') {
                    result.setCharAt(i, 'e');
                }
            }
        }

        int suffixIndex = result.length() - 1;
        if (suffixIndex >= 0 && isUppercaseFloatingSuffix(result.charAt(suffixIndex))
                && (!isHex || containsHexExponent(result))) {
            result.setCharAt(suffixIndex, Character.toLowerCase(result.charAt(suffixIndex)));
        }

        return result.toString();
    }

    private static boolean containsHexExponent(CharSequence token)
    {
        for (int i = 0; i < token.length(); i++) {
            if (token.charAt(i) == 'p') {
                return true;
            }
        }
        return false;
    }

    private static boolean isUppercaseFloatingSuffix(char character)
    {
        return character == 'F' || character == 'D';
    }
}
