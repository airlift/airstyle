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
package io.airlift.airstyle.engine.java;

import io.airlift.airstyle.engine.Spacing;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;

final class JavaSpacingPolicy
{
    private final JavaSourceContext sourceContext;
    private final JavaFormatMetadata metadata;

    JavaSpacingPolicy(JavaSourceContext sourceContext, JavaFormatMetadata metadata)
    {
        this.sourceContext = sourceContext;
        this.metadata = metadata;
    }

    Spacing between(
            int prevType,
            int prevStart,
            int prevEnd,
            JavaTokens.Token next,
            int prevPrevNonCommentType,
            int prevPrevNonCommentStart)
    {
        if (metadata.genericOpens().contains(prevStart) || metadata.genericOpens().contains(next.start())
                || metadata.genericCloses().contains(prevStart) || metadata.genericCloses().contains(next.start())) {
            return genericSpacing(prevType, prevStart, next);
        }

        Spacing spacing = JavaSpacingRules.between(prevType, next.type());
        spacing = applyMetadataOverrides(spacing, prevType, prevStart, prevEnd, next, prevPrevNonCommentType, prevPrevNonCommentStart);
        spacing = preserveAllowedAdjacentSyntax(spacing, prevType, prevStart, prevEnd, next);
        spacing = preserveCommentAlignment(spacing, prevType, prevEnd, next);
        return spacing;
    }

    private Spacing genericSpacing(int prevType, int prevStart, JavaTokens.Token next)
    {
        if (metadata.genericCloses().contains(prevStart)) {
            if (metadata.methodTypeArgCloses().contains(prevStart)) {
                return JavaSpacingRules.noSpace();
            }
            if (needsSpaceAfterGenericClose(next.type())) {
                return JavaSpacingRules.oneSpace();
            }
            return JavaSpacingRules.noSpace();
        }
        if (metadata.genericOpens().contains(next.start()) && !isGenericBracketPredecessor(prevType)) {
            return JavaSpacingRules.oneSpace();
        }
        return JavaSpacingRules.noSpace();
    }

    private Spacing applyMetadataOverrides(
            Spacing spacing,
            int prevType,
            int prevStart,
            int prevEnd,
            JavaTokens.Token next,
            int prevPrevNonCommentType,
            int prevPrevNonCommentStart)
    {
        if (next.type() == ITerminalSymbols.TokenNameLBRACKET
                && metadata.typeUseArrayLBrackets().contains(next.start())) {
            spacing = JavaSpacingRules.oneSpace();
        }
        if ((prevType == ITerminalSymbols.TokenNameGREATER
                && !metadata.genericCloses().contains(prevStart))
                || (prevType == ITerminalSymbols.TokenNameLESS
                && !metadata.genericOpens().contains(prevStart))) {
            spacing = JavaSpacingRules.oneSpace();
        }
        if ((next.type() == ITerminalSymbols.TokenNameGREATER
                && !metadata.genericCloses().contains(next.start()))
                || (next.type() == ITerminalSymbols.TokenNameLESS
                && !metadata.genericOpens().contains(next.start()))) {
            spacing = JavaSpacingRules.oneSpace();
        }
        if (prevType == ITerminalSymbols.TokenNamePLUS
                || prevType == ITerminalSymbols.TokenNameMINUS) {
            if (metadata.unaryPrefixOps().contains(prevStart)) {
                spacing = JavaSpacingRules.noSpace();
            }
            else if (prevPrevNonCommentType != -1) {
                boolean precedingIsCastRParen = prevPrevNonCommentType == ITerminalSymbols.TokenNameRPAREN
                        && prevPrevNonCommentStart >= 0
                        && metadata.castRParens().contains(prevPrevNonCommentStart);
                if (precedingIsCastRParen || !isValueLikeToken(prevPrevNonCommentType)) {
                    spacing = JavaSpacingRules.noSpace();
                }
            }
        }
        if ((next.type() == ITerminalSymbols.TokenNamePLUS
                || next.type() == ITerminalSymbols.TokenNameMINUS)
                && metadata.unaryPrefixOps().contains(next.start())
                && prevType == ITerminalSymbols.TokenNameLBRACE) {
            spacing = JavaSpacingRules.noSpace();
        }
        if (next.type() == ITerminalSymbols.TokenNameLPAREN
                && prevType == ITerminalSymbols.TokenNameIdentifier
                && prevEnd - prevStart == "when".length()
                && "when".equals(sourceContext.source().substring(prevStart, prevEnd))
                && metadata.switchGuardWhens().contains(prevStart)) {
            spacing = JavaSpacingRules.oneSpace();
        }
        if (next.type() == ITerminalSymbols.TokenNameCOLON
                && (metadata.ternaryColons().contains(next.start()) || metadata.assertColons().contains(next.start()))) {
            spacing = JavaSpacingRules.oneSpace();
        }
        return spacing;
    }

    private Spacing preserveAllowedAdjacentSyntax(Spacing spacing, int prevType, int prevStart, int prevEnd, JavaTokens.Token next)
    {
        if (prevEnd == next.start()
                && spacing.minSpaces() > 0
                && spacing == JavaSpacingRules.keepLineOrSpace()
                && canKeepAdjacentNoSpace(prevType, prevStart, next.type())) {
            return JavaSpacingRules.noSpace();
        }
        return spacing;
    }

