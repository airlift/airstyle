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
package io.airlift.airstyle.normalizer;

import io.airlift.airstyle.model.SourceModel;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.util.ArrayList;
import java.util.List;

/// Removes a stray `;` placed after a top-level class / interface / enum /
/// record declaration.
///
/// ### Example
///
/// Before:
/// ```java
/// public class Test
/// {
/// };
/// ```
///
/// After:
/// ```java
/// public class Test
/// {
/// }
/// ```
public final class OuterTypeSemicolonNormalizer
{
    private OuterTypeSemicolonNormalizer() {}

    public static String normalize(String source)
    {
        CompilationUnit compilationUnit = SourceModel.create(source).compilationUnit();
        List<Replacement> replacements = new ArrayList<>();

        @SuppressWarnings("unchecked")
        List<ASTNode> types = compilationUnit.types();
        for (ASTNode type : types) {
            int end = type.getStartPosition() + type.getLength();
            int index = skipWhitespace(source, end);
            if (index >= 0 && index < source.length() && source.charAt(index) == ';') {
                replacements.add(new Replacement(index, index + 1, ""));
            }
        }
        return Replacement.applyAll(source, replacements);
    }

    private static int skipWhitespace(String source, int index)
    {
        int current = Math.clamp(index, 0, source.length());
        while (current < source.length()) {
            char character = source.charAt(current);
            if (!Character.isWhitespace(character)) {
                return current;
            }
            current++;
        }
        return -1;
    }
}
