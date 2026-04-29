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
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/// Reorders declaration modifiers to the canonical Java order: annotations
/// first, then `public`/`protected`/`private`, `abstract`, `default`,
/// `static`, `sealed`/`non-sealed`, `final`, `transient`, `volatile`,
/// `synchronized`, `native`, `strictfp`.
///
/// ### Example
///
/// Before:
/// ```java
/// final static public int MAX = 100;
/// synchronized public void run() {}
/// ```
///
/// After:
/// ```java
/// public static final int MAX = 100;
/// public synchronized void run() {}
/// ```
public final class ModifierOrderNormalizer
{
    private ModifierOrderNormalizer() {}

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
            public boolean visit(TypeDeclaration node)
            {
                collectModifierReorder(node, node.getModifiersProperty(), node.modifiers(), rewrite, edits);
                return true;
            }

            @Override
            public boolean visit(EnumDeclaration node)
            {
                collectModifierReorder(node, node.getModifiersProperty(), node.modifiers(), rewrite, edits);
                return true;
            }

            @Override
            public boolean visit(RecordDeclaration node)
            {
                collectModifierReorder(node, node.getModifiersProperty(), node.modifiers(), rewrite, edits);
                return true;
            }

            @Override
            public boolean visit(AnnotationTypeDeclaration node)
            {
                collectModifierReorder(node, node.getModifiersProperty(), node.modifiers(), rewrite, edits);
                return true;
            }

            @Override
            public boolean visit(AnnotationTypeMemberDeclaration node)
            {
                collectModifierReorder(node, node.getModifiersProperty(), node.modifiers(), rewrite, edits);
                return true;
            }

            @Override
            public boolean visit(FieldDeclaration node)
            {
                collectModifierReorder(node, node.getModifiersProperty(), node.modifiers(), rewrite, edits);
                return true;
            }

            @Override
            public boolean visit(MethodDeclaration node)
            {
                collectModifierReorder(node, node.getModifiersProperty(), node.modifiers(), rewrite, edits);
                return true;
            }

            @Override
            public boolean visit(Initializer node)
            {
                collectModifierReorder(node, node.getModifiersProperty(), node.modifiers(), rewrite, edits);
                return true;
            }

            @Override
            public boolean visit(EnumConstantDeclaration node)
            {
                collectModifierReorder(node, node.getModifiersProperty(), node.modifiers(), rewrite, edits);
                return true;
            }

            @Override
            public boolean visit(SingleVariableDeclaration node)
            {
                collectModifierReorder(node, SingleVariableDeclaration.MODIFIERS2_PROPERTY, node.modifiers(), rewrite, edits);
                return true;
            }

            @Override
            public boolean visit(VariableDeclarationStatement node)
            {
                collectModifierReorder(node, VariableDeclarationStatement.MODIFIERS2_PROPERTY, node.modifiers(), rewrite, edits);
                return true;
            }

            @Override
            public boolean visit(VariableDeclarationExpression node)
            {
                collectModifierReorder(node, VariableDeclarationExpression.MODIFIERS2_PROPERTY, node.modifiers(), rewrite, edits);
                return true;
            }
        });
        return edits;
    }

    private static void collectModifierReorder(
            ASTNode owner,
            ChildListPropertyDescriptor property,
            List<?> modifiers,
            ASTRewrite rewrite,
            List<Runnable> edits)
    {
        List<Modifier> modifierNodes = new ArrayList<>();
        for (Object modifierObject : modifiers) {
            IExtendedModifier modifier = (IExtendedModifier) modifierObject;
            if (modifier.isModifier()) {
                modifierNodes.add((Modifier) modifier);
            }
        }
        if (modifierNodes.size() < 2) {
            return;
        }

        List<Modifier> sorted = new ArrayList<>(modifierNodes);
        sorted.sort(Comparator.comparingInt(modifier -> rank(modifier.getKeyword())));
        if (sameOrder(modifierNodes, sorted)) {
            return;
        }

        edits.add(() -> applyReorder(owner, property, modifierNodes, sorted, rewrite));
    }

    private static void applyReorder(
            ASTNode owner,
            ChildListPropertyDescriptor property,
            List<Modifier> originalOrder,
            List<Modifier> sortedOrder,
            ASTRewrite rewrite)
    {
        ListRewrite listRewrite = rewrite.getListRewrite(owner, property);
        AST ast = owner.getAST();
        for (Modifier modifier : originalOrder) {
            listRewrite.remove(modifier, null);
        }
        for (Modifier modifier : sortedOrder) {
            listRewrite.insertLast(ASTNode.copySubtree(ast, modifier), null);
        }
    }

    private static boolean sameOrder(List<Modifier> left, List<Modifier> right)
    {
        for (int index = 0; index < left.size(); index++) {
            if (!left.get(index).equals(right.get(index))) {
                return false;
            }
        }
        return true;
    }

    private static int rank(Modifier.ModifierKeyword keyword)
    {
        if (keyword == Modifier.ModifierKeyword.PUBLIC_KEYWORD) {
            return 1;
        }
        if (keyword == Modifier.ModifierKeyword.PROTECTED_KEYWORD) {
            return 2;
        }
        if (keyword == Modifier.ModifierKeyword.PRIVATE_KEYWORD) {
            return 3;
        }
        if (keyword == Modifier.ModifierKeyword.ABSTRACT_KEYWORD) {
            return 4;
        }
        if (keyword == Modifier.ModifierKeyword.DEFAULT_KEYWORD) {
            return 5;
        }
        if (keyword == Modifier.ModifierKeyword.STATIC_KEYWORD) {
            return 6;
        }
        if (keyword == Modifier.ModifierKeyword.SEALED_KEYWORD) {
            return 7;
        }
        if (keyword == Modifier.ModifierKeyword.NON_SEALED_KEYWORD) {
            return 8;
        }
        if (keyword == Modifier.ModifierKeyword.FINAL_KEYWORD) {
            return 9;
        }
        if (keyword == Modifier.ModifierKeyword.TRANSIENT_KEYWORD) {
            return 10;
        }
        if (keyword == Modifier.ModifierKeyword.VOLATILE_KEYWORD) {
            return 11;
        }
        if (keyword == Modifier.ModifierKeyword.SYNCHRONIZED_KEYWORD) {
            return 12;
        }
        if (keyword == Modifier.ModifierKeyword.NATIVE_KEYWORD) {
            return 13;
        }
        if (keyword == Modifier.ModifierKeyword.STRICTFP_KEYWORD) {
            return 14;
        }
        return 100;
    }
}
