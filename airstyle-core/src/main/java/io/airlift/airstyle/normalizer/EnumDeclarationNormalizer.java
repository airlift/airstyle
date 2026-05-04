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
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;

import java.util.ArrayList;
import java.util.List;

import static io.airlift.airstyle.AirstyleFormatter.INDENTATION;
import static java.lang.Math.max;

/// Normalizes simple enum declaration bodies so mixed multiline enums expand
/// to one constant per line. Compact single-line forms (e.g. `enum E { A, B }`)
/// are preserved when small enough.
///
/// ### Example
///
/// Before:
/// ```java
/// enum Color {
///     RED, GREEN,
///     BLUE, YELLOW
/// }
/// ```
///
/// After:
/// ```java
/// enum Color
/// {
///     RED,
///     GREEN,
///     BLUE,
///     YELLOW
/// }
/// ```
public final class EnumDeclarationNormalizer
{
    private EnumDeclarationNormalizer() {}

    public static String normalize(String source)
    {
        SourceModel sourceModel = SourceModel.create(source);
        List<Replacement> replacements = new ArrayList<>();

        sourceModel.compilationUnit().accept(new ASTVisitor()
        {
            @Override
            public boolean visit(EnumDeclaration node)
            {
                LiteralLayoutModel.EnumLayout layout = LiteralLayoutModel.forEnumDeclaration(sourceModel, node);
                if (!layout.valid() || !layout.hasConstants()) {
                    return true;
                }
                if (layout.constants().stream().anyMatch(constant -> constant.getAnonymousClassDeclaration() != null)) {
                    return true;
                }

                if (!shouldNormalize(sourceModel, layout)) {
                    return true;
                }

                int openBrace = layout.openBrace();
                String constantsLayout = constantsLayout(sourceModel, layout);
                int declarationLineStart = sourceModel.lineStart(node.getStartPosition());
                String declarationIndent = sourceModel.lineIndent(declarationLineStart);
                if (layout.hasBodyDeclarations()) {
                    if (!sourceModel.rewriteSafety(openBrace + 1, layout.lastConstantEnd()).safeToReplace()) {
                        return true;
                    }

                    String replacement = "{\n" + constantsLayout;
                    if (!source.regionMatches(openBrace, replacement, 0, replacement.length())
                            || openBrace + replacement.length() != layout.lastConstantEnd()) {
                        replacements.add(new Replacement(openBrace, layout.lastConstantEnd(), replacement));
                    }
                    return true;
                }

                int closeBrace = layout.closeBrace();
                if (sourceModel.rewriteSafety(openBrace + 1, closeBrace).safeToReplace()) {
                    String replacement = "{\n" + constantsLayout + "\n" + declarationIndent + "}";
                    if (!source.regionMatches(openBrace, replacement, 0, replacement.length())
                            || openBrace + replacement.length() != closeBrace + 1) {
                        replacements.add(new Replacement(openBrace, closeBrace + 1, replacement));
                    }
                    return true;
                }

                int constantsRewriteEnd = layout.lastConstantEnd();
                int trailingComma = sourceModel.findCommaBetween(layout.lastConstantEnd(), closeBrace);
                if (trailingComma >= 0) {
                    constantsRewriteEnd = trailingComma + 1;
                }

                if (!sourceModel.rewriteSafety(openBrace + 1, constantsRewriteEnd).safeToReplace()) {
                    return true;
                }
                if (!sourceModel.containsOnlyTokensWhitespaceAndComments(constantsRewriteEnd, closeBrace, ITerminalSymbols.TokenNameCOMMA)) {
                    return true;
                }

                String replacement = "{\n" + constantsLayout;
                if (!source.regionMatches(openBrace, replacement, 0, replacement.length())
                        || openBrace + replacement.length() != constantsRewriteEnd) {
                    replacements.add(new Replacement(openBrace, constantsRewriteEnd, replacement));
                }

                return true;
            }
        });
        return Replacement.applyAll(source, replacements);
    }

