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
package io.airlift.airstyle;

import io.airlift.airstyle.engine.java.JavaEngineFormatter;
import io.airlift.airstyle.format.LineBreakFormatter;
import io.airlift.airstyle.format.TokenSpacingFormatter;
import io.airlift.airstyle.model.SourceModel;
import io.airlift.airstyle.normalizer.ArrayInitializerBlankLineNormalizer;
import io.airlift.airstyle.normalizer.ArrayTypeStyleNormalizer;
import io.airlift.airstyle.normalizer.AvoidStarImportNormalizer;
import io.airlift.airstyle.normalizer.EmptyStatementNormalizer;
import io.airlift.airstyle.normalizer.EnumDeclarationNormalizer;
import io.airlift.airstyle.normalizer.EnumSemicolonNormalizer;
import io.airlift.airstyle.normalizer.ExplicitInitializationNormalizer;
import io.airlift.airstyle.normalizer.GenericTypeArgumentNormalizer;
import io.airlift.airstyle.normalizer.HexLiteralCaseNormalizer;
import io.airlift.airstyle.normalizer.InvalidJavadocPositionNormalizer;
import io.airlift.airstyle.normalizer.JavadocBlockTagGroupNormalizer;
import io.airlift.airstyle.normalizer.JavadocContinuationIndentationNormalizer;
import io.airlift.airstyle.normalizer.JavadocTagSpacingNormalizer;
import io.airlift.airstyle.normalizer.LambdaParameterParenthesesNormalizer;
import io.airlift.airstyle.normalizer.LineCommentJavadocSeparatorNormalizer;
import io.airlift.airstyle.normalizer.MarkdownJavadocBracketSpacingNormalizer;
import io.airlift.airstyle.normalizer.ModifierAnnotationLineBreakNormalizer;
import io.airlift.airstyle.normalizer.ModifierOrderNormalizer;
import io.airlift.airstyle.normalizer.MultipleVariableDeclarationsNormalizer;
import io.airlift.airstyle.normalizer.NeedBracesNormalizer;
import io.airlift.airstyle.normalizer.NoLineWrapNormalizer;
import io.airlift.airstyle.normalizer.NumericalLiteralCaseNormalizer;
import io.airlift.airstyle.normalizer.OneLineJavadocNormalizer;
import io.airlift.airstyle.normalizer.OuterTypeSemicolonNormalizer;
import io.airlift.airstyle.normalizer.RedundantImportNormalizer;
import io.airlift.airstyle.normalizer.RedundantModifierNormalizer;
import io.airlift.airstyle.normalizer.StandaloneBlockBlankLineNormalizer;
import io.airlift.airstyle.normalizer.StaticImportRuleNormalizer;
import io.airlift.airstyle.normalizer.SwitchRuleNormalizer;
import io.airlift.airstyle.normalizer.TextBlockMarginNormalizer;
import io.airlift.airstyle.normalizer.TrailingCommentSpacingNormalizer;
import io.airlift.airstyle.normalizer.TryWithResourcesSemicolonNormalizer;
import io.airlift.airstyle.normalizer.TypeHierarchyClauseNormalizer;
import io.airlift.airstyle.normalizer.TypeMemberSemicolonNormalizer;
import io.airlift.airstyle.normalizer.UnusedImportNormalizer;
import io.airlift.airstyle.normalizer.UnusedLambdaParameterNormalizer;
import io.airlift.airstyle.normalizer.UpperEllNormalizer;
import io.airlift.airstyle.normalizer.WrappedListNormalizer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

/// Airstyle Java formatter — enforces a single built-in formatting style.
///
/// Applies a pipeline of structural normalizations (comments, imports,
/// wrapped argument lists, braces, semicolons, etc.) and format cleanup
/// phases to produce the canonical Airstyle style.
public class AirstyleFormatter
{
    public static final int INDENTATION_SIZE = 4;
    public static final int CONTINUATION_INDENT_SIZE = 8;

    public static final String INDENTATION = " ".repeat(INDENTATION_SIZE);
    public static final String CONTINUATION_INDENT = " ".repeat(CONTINUATION_INDENT_SIZE);

    private static final List<FormatPhase> PHASE_REGISTRY = createPhaseRegistry();

    private final boolean rewriteUnusedLambdaParameters;

    public AirstyleFormatter()
    {
        this(true);
    }

    public AirstyleFormatter(boolean rewriteUnusedLambdaParameters)
    {
        this.rewriteUnusedLambdaParameters = rewriteUnusedLambdaParameters;
    }

    /// Formats Java source code.
    ///
    /// @param source the source code to format
    /// @return the formatted source code
    public String format(String source)
    {
        // Run up to 3 passes for stabilization.
        // MethodBrace and other structural changes may require a second formatting pass.
        String current = source;
        for (int round = 0; round < 3; round++) {
            FormatPassResult result = runPipeline(current);
            if (result.syntaxErrorFallback()) {
                return source;
            }
            if (result.formattedSource().equals(current)) {
                return result.formattedSource();
            }
            current = result.formattedSource();
        }
        return current;
    }

