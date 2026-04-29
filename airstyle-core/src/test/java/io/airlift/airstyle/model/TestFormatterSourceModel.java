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
package io.airlift.airstyle.model;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextBlock;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestFormatterSourceModel
{
    @Test
    void testTopLevelJavadocTagsPreserveDescriptionAndBlockTags()
    {
        String source =
                """
                class Test
                {
                    /**
                     * Example usage:
                     * <pre>{@code
                     * @ExampleTest
                     * class MyTest {}
                     * }</pre>
                     * @param value input value
                     * @return value
                     */
                    String run(String value) {}
                }
                """;

        SourceModel model = SourceModel.create(source);
        MethodDeclaration method = firstNode(model.compilationUnit(), MethodDeclaration.class);
        assertNotNull(method);

        Javadoc javadoc = method.getJavadoc();
        assertNotNull(javadoc);
        List<TagElement> tags = model.topLevelJavadocTags(javadoc);

        assertEquals(3, tags.size());
        assertNull(tags.get(0).getTagName());
        assertEquals("@param", tags.get(1).getTagName());
        assertEquals("@return", tags.get(2).getTagName());
        assertTrue(tags.get(0).getLength() > 0);
    }

    @Test
    void testCommentOverlapDetectionUsesCommentRanges()
    {
        String source =
                """
                enum State
                {
                    FIRST,
                    // keep separate
                    SECOND
                }
                """;

        SourceModel model = SourceModel.create(source);
        EnumDeclaration enumDeclaration = firstNode(model.compilationUnit(), EnumDeclaration.class);
        assertNotNull(enumDeclaration);

        int openBrace = source.indexOf('{', enumDeclaration.getStartPosition());
        int closeBrace = source.lastIndexOf('}');

        assertTrue(model.commentOverlaps(openBrace + 1, closeBrace));
        assertFalse(model.commentOverlaps(enumDeclaration.getStartPosition(), openBrace));
    }

    @Test
    void testCommentsContainedInRangeReturnClauseCommentText()
    {
        String source =
                """
                interface Test extends /* extends marker */ Base {}
                """;

        SourceModel model = SourceModel.create(source);
        TypeDeclaration declaration = firstNode(model.compilationUnit(), TypeDeclaration.class);

        int start = source.indexOf("/*");
        int end = ((Type) declaration.superInterfaceTypes().getFirst()).getStartPosition();
        List<SourceModel.CommentRange> comments = model.commentsContainedIn(start, end);

        assertEquals(1, comments.size());
        assertEquals("/* extends marker */", model.text(comments.getFirst()));
    }

    @Test
    void testAttachedLeadingCommentStartLineTracksCommentBlock()
    {
        String source =
                """
                // keep this header
                /* and this header */
                class Test {}
                """;

        SourceModel model = SourceModel.create(source);
        TypeDeclaration declaration = firstNode(model.compilationUnit(), TypeDeclaration.class);

        assertEquals(0, model.attachedLeadingCommentStartLine(declaration));
    }

    @Test
    void testTokenQueriesFindSelectorDotsParensAndCommas()
    {
        String source =
                """
                class Test
                {
                    Object run(Object a, Object b, Object c)
                    {
                        return outer(inner(a, b).map(c), value);
                    }
                }
                """;

        SourceModel model = SourceModel.create(source);
        List<MethodInvocation> invocations = allNodes(model.compilationUnit(), MethodInvocation.class);
        MethodInvocation outer = invocations.stream()
                .filter(node -> node.getName().getIdentifier().equals("outer"))
                .findFirst()
                .orElseThrow();
        MethodInvocation inner = invocations.stream()
                .filter(node -> node.getName().getIdentifier().equals("inner"))
                .findFirst()
                .orElseThrow();
        MethodInvocation map = invocations.stream()
                .filter(node -> node.getName().getIdentifier().equals("map"))
                .findFirst()
                .orElseThrow();

        int openParen = model.findOpeningParen(outer.getName().getStartPosition(), outer.getStartPosition() + outer.getLength());
        assertEquals(source.indexOf('(', outer.getName().getStartPosition()), openParen);
        assertEquals(source.lastIndexOf(')'), model.findMatchingParen(openParen, outer.getStartPosition() + outer.getLength()));

        int comma = model.findCommaBetween(inner.getStartPosition(), outer.getStartPosition() + outer.getLength());
        assertTrue(comma > inner.getStartPosition());
        assertEquals(source.indexOf('.', inner.getStartPosition() + inner.getLength()), model.findDotBetween(inner.getStartPosition() + inner.getLength(), map.getName().getStartPosition()));
        assertTrue(model.containsTokenBetween(outer.getStartPosition(), outer.getStartPosition() + outer.getLength(), ITerminalSymbols.TokenNameCOMMA));
        int closingBraceLineStart = model.lineStart(source.lastIndexOf('}'));
        int closingBraceStart = model.firstNonWhitespaceOnLine(closingBraceLineStart);
        assertTrue(model.containsOnlyTokenBetween(closingBraceStart, model.lineEnd(closingBraceLineStart), ITerminalSymbols.TokenNameRBRACE));
    }

    @Test
    void testStartsWithTextBlockRecognizesFormattedTextBlockExpressions()
    {
        String source =
                """
                class Test
                {
                    Object run(Object value)
                    {
                        return Map.of(
                                "hello",
                                \"""
                                text block here
                                \""".formatted(value),
                                "bye",
                                "value");
                    }
                }
                """;

        SourceModel model = SourceModel.create(source);
        MethodInvocation mapOf = allNodes(model.compilationUnit(), MethodInvocation.class).stream()
                .filter(node -> node.getName().getIdentifier().equals("of"))
                .findFirst()
                .orElseThrow();

        @SuppressWarnings("unchecked")
        List<Expression> arguments = mapOf.arguments();
        assertTrue(model.startsWithTextBlock(arguments.get(1)));
        assertFalse(model.startsWithTextBlock(arguments.get(3)));
    }

    @Test
    void testTextBlockLinesIncludeBlankAndClosingDelimiter()
    {
        String source =
                """
                class Test
                {
                    Object run(Object value)
                    {
                        return \"""
                                first

                                second
                                \""".formatted(value);
                    }
                }
                """;

        SourceModel model = SourceModel.create(source);
        TextBlock textBlock = firstNode(model.compilationUnit(), TextBlock.class);
        assertNotNull(textBlock);

        List<SourceModel.TextBlockLine> lines = model.textBlockLines(textBlock);
        assertEquals(4, lines.size());
        assertEquals("                first", source.substring(lines.get(0).lineStart(), lines.get(0).lineEnd()));
        assertFalse(lines.get(0).blank());
        assertTrue(lines.get(1).blank());
        assertEquals("                second", source.substring(lines.get(2).lineStart(), lines.get(2).lineEnd()));
        assertEquals("                \"\"\".formatted(value);", source.substring(lines.get(3).lineStart(), lines.get(3).lineEnd()));
    }

    @Test
    void testLineQueriesAndQualificationHelpers()
    {
        String source =
                """
                package example;

                class Test
                {
                    void run()
                    {
                        call(
                                value,
                                foo.bar.Baz.qux());
                    }
                }
                """;

        SourceModel model = SourceModel.create(source);
        CompilationUnit compilationUnit = model.compilationUnit();
        MethodDeclaration method = firstNode(compilationUnit, MethodDeclaration.class);
        MethodInvocation call = allNodes(compilationUnit, MethodInvocation.class).stream()
                .filter(node -> node.getName().getIdentifier().equals("call"))
                .findFirst()
                .orElseThrow();
        MethodInvocation qux = allNodes(compilationUnit, MethodInvocation.class).stream()
                .filter(node -> node.getName().getIdentifier().equals("qux"))
                .findFirst()
                .orElseThrow();
        Name qualifiedName = firstNode(compilationUnit, Name.class, node -> node.getFullyQualifiedName().equals("foo.bar.Baz"));

        int methodLineStart = model.lineStart(method.getStartPosition());
        assertEquals("    ", model.lineIndent(methodLineStart));
        assertEquals(methodLineStart, model.lineStartForLine(model.lineNumber(method.getStartPosition())));
        assertEquals(method.getStartPosition(), model.firstNonWhitespaceOnLine(methodLineStart));
        assertTrue(model.containsLineBreak(call.getStartPosition(), call.getStartPosition() + call.getLength()));
        assertTrue(model.startsOnOwnLine(method));
        assertFalse(model.startsAtLineIndent(qux.getName().getStartPosition()));
        assertTrue(model.isQualified(qualifiedName));
        assertEquals(source.indexOf("call("), model.findKeywordBetween(method.getStartPosition(), qux.getStartPosition(), "call"));
    }

    @Test
    void testCacheScopeReusesModelForSameSourceInstance()
    {
        String source =
                """
                class Test
                {
                    void run() {}
                }
                """;

        SourceModel first = SourceModel.create(source);
        SourceModel second = SourceModel.create(source);
        assertNotSame(first, second);

        try (SourceModel.CacheScope ignored = SourceModel.openCache()) {
            SourceModel cachedFirst = SourceModel.create(source);
            SourceModel cachedSecond = SourceModel.create(source);
            assertSame(cachedFirst, cachedSecond);
        }

        SourceModel afterScope = SourceModel.create(source);
        assertNotSame(first, afterScope);
    }

    @Test
    void testCachePruningRetainsRequestedSourceInstances()
    {
        String firstSource =
                """
                class First {}
                """;
        String secondSource =
                """
                class Second {}
                """;

        try (SourceModel.CacheScope ignored = SourceModel.openCache()) {
            SourceModel first = SourceModel.create(firstSource);
            SourceModel second = SourceModel.create(secondSource);

            SourceModel.pruneCacheKeeping(secondSource);

            assertSame(second, SourceModel.create(secondSource));
            assertNotSame(first, SourceModel.create(firstSource));
        }
    }

    @Test
    void testCacheScopeReusesModelForEqualSourceText()
    {
        String firstSource = new StringBuilder().append("class Test {}\n").toString();
        String secondSource = new StringBuilder().append("class Test {}\n").toString();

        assertNotSame(firstSource, secondSource);

        try (SourceModel.CacheScope ignored = SourceModel.openCache()) {
            SourceModel first = SourceModel.create(firstSource);
            SourceModel second = SourceModel.create(secondSource);

            assertSame(first, second);
        }
    }

    @Test
    void testSeededCacheReusesProvidedModel()
    {
        String source = "class Test {}\n";
        SourceModel seeded = SourceModel.create(source);

        try (SourceModel.CacheScope ignored = SourceModel.openCache(seeded)) {
            assertSame(seeded, SourceModel.create(source));
        }
    }

    @Test
    void testFindLastKeywordBetweenChoosesHierarchyClauseAfterGenericBound()
    {
        String source =
                """
                abstract class Test<T extends Enum<T>> extends Base {}
                """;

        SourceModel model = SourceModel.create(source);
        TypeDeclaration typeDeclaration = firstNode(model.compilationUnit(), TypeDeclaration.class);

        assertEquals(
                source.lastIndexOf("extends"),
                model.findLastKeywordBetween(typeDeclaration.getStartPosition(), typeDeclaration.getSuperclassType().getStartPosition(), "extends"));
    }

    @Test
    void testCommentAffinityHelpersCoverLineRangeAndNodeAttachment()
    {
        String source =
                """
                // class header
                /* class docs */
                class Test
                {
                    /* method
                     * note */
                    void run() {}
                }
                """;

        SourceModel model = SourceModel.create(source);
        TypeDeclaration type = firstNode(model.compilationUnit(), TypeDeclaration.class);
        MethodDeclaration method = firstNode(model.compilationUnit(), MethodDeclaration.class);

        assertEquals(2, model.commentsAttachedToNode(type).size());
        assertEquals(1, model.commentsAttachedToNode(method).size());

        int methodCommentLine = model.lineNumber(source.indexOf("/* method"));
        assertEquals(1, model.commentsOverlappingLine(methodCommentLine).size());

        int start = source.indexOf("method");
        int end = source.indexOf("run()");
        assertEquals(1, model.commentsOverlappingRange(start, end).size());
    }

    @Test
    void testJavadocPreRegionHelpersExposeTagAndPreformattedLines()
    {
        String source =
                """
                class Test
                {
                    /**
                     * Intro.
                     * <pre>{@code
                     * value
                     * }</pre>
                     * @return ok
                     */
                    String run() {}
                }
                """;

        SourceModel model = SourceModel.create(source);
        MethodDeclaration method = firstNode(model.compilationUnit(), MethodDeclaration.class);
        Javadoc javadoc = method.getJavadoc();
        assertNotNull(javadoc);

        List<SourceModel.LineRegion> tagRegions = model.javadocPreTagLineRegions(javadoc);
        assertEquals(1, tagRegions.size());
        assertTrue(tagRegions.getFirst().endLine() >= tagRegions.getFirst().startLine());

        List<SourceModel.LineRegion> bodyRegions = model.javadocPreformattedLineRegions(javadoc);
        assertEquals(1, bodyRegions.size());

        int preContentLine = model.lineNumber(source.indexOf("value"));
        int returnTagLine = model.lineNumber(source.indexOf("@return"));
        assertTrue(model.isInsideJavadocPreBlock(javadoc, source.indexOf("value")));
        assertFalse(model.isInsideJavadocPreBlock(javadoc, source.indexOf("@return")));
        assertTrue(bodyRegions.getFirst().contains(preContentLine));
        assertFalse(bodyRegions.getFirst().contains(returnTagLine));
    }

    @Test
    void testTrailingLineCommentHelpersReportAlignmentData()
    {
        String source =
                """
                class Test
                {
                    // top-level comment only
                    void run()
                    {
                        int value = 1;    // trailing
                    }
                }
                """;

        SourceModel model = SourceModel.create(source);
        int trailingLine = model.lineNumber(source.indexOf("int value"));
        SourceModel.TrailingLineComment trailing = model.trailingLineComment(trailingLine);
        assertNotNull(trailing);
        assertEquals(4, trailing.spacingBeforeComment());
        assertTrue(trailing.commentText().startsWith("// trailing"));

        int standaloneCommentLine = model.lineNumber(source.indexOf("// top-level"));
        assertNull(model.trailingLineComment(standaloneCommentLine));
        assertEquals(1, model.trailingLineComments(trailingLine, trailingLine).size());
    }

    @Test
    void testRewriteSafetyReportsWhetherRangeCanBeReplacedWithoutTouchingComments()
    {
        String source =
                """
                class Test
                {
                    void run()
                    {
                        execute(
                                first, // keep-this-comment
                                second);
                    }
                }
                """;

        SourceModel model = SourceModel.create(source);
        int commentStart = source.indexOf("// keep-this-comment");
        SourceModel.RewriteSafety safePrefix = model.rewriteSafety(source.indexOf("execute("), source.indexOf("first"));
        SourceModel.RewriteSafety unsafeArgumentGap = model.rewriteSafety(source.indexOf("first"), source.indexOf("second"));

        assertTrue(safePrefix.safeToReplace());
        assertFalse(safePrefix.hasComments());
        assertFalse(unsafeArgumentGap.safeToReplace());
        assertTrue(unsafeArgumentGap.hasComments());
        assertEquals(1, unsafeArgumentGap.overlappingComments().size());
        assertEquals(commentStart, unsafeArgumentGap.overlappingComments().getFirst().start());
    }

    @Test
    void testCommentRangeQueriesReuseCachedResults()
    {
        String source =
                """
                class Test
                {
                    void run()
                    {
                        call(
                                first, // keep-this-comment
                                second);
                    }
                }
                """;

        SourceModel model = SourceModel.create(source);
        int start = source.indexOf("first");
        int end = source.indexOf("second");

        assertSame(model.commentsOverlappingRange(start, end), model.commentsOverlappingRange(start, end));
        assertSame(model.commentsContainedIn(start, end), model.commentsContainedIn(start, end));
        assertSame(model.rewriteSafety(start, end), model.rewriteSafety(start, end));
    }

    @Test
    void testTextBlockContentAndMarginHelpers()
    {
        String source =
                """
                class Test
                {
                    String run(Object value)
                    {
                        return \"""
                                alpha

                                  beta
                                \""".formatted(value);
                    }
                }
                """;

        SourceModel model = SourceModel.create(source);
        TextBlock textBlock = firstNode(model.compilationUnit(), TextBlock.class);
        assertNotNull(textBlock);

        List<SourceModel.TextBlockLine> allLines = model.textBlockLines(textBlock);
        List<SourceModel.TextBlockLine> contentLines = model.textBlockContentLines(textBlock);
        SourceModel.TextBlockLine closingLine = model.textBlockClosingLine(textBlock);

        assertEquals(allLines.size() - 1, contentLines.size());
        assertNotNull(closingLine);
        assertTrue(source.substring(closingLine.lineStart(), closingLine.lineEnd()).contains("\"\"\""));
        assertEquals(contentLines.getFirst().indentWidth(), model.textBlockMinimumContentIndent(textBlock));
        assertEquals(contentLines.getFirst().firstNonWhitespace(), model.textBlockMinimumMarginColumn(textBlock));
    }

    @Test
    void testVisualWidthHelpersMatchRangeBasedIndentQueries()
    {
        String source = "class Test\n{\n\tvoid run()\n\t{\n\t    call();\n\t}\n}\n";

        SourceModel model = SourceModel.create(source);
        int lineStart = source.indexOf("\t    call();");
        int callStart = source.indexOf("call();");

        assertEquals(8, model.indentWidth(lineStart));
        assertEquals(8, model.indentWidth(lineStart, callStart));
        assertEquals(8, model.visualColumn(callStart));
    }

    @Test
    void testLineEndUsesPrecomputedLineStartsForIntermediateAndFinalLines()
    {
        String source = "first\nsecond";

        SourceModel model = SourceModel.create(source);

        assertEquals(source.indexOf('\n'), model.lineEnd(0));
        assertEquals(source.length(), model.lineEnd(source.indexOf("second")));
    }

    private static <T extends ASTNode> T firstNode(CompilationUnit compilationUnit, Class<T> type)
    {
        return firstNode(compilationUnit, type, _ -> true);
    }

    private static <T extends ASTNode> T firstNode(CompilationUnit compilationUnit, Class<T> type, Predicate<T> predicate)
    {
        AtomicReference<T> result = new AtomicReference<>();
        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public void preVisit(ASTNode node)
            {
                if (result.get() != null || !type.isInstance(node)) {
                    return;
                }
                T cast = type.cast(node);
                if (predicate.test(cast)) {
                    result.compareAndSet(null, cast);
                }
            }
        });
        return result.get();
    }

    private static <T extends ASTNode> List<T> allNodes(CompilationUnit compilationUnit, Class<T> type)
    {
        List<T> result = new ArrayList<>();
        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public void preVisit(ASTNode node)
            {
                if (type.isInstance(node)) {
                    result.add(type.cast(node));
                }
            }
        });
        return result;
    }
}
