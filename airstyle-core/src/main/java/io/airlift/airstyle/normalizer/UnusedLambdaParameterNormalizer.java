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
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import java.util.ArrayList;
import java.util.List;

/// Rewrites unused lambda parameters to Java's unnamed variable `_`, matching
/// Checkstyle's `UnusedLambdaParameterShouldBeUnnamed` rule. The analysis is
/// deliberately syntactic and conservative: any matching simple name in the
/// lambda body keeps the parameter unchanged.
public final class UnusedLambdaParameterNormalizer
{
    private UnusedLambdaParameterNormalizer() {}

    public static String normalize(String source)
    {
        SourceModel sourceModel = SourceModel.create(source);
        CompilationUnit compilationUnit = sourceModel.compilationUnit();
        List<Replacement> replacements = new ArrayList<>();

        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(LambdaExpression node)
            {
                collect(node, replacements);
                return true;
            }
        });

        return Replacement.applyAll(source, replacements);
    }

    private static void collect(LambdaExpression lambda, List<Replacement> replacements)
    {
        ASTNode body = lambda.getBody();
        if (body == null) {
            return;
        }

        for (Object parameter : lambda.parameters()) {
            if (!(parameter instanceof VariableDeclaration declaration)) {
                continue;
            }
            SimpleName name = declaration.getName();
            if (name == null || name.getIdentifier().equals("_") || isUsed(body, name.getIdentifier())) {
                continue;
            }
            replacements.add(new Replacement(name.getStartPosition(), name.getStartPosition() + name.getLength(), "_"));
        }
    }

    private static boolean isUsed(ASTNode body, String name)
    {
        boolean[] used = {false};
        body.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(SimpleName node)
            {
                if (node.getIdentifier().equals(name)) {
                    used[0] = true;
                    return false;
                }
                return true;
            }
        });
        return used[0];
    }
}