    private FormatPassResult runPipeline(String source)
    {
        try (var _ = SourceModel.openCache()) {
            if (hasSyntaxErrors(source)) {
                return new FormatPassResult(source, true);
            }

            String normalized = normalizeLineEndingsToLf(source);
            PhaseContext context = new PhaseContext(normalized);
            String preFormat = applyPhases(normalized, context, PhaseStage.PRE_FORMAT);
            String formatted = applyPhases(preFormat, context.withPreFormatSource(preFormat), PhaseStage.FORMAT);
            formatted = applyPhases(formatted, context.withPreFormatSource(preFormat), PhaseStage.POST_FORMAT);

            return hasSyntaxErrors(formatted)
                    ? new FormatPassResult(source, true)
                    : new FormatPassResult(formatted, false);
        }
    }

    private String applyPhases(String source, PhaseContext context, PhaseStage stage)
    {
        String current = source;
        for (FormatPhase phase : PHASE_REGISTRY) {
            if (phase.stage() != stage || !isPhaseEnabled(phase)) {
                continue;
            }
            String after = phase.apply(current, context);
            if (!current.equals(after)) {
                SourceModel.pruneCacheKeeping(after, context.preFormatSource());
            }
            current = after;
        }
        return current;
    }

    private boolean isPhaseEnabled(FormatPhase phase)
    {
        return switch (phase.name()) {
            case "unusedLambdaParameter" -> rewriteUnusedLambdaParameters;
            default -> true;
        };
    }

    private static String ensureTrailingNewline(String source)
    {
        if (source.isEmpty() || source.endsWith("\n")) {
            return source;
        }
        return source + "\n";
    }

    boolean hasSyntaxErrors(String source)
    {
        return SourceModel.create(source).hasErrors();
    }

    private static String normalizeLineEndingsToLf(String source)
    {
        if (source.indexOf('\r') < 0) {
            return source;
        }

        StringBuilder normalized = new StringBuilder(source.length());
        int index = 0;
        while (index < source.length()) {
            char current = source.charAt(index);
            if (current == '\r') {
                normalized.append('\n');
                index += (index + 1 < source.length() && source.charAt(index + 1) == '\n') ? 2 : 1;
            }
            else {
                normalized.append(current);
                index++;
            }
        }
        return normalized.toString();
    }

    private static String removeTrailingBlankLines(String source)
    {
        while (source.endsWith("\n\n")) {
            source = source.substring(0, source.length() - 1);
        }
        return source;
    }

    private static FormatPhase phase(String name, PhaseStage stage, UnaryOperator<String> operation)
    {
        return new FormatPhase(name, stage, (source, _) -> operation.apply(source));
    }

    /// Variant for normalizers whose `normalize(source, preFormatSource)`
    /// needs the pre-format output (e.g. to reference original indent anchors).
    private static FormatPhase phase(String name, PhaseStage stage, BinaryOperator<String> operation)
    {
        return new FormatPhase(name, stage, (source, context) -> operation.apply(source, context.preFormatSource()));
    }

    private static String applyFormatCleanup(String source)
    {
        source = TokenSpacingFormatter.replaceTabsInWhitespace(source, INDENTATION);
        source = TokenSpacingFormatter.normalizeMultipleSpaces(source);
        source = TokenSpacingFormatter.format(source); // trailing whitespace
        source = LineBreakFormatter.removeBlankLinesAtBlockBoundaries(source);
        return removeTrailingBlankLines(source);
    }