    private static String inlineConstantsLine(SourceModel sourceModel, List<EnumConstantDeclaration> constants)
    {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < constants.size(); i++) {
            if (i > 0) {
                result.append(", ");
            }
            result.append(constantText(sourceModel, constants.get(i)));
        }
        return result.toString();
    }

    private static String constantsLayout(SourceModel sourceModel, LiteralLayoutModel.EnumLayout layout)
    {
        int declarationLineStart = sourceModel.lineStart(layout.node().getStartPosition());
        String declarationIndent = sourceModel.lineIndent(declarationLineStart);
        String constantIndent = declarationIndent + INDENTATION;
        List<EnumConstantDeclaration> constants = layout.constants();

        if (shouldUseInlineConstants(layout)) {
            return constantIndent + inlineConstantsLine(sourceModel, constants) + (shouldPreserveTrailingComma(layout) ? "," : "");
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < constants.size(); i++) {
            if (i > 0) {
                result.append('\n');
                if (shouldPreserveBlankLineBetweenConstants(sourceModel, constants.get(i - 1), constants.get(i))) {
                    result.append('\n');
                }
            }
            result.append(constantIndent)
                    .append(constantText(sourceModel, constants.get(i)));
            if (i < constants.size() - 1 || shouldPreserveTrailingComma(layout)) {
                result.append(',');
            }
        }
        return result.toString();
    }

    private static String constantText(SourceModel sourceModel, EnumConstantDeclaration constant)
    {
        int start = constant.getStartPosition();
        int end = start + constant.getLength();
        return sourceModel.source().substring(start, end).strip();
    }

    private static boolean shouldNormalize(SourceModel sourceModel, LiteralLayoutModel.EnumLayout layout)
    {
        List<EnumConstantDeclaration> constants = layout.constants();
        boolean hasBlankLineBetweenConstants = hasBlankLineBetweenConstants(sourceModel, constants);

        EnumConstantDeclaration first = constants.getFirst();
        EnumConstantDeclaration last = constants.getLast();

        int openBraceLine = sourceModel.lineNumber(layout.openBrace());
        int firstConstantLine = sourceModel.lineNumber(first.getStartPosition());
        int lastConstantLine = sourceModel.lineNumber(max(last.getStartPosition(), layout.lastConstantEnd() - 1));
        int closeBraceLine = sourceModel.lineNumber(layout.closeBrace());

        if (hasBlankLineBetweenConstants) {
            return true;
        }

        // Preserve canonical wrapped enums unless there is a redundant trailing semicolon to remove
        // or the closing brace shares the last constant line.
        if (layout.oneConstantPerLine()
                && !sourceModel.containsTokenBetween(layout.lastConstantEnd(), layout.closeBrace(), ITerminalSymbols.TokenNameSEMICOLON)) {
            if (lastConstantLine < closeBraceLine) {
                return false;
            }
            if (layout.constants().size() == 1 && !layout.simpleEnum()) {
                return false;
            }
        }

        if (layout.allConstantsOnSameLine()
                && openBraceLine < firstConstantLine
                && lastConstantLine < closeBraceLine) {
            return false;
        }
        if (openBraceLine == firstConstantLine) {
            return true;
        }
        if (lastConstantLine == closeBraceLine) {
            return true;
        }
        return !layout.oneConstantPerLine();
    }

    private static boolean hasBlankLineBetweenConstants(SourceModel sourceModel, List<EnumConstantDeclaration> constants)
    {
        for (int index = 1; index < constants.size(); index++) {
            if (blankLinesBetweenConstants(sourceModel, constants.get(index - 1), constants.get(index)) > 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean shouldPreserveBlankLineBetweenConstants(SourceModel sourceModel, EnumConstantDeclaration previous, EnumConstantDeclaration current)
    {
        return blankLinesBetweenConstants(sourceModel, previous, current) > 0
                && (hasAnnotations(previous) || hasAnnotations(current));
    }

    private static int blankLinesBetweenConstants(SourceModel sourceModel, EnumConstantDeclaration previous, EnumConstantDeclaration current)
    {
        int previousEndLine = sourceModel.lineNumber(previous.getStartPosition() + previous.getLength() - 1);
        int currentStartLine = sourceModel.lineNumber(current.getStartPosition());
        return max(0, currentStartLine - previousEndLine - 1);
    }

    private static boolean hasAnnotations(EnumConstantDeclaration constant)
    {
        for (Object modifier : constant.modifiers()) {
            if (modifier instanceof IExtendedModifier extendedModifier && extendedModifier.isAnnotation()) {
                return true;
            }
        }
        return false;
    }

    private static boolean shouldUseInlineConstants(LiteralLayoutModel.EnumLayout layout)
    {
        return layout.allConstantsOnSameLine();
    }

    private static boolean shouldPreserveTrailingComma(LiteralLayoutModel.EnumLayout layout)
    {
        return !layout.hasBodyDeclarations() && layout.constants().size() > 1 && layout.hasTrailingComma();
    }
}
