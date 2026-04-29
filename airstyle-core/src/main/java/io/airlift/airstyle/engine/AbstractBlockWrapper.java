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

import java.util.EnumSet;
import java.util.Set;

/// Internal wrapper around a [Block] that carries mutable layout state —
/// the computed whitespace, parent/child links, and the flags that drive
/// [#getChildOffset].
///
/// Mirrors IntelliJ's `com.intellij.formatting.AbstractBlockWrapper`.
/// The two subclasses [LeafBlockWrapper] and [CompositeBlockWrapper]
/// represent terminal and non-terminal blocks respectively.
public abstract sealed class AbstractBlockWrapper
        permits LeafBlockWrapper, CompositeBlockWrapper
{
    /// Indent types that consult `indentAlreadyUsedBefore` when applied on the same line.
    static final Set<Indent.Type> RELATIVE_INDENT_TYPES = EnumSet.of(
            Indent.Type.NORMAL, Indent.Type.CONTINUATION);

    private final Block block;
    private final WhiteSpace whiteSpaceBefore;
    private final int startOffset;
    private final int endOffset;
    private CompositeBlockWrapper parent;

    /// Controls whether the block may inherit its effective indent from its first child.
    protected Block.FirstChildIndentPolicy firstChildIndentPolicy;

    protected AbstractBlockWrapper(Block block, WhiteSpace whiteSpaceBefore)
    {
        this.block = block;
        this.whiteSpaceBefore = whiteSpaceBefore;
        this.startOffset = block.startOffset();
        this.endOffset = block.endOffset();
        this.firstChildIndentPolicy = block.firstChildIndentPolicy();
    }

    public final Block block()
    {
        return block;
    }

    public final Indent indent()
    {
        return block.indent();
    }

    public final WhiteSpace whiteSpace()
    {
        return whiteSpaceBefore;
    }

    public final int startOffset()
    {
        return startOffset;
    }

    public final int endOffset()
    {
        return endOffset;
    }

    public final CompositeBlockWrapper parent()
    {
        return parent;
    }

    public final void setParent(CompositeBlockWrapper parent)
    {
        this.parent = parent;
    }

    /// Returns the column of this block's start (counting from line start,
    /// including the whitespace that precedes it).
    public IndentInfo symbolsBeforeBlock()
    {
        int column = whiteSpaceBefore.totalSpaces();
        for (AbstractBlockWrapper current = parent; current != null; current = current.parent) {
            if (current.whiteSpace().containsLineFeeds()) {
                column += current.whiteSpace().indentSpaces();
                break;
            }
        }
        return new IndentInfo(0, column, 0);
    }

    /// Core offset computation: returns the column at which `child`
    /// should appear. Mirrors IntelliJ's `AbstractBlockWrapper.getChildOffset`.
    public IndentData getChildOffset(AbstractBlockWrapper child, int targetStartOffset)
    {
        boolean childStartsNewLine = child.whiteSpace().containsLineFeeds();
        IndentData childContribution;

        if (childStartsNewLine
                || (!whiteSpaceBefore.containsLineFeeds()
                && RELATIVE_INDENT_TYPES.contains(child.indent().type())
                && indentAlreadyUsedBefore(child))
                || child.indent().isEnforceIndentToChildren()) {
            childContribution = CoreFormatterUtil.indentContribution(child);
        }
        else {
            childContribution = IndentData.zero();
        }

        if (childStartsNewLine && child.indent().isAbsolute()) {
            clearFirstChildIndentFlagChain();
            return childContribution;
        }

        // relativeToDirectParent: child's indent is relative to this block's
        // column position rather than computed via the recursive parent walk.
        if (childStartsNewLine && child.indent().isRelativeToDirectParent()
                && child.startOffset() > startOffset) {
            IndentInfo info = symbolsBeforeBlock();
            return childContribution.add(new IndentData(info.indentSpaces(), info.alignmentSpaces()));
        }

        if (child.startOffset() == startOffset) {
            boolean newValue = usesFirstChildIndentAsBlockIndent()
                    && child.usesFirstChildIndentAsBlockIndent()
                    && childContribution.isEmpty();
            if (!newValue && usesFirstChildIndentAsBlockIndent()) {
                useBlockIndent();
            }
        }

        if (startOffset == targetStartOffset || !whiteSpaceBefore.containsLineFeeds()) {
            return (parent == null)
                    ? childContribution
                    : childContribution.add(parent.getChildOffset(this, targetStartOffset));
        }

        if (parent == null) {
            return childContribution.add(whiteSpaceBefore.indentSpaces());
        }
        if (indent().isAbsolute() && parent.parent() != null) {
            return childContribution.add(parent.parent().getChildOffset(parent, targetStartOffset));
        }
        if (usesFirstChildIndentAsBlockIndent()) {
            return childContribution.add(whiteSpaceBefore.indentSpaces());
        }
        return childContribution.add(parent.getChildOffset(this, targetStartOffset));
    }

    private boolean usesFirstChildIndentAsBlockIndent()
    {
        return firstChildIndentPolicy == Block.FirstChildIndentPolicy.USE_FIRST_CHILD_INDENT;
    }

    private void useBlockIndent()
    {
        firstChildIndentPolicy = Block.FirstChildIndentPolicy.USE_BLOCK_INDENT;
    }

    private void clearFirstChildIndentFlagChain()
    {
        AbstractBlockWrapper current = this;
        while (current != null && current.startOffset == startOffset) {
            current.useBlockIndent();
            current = current.parent();
        }
    }

    /// Returns `true` if a sibling of `child` (appearing earlier in
    /// the tree) contains a line feed. The contract comes from IntelliJ's
    /// `indentAlreadyUsedBefore` in `CompositeBlockWrapper`.
    protected abstract boolean indentAlreadyUsedBefore(AbstractBlockWrapper child);
}
