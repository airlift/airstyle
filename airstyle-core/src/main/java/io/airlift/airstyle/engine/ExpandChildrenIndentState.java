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

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/// Post-sweep pass that expands [Indent.Expandable] instances.
///
/// When at least one block sharing an expandable indent has wrapped to a
/// new line during the main sweep, expanding the indent retroactively forces
/// the indent on all blocks in the group — including those that did not wrap.
/// Ported from IntelliJ's `ExpandChildrenIndentState`.
public final class ExpandChildrenIndentState
{
    private final IndentAdjuster indentAdjuster;

    public ExpandChildrenIndentState(IndentAdjuster indentAdjuster)
    {
        this.indentAdjuster = indentAdjuster;
    }

    public void run(AbstractBlockWrapper root)
    {
        Map<Indent.Expandable, List<AbstractBlockWrapper>> groups = new IdentityHashMap<>();
        collectExpandableGroups(root, groups);
        for (Map.Entry<Indent.Expandable, List<AbstractBlockWrapper>> entry : groups.entrySet()) {
            if (shouldExpand(entry.getValue())) {
                entry.getKey().enforce();
                for (AbstractBlockWrapper block : entry.getValue()) {
                    reindent(block);
                }
            }
        }
    }

    private static void collectExpandableGroups(
            AbstractBlockWrapper block,
            Map<Indent.Expandable, List<AbstractBlockWrapper>> groups)
    {
        if (block.indent() instanceof Indent.Expandable expandable) {
            groups.computeIfAbsent(expandable, _ -> new ArrayList<>()).add(block);
        }
        if (block instanceof CompositeBlockWrapper composite) {
            for (AbstractBlockWrapper child : composite.children()) {
                collectExpandableGroups(child, groups);
            }
        }
    }

    private static boolean shouldExpand(List<AbstractBlockWrapper> group)
    {
        for (AbstractBlockWrapper block : group) {
            if (block.whiteSpace().containsLineFeeds()) {
                return true;
            }
        }
        return false;
    }

    private void reindent(AbstractBlockWrapper block)
    {
        if (block instanceof LeafBlockWrapper leaf && leaf.whiteSpace().containsLineFeeds()) {
            indentAdjuster.adjustIndent(leaf);
            return;
        }
        if (block instanceof CompositeBlockWrapper composite) {
            for (AbstractBlockWrapper child : composite.children()) {
                reindent(child);
            }
        }
    }
}
