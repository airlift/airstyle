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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchExpression;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.YieldStatement;

import java.util.ArrayList;
import java.util.List;

/// Canonicalizes standalone comments between an arrow (`->`) switch rule and
/// its body so they stay with the case label rather than becoming part of the
/// rule body. Body placement itself is owned by the formatter engine.
///
/// ### Example
///
/// Before:
/// ```java
/// String describe(int n)
/// {
///     return switch (n) {
///         case 0 ->
///             "zero";
///         case 1 ->
///             "one";
///         default ->
///             String.valueOf(n);
///     };
/// }
/// ```
///
/// After:
/// ```java
/// String describe(int n)
/// {
///     return switch (n) {
///         case 0 -> "zero";
///         case 1 -> "one";
///         default -> String.valueOf(n);
///     };
/// }
/// ```
public final class SwitchRuleNormalizer
{
    private SwitchRuleNormalizer() {}

    public static String normalize(String source)
    {
        SourceModel sourceModel = SourceModel.create(source);
        CompilationUnit compilationUnit = sourceModel.compilationUnit();
        List<Replacement> replacements = new ArrayList<>();
        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(SwitchExpression node)
            {
                addRuleBodyPlacementReplacements(sourceModel, node.statements(), replacements);
                return true;
            }

            @Override
            public boolean visit(SwitchStatement node)
            {
                addRuleBodyPlacementReplacements(sourceModel, node.statements(), replacements);
                return true;
            }
        });
        return Replacement.applyAll(source, replacements);
    }

    private static void addRuleBodyPlacementReplacements(SourceModel sourceModel, List<?> statements, List<Replacement> replacements)
    {
        String source = sourceModel.source();
        for (int index = 0; index < statements.size() - 1; index++) {
            if (!(statements.get(index) instanceof SwitchCase switchCase)) {
                continue;
            }
            if (!switchCase.isSwitchLabeledRule()) {
                continue;
            }

            Statement body = switchRuleBody(statements.get(index + 1));
            if (body == null) {
                continue;
            }

            int arrowPosition = sourceModel.findArrowBetween(switchCase.getStartPosition(), body.getStartPosition());
            if (arrowPosition < 0) {
                continue;
            }

            int bodyStart = body.getStartPosition();
            int bodyEnd = bodyStart + body.getLength();
            if (bodyStart < 0 || bodyEnd > source.length()) {
                continue;
            }

            int arrowEnd = arrowPosition + 2;
            if (arrowEnd >= bodyStart) {
                continue;
            }

            addBeforeArrowCommentReplacements(sourceModel, switchCase, arrowPosition, replacements);

            if (!sourceModel.containsLineBreak(arrowEnd, bodyStart)) {
                continue;
            }

            List<SourceModel.CommentRange> comments = sourceModel.rewriteSafety(arrowEnd, bodyStart).containedComments();
            if (comments.isEmpty()) {
                continue;
            }
            if (!allStandaloneLineComments(sourceModel, comments)) {
                continue;
            }

            replacements.add(commentMoveReplacement(sourceModel, switchCase, comments));
            replacements.add(new Replacement(arrowEnd, bodyStart, " "));
        }
    }

    private static void addBeforeArrowCommentReplacements(
            SourceModel sourceModel,
            SwitchCase switchCase,
            int arrowPosition,
            List<Replacement> replacements)
    {
        int labelContentEnd = labelContentEnd(switchCase);
        if (labelContentEnd < 0 || labelContentEnd >= arrowPosition) {
            return;
        }
        if (!sourceModel.containsLineBreak(labelContentEnd, arrowPosition)) {
            return;
        }

        List<SourceModel.CommentRange> comments = sourceModel.rewriteSafety(labelContentEnd, arrowPosition).containedComments();
        if (comments.isEmpty()) {
            return;
        }
        if (!sourceModel.containsOnlyTokensWhitespaceAndComments(labelContentEnd, arrowPosition)) {
            return;
        }
        if (!allStandaloneLineComments(sourceModel, comments)) {
            return;
        }

        replacements.add(commentMoveReplacement(sourceModel, switchCase, comments));
        replacements.add(new Replacement(labelContentEnd, arrowPosition, " "));
    }

    private static int labelContentEnd(SwitchCase switchCase)
    {
        if (switchCase.expressions().isEmpty()) {
            return -1;
        }
        ASTNode last = (ASTNode) switchCase.expressions().getLast();
        return last.getStartPosition() + last.getLength();
    }

    private static Statement switchRuleBody(Object candidate)
    {
        if (candidate instanceof YieldStatement yieldStatement) {
            return yieldStatement;
        }
        if (candidate instanceof ExpressionStatement expressionStatement) {
            return expressionStatement;
        }
        if (candidate instanceof ThrowStatement throwStatement) {
            return throwStatement;
        }
        return null;
    }

    private static boolean allStandaloneLineComments(SourceModel sourceModel, List<SourceModel.CommentRange> comments)
    {
        String source = sourceModel.source();
        for (SourceModel.CommentRange comment : comments) {
            String text = source.substring(comment.start(), comment.end());
            if (!text.startsWith("//")) {
                return false;
            }
            if (!sourceModel.startsAtLineIndent(comment.start())) {
                return false;
            }
        }
        return true;
    }

    private static Replacement commentMoveReplacement(SourceModel sourceModel, SwitchCase switchCase, List<SourceModel.CommentRange> comments)
    {
        String source = sourceModel.source();
        int caseLineStart = sourceModel.lineStart(switchCase.getStartPosition());
        int caseIndentEnd = sourceModel.firstNonWhitespaceOnLine(caseLineStart);
        String caseIndent = source.substring(caseLineStart, caseIndentEnd);

        StringBuilder movedComments = new StringBuilder();
        for (SourceModel.CommentRange comment : comments) {
            movedComments.append(caseIndent)
                    .append(sourceModel.text(comment).stripLeading())
                    .append("\n");
        }
        return new Replacement(caseLineStart, caseLineStart, movedComments.toString());
    }
}
