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
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PackageDeclaration;

import java.util.ArrayList;
import java.util.List;

/// Removes line wrapping inside package and import qualified names, matching
/// Checkstyle's default `NoLineWrap` token set.
public final class NoLineWrapNormalizer
{
    private NoLineWrapNormalizer() {}

    public static String normalize(String source)
    {
        SourceModel sourceModel = SourceModel.create(source);
        CompilationUnit compilationUnit = sourceModel.compilationUnit();
        List<Replacement> replacements = new ArrayList<>();

        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(PackageDeclaration node)
            {
                collect(sourceModel, node.getName(), replacements);
                return true;
            }

            @Override
            public boolean visit(ImportDeclaration node)
            {
                collect(sourceModel, node.getName(), replacements);
                return true;
            }
        });

        return Replacement.applyAll(source, replacements);
    }

    private static void collect(SourceModel sourceModel, Name name, List<Replacement> replacements)
    {
        String source = sourceModel.source();
        int start = name.getStartPosition();
        int end = start + name.getLength();
        for (int index = start; index < end; index++) {
            if (source.charAt(index) != '.') {
                continue;
            }

            int before = index - 1;
            while (before >= start && Character.isWhitespace(source.charAt(before))) {
                before--;
            }
            if (before + 1 < index) {
                replacements.add(new Replacement(before + 1, index, ""));
            }

            int after = index + 1;
            while (after < end && Character.isWhitespace(source.charAt(after))) {
                after++;
            }
            if (after > index + 1) {
                replacements.add(new Replacement(index + 1, after, ""));
            }
        }
    }
}
