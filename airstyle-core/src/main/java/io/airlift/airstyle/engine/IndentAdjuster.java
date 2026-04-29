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

/// Resolves the final column for a leaf that starts on a new line by walking
/// up the wrapper tree via [AbstractBlockWrapper#getChildOffset]. Ported
/// from IntelliJ's `com.intellij.formatting.engine.IndentAdjuster`.
public final class IndentAdjuster
{
    public void adjustIndent(LeafBlockWrapper current)
    {
        if (!current.whiteSpace().containsLineFeeds()) {
            // Inline — indent is irrelevant; spacing is handled elsewhere.
            return;
        }
        AbstractBlockWrapper parent = current.parent();
        IndentData offset;
        if (parent == null) {
            offset = IndentData.zero();
        }
        else {
            offset = parent.getChildOffset(current, current.startOffset());
        }
        current.whiteSpace().setIndent(offset.total());
    }
}
