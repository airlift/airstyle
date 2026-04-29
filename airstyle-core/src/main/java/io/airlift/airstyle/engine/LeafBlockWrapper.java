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

/// Terminal block wrapper. Leaves form a doubly-linked list that the layout
/// engine walks left-to-right. Each leaf carries its spacing-before rule.
public final class LeafBlockWrapper
        extends AbstractBlockWrapper
{
    private Spacing spacing;
    private LeafBlockWrapper next;
    private LeafBlockWrapper previous;
    private int symbolsAtLastLine;

    public LeafBlockWrapper(Block block, WhiteSpace whiteSpaceBefore)
    {
        super(block, whiteSpaceBefore);
    }

    public Spacing spacing()
    {
        return spacing;
    }

    public void setSpacing(Spacing spacing)
    {
        this.spacing = spacing;
    }

    public LeafBlockWrapper nextBlock()
    {
        return next;
    }

    public void setNextBlock(LeafBlockWrapper next)
    {
        this.next = next;
    }

    public void setPreviousBlock(LeafBlockWrapper previous)
    {
        this.previous = previous;
    }

    public void setSymbolsAtLastLine(int symbols)
    {
        this.symbolsAtLastLine = symbols;
    }

    @Override
    public IndentInfo symbolsBeforeBlock()
    {
        int spaces = whiteSpace().spaces();
        int indentSpaces = whiteSpace().indentSpaces();
        if (whiteSpace().containsLineFeeds()) {
            return new IndentInfo(0, indentSpaces, spaces);
        }
        for (LeafBlockWrapper current = previous; current != null; current = current.previous) {
            spaces += current.whiteSpace().spaces();
            spaces += current.symbolsAtLastLine;
            indentSpaces += current.whiteSpace().indentSpaces();
            if (current.whiteSpace().containsLineFeeds()) {
                break;
            }
        }
        return new IndentInfo(0, indentSpaces, spaces);
    }

    @Override
    protected boolean indentAlreadyUsedBefore(AbstractBlockWrapper child)
    {
        return false;
    }
}
