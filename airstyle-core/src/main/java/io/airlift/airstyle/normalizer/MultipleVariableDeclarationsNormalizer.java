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
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import java.util.ArrayList;
import java.util.List;

/// Splits declarations with multiple variable fragments (`int a, b, c;`) into
/// one declaration per variable, so each variable owns its own line and
/// its own modifiers.
///
/// ### Example
///
/// Before:
/// ```java
/// private int width, height, depth = 0;
///
/// void run()
/// {
///     String first, second = "x", third;
/// }
/// ```
///
/// After:
/// ```java
/// private int width;
/// private int height;
/// private int depth = 0;
///
/// void run()
/// {
///     String first;
///     String second = "x";
///     String third;
/// }
/// ```
public final class MultipleVariableDeclarationsNormalizer
{
    private MultipleVariableDeclarationsNormalizer() {}

    public static String normalize(String source)
    {
        CompilationUnit compilationUnit = SourceModel.create(source).compilationUnit();
        ASTRewrite rewrite = ASTRewrite.create(compilationUnit.getAST());
        List<Runnable> edits = collectEdits(compilationUnit, rewrite);
        if (edits.isEmpty()) {
            return source;
        }

        edits.forEach(Runnable::run);

        return AstRewrites.apply(source, rewrite);
    }

    private static List<Runnable> collectEdits(CompilationUnit compilationUnit, ASTRewrite rewrite)
    {
        List<Runnable> edits = new ArrayList<>();
        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(FieldDeclaration node)
            {
                if (node.fragments().size() > 1) {
                    edits.add(() -> splitFieldDeclaration(node, rewrite));
                }
                return true;
            }

            @Override
            public boolean visit(VariableDeclarationStatement node)
            {
                if (node.fragments().size() > 1) {
                    edits.add(() -> splitVariableDeclarationStatement(node, rewrite));
                }
                return true;
            }
        });
        return edits;
    }

    private static void splitFieldDeclaration(FieldDeclaration declaration, ASTRewrite rewrite)
    {
        if (!(declaration.getLocationInParent() instanceof ChildListPropertyDescriptor location)) {
            return;
        }

        ListRewrite listRewrite = rewrite.getListRewrite(declaration.getParent(), location);
        AST ast = declaration.getAST();
        List<VariableDeclarationFragment> fragments = copyFragments(declaration.fragments());

        for (int index = 0; index < fragments.size(); index++) {
            VariableDeclarationFragment fragment = fragments.get(index);
            FieldDeclaration replacement = ast.newFieldDeclaration(copyFragment(ast, fragment));
            replacement.setType(copyType(ast, declaration.getType()));

            modifiers(replacement).addAll(copyModifiers(ast, declaration.modifiers()));
            if (index == 0 && declaration.getJavadoc() != null) {
                replacement.setJavadoc((Javadoc) ASTNode.copySubtree(ast, declaration.getJavadoc()));
            }

            listRewrite.insertBefore(replacement, declaration, null);
        }

        listRewrite.remove(declaration, null);
    }

    private static void splitVariableDeclarationStatement(VariableDeclarationStatement declaration, ASTRewrite rewrite)
    {
        if (!(declaration.getLocationInParent() instanceof ChildListPropertyDescriptor location)) {
            return;
        }

        ListRewrite listRewrite = rewrite.getListRewrite(declaration.getParent(), location);
        AST ast = declaration.getAST();
        List<VariableDeclarationFragment> fragments = copyFragments(declaration.fragments());

        for (VariableDeclarationFragment fragment : fragments) {
            VariableDeclarationStatement replacement = ast.newVariableDeclarationStatement(copyFragment(ast, fragment));
            replacement.setType(copyType(ast, declaration.getType()));
            modifiers(replacement).addAll(copyModifiers(ast, declaration.modifiers()));
            listRewrite.insertBefore(replacement, declaration, null);
        }

        listRewrite.remove(declaration, null);
    }

    private static VariableDeclarationFragment copyFragment(AST ast, VariableDeclarationFragment fragment)
    {
        return (VariableDeclarationFragment) ASTNode.copySubtree(ast, fragment);
    }

    private static List<VariableDeclarationFragment> copyFragments(List<?> fragments)
    {
        List<VariableDeclarationFragment> copies = new ArrayList<>(fragments.size());
        for (Object fragment : fragments) {
            copies.add((VariableDeclarationFragment) fragment);
        }
        return List.copyOf(copies);
    }

    private static List<IExtendedModifier> copyModifiers(AST ast, List<?> modifiers)
    {
        List<IExtendedModifier> copies = new ArrayList<>(modifiers.size());
        for (Object modifier : modifiers) {
            copies.add((IExtendedModifier) ASTNode.copySubtree(ast, (ASTNode) modifier));
        }
        return copies;
    }

    @SuppressWarnings("unchecked")
    private static List<IExtendedModifier> modifiers(FieldDeclaration declaration)
    {
        return declaration.modifiers();
    }

    @SuppressWarnings("unchecked")
    private static List<IExtendedModifier> modifiers(VariableDeclarationStatement declaration)
    {
        return declaration.modifiers();
    }

    private static Type copyType(AST ast, Type type)
    {
        return (Type) ASTNode.copySubtree(ast, type);
    }
}
