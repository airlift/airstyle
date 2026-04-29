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

/// Flattens a [Block] tree into a doubly-linked list of
/// [LeafBlockWrapper]s plus a parallel tree of wrappers. Every leaf
/// carries a [Spacing] derived from the parent block's
/// [Block#spacingBetween(Block,Block)] rules.
///
/// Port of IntelliJ's `InitialInfoBuilder`, but simplified: airstyle's
/// fixed style doesn't need progress reporting, cancellation, or incremental
/// formatting, and the leaf/composite distinction is decided structurally
/// rather than through a separate policy.
public final class InitialInfoBuilder
{
    private final CharSequence source;
    private final List<LeafBlockWrapper> leaves = new ArrayList<>();

    private LeafBlockWrapper firstLeaf;
    private LeafBlockWrapper lastLeaf;
    private WhiteSpace pendingWhitespace;
    private Spacing pendingSpacing;

    public InitialInfoBuilder(CharSequence source)
    {
        this.source = source;
    }

    public AbstractBlockWrapper build(Block root)
    {
        pendingWhitespace = new WhiteSpace(0, true);
        validateRootRange(root);
        AbstractBlockWrapper rootWrapper = buildWrapperTree(root, null);
        // Trailing whitespace at end of file — not attached to anything.
        pendingWhitespace.setEndOffset(source.length(), source);
        return rootWrapper;
    }

    public LeafBlockWrapper firstLeaf()
    {
        return firstLeaf;
    }

    public List<LeafBlockWrapper> leaves()
    {
        return leaves;
    }

    private AbstractBlockWrapper buildWrapperTree(Block block, CompositeBlockWrapper parent)
    {
        validateRangeWithinParent(block, parent == null ? null : parent.block());

        if (block.isLeaf()) {
            // A childless composite whose range is pure whitespace (emitted
            // by buildTokensRange when tokensIn(start, end) returns empty) is
            // a phantom: it has no actual token content but, via Block.isLeaf()
            // = subBlocks().isEmpty(), is treated as a leaf here. Creating a
            // LeafBlockWrapper for it would reset pendingWhitespace past the
            // end of the whitespace range and destroy line-feed tracking
            // when a wrapped-arg CIC head's PostParen range covers the
            // newline before a chain selector.
            // Return null to skip emission; the inner recursive call discards
            // the return value (only the root return matters, and the root is
            // never a whitespace-only block).
            if (isSkippedWhitespaceLeaf(block)) {
                return null;
            }
            WhiteSpace whitespace = consumeWhitespaceBefore(block.startOffset());
            LeafBlockWrapper leaf = new LeafBlockWrapper(block, whitespace);
            if (parent != null) {
                parent.addChild(leaf);
            }
            leaf.setSymbolsAtLastLine(computeSymbolsAtLastLine(block.startOffset(), block.endOffset()));
            registerLeaf(leaf);
            pendingWhitespace = new WhiteSpace(block.endOffset(), false);
            return leaf;
        }

        WhiteSpace whitespace = consumeWhitespaceBefore(block.startOffset());
        CompositeBlockWrapper composite = new CompositeBlockWrapper(block, whitespace);
        if (parent != null) {
            parent.addChild(composite);
        }

        List<? extends Block> subBlocks = block.subBlocks();
        validateChildOrdering(block, subBlocks);

        int coveredUntil = block.startOffset();
        Block previousChild = null;
        for (Block child : subBlocks) {
            validateChildRange(block, child);
            assertWhitespaceOnlyGap(block, coveredUntil, child.startOffset());

            if (!isSkippedWhitespaceLeaf(child) && previousChild != null) {
                Spacing spacing = block.spacingBetween(previousChild, child);
                if (spacing != null) {
                    // The spacing applies to the whitespace-before-child leaf we're about to create.
                    pendingSpacing = spacing;
                }
            }
            AbstractBlockWrapper childWrapper = buildWrapperTree(child, composite);
            if (childWrapper != null) {
                previousChild = child;
            }
            coveredUntil = child.endOffset();
        }
        assertWhitespaceOnlyGap(block, coveredUntil, block.endOffset());
        return composite;
    }

