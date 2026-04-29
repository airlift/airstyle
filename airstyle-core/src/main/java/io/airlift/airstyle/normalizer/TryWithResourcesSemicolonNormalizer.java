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
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TryStatement;

import java.util.ArrayList;
import java.util.List;

/// Removes the optional trailing semicolon after the last try-with-resources
/// resource. Checkstyle's `UnnecessarySemicolonInTryWithResources` only flags
/// some layouts by default, but the semicolon is always redundant when no
/// comment is attached to it.
public final class TryWithResourcesSemicolonNormalizer
{
    private TryWithResourcesSemicolonNormalizer() {}

    public static String normalize(String source)
    {
        SourceModel sourceModel = SourceModel.create(source);
        CompilationUnit compilationUnit = sourceModel.compilationUnit();
        List<Replacement> replacements = new ArrayList<>();

        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(TryStatement node)
            {
                collect(sourceModel, node, replacements);
                return true;
            }
        });

        return Replacement.applyAll(source, replacements);
    }

    private static void collect(SourceModel sourceModel, TryStatement node, List<Replacement> replacements)
    {
        List<?> resources = node.resources();
        if (resources.isEmpty()) {
            return;
        }

        String source = sourceModel.source();
        if (!(resources.getLast() instanceof ASTNode lastResource)) {
            return;
        }
        int lastResourceEnd = lastResource.getStartPosition() + lastResource.getLength();
        int openParen = sourceModel.findOpeningParen(node.getStartPosition(), node.getStartPosition() + node.getLength());
        int closeParen = sourceModel.findMatchingParen(openParen, node.getStartPosition() + node.getLength());
        if (closeParen < 0 || lastResourceEnd >= closeParen) {
            return;
        }

        int semicolon = sourceModel.findLastTokenBetween(lastResourceEnd, closeParen, ITerminalSymbols.TokenNameSEMICOLON);
        if (semicolon < 0 || !sourceModel.containsOnlyWhitespace(semicolon + 1, closeParen)) {
            return;
        }

        // Keep comments attached to the resource untouched; only remove a bare
        // redundant delimiter after the final resource.
        if (!sourceModel.containsOnlyWhitespace(lastResourceEnd, semicolon)) {
            return;
        }

        replacements.add(new Replacement(semicolon, semicolon + 1, ""));
        if (semicolon + 1 < source.length() && source.charAt(semicolon + 1) == ' ' && closeParen == semicolon + 2) {
            replacements.add(new Replacement(semicolon + 1, semicolon + 2, ""));
        }
    }
}
