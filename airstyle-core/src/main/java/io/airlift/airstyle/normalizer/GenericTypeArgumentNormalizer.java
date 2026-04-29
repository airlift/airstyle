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
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.Type;

import java.util.ArrayList;
import java.util.List;

/// Tightens whitespace inside generic type argument lists: strips padding
/// immediately after `<` and before `>`, and collapses the gap after each
/// comma to a single space.
///
/// ### Example
///
/// Before:
/// ```java
/// Map< String ,  List<Integer > > map;
/// ```
///
/// After:
/// ```java
/// Map<String, List<Integer>> map;
/// ```
public final class GenericTypeArgumentNormalizer
{
    private GenericTypeArgumentNormalizer() {}

    public static String normalize(String source)
    {
        SourceModel sourceModel = SourceModel.create(source);
        List<Replacement> replacements = new ArrayList<>();

        sourceModel.compilationUnit().accept(new ASTVisitor()
        {
            @Override
            public boolean visit(ParameterizedType node)
            {
                @SuppressWarnings("unchecked")
                List<Type> typeArguments = List.copyOf(node.typeArguments());
                if (typeArguments.isEmpty()) {
                    return true;
                }

                int nodeStart = node.getStartPosition();
                int nodeEnd = nodeStart + node.getLength();
                if (!sourceModel.containsLineBreak(nodeStart, nodeEnd) || sourceModel.commentOverlaps(nodeStart, nodeEnd)) {
                    return true;
                }

                int typeEnd = node.getType().getStartPosition() + node.getType().getLength();
                Type firstArgument = typeArguments.getFirst();
                int openAngle = sourceModel.findTokenTextBetween(typeEnd, firstArgument.getStartPosition(), "<");
                if (openAngle >= 0 && sourceModel.containsLineBreak(openAngle + 1, firstArgument.getStartPosition())) {
                    replacements.add(new Replacement(openAngle + 1, firstArgument.getStartPosition(), ""));
                }

                for (int index = 0; index < typeArguments.size() - 1; index++) {
                    Type left = typeArguments.get(index);
                    Type right = typeArguments.get(index + 1);
                    int separatorStart = left.getStartPosition() + left.getLength();
                    int separatorEnd = right.getStartPosition();
                    if (sourceModel.containsLineBreak(separatorStart, separatorEnd) && !sourceModel.commentOverlaps(separatorStart, separatorEnd)) {
                        replacements.add(new Replacement(separatorStart, separatorEnd, ", "));
                    }
                }

                Type lastArgument = typeArguments.getLast();
                int closeAngle = firstGreaterThan(source, lastArgument.getStartPosition() + lastArgument.getLength(), nodeEnd);
                if (closeAngle >= 0 && sourceModel.containsLineBreak(lastArgument.getStartPosition() + lastArgument.getLength(), closeAngle)) {
                    replacements.add(new Replacement(lastArgument.getStartPosition() + lastArgument.getLength(), closeAngle, ""));
                }

                return true;
            }
        });
        return Replacement.applyAll(source, replacements);
    }

    private static int firstGreaterThan(String source, int start, int endExclusive)
    {
        int boundedStart = Math.clamp(start, 0, source.length());
        int boundedEnd = Math.clamp(endExclusive, boundedStart, source.length());
        for (int index = boundedStart; index < boundedEnd; index++) {
            if (source.charAt(index) == '>') {
                return index;
            }
        }
        return -1;
    }
}
