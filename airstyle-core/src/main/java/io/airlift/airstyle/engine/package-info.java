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
/// IntelliJ-style layout engine. Drives the FORMAT stage of the airstyle
/// formatter via [io.airlift.airstyle.engine.java.JavaEngineFormatter].
/// ## Data model
///
///   - [io.airlift.airstyle.engine.Block] — the language-agnostic
///     block-tree interface.
///   - [io.airlift.airstyle.engine.Indent] (including expandable
///     indents), `Wrap` (`CHOP_IF_NEEDED` backtracking),
///     [io.airlift.airstyle.engine.Spacing],
///     [io.airlift.airstyle.engine.WhiteSpace].
///
/// ## Pipeline
///
///   - [io.airlift.airstyle.engine.AbstractBlockWrapper] +
///     [io.airlift.airstyle.engine.LeafBlockWrapper] +
///     [io.airlift.airstyle.engine.CompositeBlockWrapper] wrap the
///     user-supplied block tree with mutable layout state.
///   - [io.airlift.airstyle.engine.InitialInfoBuilder] populates the
///     wrappers from a [io.airlift.airstyle.engine.Block].
///   - [io.airlift.airstyle.engine.AdjustWhiteSpacesState] runs the
///     main sweep, driving `WrapProcessor` and
///     [io.airlift.airstyle.engine.IndentAdjuster].
///   - [io.airlift.airstyle.engine.ExpandChildrenIndentState] expands
///     [io.airlift.airstyle.engine.Indent.Expandable] indents when
///     any child of a block has wrapped.
///   - [io.airlift.airstyle.engine.ApplyChangesState] renders the
///     resolved whitespaces back into the source.
///
/// The Java-specific block builder lives in
/// [io.airlift.airstyle.engine.java.JavaBlockBuilder]; per-pair spacing
/// rules live in [io.airlift.airstyle.engine.java.JavaSpacingRules].
package io.airlift.airstyle.engine;
