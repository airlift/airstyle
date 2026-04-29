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
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.ArrayList;
import java.util.List;

/// Removes a stray `;` placed after a type member declaration (method,
/// constructor, nested type, initializer) where no semicolon is required.
///
/// ### Example
///
/// Before:
/// ```java
/// class Test
/// {
///     void run() {};
///
///     static class Inner {};
/// }
/// ```
///
/// After:
/// ```java
/// class Test
/// {
///     void run() {}
///
///     static class Inner {}
/// }
/// ```
public final class TypeMemberSemicolonNormalizer
{
    private TypeMemberSemicolonNormalizer() {}

    public static String normalize(String source)
    {
        CompilationUnit compilationUnit = SourceModel.create(source).compilationUnit();
        List<Replacement> replacements = new ArrayList<>();

        collectLeadingMemberSemicolons(source, compilationUnit, replacements);
        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(TypeDeclaration node)
            {
                if (!(node.getParent() instanceof CompilationUnit)) {
                    addSemicolonAfterNode(source, node, replacements);
                }
                return true;
            }

            @Override
            public boolean visit(EnumDeclaration node)
            {
                if (!(node.getParent() instanceof CompilationUnit)) {
                    addSemicolonAfterNode(source, node, replacements);
                }
                return true;
            }

            @Override
            public boolean visit(AnnotationTypeDeclaration node)
            {
                if (!(node.getParent() instanceof CompilationUnit)) {
                    addSemicolonAfterNode(source, node, replacements);
                }
                return true;
            }

            @Override
            public boolean visit(RecordDeclaration node)
            {
                if (!(node.getParent() instanceof CompilationUnit)) {
                    addSemicolonAfterNode(source, node, replacements);
                }
                return true;
            }

            @Override
            public boolean visit(MethodDeclaration node)
            {
                addSemicolonAfterNode(source, node, replacements);
                return true;
            }

            @Override
            public boolean visit(Initializer node)
            {
                addSemicolonAfterNode(source, node, replacements);
                return true;
            }

            @Override
            public boolean visit(FieldDeclaration node)
            {
                addSemicolonAfterNode(source, node, replacements);
                return true;
            }

            @Override
            public boolean visit(AnnotationTypeMemberDeclaration node)
            {
                addSemicolonAfterNode(source, node, replacements);
                return true;
            }

            @Override
            public boolean visit(EnumConstantDeclaration node)
            {
                int firstSemicolon = semicolonAfterNode(source, node);
                if (firstSemicolon >= 0) {
                    int second = skipWhitespace(source, firstSemicolon + 1);
                    if (second >= 0 && second < source.length() && source.charAt(second) == ';') {
                        replacements.add(new Replacement(second, second + 1, ""));
                    }
                }
                return true;
            }
        });
        return Replacement.applyAll(source, replacements);
    }

    private static void collectLeadingMemberSemicolons(String source, CompilationUnit compilationUnit, List<Replacement> replacements)
    {
        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(TypeDeclaration node)
            {
                removeLeadingSemicolonInBody(source, node, replacements);
                return true;
            }

            @Override
            public boolean visit(RecordDeclaration node)
            {
                removeLeadingSemicolonInBody(source, node, replacements);
                return true;
            }

            @Override
            public boolean visit(AnnotationTypeDeclaration node)
            {
                removeLeadingSemicolonInBody(source, node, replacements);
                return true;
            }
        });
    }

    private static void removeLeadingSemicolonInBody(String source, ASTNode node, List<Replacement> replacements)
    {
        int openBrace = openingBrace(source, node);
        if (openBrace < 0) {
            return;
        }
        int first = skipWhitespace(source, openBrace + 1);
        if (first >= 0 && first < source.length() && source.charAt(first) == ';') {
            replacements.add(new Replacement(first, first + 1, ""));
        }
    }

    private static void addSemicolonAfterNode(String source, ASTNode node, List<Replacement> replacements)
    {
        int semicolon = semicolonAfterNode(source, node);
        if (semicolon >= 0) {
            replacements.add(new Replacement(semicolon, semicolon + 1, ""));
        }
    }

    private static int semicolonAfterNode(String source, ASTNode node)
    {
        int index = skipWhitespace(source, node.getStartPosition() + node.getLength());
        if (index >= 0 && index < source.length() && source.charAt(index) == ';') {
            return index;
        }
        return -1;
    }

    private static int openingBrace(String source, ASTNode node)
    {
        int start = node.getStartPosition();
        int end = Math.min(source.length(), start + node.getLength());
        for (int i = start; i < end; i++) {
            if (source.charAt(i) == '{') {
                return i;
            }
        }
        return -1;
    }

    private static int skipWhitespace(String source, int index)
    {
        int current = Math.clamp(index, 0, source.length());
        while (current < source.length()) {
            if (!Character.isWhitespace(source.charAt(current))) {
                return current;
            }
            current++;
        }
        return -1;
    }
}
