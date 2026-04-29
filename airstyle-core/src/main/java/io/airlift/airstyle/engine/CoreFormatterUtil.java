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

/// Low-level helpers used by the layout engine to compute indent contributions
/// and column positions. Ported from IntelliJ's
/// `com.intellij.formatting.CoreFormatterUtil`.
final class CoreFormatterUtil
{
    private CoreFormatterUtil() {}

    /// Compute the indent contribution of a single block — the amount of
    /// whitespace its own [Indent] contributes to its column position.
    static IndentData indentContribution(AbstractBlockWrapper block)
    {
        Indent indent = block.indent();
        return switch (indent.type()) {
            case NONE -> IndentData.zero();
            case NORMAL -> new IndentData(Indent.NORMAL_SIZE, 0);
            case CONTINUATION -> new IndentData(Indent.CONTINUATION_SIZE, 0);
            case SPACES -> new IndentData(indent.spaces(), 0);
        };
    }
}