    private static List<FormatPhase> createPhaseRegistry()
    {
        List<FormatPhase> phases = new ArrayList<>();
        phases.add(phase("needBraces", PhaseStage.PRE_FORMAT, NeedBracesNormalizer::normalize));
        phases.add(phase("redundantModifier", PhaseStage.PRE_FORMAT, RedundantModifierNormalizer::normalize));
        phases.add(phase("emptyStatement", PhaseStage.PRE_FORMAT, EmptyStatementNormalizer::normalize));
        phases.add(phase("outerTypeSemicolon", PhaseStage.PRE_FORMAT, OuterTypeSemicolonNormalizer::normalize));
        phases.add(phase("typeMemberSemicolon", PhaseStage.PRE_FORMAT, TypeMemberSemicolonNormalizer::normalize));
        phases.add(phase("enumSemicolon", PhaseStage.PRE_FORMAT, EnumSemicolonNormalizer::normalize));
        phases.add(phase("tryWithResourcesSemicolon", PhaseStage.PRE_FORMAT, TryWithResourcesSemicolonNormalizer::normalize));
        phases.add(phase("unusedLambdaParameter", PhaseStage.PRE_FORMAT, UnusedLambdaParameterNormalizer::normalize));
        phases.add(phase("lambdaParameterParentheses", PhaseStage.PRE_FORMAT, LambdaParameterParenthesesNormalizer::normalize));
        phases.add(phase("invalidJavadocPosition", PhaseStage.PRE_FORMAT, InvalidJavadocPositionNormalizer::normalize));
        phases.add(phase("modifierOrder", PhaseStage.PRE_FORMAT, ModifierOrderNormalizer::normalize));
        phases.add(phase("modifierAnnotationLineBreak", PhaseStage.PRE_FORMAT, ModifierAnnotationLineBreakNormalizer::normalize));
        phases.add(phase("multipleVariableDeclarations", PhaseStage.PRE_FORMAT, MultipleVariableDeclarationsNormalizer::normalize));
        phases.add(phase("arrayTypeStyle", PhaseStage.PRE_FORMAT, ArrayTypeStyleNormalizer::normalize));
        phases.add(phase("explicitInitialization", PhaseStage.PRE_FORMAT, ExplicitInitializationNormalizer::normalize));
        phases.add(phase("upperEll", PhaseStage.PRE_FORMAT, UpperEllNormalizer::normalize));
        phases.add(phase("hexLiteralCase", PhaseStage.PRE_FORMAT, HexLiteralCaseNormalizer::normalize));
        phases.add(phase("numericalLiteralCase", PhaseStage.PRE_FORMAT, NumericalLiteralCaseNormalizer::normalize));
        phases.add(phase("noLineWrap", PhaseStage.PRE_FORMAT, NoLineWrapNormalizer::normalize));
        phases.add(phase("switchRule", PhaseStage.PRE_FORMAT, SwitchRuleNormalizer::normalize));
        phases.add(phase("staticImportRule", PhaseStage.PRE_FORMAT, StaticImportRuleNormalizer::normalize));
        phases.add(phase("avoidStarImport", PhaseStage.PRE_FORMAT, AvoidStarImportNormalizer::normalize));
        phases.add(phase("redundantImport", PhaseStage.PRE_FORMAT, RedundantImportNormalizer::normalize));
        phases.add(phase("unusedImport", PhaseStage.PRE_FORMAT, UnusedImportNormalizer::normalize));
        phases.add(phase("organizeImports", PhaseStage.PRE_FORMAT, AirstyleImportOrganizer::organizeImports));
        phases.add(phase("arrayInitializerBlankLine", PhaseStage.PRE_FORMAT, ArrayInitializerBlankLineNormalizer::normalize));
        phases.add(phase("enumDeclaration", PhaseStage.PRE_FORMAT, EnumDeclarationNormalizer::normalize));
        phases.add(phase("wrappedListPreFormat", PhaseStage.PRE_FORMAT, WrappedListNormalizer::normalize));
        phases.add(phase("typeHierarchyClause", PhaseStage.PRE_FORMAT, TypeHierarchyClauseNormalizer::normalize));
        phases.add(phase("genericTypeArgument", PhaseStage.PRE_FORMAT, GenericTypeArgumentNormalizer::normalize));

        // FORMAT stage: the engine produces final layout.
        phases.add(phase("engineLayout", PhaseStage.FORMAT, JavaEngineFormatter::format));

        // POST_FORMAT: edge-case layout fixups the engine doesn't cover, comment/brace/blank-line adjustments, and cleanup.
        phases.add(phase("standaloneBlockBlankLine", PhaseStage.POST_FORMAT, StandaloneBlockBlankLineNormalizer::normalize));
        phases.add(phase("formatCleanup", PhaseStage.POST_FORMAT, AirstyleFormatter::applyFormatCleanup));
        phases.add(phase("textBlockMargin", PhaseStage.POST_FORMAT, TextBlockMarginNormalizer::normalize));
        phases.add(phase("lineCommentJavadocSeparator", PhaseStage.POST_FORMAT, LineCommentJavadocSeparatorNormalizer::normalize));
        phases.add(phase("oneLineJavadoc", PhaseStage.POST_FORMAT, OneLineJavadocNormalizer::normalize));
        phases.add(phase("javadocBlockTagGroup", PhaseStage.POST_FORMAT, JavadocBlockTagGroupNormalizer::normalize));
        phases.add(phase("javadocTagSpacing", PhaseStage.POST_FORMAT, JavadocTagSpacingNormalizer::normalize));
        phases.add(phase("javadocContinuationIndentation", PhaseStage.POST_FORMAT, JavadocContinuationIndentationNormalizer::normalize));
        phases.add(phase("markdownJavadocBracketSpacing", PhaseStage.POST_FORMAT, MarkdownJavadocBracketSpacingNormalizer::normalize));
        phases.add(phase("trailingCommentSpacing", PhaseStage.POST_FORMAT, TrailingCommentSpacingNormalizer::normalize));
        phases.add(phase("trailingNewline", PhaseStage.POST_FORMAT, AirstyleFormatter::ensureTrailingNewline));
        return List.copyOf(phases);
    }

    private record PhaseContext(String preFormatSource)
    {
        private PhaseContext withPreFormatSource(String preFormatSource)
        {
            return new PhaseContext(preFormatSource);
        }
    }

    private record FormatPassResult(String formattedSource, boolean syntaxErrorFallback) {}

    private record FormatPhase(String name, PhaseStage stage, PhaseOperation operation)
    {
        private String apply(String source, PhaseContext context)
        {
            return operation.apply(source, context);
        }
    }

    private enum PhaseStage
    {
        PRE_FORMAT,
        FORMAT,
        POST_FORMAT,
    }

    @FunctionalInterface
    private interface PhaseOperation
    {
        String apply(String source, PhaseContext context);
    }
}
