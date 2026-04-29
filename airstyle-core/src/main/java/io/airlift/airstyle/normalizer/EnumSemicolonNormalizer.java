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

import io.airlift.airstyle.model.LiteralLayoutModel;
import io.airlift.airstyle.model.SourceModel;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;

import java.util.ArrayList;
import java.util.List;

/// Removes the semicolon that separates enum constants from member
/// declarations when the enum has no members to declare.
///
/// ### Example
///
/// Before:
/// ```java
/// enum Color
/// {
///     RED,
///     GREEN,
///     BLUE;
/// }
/// ```
///
/// After:
/// ```java
/// enum Color
/// {
///     RED,
///     GREEN,
///     BLUE
/// }
/// ```
public final class EnumSemicolonNormalizer
{
    private EnumSemicolonNormalizer() {}

    public static String normalize(String source)
    {
        SourceModel sourceModel = SourceModel.create(source);
        CompilationUnit compilationUnit = sourceModel.compilationUnit();
        List<Replacement> replacements = new ArrayList<>();

        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(EnumDeclaration node)
            {
                LiteralLayoutModel.EnumLayout layout = LiteralLayoutModel.forEnumDeclaration(sourceModel, node);
                if (!layout.valid() || layout.hasBodyDeclarations()) {
                    return true;
                }

                int openBrace = layout.openBrace();
                int closeBrace = layout.closeBrace();
                if (closeBrace <= openBrace) {
                    return true;
                }

                int semicolonSearchStart = layout.hasConstants() ? layout.lastConstantEnd() : openBrace + 1;
                int semicolon = sourceModel.findTokenBetween(semicolonSearchStart, closeBrace, ITerminalSymbols.TokenNameSEMICOLON);
                boolean needsTrailingComma = needsTrailingComma(layout);
                if (semicolon >= 0 && sourceModel.firstTokenTypeBetween(semicolon + 1, closeBrace) == ITerminalSymbols.TokenNameEOF) {
                    replacements.add(new Replacement(semicolon, semicolon + 1, needsTrailingComma ? "," : ""));
                    return true;
                }
                if (needsTrailingComma) {
                    addTrailingCommaReplacement(source, layout, replacements);
                }
                return true;
            }
        });
        return Replacement.applyAll(source, replacements);
    }

    private static void addTrailingCommaReplacement(String source, LiteralLayoutModel.EnumLayout layout, List<Replacement> replacements)
    {
        if (!layout.hasConstants()) {
            return;
        }

        EnumConstantDeclaration lastConstant = layout.lastConstant();
        int insertPosition = lastConstant.getStartPosition() + lastConstant.getLength();
        if (insertPosition < 0 || insertPosition > source.length()) {
            return;
        }
        replacements.add(new Replacement(insertPosition, insertPosition, ","));
    }

    private static boolean needsTrailingComma(LiteralLayoutModel.EnumLayout layout)
    {
        if (!layout.hasConstants() || !layout.simpleEnum() || !layout.multiline()) {
            return false;
        }
        if (layout.constants().size() > 1 && layout.allConstantsOnSameLine()) {
            return false;
        }
        return !layout.hasTrailingComma();
    }
}