    private boolean isSkippedWhitespaceLeaf(Block block)
    {
        return block.isLeaf()
                && block.endOffset() > block.startOffset()
                && isRangeWhitespaceOnly(block.startOffset(), block.endOffset());
    }

    private void validateRootRange(Block block)
    {
        if (block.startOffset() < 0 || block.endOffset() < block.startOffset() || block.endOffset() > source.length()) {
            throw new IllegalStateException("Block " + formatRange(block.startOffset(), block.endOffset()) + " is outside source range " + formatRange(0, source.length()));
        }
    }

    private void validateRangeWithinParent(Block block, Block parent)
    {
        if (parent == null) {
            return;
        }
        if (block.startOffset() < parent.startOffset() || block.endOffset() > parent.endOffset() || block.endOffset() < block.startOffset()) {
            throw new IllegalStateException("Block " + formatRange(parent.startOffset(), parent.endOffset()) + " has child outside parent range: " + formatRange(block.startOffset(), block.endOffset()));
        }
    }

    private void validateChildOrdering(Block parent, List<? extends Block> children)
    {
        Block previousChild = null;
        for (Block child : children) {
            validateChildRange(parent, child);
            if (previousChild != null && child.startOffset() < previousChild.endOffset()) {
                throw new IllegalStateException("Block " + formatRange(parent.startOffset(), parent.endOffset()) + " has overlapping or out-of-order children: "
                        + formatRange(previousChild.startOffset(), previousChild.endOffset()) + " then " + formatRange(child.startOffset(), child.endOffset()));
            }
            previousChild = child;
        }
    }

    private void validateChildRange(Block parent, Block child)
    {
        if (child.startOffset() < parent.startOffset() || child.endOffset() > parent.endOffset() || child.endOffset() < child.startOffset()) {
            throw new IllegalStateException("Block " + formatRange(parent.startOffset(), parent.endOffset()) + " has child outside parent range: " + formatRange(child.startOffset(), child.endOffset()));
        }
    }

    private void assertWhitespaceOnlyGap(Block block, int start, int end)
    {
        if (start >= end || isRangeWhitespaceOnly(start, end)) {
            return;
        }
        throw new IllegalStateException("Block " + formatRange(block.startOffset(), block.endOffset()) + " leaves uncovered non-whitespace text in " + formatRange(start, end));
    }

    private static String formatRange(int start, int end)
    {
        return "[" + start + "," + end + ")";
    }

    private boolean isRangeWhitespaceOnly(int start, int end)
    {
        for (int i = start; i < end; i++) {
            char c = source.charAt(i);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                return false;
            }
        }
        return true;
    }

    private WhiteSpace consumeWhitespaceBefore(int offset)
    {
        pendingWhitespace.setEndOffset(offset, source);
        return pendingWhitespace;
    }

    private int computeSymbolsAtLastLine(int startOffset, int endOffset)
    {
        // Find the last newline in the token text. Count characters from
        // there to the end. For single-line tokens this equals the token width.
        int lastNewline = -1;
        for (int i = endOffset - 1; i >= startOffset; i--) {
            if (i < source.length() && source.charAt(i) == '\n') {
                lastNewline = i;
                break;
            }
        }
        int start = (lastNewline >= 0) ? lastNewline + 1 : startOffset;
        return Math.min(endOffset, source.length()) - start;
    }

    private void registerLeaf(LeafBlockWrapper leaf)
    {
        if (firstLeaf == null) {
            firstLeaf = leaf;
        }
        if (lastLeaf != null) {
            lastLeaf.setNextBlock(leaf);
            leaf.setPreviousBlock(lastLeaf);
            if (pendingSpacing != null) {
                leaf.setSpacing(pendingSpacing);
                pendingSpacing = null;
            }
        }
        lastLeaf = leaf;
        leaves.add(leaf);
    }
}
