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
package io.airlift.airstyle.engine.java;

import io.airlift.airstyle.engine.Block;
import io.airlift.airstyle.engine.Indent;
import io.airlift.airstyle.engine.Spacing;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/// Engine [Block] specialized for Java constructs. Mirrors IntelliJ's
/// `com.intellij.psi.formatter.java.AbstractJavaBlock` / `SimpleJavaBlock`
/// — carries an [Indent], a list of children, and a pairwise spacing
/// table.
public final class JavaBlock
        implements Block
{
    private final int start;
    private final int end;
    private final Indent indent;
    private final List<Block> children;
    private final Map<Block, Map<Block, Spacing>> spacing;
    private final FirstChildIndentPolicy firstChildIndentPolicy;
    private final String debugName;

    private JavaBlock(Builder builder)
    {
        this.start = builder.start;
        this.end = builder.end;
        this.indent = builder.indent == null ? Indent.noneIndent() : builder.indent;
        this.children = List.copyOf(builder.children);
        this.spacing = builder.spacing == null ? null : Map.copyOf(builder.spacing);
        this.firstChildIndentPolicy = builder.firstChildIndentPolicy;
        this.debugName = builder.debugName;
    }

    public static Builder builder(int start, int end, String debugName)
    {
        return new Builder(start, end, debugName);
    }

    public static JavaBlock leaf(int start, int end, String debugName)
    {
        return new Builder(start, end, debugName).build();
    }

    /// Wrap `inner` in a CONTINUATION-indent composite. The wrapper
    /// sets `canUseFirstChildIndent=false` so its CONT adds a real +8
    /// contribution rather than inheriting the inner block's indent. Use
    /// wherever a handler needs to route a sub-expression through an indent
    /// barrier — for-each iterable, assignment RHS on its own line,
    /// array-creation dimension, etc.
    public static JavaBlock continuationWrap(int start, int end, Block inner, String debugName)
    {
        return builder(start, end, debugName)
                .indent(Indent.continuationIndent())
                .firstChildIndentPolicy(FirstChildIndentPolicy.USE_BLOCK_INDENT)
                .child(inner)
                .build();
    }

    @Override
    public int startOffset()
    {
        return start;
    }

    @Override
    public int endOffset()
    {
        return end;
    }

    @Override
    public Indent indent()
    {
        return indent;
    }

    @Override
    public List<Block> subBlocks()
    {
        return children;
    }

    @Override
    public Spacing spacingBetween(Block first, Block second)
    {
        if (spacing == null) {
            return null;
        }
        Map<Block, Spacing> row = spacing.get(first);
        return row == null ? null : row.get(second);
    }

    @Override
    public boolean canUseFirstChildIndentAsBlockIndent()
    {
        return firstChildIndentPolicy == FirstChildIndentPolicy.USE_FIRST_CHILD_INDENT;
    }

    @Override
    public FirstChildIndentPolicy firstChildIndentPolicy()
    {
        return firstChildIndentPolicy;
    }

    @Override
    public String toString()
    {
        String name = debugName == null ? "JavaBlock" : debugName;
        return name + "[" + start + "," + end + "]";
    }

    public static final class Builder
    {
        private final int start;
        private final int end;
        private final String debugName;
        private Indent indent;
        private final List<Block> children = new ArrayList<>();
        private Map<Block, Map<Block, Spacing>> spacing;
        private FirstChildIndentPolicy firstChildIndentPolicy = FirstChildIndentPolicy.USE_FIRST_CHILD_INDENT;

        private Builder(int start, int end, String debugName)
        {
            this.start = start;
            this.end = end;
            this.debugName = debugName;
        }

        public Builder indent(Indent indent)
        {
            this.indent = indent;
            return this;
        }

        public Builder child(Block child)
        {
            if (child != null) {
                children.add(child);
            }
            return this;
        }

        public Builder spacing(Block first, Block second, Spacing spacing)
        {
            if (first == null || second == null || spacing == null) {
                return this;
            }
            if (this.spacing == null) {
                this.spacing = new IdentityHashMap<>();
            }
            this.spacing.computeIfAbsent(first, _ -> new IdentityHashMap<>()).put(second, spacing);
            return this;
        }

        public Builder canUseFirstChildIndent(boolean value)
        {
            return firstChildIndentPolicy(value ? FirstChildIndentPolicy.USE_FIRST_CHILD_INDENT : FirstChildIndentPolicy.USE_BLOCK_INDENT);
        }

        public Builder firstChildIndentPolicy(FirstChildIndentPolicy policy)
        {
            this.firstChildIndentPolicy = policy;
            return this;
        }

        public JavaBlock build()
        {
            validate();
            return new JavaBlock(this);
        }

        public Block lastChild()
        {
            return children.isEmpty() ? null : children.getLast();
        }

        private void validate()
        {
            if (start < 0 || start > end) {
                throw new IllegalArgumentException("Block %s has invalid range [%s,%s)"
                        .formatted(debugName, start, end));
            }

            Block previous = null;
            for (Block child : children) {
                if (child.startOffset() < start || child.endOffset() > end) {
                    throw new IllegalArgumentException("Block %s child %s is outside parent range [%s,%s)"
                            .formatted(debugName, child, start, end));
                }
                if (previous != null && child.startOffset() < previous.endOffset()) {
                    throw new IllegalArgumentException("Block %s has overlapping or out-of-order children: %s then %s"
                            .formatted(debugName, previous, child));
                }
                previous = child;
            }
        }
    }
}
