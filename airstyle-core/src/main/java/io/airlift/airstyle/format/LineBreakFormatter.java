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
package io.airlift.airstyle.format;

import io.airlift.airstyle.model.SourceModel;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TextBlock;

import java.util.ArrayList;
import java.util.List;

/// Utility method for removing blank lines at block boundaries, used in the
/// format cleanup phase.
public final class LineBreakFormatter
{
    private LineBreakFormatter() {}

    public static String removeBlankLinesAtBlockBoundaries(String source)
    {
        String result = source;
        String prev;
        do {
            prev = result;
            result = removeBlankLinesAtBlockBoundariesOnce(result);
        }
        while (!result.equals(prev));
        return result;
    }

    private static String removeBlankLinesAtBlockBoundariesOnce(String source)
    {
        SourceModel sourceModel = SourceModel.create(source);
        List<SourceRange> textBlockRanges = collectTextBlockRanges(sourceModel.compilationUnit());
        StringBuilder result = new StringBuilder(source);

        for (int index = 0; index + 2 < source.length(); index++) {
            if (source.charAt(index) != '{' || source.charAt(index + 1) != '\n' || source.charAt(index + 2) != '\n') {
                continue;
            }
            if (!isStructuralTokenAt(sourceModel, index, ITerminalSymbols.TokenNameLBRACE)
                    || isProtectedPosition(sourceModel, textBlockRanges, index)) {
                continue;
            }
            result.deleteCharAt(index + 2);
            break;
        }

        source = result.toString();
        sourceModel = SourceModel.create(source);
        textBlockRanges = collectTextBlockRanges(sourceModel.compilationUnit());
        result = new StringBuilder(source);
        for (int index = 0; index < source.length(); index++) {
            if (source.charAt(index) != '\n') {
                continue;
            }
            int blankLineStart = index + 1;
            int blankLineEnd = blankLineStart;
            while (blankLineEnd < source.length() && (source.charAt(blankLineEnd) == ' ' || source.charAt(blankLineEnd) == '\t')) {
                blankLineEnd++;
            }
            if (blankLineEnd >= source.length() || source.charAt(blankLineEnd) != '\n') {
                continue;
            }
            int braceStart = blankLineEnd + 1;
            while (braceStart < source.length() && (source.charAt(braceStart) == ' ' || source.charAt(braceStart) == '\t')) {
                braceStart++;
            }
            if (braceStart >= source.length() || source.charAt(braceStart) != '}') {
                continue;
            }
            if (!isStructuralTokenAt(sourceModel, braceStart, ITerminalSymbols.TokenNameRBRACE)
                    || isProtectedPosition(sourceModel, textBlockRanges, braceStart)) {
                continue;
            }
            result.delete(blankLineStart, blankLineEnd + 1);
            break;
        }
        return result.toString();
    }

    private static boolean isStructuralTokenAt(SourceModel sourceModel, int position, int token)
    {
        return sourceModel.findTokenBetween(position, position + 1, token) == position;
    }

    private static boolean isProtectedPosition(SourceModel sourceModel, List<SourceRange> textBlockRanges, int position)
    {
        if (sourceModel.commentOverlaps(position, position + 1)) {
            return true;
        }
        for (SourceRange range : textBlockRanges) {
            if (range.contains(position)) {
                return true;
            }
        }
        return false;
    }

    private static List<SourceRange> collectTextBlockRanges(CompilationUnit compilationUnit)
    {
        List<SourceRange> ranges = new ArrayList<>();
        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(TextBlock node)
            {
                ranges.add(new SourceRange(node.getStartPosition(), node.getStartPosition() + node.getLength()));
                return false;
            }
        });
        return ranges;
    }

    private record SourceRange(int start, int endExclusive)
    {
        private boolean contains(int position)
        {
            return position >= start && position < endExclusive;
        }
    }
}
