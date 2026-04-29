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

import io.airlift.airstyle.engine.Block;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestJavaBlock
{
    @Test
    void testFormatterKeepsDefaultFirstChildIndentPolicy()
    {
        JavaBlock block = JavaBlock.builder(0, 1, "Default").build();

        assertEquals(Block.FirstChildIndentPolicy.USE_FIRST_CHILD_INDENT, block.firstChildIndentPolicy());
    }

    @Test
    void testFormatterFixesContinuationWrapByUsingBlockIndentPolicy()
    {
        JavaBlock wrapped = JavaBlock.continuationWrap(0, 1, JavaBlock.leaf(0, 1, "Inner"), "Wrapped");

        assertEquals(Block.FirstChildIndentPolicy.USE_BLOCK_INDENT, wrapped.firstChildIndentPolicy());
    }

    @Test
    void testFormatterFixesInvalidBlockRangeByRejectingIt()
    {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> JavaBlock.builder(3, 2, "Invalid").build());
        assertEquals("Block Invalid has invalid range [3,2)", exception.getMessage());
    }

    @Test
    void testFormatterFixesChildOutsideParentByRejectingIt()
    {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> JavaBlock.builder(1, 3, "Parent")
                        .child(JavaBlock.leaf(0, 2, "Child"))
                        .build());
        assertEquals("Block Parent child Child[0,2] is outside parent range [1,3)", exception.getMessage());
    }

    @Test
    void testFormatterFixesOverlappingChildrenByRejectingThem()
    {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> JavaBlock.builder(0, 4, "Parent")
                        .child(JavaBlock.leaf(0, 2, "First"))
                        .child(JavaBlock.leaf(1, 3, "Second"))
                        .build());
        assertEquals("Block Parent has overlapping or out-of-order children: First[0,2] then Second[1,3]", exception.getMessage());
    }
}
