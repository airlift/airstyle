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

/// Immutable pair of `indent spaces` (logical indent level × 4) and
/// `alignment spaces` (column alignment offset) used during indent
/// computation. Modeled after IntelliJ's
/// `com.intellij.formatting.IndentData`.
public record IndentData(int indentSpaces, int alignmentSpaces)
{
    private static final IndentData ZERO = new IndentData(0, 0);

    public static IndentData zero()
    {
        return ZERO;
    }

    public int total()
    {
        return indentSpaces + alignmentSpaces;
    }

    public IndentData add(IndentData other)
    {
        return new IndentData(indentSpaces + other.indentSpaces, alignmentSpaces + other.alignmentSpaces);
    }

    public IndentData add(int spaces)
    {
        return new IndentData(indentSpaces + spaces, alignmentSpaces);
    }

    public boolean isEmpty()
    {
        return indentSpaces == 0 && alignmentSpaces == 0;
    }
}
