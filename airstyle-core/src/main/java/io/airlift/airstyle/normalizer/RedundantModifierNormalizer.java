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
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.ABSTRACT_KEYWORD;
import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.FINAL_KEYWORD;
import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.PUBLIC_KEYWORD;
import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.STATIC_KEYWORD;

/// Removes modifiers that are already implied by the enclosing declaration.
/// Interface methods are implicitly `public abstract`; interface fields are
/// implicitly `public static final`; annotation members are implicitly
/// `public abstract`.
///
/// ### Example
///
/// Before:
/// ```java
/// public interface Service
/// {
///     public static final int MAX = 100;
///     public abstract void run();
/// }
///
/// @interface Marker
/// {
///     public abstract String value();
/// }
/// ```
///
/// After:
/// ```java
/// public interface Service
/// {
///     int MAX = 100;
///     void run();
/// }
///
/// @interface Marker
/// {
///     String value();
/// }
/// ```
public final class RedundantModifierNormalizer
{
    private RedundantModifierNormalizer() {}

    private static final Set<Modifier.ModifierKeyword> INTERFACE_METHOD_MODIFIERS =
            Set.of(PUBLIC_KEYWORD, ABSTRACT_KEYWORD);
    private static final Set<Modifier.ModifierKeyword> INTERFACE_FIELD_MODIFIERS =
            Set.of(PUBLIC_KEYWORD, STATIC_KEYWORD, FINAL_KEYWORD);
    private static final Set<Modifier.ModifierKeyword> INTERFACE_TYPE_MODIFIERS =
            Set.of(PUBLIC_KEYWORD, STATIC_KEYWORD);

    public static String normalize(String source)
    {
        CompilationUnit compilationUnit = SourceModel.create(source).compilationUnit();
        ASTRewrite rewrite = ASTRewrite.create(compilationUnit.getAST());
        List<Modifier> redundantModifiers = collectRedundantModifiers(compilationUnit);

        if (redundantModifiers.isEmpty()) {
            return source;
        }

        for (Modifier modifier : redundantModifiers) {
            rewrite.remove(modifier, null);
        }

        return AstRewrites.apply(source, rewrite);
    }

    private static List<Modifier> collectRedundantModifiers(CompilationUnit compilationUnit)
    {
        List<Modifier> redundantModifiers = new ArrayList<>();
        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(TypeDeclaration node)
            {
                if (node.isInterface()) {
                    collectFromInterfaceLikeBody(node.bodyDeclarations(), redundantModifiers);
                }
                return true;
            }

            @Override
            public boolean visit(AnnotationTypeDeclaration node)
            {
                collectFromInterfaceLikeBody(node.bodyDeclarations(), redundantModifiers);
                return true;
            }
        });
        return redundantModifiers;
    }

    private static void collectFromInterfaceLikeBody(List<?> bodyDeclarations, List<Modifier> targets)
    {
        for (Object bodyDeclaration : bodyDeclarations) {
            BodyDeclaration declaration = (BodyDeclaration) bodyDeclaration;
            if (declaration instanceof MethodDeclaration) {
                collectDeclarationModifiers(declaration, INTERFACE_METHOD_MODIFIERS, targets);
                continue;
            }
            if (declaration instanceof AnnotationTypeMemberDeclaration) {
                collectDeclarationModifiers(declaration, INTERFACE_METHOD_MODIFIERS, targets);
                continue;
            }
            if (declaration instanceof FieldDeclaration) {
                collectDeclarationModifiers(declaration, INTERFACE_FIELD_MODIFIERS, targets);
                continue;
            }
            if (declaration instanceof TypeDeclaration typeDeclaration) {
                collectDeclarationModifiers(declaration, INTERFACE_TYPE_MODIFIERS, targets);
                if (typeDeclaration.isInterface()) {
                    collectDeclarationModifiers(declaration, Set.of(ABSTRACT_KEYWORD), targets);
                }
                continue;
            }
            if (declaration instanceof AnnotationTypeDeclaration || declaration instanceof RecordDeclaration) {
                collectDeclarationModifiers(declaration, INTERFACE_TYPE_MODIFIERS, targets);
            }
        }
    }

    private static void collectDeclarationModifiers(
            BodyDeclaration declaration,
            Set<Modifier.ModifierKeyword> keywords,
            List<Modifier> targets)
    {
        for (Object modifierObject : declaration.modifiers()) {
            IExtendedModifier modifier = (IExtendedModifier) modifierObject;
            if (!modifier.isModifier()) {
                continue;
            }
            Modifier concreteModifier = (Modifier) modifier;
            if (keywords.contains(concreteModifier.getKeyword())) {
                targets.add(concreteModifier);
            }
        }
    }
}
