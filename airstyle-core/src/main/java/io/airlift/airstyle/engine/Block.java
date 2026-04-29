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

import java.util.List;

/// The user-facing block model for the layout engine. A [Block] describes
/// a contiguous range of source text plus its formatting attributes:
/// [Indent] and the [Spacing] between each pair of children.
///
/// Modeled after IntelliJ's `com.intellij.formatting.Block`. Language
/// support produces a tree of these and hands it to [FormatProcessor]
/// for layout resolution.
public interface Block
{
    /// Whether a composite may inherit its block indent from its first child,
    /// or must anchor child indentation on its own indent instead.
    enum FirstChildIndentPolicy
    {
        USE_FIRST_CHILD_INDENT,
        USE_BLOCK_INDENT,
    }

    /// Inclusive start offset in the source text.
    int startOffset();

    /// Exclusive end offset in the source text.
    int endOffset();

    Indent indent();

    /// Empty for leaves.
    List<? extends Block> subBlocks();

    /// Returns the spacing between `first` and `second` — both must
    /// be direct children of this block. Returns `null` for "no opinion"
    /// (the engine falls back to [Spacing#oneSpace()]).
    Spacing spacingBetween(Block first, Block second);

    /// True when the block contains no sub-blocks; terminal token.
    default boolean isLeaf()
    {
        return subBlocks().isEmpty();
    }

    /// Controls whether this block may inherit its effective block indent from
    /// its first child. This mirrors IntelliJ's
    /// `CAN_USE_FIRST_CHILD_INDENT_AS_BLOCK_INDENT` flag, but makes the
    /// two states explicit in the block model.
    default FirstChildIndentPolicy firstChildIndentPolicy()
    {
        return FirstChildIndentPolicy.USE_FIRST_CHILD_INDENT;
    }

    /// When true, the first child's indent is treated as the block's own
    /// indent. Enables IntelliJ's
    /// `CAN_USE_FIRST_CHILD_INDENT_AS_BLOCK_INDENT` behavior.
    default boolean canUseFirstChildIndentAsBlockIndent()
    {
        return firstChildIndentPolicy() == FirstChildIndentPolicy.USE_FIRST_CHILD_INDENT;
    }
}
