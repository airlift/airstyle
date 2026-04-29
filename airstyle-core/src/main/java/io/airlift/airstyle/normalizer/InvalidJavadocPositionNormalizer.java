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
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import java.util.ArrayList;
import java.util.List;

/// Moves declaration Javadocs that appear after declaration annotations to the
/// front of the modifier block. Checkstyle's `InvalidJavadocPosition` follows
/// the standard doclet rule that Javadocs must appear immediately before the
/// declaration, which means before any annotations.
public final class InvalidJavadocPositionNormalizer
{
    private InvalidJavadocPositionNormalizer() {}

    public static String normalize(String source)
    {
        SourceModel sourceModel = SourceModel.create(source);
        CompilationUnit compilationUnit = sourceModel.compilationUnit();
        List<Replacement> replacements = new ArrayList<>();

        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(TypeDeclaration node)
            {
                collect(sourceModel, node, replacements);
                return true;
            }

            @Override
            public boolean visit(EnumDeclaration node)
            {
                collect(sourceModel, node, replacements);
                return true;
            }

            @Override
            public boolean visit(RecordDeclaration node)
            {
                collect(sourceModel, node, replacements);
                return true;
            }

            @Override
            public boolean visit(AnnotationTypeDeclaration node)
            {
                collect(sourceModel, node, replacements);
                return true;
            }

            @Override
            public boolean visit(FieldDeclaration node)
            {
                collect(sourceModel, node, replacements);
                return true;
            }

            @Override
            public boolean visit(MethodDeclaration node)
            {
                collect(sourceModel, node, replacements);
                return true;
            }

            @Override
            public boolean visit(AnnotationTypeMemberDeclaration node)
            {
                collect(sourceModel, node, replacements);
                return true;
            }
        });

        return Replacement.applyAll(source, replacements);
    }

    private static void collect(SourceModel sourceModel, BodyDeclaration declaration, List<Replacement> replacements)
    {
        int firstAnnotationStart = firstLeadingAnnotationStart(declaration);
        if (firstAnnotationStart < 0) {
            return;
        }

        int prefixEnd = declarationPrefixEnd(declaration);
        if (prefixEnd <= firstAnnotationStart) {
            return;
        }

        for (Comment comment : sourceModel.comments()) {
            if (!(comment instanceof Javadoc javadoc)) {
                continue;
            }
            if (declaration.getJavadoc() == javadoc) {
                continue;
            }
            int commentStart = javadoc.getStartPosition();
            int commentEnd = commentStart + javadoc.getLength();
            if (commentStart <= firstAnnotationStart || commentEnd > prefixEnd) {
                continue;
            }
            collectMove(sourceModel, firstAnnotationStart, commentStart, commentEnd, replacements);
        }
    }

    private static int firstLeadingAnnotationStart(BodyDeclaration declaration)
    {
        for (Object modifier : declaration.modifiers()) {
            if (modifier instanceof Annotation annotation) {
                return annotation.getStartPosition();
            }
            break;
        }
        return -1;
    }

    private static int declarationPrefixEnd(BodyDeclaration declaration)
    {
        return switch (declaration) {
            case TypeDeclaration node -> node.getName().getStartPosition();
            case EnumDeclaration node -> node.getName().getStartPosition();
            case RecordDeclaration node -> node.getName().getStartPosition();
            case AnnotationTypeDeclaration node -> node.getName().getStartPosition();
            case MethodDeclaration node -> node.getName().getStartPosition();
            case AnnotationTypeMemberDeclaration node -> node.getName().getStartPosition();
            case FieldDeclaration node when !node.fragments().isEmpty()
                    && node.fragments().getFirst() instanceof VariableDeclarationFragment fragment -> fragment.getName().getStartPosition();
            default -> declaration.getStartPosition();
        };
    }

    private static void collectMove(
            SourceModel sourceModel,
            int firstAnnotationStart,
            int commentStart,
            int commentEnd,
            List<Replacement> replacements)
    {
        String source = sourceModel.source();
        int removeStart = sourceModel.lineStart(commentStart);
        if (!sourceModel.containsOnlyWhitespace(removeStart, commentStart)) {
            return;
        }

        int commentLastLineEnd = sourceModel.lineEnd(commentEnd - 1);
        int removeEnd = commentLastLineEnd < source.length() ? commentLastLineEnd + 1 : commentLastLineEnd;
        String javadocText = source.substring(removeStart, removeEnd);
        if (!javadocText.endsWith("\n")) {
            javadocText += "\n";
        }

        int insertStart = sourceModel.lineStart(firstAnnotationStart);
        replacements.add(new Replacement(insertStart, insertStart, javadocText));
        replacements.add(new Replacement(removeStart, removeEnd, ""));
    }
}
