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
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import java.util.ArrayList;
import java.util.List;

/// Removes stray `;` empty statements — standalone semicolons that carry no
/// effect and trip the Checkstyle `EmptyStatement` rule.
///
/// ### Example
///
/// Before:
/// ```java
/// void run()
/// {
///     doWork();;
///     ;
///     if (condition);
/// }
/// ```
///
/// After:
/// ```java
/// void run()
/// {
///     doWork();
///     if (condition) {
///     }
/// }
/// ```
public final class EmptyStatementNormalizer
{
    private EmptyStatementNormalizer() {}

    public static String normalize(String source)
    {
        CompilationUnit compilationUnit = SourceModel.create(source).compilationUnit();
        ASTRewrite rewrite = ASTRewrite.create(compilationUnit.getAST());
        List<EmptyStatement> emptyStatements = new ArrayList<>();

        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(EmptyStatement node)
            {
                emptyStatements.add(node);
                return true;
            }
        });

        if (emptyStatements.isEmpty()) {
            return source;
        }

        for (EmptyStatement emptyStatement : emptyStatements) {
            rewrite.remove(emptyStatement, null);
        }

        return AstRewrites.apply(source, rewrite);
    }
}
