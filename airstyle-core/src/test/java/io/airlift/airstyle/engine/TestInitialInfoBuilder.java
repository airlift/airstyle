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

import io.airlift.airstyle.engine.java.JavaBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestInitialInfoBuilder
{
    @Test
    void testFormatterFixesNonWhitespaceGapsByRejectingThem()
    {
        Block root = composite(
                0,
                3,
                leaf(0, 1),
                leaf(2, 3));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new InitialInfoBuilder("abc").build(root));
        assertEquals("Block [0,3) leaves uncovered non-whitespace text in [1,2)", exception.getMessage());
    }

    @Test
    void testFormatterKeepsWhitespaceOnlyGapsAllowed()
    {
        Block root = composite(
                0,
                3,
                leaf(0, 1),
                leaf(2, 3));

        InitialInfoBuilder builder = new InitialInfoBuilder("a c");
        assertDoesNotThrow(() -> builder.build(root));
        assertEquals(2, builder.leaves().size());
    }

    @Test
    void testFormatterIgnoresSkippedWhitespaceLeafForSiblingSpacing()
    {
        Block first = leaf(0, 1);
        Block whitespaceOnly = leaf(1, 3);
        Block second = leaf(3, 4);
        Block root = JavaBlock.builder(0, 4, "root")
                .child(first)
                .child(whitespaceOnly)
                .child(second)
                .spacing(first, second, Spacing.none())
                .build();

        assertEquals("ab", new FormatProcessor().format(root, "a  b"));
    }

    @Test
    void testFormatterFixesOverlappingChildrenByRejectingThem()
    {
        Block root = composite(
                0,
                3,
                leaf(0, 2),
                leaf(1, 3));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new InitialInfoBuilder("abc").build(root));
        assertEquals("Block [0,3) has overlapping or out-of-order children: [0,2) then [1,3)", exception.getMessage());
    }

    @Test
    void testFormatterFixesOutOfOrderChildrenByRejectingThem()
    {
        Block root = composite(
                0,
                3,
                leaf(2, 3),
                leaf(0, 1));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new InitialInfoBuilder("abc").build(root));
        assertEquals("Block [0,3) has overlapping or out-of-order children: [2,3) then [0,1)", exception.getMessage());
    }

    @Test
    void testFormatterFixesTrailingNonWhitespaceGapByRejectingIt()
    {
        Block root = composite(
                0,
                3,
                leaf(0, 2));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new InitialInfoBuilder("abc").build(root));
        assertEquals("Block [0,3) leaves uncovered non-whitespace text in [2,3)", exception.getMessage());
    }

    private static Block composite(int start, int end, Block... children)
    {
        return new TestBlock(start, end, List.of(children));
    }

    private static Block leaf(int start, int end)
    {
        return new TestBlock(start, end, List.of());
    }

    private record TestBlock(int startOffset, int endOffset, List<Block> subBlocks)
            implements Block
    {
        @Override
        public Indent indent()
        {
            return Indent.noneIndent();
        }

        @Override
        public Spacing spacingBetween(Block first, Block second)
        {
            return null;
        }
    }
}
