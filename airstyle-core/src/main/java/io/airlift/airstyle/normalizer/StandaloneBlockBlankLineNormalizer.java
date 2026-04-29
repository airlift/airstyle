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
import org.eclipse.jdt.core.dom.Block;

import java.util.ArrayList;
import java.util.List;

/// Preserves blank lines that were present before a standalone `{…}` block
/// statement in the original source, so intentional visual grouping survives
/// the engine pass.
///
/// ### Example
///
/// Before (original source):
/// ```java
/// void run()
/// {
///     setup();
///
///     {
///         // scoped work
///         int tmp = compute();
///         use(tmp);
///     }
///
///     teardown();
/// }
/// ```
///
/// After (engine pass would drop the blank lines; this normalizer restores them):
/// ```java
/// void run()
/// {
///     setup();
///
///     {
///         int tmp = compute();
///         use(tmp);
///     }
///
///     teardown();
/// }
/// ```
public final class StandaloneBlockBlankLineNormalizer
{
    private StandaloneBlockBlankLineNormalizer() {}

    public static String normalize(String source, String referenceSource)
    {
        SourceModel sourceModel = SourceModel.create(source);
        SourceModel referenceModel = SourceModel.create(referenceSource);

        List<Block> sourceBlocks = standaloneBlocks(sourceModel);
        List<Block> referenceBlocks = standaloneBlocks(referenceModel);
        if (sourceBlocks.size() != referenceBlocks.size()) {
            return source;
        }

        List<Replacement> replacements = new ArrayList<>();
        for (int index = 0; index < sourceBlocks.size(); index++) {
            Block sourceBlock = sourceBlocks.get(index);
            Block referenceBlock = referenceBlocks.get(index);

            if (!hasBlankLineBefore(referenceModel, referenceBlock.getStartPosition())) {
                continue;
            }
            if (hasBlankLineBefore(sourceModel, sourceBlock.getStartPosition())) {
                continue;
            }

            int insertionPoint = sourceModel.lineStart(sourceBlock.getStartPosition());
            replacements.add(new Replacement(insertionPoint, insertionPoint, "\n"));
        }
        return Replacement.applyAll(source, replacements);
    }

    private static List<Block> standaloneBlocks(SourceModel sourceModel)
    {
        List<Block> blocks = new ArrayList<>();
        sourceModel.compilationUnit().accept(new ASTVisitor()
        {
            @Override
            public boolean visit(Block node)
            {
                if (isStandaloneBlockStatement(node)) {
                    blocks.add(node);
                }
                return true;
            }
        });
        return blocks;
    }

    private static boolean isStandaloneBlockStatement(Block block)
    {
        return block.getParent() instanceof Block parent
                && parent.statements().contains(block);
    }

    private static boolean hasBlankLineBefore(SourceModel sourceModel, int position)
    {
        int line = sourceModel.lineNumber(position);
        if (line <= 0) {
            return false;
        }

        int previousLineStart = sourceModel.lineStartForLine(line - 1);
        if (previousLineStart < 0) {
            return false;
        }
        return sourceModel.containsOnlyWhitespace(previousLineStart, sourceModel.lineEnd(previousLineStart));
    }
}
