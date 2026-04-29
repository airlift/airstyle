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
package io.airlift.airstyle.model;

import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;

import java.util.List;

import static io.airlift.airstyle.AirstyleFormatter.CONTINUATION_INDENT;
import static java.lang.Math.max;

public final class LiteralLayoutModel
{
    private LiteralLayoutModel() {}

    public static ArrayLayout forArrayInitializer(SourceModel sourceModel, ArrayInitializer node)
    {
        @SuppressWarnings("unchecked")
        List<Expression> expressions = List.copyOf(node.expressions());

        int openBrace = node.getStartPosition();
        int closeBrace = openBrace + node.getLength() - 1;
        boolean valid = openBrace >= 0
                && closeBrace > openBrace
                && closeBrace < sourceModel.source().length();

        if (!valid) {
            return new ArrayLayout(false, node, openBrace, closeBrace, expressions, false, false, false, false, "", "");
        }

        String braceIndent = lineIndentAt(sourceModel, openBrace);
        String elementIndent = braceIndent + CONTINUATION_INDENT;
        boolean multiline = sourceModel.containsLineBreak(openBrace, closeBrace + 1);
        boolean nested = node.getParent() instanceof ArrayInitializer;
        boolean annotationContext = node.getParent() instanceof SingleMemberAnnotation
                || node.getParent() instanceof MemberValuePair
                || node.getParent() instanceof AnnotationTypeMemberDeclaration;
        boolean hasOwnLineElement = expressions.stream().anyMatch(expression -> sourceModel.startsAtLineIndent(expression.getStartPosition()));

        return new ArrayLayout(true, node, openBrace, closeBrace, expressions, multiline, nested, annotationContext, hasOwnLineElement, braceIndent, elementIndent);
    }

    public static EnumLayout forEnumDeclaration(SourceModel sourceModel, EnumDeclaration node)
    {
        @SuppressWarnings("unchecked")
        List<EnumConstantDeclaration> constants = List.copyOf(node.enumConstants());
        int openBrace = sourceModel.findTokenBetween(node.getStartPosition(), node.getStartPosition() + node.getLength(), ITerminalSymbols.TokenNameLBRACE);
        int closeBrace = sourceModel.findLastTokenBetween(node.getStartPosition(), node.getStartPosition() + node.getLength(), ITerminalSymbols.TokenNameRBRACE);
        boolean valid = openBrace >= 0
                && closeBrace > openBrace
                && closeBrace < sourceModel.source().length();

        if (!valid) {
            return new EnumLayout(false, node, constants, openBrace, closeBrace, false, false, false, true, true, max(0, openBrace + 1), false);
        }

        boolean hasBodyDeclarations = !node.bodyDeclarations().isEmpty();
        boolean simpleEnum = constants.stream().allMatch(constant ->
                constant.arguments().isEmpty()
                        && constant.getAnonymousClassDeclaration() == null
                        && constant.modifiers().isEmpty());
        boolean allConstantsOnSameLine = areAllConstantsOnSameLine(sourceModel, constants);
        boolean oneConstantPerLine = areOneConstantPerLine(sourceModel, constants);
        int lastConstantEnd = constants.isEmpty()
                ? openBrace + 1
                : constants.getLast().getStartPosition() + constants.getLast().getLength();
        boolean multiline = isMultilineEnum(sourceModel, constants, openBrace, closeBrace);
        boolean hasTrailingComma = !constants.isEmpty()
                && sourceModel.containsTokenBetween(lastConstantEnd, closeBrace, ITerminalSymbols.TokenNameCOMMA);

        return new EnumLayout(
                true,
                node,
                constants,
                openBrace,
                closeBrace,
                hasBodyDeclarations,
                simpleEnum,
                multiline,
                allConstantsOnSameLine,
                oneConstantPerLine,
                lastConstantEnd,
                hasTrailingComma);
    }

    private static boolean areAllConstantsOnSameLine(SourceModel sourceModel, List<EnumConstantDeclaration> constants)
    {
        if (constants.isEmpty()) {
            return false;
        }
        int firstLine = sourceModel.lineNumber(constants.getFirst().getStartPosition());
        return constants.stream().allMatch(constant -> sourceModel.lineNumber(constant.getStartPosition()) == firstLine);
    }

    private static boolean areOneConstantPerLine(SourceModel sourceModel, List<EnumConstantDeclaration> constants)
    {
        if (constants.size() < 2) {
            return true;
        }
        for (int index = 1; index < constants.size(); index++) {
            int previousLine = sourceModel.lineNumber(constants.get(index - 1).getStartPosition());
            int currentLine = sourceModel.lineNumber(constants.get(index).getStartPosition());
            if (previousLine == currentLine) {
                return false;
            }
        }
        return true;
    }

    private static boolean isMultilineEnum(SourceModel sourceModel, List<EnumConstantDeclaration> constants, int openBrace, int closeBrace)
    {
        if (constants.isEmpty()) {
            return sourceModel.lineNumber(openBrace) != sourceModel.lineNumber(closeBrace);
        }
        int firstConstantLine = sourceModel.lineNumber(constants.getFirst().getStartPosition());
        int lastConstantLine = sourceModel.lineNumber(constants.getLast().getStartPosition());
        return sourceModel.lineNumber(openBrace) != firstConstantLine
                || sourceModel.lineNumber(closeBrace) != lastConstantLine;
    }

    private static String lineIndentAt(SourceModel sourceModel, int position)
    {
        int lineStart = sourceModel.lineStart(position);
        int indentEnd = sourceModel.firstNonWhitespaceOnLine(lineStart);
        return sourceModel.source().substring(lineStart, indentEnd);
    }

    public record ArrayLayout(
            boolean valid,
            ArrayInitializer node,
            int openBrace,
            int closeBrace,
            List<Expression> expressions,
            boolean multiline,
            boolean nested,
            boolean annotationContext,
            boolean hasOwnLineElement,
            String braceIndent,
            String elementIndent)
    {
        public int lastExpressionEnd()
        {
            Expression last = expressions.getLast();
            return last.getStartPosition() + last.getLength();
        }
    }

    public record EnumLayout(
            boolean valid,
            EnumDeclaration node,
            List<EnumConstantDeclaration> constants,
            int openBrace,
            int closeBrace,
            boolean hasBodyDeclarations,
            boolean simpleEnum,
            boolean multiline,
            boolean allConstantsOnSameLine,
            boolean oneConstantPerLine,
            int lastConstantEnd,
            boolean hasTrailingComma)
    {
        public EnumConstantDeclaration lastConstant()
        {
            return constants.getLast();
        }

        public boolean hasConstants()
        {
            return !constants.isEmpty();
        }
    }
}