    private Spacing preserveCommentAlignment(Spacing spacing, int prevType, int prevEnd, JavaTokens.Token next)
    {
        boolean prevWasLineComment = prevType == ITerminalSymbols.TokenNameCOMMENT_LINE
                || prevType == ITerminalSymbols.TokenNameCOMMENT_MARKDOWN;
        if (prevEnd <= 0 || prevWasLineComment || sourceContext.containsLineBreak(prevEnd, next.start())) {
            return spacing;
        }
        int sourceSpaces = next.start() - prevEnd;
        boolean beforeLineComment = next.isComment()
                && next.type() == ITerminalSymbols.TokenNameCOMMENT_LINE;
        boolean beforeBlockComment = next.isComment()
                && next.type() == ITerminalSymbols.TokenNameCOMMENT_BLOCK;
        boolean afterBlockComment = prevType == ITerminalSymbols.TokenNameCOMMENT_BLOCK;
        if ((beforeLineComment || beforeBlockComment || afterBlockComment) && sourceSpaces > 1) {
            return Spacing.createSpacing(sourceSpaces, sourceSpaces, 0, false, 0);
        }
        return spacing;
    }

    private boolean canKeepAdjacentNoSpace(int prevType, int prevStart, int nextType)
    {
        return nextType == ITerminalSymbols.TokenNameLPAREN
                || nextType == ITerminalSymbols.TokenNameLBRACKET
                || nextType == ITerminalSymbols.TokenNameRPAREN
                || nextType == ITerminalSymbols.TokenNameRBRACKET
                || nextType == ITerminalSymbols.TokenNameRBRACE
                || nextType == ITerminalSymbols.TokenNameSEMICOLON
                || nextType == ITerminalSymbols.TokenNameCOLON
                || nextType == ITerminalSymbols.TokenNameCOMMA
                || nextType == ITerminalSymbols.TokenNameDOT
                || nextType == ITerminalSymbols.TokenNameCOLON_COLON
                || nextType == ITerminalSymbols.TokenNameELLIPSIS
                || prevType == ITerminalSymbols.TokenNameLPAREN
                || prevType == ITerminalSymbols.TokenNameLBRACKET
                || prevType == ITerminalSymbols.TokenNameLBRACE
                || prevType == ITerminalSymbols.TokenNameDOT
                || prevType == ITerminalSymbols.TokenNameCOLON_COLON
                || prevType == ITerminalSymbols.TokenNameAT
                || (prevType == ITerminalSymbols.TokenNameRPAREN && metadata.castRParens().contains(prevStart));
    }

    private static boolean isGenericBracketPredecessor(int token)
    {
        return token == ITerminalSymbols.TokenNameIdentifier
                || token == ITerminalSymbols.TokenNameGREATER
                || token == ITerminalSymbols.TokenNameRPAREN
                || token == ITerminalSymbols.TokenNameRBRACKET
                || token == ITerminalSymbols.TokenNameDOT
                || token == ITerminalSymbols.TokenNameLESS
                || token == ITerminalSymbols.TokenNameCOLON_COLON;
    }

    private static boolean needsSpaceAfterGenericClose(int next)
    {
        int[] punctuationTokens = {
                ITerminalSymbols.TokenNameDOT,
                ITerminalSymbols.TokenNameCOMMA,
                ITerminalSymbols.TokenNameLPAREN,
                ITerminalSymbols.TokenNameRPAREN,
                ITerminalSymbols.TokenNameLBRACKET,
                ITerminalSymbols.TokenNameRBRACKET,
                ITerminalSymbols.TokenNameSEMICOLON,
                ITerminalSymbols.TokenNameLESS,
                ITerminalSymbols.TokenNameGREATER,
                ITerminalSymbols.TokenNameRIGHT_SHIFT,
                ITerminalSymbols.TokenNameUNSIGNED_RIGHT_SHIFT,
                ITerminalSymbols.TokenNameRBRACE,
                ITerminalSymbols.TokenNameCOLON_COLON,
                ITerminalSymbols.TokenNameELLIPSIS,
        };
        for (int token : punctuationTokens) {
            if (next == token) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValueLikeToken(int token)
    {
        return token == ITerminalSymbols.TokenNameIdentifier
                || token == ITerminalSymbols.TokenNameIntegerLiteral
                || token == ITerminalSymbols.TokenNameLongLiteral
                || token == ITerminalSymbols.TokenNameDoubleLiteral
                || token == ITerminalSymbols.TokenNameFloatingPointLiteral
                || token == ITerminalSymbols.TokenNameCharacterLiteral
                || token == ITerminalSymbols.TokenNameStringLiteral
                || token == ITerminalSymbols.TokenNameRPAREN
                || token == ITerminalSymbols.TokenNameRBRACKET
                || token == ITerminalSymbols.TokenNamethis
                || token == ITerminalSymbols.TokenNamesuper
                || token == ITerminalSymbols.TokenNamenull
                || token == ITerminalSymbols.TokenNametrue
                || token == ITerminalSymbols.TokenNamefalse
                || token == ITerminalSymbols.TokenNamePLUS_PLUS
                || token == ITerminalSymbols.TokenNameMINUS_MINUS;
    }
}
