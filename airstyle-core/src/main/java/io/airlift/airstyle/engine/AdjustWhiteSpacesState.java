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
package io.airlift.airstyle.engine;

/// Main layout sweep: walk leaves left-to-right, applying spacing and indent
/// decisions to each.
///
/// Simplified port of IntelliJ's
/// `com.intellij.formatting.engine.AdjustWhiteSpacesState`. Airstyle
/// never chops for right-margin reasons, so this state has no wrap phase and
/// no backtracking — wrapping is driven entirely by source-shape-aware
/// [Spacing] emitted at block build time.
public final class AdjustWhiteSpacesState
{
    private final IndentAdjuster indentAdjuster;

    public AdjustWhiteSpacesState(IndentAdjuster indentAdjuster)
    {
        this.indentAdjuster = indentAdjuster;
    }

    public void run(LeafBlockWrapper firstLeaf)
    {
        for (LeafBlockWrapper current = firstLeaf; current != null; current = current.nextBlock()) {
            applySpacing(current);
            indentAdjuster.adjustIndent(current);
        }
    }

    private static void applySpacing(LeafBlockWrapper current)
    {
        Spacing spacing = current.spacing();
        if (spacing == null) {
            if (!current.whiteSpace().isFirstWhitespace()) {
                current.whiteSpace().applySpacing(Spacing.oneSpace());
            }
            return;
        }
        current.whiteSpace().applySpacing(spacing);
    }
}
