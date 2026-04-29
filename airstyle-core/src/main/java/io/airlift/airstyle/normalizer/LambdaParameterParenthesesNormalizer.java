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
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import java.util.ArrayList;
import java.util.List;

/// Removes redundant parentheses from single-parameter lambdas when the
/// parameter has no declared type.
public final class LambdaParameterParenthesesNormalizer
{
    private LambdaParameterParenthesesNormalizer() {}

    public static String normalize(String source)
    {
        SourceModel sourceModel = SourceModel.create(source);
        CompilationUnit compilationUnit = sourceModel.compilationUnit();
        ASTRewrite rewrite = ASTRewrite.create(compilationUnit.getAST());
        List<Runnable> edits = new ArrayList<>();

        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(LambdaExpression node)
            {
                if (shouldRemoveParentheses(sourceModel, node)) {
                    edits.add(() -> rewrite.set(node, LambdaExpression.PARENTHESES_PROPERTY, false, null));
                }
                return true;
            }
        });

        if (edits.isEmpty()) {
            return source;
        }

        edits.forEach(Runnable::run);
        return AstRewrites.apply(source, rewrite);
    }

    private static boolean shouldRemoveParentheses(SourceModel sourceModel, LambdaExpression lambda)
    {
        if (!lambda.hasParentheses() || lambda.parameters().size() != 1) {
            return false;
        }

        if (!(lambda.parameters().getFirst() instanceof VariableDeclarationFragment parameter)) {
            return false;
        }

        int bodyStart = lambda.getBody().getStartPosition();
        if (!sourceModel.commentsContainedIn(lambda.getStartPosition(), bodyStart).isEmpty()) {
            return false;
        }

        return parameter.extraDimensions().isEmpty();
    }
}
