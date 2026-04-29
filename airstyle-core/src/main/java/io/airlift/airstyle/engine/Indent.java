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

/// An indent describes how a block is indented relative to its parent or its
/// line-start ancestor. Indents are resolved by the layout engine via
/// `AbstractBlockWrapper.getChildOffset`, which walks bottom-up through
/// the block tree accumulating contributions.
///
/// Mirrors IntelliJ's `com.intellij.formatting.Indent`. Airstyle uses
/// a fixed style, so indent sizes are hardcoded (NORMAL=4, CONTINUATION=8).
public sealed class Indent
        permits Indent.Expandable
{
    /// Factory for the four indent kinds.
    public enum Type
    {
        /// No indent.
        NONE,
        /// One unit of block indent.
        NORMAL,
        /// One unit of continuation indent.
        CONTINUATION,
        /// Absolute-spaces indent, e.g. for single `case` labels.
        SPACES,
    }

    public static final int NORMAL_SIZE = 4;
    public static final int CONTINUATION_SIZE = 8;

    private static final Indent NONE = new Indent(Type.NONE, false, 0, false, false);
    private static final Indent NORMAL = new Indent(Type.NORMAL, false, 0, false, false);
    private static final Indent CONTINUATION = new Indent(Type.CONTINUATION, false, 0, false, false);
    private static final Indent ABSOLUTE_NONE = new Indent(Type.SPACES, true, 0, false, false);

    private final Type type;
    private final boolean absolute;
    private final int spaces;
    private final boolean enforceIndentToChildren;
    private final boolean relativeToDirectParent;

    Indent(Type type, boolean absolute, int spaces, boolean enforceIndentToChildren, boolean relativeToDirectParent)
    {
        this.type = type;
        this.absolute = absolute;
        this.spaces = spaces;
        this.enforceIndentToChildren = enforceIndentToChildren;
        this.relativeToDirectParent = relativeToDirectParent;
    }

    public Type type()
    {
        return type;
    }

    public boolean isAbsolute()
    {
        return absolute;
    }

    public int spaces()
    {
        return spaces;
    }

    /// When true, children of a block carrying this indent must respect the
    /// indent even when they do not start a new line. Used by
    /// `ExpandableIndent` after expansion.
    public boolean isEnforceIndentToChildren()
    {
        return enforceIndentToChildren;
    }

    public boolean isRelativeToDirectParent()
    {
        return relativeToDirectParent;
    }

    public static Indent noneIndent()
    {
        return NONE;
    }

    /// Absolute indent at column 0, ignoring the parent indent chain.
    public static Indent absoluteNoneIndent()
    {
        return ABSOLUTE_NONE;
    }

    public static Indent normalIndent()
    {
        return NORMAL;
    }

    public static Indent continuationIndent()
    {
        return CONTINUATION;
    }

    /// CONTINUATION indent that enforces itself on all descendants on new
    /// lines, not only the block's first leaf. Used for wraps whose children
    /// may start inline (no line feeds before the block) but contain later
    /// descendants that DO wrap — e.g. a lambda body whose chain selector
    /// wraps while the head stays inline with the arrow.
    public static Indent continuationEnforcedIndent()
    {
        return new Indent(Type.CONTINUATION, false, 0, true, false);
    }

    /// Absolute indent at a fixed column, ignoring the parent indent chain.
    public static Indent absoluteSpaceIndent(int spaces)
    {
        return new Indent(Type.SPACES, true, spaces, false, false);
    }

    /// Spaces indent relative to the direct parent block's column position.
    /// Matches IntelliJ's `Indent.getSpaceIndent(spaces, true)` — the
    /// child lands at `parent_column + spaces` rather than adding to
    /// the recursive parent walk. Used for nested conditional branches.
    public static Indent relativeSpaceIndent(int spaces)
    {
        return new Indent(Type.SPACES, false, spaces, false, true);
    }

    /// A [Expandable] indent starts non-enforcing. During
    /// `ExpandChildrenIndentState`, it is expanded to enforcing when at
    /// least one block in the group wraps. Models IntelliJ's SharedGroup
    /// SmartIndent behavior.
    public static Expandable smartIndent(Type type)
    {
        return new Expandable(type, false);
    }

    /// [#smartIndent(Type)] variant whose resolution anchors to the
    /// direct parent block's column rather than walking up the parent chain.
    /// Mirrors IntelliJ's `Indent.getSmartIndent(Type, useRelativeIndents=true)`.
    public static Expandable smartIndent(Type type, boolean relativeToDirectParent)
    {
        return new Expandable(type, relativeToDirectParent);
    }

    public static final class Expandable
            extends Indent
    {
        private boolean enforced;

        private Expandable(Type type, boolean relativeToDirectParent)
        {
            super(type, false, 0, false, relativeToDirectParent);
        }

        @Override
        public boolean isEnforceIndentToChildren()
        {
            return enforced;
        }

        /// Expand this indent. Subsequent queries return enforcing.
        public void enforce()
        {
            enforced = true;
        }
    }
}
