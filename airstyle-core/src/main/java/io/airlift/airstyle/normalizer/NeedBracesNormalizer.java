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
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.WhileStatement;

import java.util.ArrayList;
import java.util.List;

/// Adds braces to control-flow statements (`if`, `else`, `for`, `while`,
/// `do-while`) that use a single-statement body without braces. The body is
/// wrapped in a block so every branch has an explicit `{ … }`.
///
/// ### Example
///
/// Before:
/// ```java
/// if (condition) doSomething();
/// else doOther();
///
/// while (running)
///     process();
/// ```
///
/// After:
/// ```java
/// if (condition) {
///     doSomething();
/// }
/// else {
///     doOther();
/// }
///
/// while (running) {
///     process();
/// }
/// ```
public final class NeedBracesNormalizer
{
    private NeedBracesNormalizer() {}

    public static String normalize(String source)
    {
        SourceModel sourceModel = SourceModel.create(source);
        CompilationUnit compilationUnit = sourceModel.compilationUnit();
        List<Statement> targets = collectMissingBraceBodies(compilationUnit);
        if (targets.isEmpty()) {
            return source;
        }

        List<Replacement> replacements = new ArrayList<>();
        for (Statement statement : targets) {
            if (statement == null || statement.getParent() == null || statement instanceof Block || statement.getStartPosition() < 0) {
                continue;
            }
            int start = statement.getStartPosition();
            int end = start + statement.getLength();
            if (end > source.length() || start >= end) {
                continue;
            }
            replacements.add(new Replacement(start, end, "{ " + source.substring(start, end) + " }"));
        }
        replacements.addAll(collectDoWhileKeywordReplacements(compilationUnit, sourceModel));
        return Replacement.applyAll(source, replacements);
    }

    private static List<Statement> collectMissingBraceBodies(CompilationUnit compilationUnit)
    {
        List<Statement> targets = new ArrayList<>();
        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(IfStatement node)
            {
                addIfNeeded(targets, node.getThenStatement());
                if (!(node.getElseStatement() instanceof IfStatement)) {
                    addIfNeeded(targets, node.getElseStatement());
                }
                return true;
            }

            @Override
            public boolean visit(ForStatement node)
            {
                addIfNeeded(targets, node.getBody());
                return true;
            }

            @Override
            public boolean visit(EnhancedForStatement node)
            {
                addIfNeeded(targets, node.getBody());
                return true;
            }

            @Override
            public boolean visit(WhileStatement node)
            {
                addIfNeeded(targets, node.getBody());
                return true;
            }

            @Override
            public boolean visit(DoStatement node)
            {
                addIfNeeded(targets, node.getBody());
                return true;
            }
        });
        return targets;
    }

    private static List<Replacement> collectDoWhileKeywordReplacements(CompilationUnit compilationUnit, SourceModel sourceModel)
    {
        List<Replacement> replacements = new ArrayList<>();
        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(DoStatement node)
            {
                if (node.getBody() instanceof Block) {
                    return true;
                }

                int bodyEnd = node.getBody().getStartPosition() + node.getBody().getLength();
                int statementEnd = node.getStartPosition() + node.getLength();
                int whilePosition = sourceModel.findKeywordBetween(bodyEnd, statementEnd, "while");
                if (whilePosition >= 0 && !sourceModel.commentOverlaps(bodyEnd, whilePosition)) {
                    replacements.add(new Replacement(bodyEnd, whilePosition, " "));
                }
                return true;
            }
        });
        return replacements;
    }

    private static void addIfNeeded(List<Statement> targets, Statement statement)
    {
        if (statement != null && !(statement instanceof Block)) {
            targets.add(statement);
        }
    }
}
