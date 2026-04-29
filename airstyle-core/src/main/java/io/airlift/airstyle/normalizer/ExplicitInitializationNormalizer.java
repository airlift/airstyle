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
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import java.util.ArrayList;
import java.util.List;

/// Removes explicit field initialization to Java's default values — `0` for
/// numeric primitives, `false` for `boolean`, `null` for references. The JVM
/// already assigns these defaults, so the initializer is noise.
///
/// ### Example
///
/// Before:
/// ```java
/// class Counter
/// {
///     private int count = 0;
///     private boolean active = false;
///     private String name = null;
/// }
/// ```
///
/// After:
/// ```java
/// class Counter
/// {
///     private int count;
///     private boolean active;
///     private String name;
/// }
/// ```
public final class ExplicitInitializationNormalizer
{
    private ExplicitInitializationNormalizer() {}

    public static String normalize(String source)
    {
        CompilationUnit compilationUnit = SourceModel.create(source).compilationUnit();
        ASTRewrite rewrite = ASTRewrite.create(compilationUnit.getAST());
        List<Expression> redundantInitializers = collectRedundantInitializers(compilationUnit);
        if (redundantInitializers.isEmpty()) {
            return source;
        }

        for (Expression initializer : redundantInitializers) {
            rewrite.remove(initializer, null);
        }

        return AstRewrites.apply(source, rewrite);
    }

    private static List<Expression> collectRedundantInitializers(CompilationUnit compilationUnit)
    {
        List<Expression> redundantInitializers = new ArrayList<>();
        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(FieldDeclaration node)
            {
                if (isFinalField(node) || isImplicitlyFinalField(node)) {
                    return true;
                }

                for (Object fragmentObject : node.fragments()) {
                    VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragmentObject;
                    Expression initializer = fragment.getInitializer();
                    if (initializer == null) {
                        continue;
                    }
                    if (isDefaultInitializer(node.getType(), fragment.extraDimensions().size(), initializer)) {
                        redundantInitializers.add(initializer);
                    }
                }
                return true;
            }
        });
        return redundantInitializers;
    }

    private static boolean isFinalField(FieldDeclaration node)
    {
        for (Object modifierObject : node.modifiers()) {
            if (!(modifierObject instanceof Modifier modifier)) {
                continue;
            }
            if (modifier.getKeyword() == Modifier.ModifierKeyword.FINAL_KEYWORD) {
                return true;
            }
        }
        return false;
    }

    /// Interface and annotation-type fields are implicitly `public static final`
    /// (JLS §9.3), which means the initializer is mandatory — stripping
    /// it produces uncompilable code.
    private static boolean isImplicitlyFinalField(FieldDeclaration node)
    {
        ASTNode parent = node.getParent();
        if (parent instanceof TypeDeclaration type) {
            return type.isInterface();
        }
        return parent instanceof AnnotationTypeDeclaration;
    }

    private static boolean isDefaultInitializer(Type type, int fragmentExtraDimensions, Expression initializer)
    {
        if (fragmentExtraDimensions > 0 || type.isArrayType()) {
            return initializer instanceof NullLiteral;
        }
        if (!type.isPrimitiveType()) {
            return initializer instanceof NullLiteral;
        }

        PrimitiveType.Code primitive = ((PrimitiveType) type).getPrimitiveTypeCode();
        return switch (primitive.toString()) {
            case "boolean" -> initializer instanceof BooleanLiteral booleanLiteral && !booleanLiteral.booleanValue();
            case "char" -> initializer instanceof CharacterLiteral characterLiteral && characterLiteral.charValue() == '\0';
            case "byte", "short", "int", "long" -> initializer instanceof NumberLiteral numberLiteral && isIntegerZero(numberLiteral.getToken());
            case "float", "double" -> initializer instanceof NumberLiteral numberLiteral && isFloatingPointZero(numberLiteral.getToken());
            default -> false;
        };
    }

    private static boolean isIntegerZero(String token)
    {
        String normalized = token.replace("_", "");
        if (normalized.isEmpty()) {
            return false;
        }
        char last = normalized.charAt(normalized.length() - 1);
        if (last == 'l' || last == 'L') {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
            return normalized.substring(2).chars().allMatch(ch -> ch == '0');
        }
        if (normalized.startsWith("0b") || normalized.startsWith("0B")) {
            return normalized.substring(2).chars().allMatch(ch -> ch == '0');
        }
        return normalized.chars().allMatch(ch -> ch == '0');
    }

    private static boolean isFloatingPointZero(String token)
    {
        String normalized = token.replace("_", "");
        if (normalized.isEmpty()) {
            return false;
        }
        char last = normalized.charAt(normalized.length() - 1);
        if (last == 'f' || last == 'F' || last == 'd' || last == 'D') {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        try {
            return Double.parseDouble(normalized) == 0.0d;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }
}
