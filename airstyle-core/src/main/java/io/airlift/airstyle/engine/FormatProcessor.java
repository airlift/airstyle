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

/// Orchestrates the layout pipeline: build wrapper tree, run the indent
/// sweep, expand ExpandableIndent groups, render.
///
/// Ported from IntelliJ's `com.intellij.formatting.FormatProcessor`,
/// simplified for airstyle's fixed-style, single-pass use case.
public final class FormatProcessor
{
    public String format(Block root, CharSequence source)
    {
        InitialInfoBuilder builder = new InitialInfoBuilder(source);
        AbstractBlockWrapper rootWrapper = builder.build(root);

        IndentAdjuster indentAdjuster = new IndentAdjuster();
        new AdjustWhiteSpacesState(indentAdjuster).run(builder.firstLeaf());
        new ExpandChildrenIndentState(indentAdjuster).run(rootWrapper);

        return new ApplyChangesState().apply(source, builder.leaves());
    }
}
