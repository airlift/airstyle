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

import org.eclipse.jdt.core.dom.ASTNode;

import static java.util.Objects.requireNonNull;

/// Source range with an explicit structural owner. Formatter helpers that
/// inspect tokens inside a range should take one of these instead of unrelated
/// `start`/`end` pairs, so the caller has to decide which AST node owns the
/// text being scanned or flattened.
record JavaSourceRange(String owner, int start, int end)
{
    JavaSourceRange
    {
        requireNonNull(owner, "owner is null");
        if (start < 0) {
            throw new IllegalArgumentException("start is negative: " + start);
        }
        if (end < start) {
            throw new IllegalArgumentException("end is before start: [" + start + "," + end + ")");
        }
    }

    static JavaSourceRange ownedBy(ASTNode node, int start, int end)
    {
        return new JavaSourceRange(node.getClass().getSimpleName(), start, end);
    }

    static JavaSourceRange leaf(String owner, int start, int end)
    {
        return new JavaSourceRange(owner, start, end);
    }
}
