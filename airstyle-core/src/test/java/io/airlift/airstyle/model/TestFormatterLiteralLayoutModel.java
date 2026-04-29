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
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestFormatterLiteralLayoutModel
{
    @Test
    void testArrayLayoutClassifiesMultilineAnnotationArray()
    {
        String source =
                """
                @Demo({
                        "a", "b",
                        "c",
                })
                class Test {}
                """;

        SourceModel sourceModel = SourceModel.create(source);
        ArrayInitializer arrayInitializer = firstNode(sourceModel.compilationUnit(), ArrayInitializer.class);
        assertNotNull(arrayInitializer);

        LiteralLayoutModel.ArrayLayout layout = LiteralLayoutModel.forArrayInitializer(sourceModel, arrayInitializer);
        assertTrue(layout.valid());
        assertTrue(layout.multiline());
        assertTrue(layout.annotationContext());
        assertFalse(layout.nested());
        assertEquals(3, layout.expressions().size());
        assertEquals("        ", layout.elementIndent());
    }

    @Test
    void testArrayLayoutClassifiesNestedMultilineArray()
    {
        String source =
                """
                class Test
                {
                    byte[][] values = {
                            {(byte) 1},
                            {(byte) 2},
                    };
                }
                """;

        SourceModel sourceModel = SourceModel.create(source);
        ArrayInitializer outer = firstNode(sourceModel.compilationUnit(), ArrayInitializer.class, node -> node.getParent() != null && !(node.getParent() instanceof ArrayInitializer));
        assertNotNull(outer);

        LiteralLayoutModel.ArrayLayout outerLayout = LiteralLayoutModel.forArrayInitializer(sourceModel, outer);
        assertTrue(outerLayout.valid());
        assertTrue(outerLayout.multiline());
        assertFalse(outerLayout.annotationContext());
        assertFalse(outerLayout.nested());

        ArrayInitializer firstNested = (ArrayInitializer) outerLayout.expressions().getFirst();
        LiteralLayoutModel.ArrayLayout nestedLayout = LiteralLayoutModel.forArrayInitializer(sourceModel, firstNested);
        assertTrue(nestedLayout.valid());
        assertTrue(nestedLayout.nested());
    }

    @Test
    void testEnumLayoutClassifiesMixedMultilineEnum()
    {
        String source =
                """
                class Test
                {
                    private enum State {
                        UNINITIALIZED, CONFIGURED,
                        INITIALIZED;
                    }
                }
                """;

        SourceModel sourceModel = SourceModel.create(source);
        EnumDeclaration enumDeclaration = firstNode(sourceModel.compilationUnit(), EnumDeclaration.class);
        assertNotNull(enumDeclaration);

        LiteralLayoutModel.EnumLayout layout = LiteralLayoutModel.forEnumDeclaration(sourceModel, enumDeclaration);
        assertTrue(layout.valid());
        assertTrue(layout.multiline());
        assertTrue(layout.simpleEnum());
        assertFalse(layout.allConstantsOnSameLine());
        assertFalse(layout.oneConstantPerLine());
    }

    @Test
    void testEnumLayoutClassifiesCompactMultilineEnum()
    {
        String source =
                """
                class Test
                {
                    private enum StoreType
                    {
                        POSTGRESQL, MEMORY,
                    }
                }
                """;

        SourceModel sourceModel = SourceModel.create(source);
        EnumDeclaration enumDeclaration = firstNode(sourceModel.compilationUnit(), EnumDeclaration.class);
        assertNotNull(enumDeclaration);

        LiteralLayoutModel.EnumLayout layout = LiteralLayoutModel.forEnumDeclaration(sourceModel, enumDeclaration);
        assertTrue(layout.valid());
        assertTrue(layout.multiline());
        assertTrue(layout.allConstantsOnSameLine());
        assertFalse(layout.oneConstantPerLine());
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
}
