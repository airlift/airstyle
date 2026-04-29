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

/// Rewrites `long` literal suffixes from lowercase `l` to uppercase `L` so the
/// suffix is not visually confused with the digit `1`.
///
/// ### Example
///
/// Before:
/// ```java
/// long x = 10l;
/// long y = 0xFFl;
/// ```
///
/// After:
/// ```java
/// long x = 10L;
/// long y = 0xFFL;
/// ```
public final class UpperEllNormalizer
{
    private UpperEllNormalizer() {}

    public static String normalize(String source)
    {
        CompilationUnit compilationUnit = SourceModel.create(source).compilationUnit();
        ASTRewrite rewrite = ASTRewrite.create(compilationUnit.getAST());
        List<NumberLiteral> lowercaseLongLiterals = collectLowercaseLongLiterals(compilationUnit);

        if (lowercaseLongLiterals.isEmpty()) {
            return source;
        }

        for (NumberLiteral literal : lowercaseLongLiterals) {
            String token = literal.getToken();
            String normalized = token.substring(0, token.length() - 1) + 'L';
            rewrite.set(literal, NumberLiteral.TOKEN_PROPERTY, normalized, null);
        }

        return AstRewrites.apply(source, rewrite);
    }

    private static List<NumberLiteral> collectLowercaseLongLiterals(CompilationUnit compilationUnit)
    {
        List<NumberLiteral> results = new ArrayList<>();
        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(NumberLiteral node)
            {
                String token = node.getToken();
                if (token.endsWith("l")) {
                    results.add(node);
                }
                return true;
            }
        });
        return results;
    }
}
