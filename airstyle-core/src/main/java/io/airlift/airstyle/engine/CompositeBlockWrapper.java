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
import java.util.List;

/// Wrapper for non-leaf blocks. Maintains child wrappers and supports
/// `indentAlreadyUsedBefore(child)` — used to detect whether an earlier
/// sibling broke to a new line, which determines whether continuation indent
/// contributes on the current line.
public final class CompositeBlockWrapper
        extends AbstractBlockWrapper
{
    private final List<AbstractBlockWrapper> children = new ArrayList<>();

    public CompositeBlockWrapper(Block block, WhiteSpace whiteSpaceBefore)
    {
        super(block, whiteSpaceBefore);
    }

    public List<AbstractBlockWrapper> children()
    {
        return children;
    }

    public void addChild(AbstractBlockWrapper child)
    {
        children.add(child);
        child.setParent(this);
    }

    @Override
    public IndentInfo symbolsBeforeBlock()
    {
        if (!children.isEmpty()) {
            return children.getFirst().symbolsBeforeBlock();
        }
        return super.symbolsBeforeBlock();
    }

    @Override
    protected boolean indentAlreadyUsedBefore(AbstractBlockWrapper child)
    {
        for (AbstractBlockWrapper sibling : children) {
            if (sibling == child) {
                return false;
            }
            if (sibling.whiteSpace().containsLineFeeds()) {
                return true;
            }
        }
        return false;
    }
}
