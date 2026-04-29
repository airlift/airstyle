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
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import java.util.ArrayList;
import java.util.List;

/// Normalizes C-style array declarations (brackets on the variable name) to
/// Java-style array declarations (brackets on the type).
///
/// ### Example
///
/// Before:
/// ```java
/// int values[] = {1, 2, 3};
/// String args[], more[];
/// ```
///
/// After:
/// ```java
/// int[] values = {1, 2, 3};
/// String[] args;
/// String[] more;
/// ```
public final class ArrayTypeStyleNormalizer
{
    private ArrayTypeStyleNormalizer() {}

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
                addVariableDeclarationEdit(node.getType(), node.fragments(), rewrite, edits, node, FieldDeclaration.TYPE_PROPERTY);
                return true;
            }

            @Override
            public boolean visit(VariableDeclarationStatement node)
            {
                addVariableDeclarationEdit(node.getType(), node.fragments(), rewrite, edits, node, VariableDeclarationStatement.TYPE_PROPERTY);
                return true;
            }

            @Override
            public boolean visit(VariableDeclarationExpression node)
            {
                addVariableDeclarationEdit(node.getType(), node.fragments(), rewrite, edits, node, VariableDeclarationExpression.TYPE_PROPERTY);
                return true;
            }

            @Override
            public boolean visit(SingleVariableDeclaration node)
            {
                int extraDimensions = node.extraDimensions().size();
                if (extraDimensions == 0) {
                    return true;
                }
                edits.add(() -> moveDimensionsToType(
                        node,
                        node.getType(),
                        extraDimensions,
                        rewrite,
                        SingleVariableDeclaration.TYPE_PROPERTY,
                        SingleVariableDeclaration.EXTRA_DIMENSIONS2_PROPERTY));
                return true;
            }

            @Override
            public boolean visit(MethodDeclaration node)
            {
                int extraDimensions = node.extraDimensions().size();
                if (extraDimensions == 0 || node.getReturnType2() == null) {
                    return true;
                }
                edits.add(() -> moveDimensionsToType(
                        node,
                        node.getReturnType2(),
                        extraDimensions,
                        rewrite,
                        MethodDeclaration.RETURN_TYPE2_PROPERTY,
                        MethodDeclaration.EXTRA_DIMENSIONS2_PROPERTY));
                return true;
            }
        });
        return edits;
    }

    private static void addVariableDeclarationEdit(
            Type declarationType,
            List<?> fragments,
            ASTRewrite rewrite,
            List<Runnable> edits,
            ASTNode declaration,
            StructuralPropertyDescriptor typeProperty)
    {
        if (fragments.size() != 1) {
            return;
        }
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragments.getFirst();
        int extraDimensions = fragment.extraDimensions().size();
        if (extraDimensions == 0) {
            return;
        }

        edits.add(() -> moveDimensionsToType(
                declaration,
                declarationType,
                extraDimensions,
                rewrite,
                typeProperty,
                fragment,
                VariableDeclarationFragment.EXTRA_DIMENSIONS2_PROPERTY));
    }

    private static void moveDimensionsToType(
            ASTNode declaration,
            Type originalType,
            int extraDimensions,
            ASTRewrite rewrite,
            StructuralPropertyDescriptor typeProperty,
            ASTNode dimensionsOwner,
            ChildListPropertyDescriptor dimensionsProperty)
    {
        Type replacementType = createTypeWithExtraDimensions(declaration.getAST(), originalType, extraDimensions);
        rewrite.set(declaration, typeProperty, replacementType, null);

        ListRewrite listRewrite = rewrite.getListRewrite(dimensionsOwner, dimensionsProperty);
        List<?> dimensions = dimensionsOwner instanceof VariableDeclarationFragment fragment
                ? fragment.extraDimensions()
                : dimensionsOwner instanceof SingleVariableDeclaration singleVariableDeclaration
                  ? singleVariableDeclaration.extraDimensions()
                  : ((MethodDeclaration) dimensionsOwner).extraDimensions();
        for (Object dimension : dimensions) {
            listRewrite.remove((ASTNode) dimension, null);
        }
    }

    private static void moveDimensionsToType(
            ASTNode declaration,
            Type originalType,
            int extraDimensions,
            ASTRewrite rewrite,
            StructuralPropertyDescriptor typeProperty,
            ChildListPropertyDescriptor dimensionsProperty)
    {
        moveDimensionsToType(
                declaration,
                originalType,
                extraDimensions,
                rewrite,
                typeProperty,
                declaration,
                dimensionsProperty);
    }

    private static Type createTypeWithExtraDimensions(AST ast, Type originalType, int extraDimensions)
    {
        Type copiedType = (Type) ASTNode.copySubtree(ast, originalType);
        if (copiedType instanceof ArrayType arrayType) {
            Type elementType = (Type) ASTNode.copySubtree(ast, arrayType.getElementType());
            return ast.newArrayType(elementType, arrayType.getDimensions() + extraDimensions);
        }
        return ast.newArrayType(copiedType, extraDimensions);
    }
}
