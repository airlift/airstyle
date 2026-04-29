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
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static io.airlift.airstyle.model.ParenthesizedListLayoutModel.OwnerKind.CLASS_INSTANCE_CREATION;
import static io.airlift.airstyle.model.ParenthesizedListLayoutModel.OwnerKind.ENUM_CONSTANT_DECLARATION;
import static io.airlift.airstyle.model.ParenthesizedListLayoutModel.OwnerKind.METHOD_DECLARATION;
import static io.airlift.airstyle.model.ParenthesizedListLayoutModel.OwnerKind.METHOD_INVOCATION;
import static io.airlift.airstyle.model.ParenthesizedListLayoutModel.OwnerKind.NORMAL_ANNOTATION;
import static io.airlift.airstyle.model.ParenthesizedListLayoutModel.OwnerKind.SUPER_METHOD_INVOCATION;
import static io.airlift.airstyle.model.ParenthesizedListLayoutModel.ParenthesizedListIndentMode.CONTINUATION_WITHOUT_FIRST;
import static io.airlift.airstyle.model.ParenthesizedListLayoutModel.ParenthesizedListIndentMode.SMART_CALL_SITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestFormatterParenthesizedListLayoutModel
{
    @Test
    void testRecognizesMethodInvocationArgumentsAsSmartCallSite()
    {
        String source =
                """
                class Test
                {
                    Object run(Object value, Object other)
                    {
                        return outer(inner(value, other),
                                other);
                    }
                }
                """;

        SourceModel sourceModel = SourceModel.create(source);
        ParenthesizedListLayoutModel layoutModel = ParenthesizedListLayoutModel.create(sourceModel);
        MethodInvocation inner = firstNode(sourceModel, MethodInvocation.class, node -> node.getName().getIdentifier().equals("inner"));

        ParenthesizedListLayoutModel.ParenthesizedListContext context = layoutModel.contextFor(inner);

        assertNotNull(context);
        assertEquals(METHOD_INVOCATION, context.parentOwnerKind());
        assertEquals(SMART_CALL_SITE, context.indentMode());
        assertEquals(0, context.itemIndex());
        assertEquals(2, context.itemCount());
        assertTrue(context.itemStartsInlineInParent());
        assertTrue(context.itemStartsMidLine());
        assertTrue(context.isFirstInlineInMultiItemList());
    }

    @Test
    void testRecognizesNonFirstMethodInvocationArgumentsAsSmartCallSite()
    {
        String source =
                """
                class Test
                {
                    Object run(Object first, Object second)
                    {
                        return outer(first,
                                second);
                    }
                }
                """;

        SourceModel sourceModel = SourceModel.create(source);
        ParenthesizedListLayoutModel layoutModel = ParenthesizedListLayoutModel.create(sourceModel);
        MethodInvocation invocation = firstNode(sourceModel, MethodInvocation.class, node -> node.getName().getIdentifier().equals("outer"));
        @SuppressWarnings("unchecked")
        List<Expression> arguments = invocation.arguments();

        ParenthesizedListLayoutModel.ParenthesizedListContext context = layoutModel.contextFor(arguments.get(1));

        assertNotNull(context);
        assertEquals(METHOD_INVOCATION, context.parentOwnerKind());
        assertEquals(SMART_CALL_SITE, context.indentMode());
        assertEquals(1, context.itemIndex());
        assertEquals(2, context.itemCount());
        assertFalse(context.itemStartsInlineInParent());
        assertFalse(context.itemStartsMidLine());
        assertFalse(context.isFirstInlineInMultiItemList());
    }

    @Test
    void testRecognizesConstructorArgumentsAsContinuationWithoutFirst()
    {
        String source =
                """
                class Test
                {
                    Object run(Object value, Object other)
                    {
                        return outer(
                                new Holder(value, other),
                                other);
                    }

                    record Holder(Object value, Object other) {}
                }
                """;

        SourceModel sourceModel = SourceModel.create(source);
        ParenthesizedListLayoutModel layoutModel = ParenthesizedListLayoutModel.create(sourceModel);
        ClassInstanceCreation creation = firstNode(sourceModel, ClassInstanceCreation.class);

        ParenthesizedListLayoutModel.ParenthesizedListContext context = layoutModel.contextFor(creation);

        assertNotNull(context);
        assertEquals(METHOD_INVOCATION, context.parentOwnerKind());
        assertEquals(SMART_CALL_SITE, context.indentMode());
        assertEquals(0, context.itemIndex());
        assertEquals(2, context.itemCount());
        assertFalse(context.itemStartsInlineInParent());
        assertFalse(context.itemStartsMidLine());
        assertFalse(context.isFirstInlineInMultiItemList());
    }

    @Test
    void testRecognizesSuperMethodInvocationArgumentsAsSmartCallSite()
    {
        String source =
                """
                class Base
                {
                    Object run(Object first, Object second)
                    {
                        return null;
                    }
                }

                class Test
                        extends Base
                {
                    @Override
                    Object run(Object first, Object second)
                    {
                        return super.run(
                                first,
                                second);
                    }
                }
                """;

        SourceModel sourceModel = SourceModel.create(source);
        ParenthesizedListLayoutModel layoutModel = ParenthesizedListLayoutModel.create(sourceModel);
        SuperMethodInvocation invocation = firstNode(sourceModel, SuperMethodInvocation.class);
        @SuppressWarnings("unchecked")
        List<Expression> arguments = invocation.arguments();

        ParenthesizedListLayoutModel.ParenthesizedListContext context = layoutModel.contextFor(arguments.getFirst());

        assertNotNull(context);
        assertEquals(SUPER_METHOD_INVOCATION, context.parentOwnerKind());
        assertEquals(SMART_CALL_SITE, context.indentMode());
        assertEquals(0, context.itemIndex());
        assertEquals(2, context.itemCount());
        assertFalse(context.itemStartsInlineInParent());
        assertFalse(context.itemStartsMidLine());
        assertFalse(context.isFirstInlineInMultiItemList());
    }

    @Test
    void testRecognizesClassInstanceCreationArgumentsAsContinuationWithoutFirst()
    {
        String source =
                """
                class Test
                {
                    Object run(Object value, Object other)
                    {
                        return new Holder(
                                value,
                                other);
                    }

                    record Holder(Object value, Object other) {}
                }
                """;

        SourceModel sourceModel = SourceModel.create(source);
        ParenthesizedListLayoutModel layoutModel = ParenthesizedListLayoutModel.create(sourceModel);
        ClassInstanceCreation creation = firstNode(sourceModel, ClassInstanceCreation.class);
        @SuppressWarnings("unchecked")
        List<Expression> arguments = creation.arguments();

        ParenthesizedListLayoutModel.ParenthesizedListContext context = layoutModel.contextFor(arguments.getFirst());

        assertNotNull(context);
        assertEquals(CLASS_INSTANCE_CREATION, context.parentOwnerKind());
        assertEquals(CONTINUATION_WITHOUT_FIRST, context.indentMode());
        assertEquals(0, context.itemIndex());
        assertEquals(2, context.itemCount());
        assertFalse(context.itemStartsInlineInParent());
        assertFalse(context.itemStartsMidLine());
        assertFalse(context.isFirstInlineInMultiItemList());
    }

    @Test
    void testRecognizesConstructorInvocationArgumentsAsContinuationWithoutFirst()
    {
        String source =
                """
                class Test
                {
                    Test(Object first, Object second)
                    {
                        this(
                                first,
                                second,
                                true);
                    }

                    Test(Object first, Object second, boolean copy)
                    {
                    }
                }
                """;

        SourceModel sourceModel = SourceModel.create(source);
        ParenthesizedListLayoutModel layoutModel = ParenthesizedListLayoutModel.create(sourceModel);
        ConstructorInvocation invocation = firstNode(sourceModel, ConstructorInvocation.class);
        @SuppressWarnings("unchecked")
        List<Expression> arguments = invocation.arguments();

        ParenthesizedListLayoutModel.ParenthesizedListContext context = layoutModel.contextFor(arguments.getFirst());

        assertNotNull(context);
        assertEquals(CONTINUATION_WITHOUT_FIRST, context.indentMode());
        assertEquals(0, context.itemIndex());
        assertEquals(3, context.itemCount());
        assertFalse(context.itemStartsInlineInParent());
        assertFalse(context.itemStartsMidLine());
        assertFalse(context.isFirstInlineInMultiItemList());
    }

    @Test
    void testRecognizesSuperConstructorInvocationArgumentsAsContinuationWithoutFirst()
    {
        String source =
                """
                class Base
                {
                    Base(Object first, Object second)
                    {
                    }
                }

                class Test
                        extends Base
                {
                    Test(Object first, Object second)
                    {
                        super(
                                first,
                                second);
                    }
                }
                """;

        SourceModel sourceModel = SourceModel.create(source);
        ParenthesizedListLayoutModel layoutModel = ParenthesizedListLayoutModel.create(sourceModel);
        SuperConstructorInvocation invocation = firstNode(sourceModel, SuperConstructorInvocation.class);
        @SuppressWarnings("unchecked")
        List<Expression> arguments = invocation.arguments();

        ParenthesizedListLayoutModel.ParenthesizedListContext context = layoutModel.contextFor(arguments.getFirst());

        assertNotNull(context);
        assertEquals(CONTINUATION_WITHOUT_FIRST, context.indentMode());
        assertEquals(0, context.itemIndex());
        assertEquals(2, context.itemCount());
        assertFalse(context.itemStartsInlineInParent());
        assertFalse(context.itemStartsMidLine());
        assertFalse(context.isFirstInlineInMultiItemList());
    }

    @Test
    void testRecognizesMethodDeclarationParametersAsSmartCallSite()
    {
        String source =
                """
                class Test
                {
                    void run(
                            Object first,
                            Object second)
                    {
                    }
                }
                """;

        SourceModel sourceModel = SourceModel.create(source);
        ParenthesizedListLayoutModel layoutModel = ParenthesizedListLayoutModel.create(sourceModel);
        MethodDeclaration declaration = firstNode(sourceModel, MethodDeclaration.class);
        @SuppressWarnings("unchecked")
        List<SingleVariableDeclaration> parameters = declaration.parameters();

        ParenthesizedListLayoutModel.ParenthesizedListContext context = layoutModel.contextFor(parameters.getFirst());

        assertNotNull(context);
        assertEquals(METHOD_DECLARATION, context.parentOwnerKind());
        assertEquals(SMART_CALL_SITE, context.indentMode());
        assertEquals(0, context.itemIndex());
        assertEquals(2, context.itemCount());
        assertFalse(context.itemStartsInlineInParent());
        assertFalse(context.itemStartsMidLine());
    }

    @Test
    void testRecognizesRecordComponentsAsContinuationWithoutFirst()
    {
        String source =
                """
                record Test(
                        Object first,
                        Object second)
                {
                }
                """;

        SourceModel sourceModel = SourceModel.create(source);
        ParenthesizedListLayoutModel layoutModel = ParenthesizedListLayoutModel.create(sourceModel);
        RecordDeclaration declaration = firstNode(sourceModel, RecordDeclaration.class);
        @SuppressWarnings("unchecked")
        List<SingleVariableDeclaration> components = declaration.recordComponents();

        ParenthesizedListLayoutModel.ParenthesizedListContext context = layoutModel.contextFor(components.get(1));

        assertNotNull(context);
        assertEquals(CONTINUATION_WITHOUT_FIRST, context.indentMode());
        assertEquals(1, context.itemIndex());
        assertEquals(2, context.itemCount());
        assertFalse(context.itemStartsInlineInParent());
        assertFalse(context.itemStartsMidLine());
    }

    @Test
    void testRecognizesAnnotationValuesAsContinuationWithoutFirst()
    {
        String source =
                """
                @Example(
                        first = "a",
                        second = "b")
                class Test
                {
                }
                """;

        SourceModel sourceModel = SourceModel.create(source);
        ParenthesizedListLayoutModel layoutModel = ParenthesizedListLayoutModel.create(sourceModel);
        NormalAnnotation annotation = firstNode(sourceModel, NormalAnnotation.class);
        @SuppressWarnings("unchecked")
        List<MemberValuePair> values = annotation.values();

        ParenthesizedListLayoutModel.ParenthesizedListContext context = layoutModel.contextFor(values.getFirst());

        assertNotNull(context);
        assertEquals(NORMAL_ANNOTATION, context.parentOwnerKind());
        assertEquals(CONTINUATION_WITHOUT_FIRST, context.indentMode());
        assertEquals(0, context.itemIndex());
        assertEquals(2, context.itemCount());
        assertFalse(context.itemStartsInlineInParent());
        assertFalse(context.itemStartsMidLine());
    }

    @Test
    void testRecognizesSingleMemberAnnotationValueAsContinuationWithoutFirst()
    {
        String source =
                """
                @Example(
                        value)
                class Test
                {
                }
                """;

        SourceModel sourceModel = SourceModel.create(source);
        ParenthesizedListLayoutModel layoutModel = ParenthesizedListLayoutModel.create(sourceModel);
        SingleMemberAnnotation annotation = firstNode(sourceModel, SingleMemberAnnotation.class);

        ParenthesizedListLayoutModel.ParenthesizedListContext context = layoutModel.contextFor(annotation.getValue());

        assertNotNull(context);
        assertEquals(CONTINUATION_WITHOUT_FIRST, context.indentMode());
        assertEquals(0, context.itemIndex());
        assertEquals(1, context.itemCount());
        assertFalse(context.itemStartsInlineInParent());
        assertFalse(context.itemStartsMidLine());
    }

    @Test
    void testRecognizesEnumConstantArgumentsAsContinuationWithoutFirst()
    {
        String source =
                """
                enum Test
                {
                    VALUE(
                            first,
                            second);
                }
                """;

        SourceModel sourceModel = SourceModel.create(source);
        ParenthesizedListLayoutModel layoutModel = ParenthesizedListLayoutModel.create(sourceModel);
        EnumConstantDeclaration constant = firstNode(sourceModel, EnumConstantDeclaration.class);
        @SuppressWarnings("unchecked")
        List<Expression> arguments = constant.arguments();

        ParenthesizedListLayoutModel.ParenthesizedListContext context = layoutModel.contextFor(arguments.get(1));

        assertNotNull(context);
        assertEquals(ENUM_CONSTANT_DECLARATION, context.parentOwnerKind());
        assertEquals(CONTINUATION_WITHOUT_FIRST, context.indentMode());
        assertEquals(1, context.itemIndex());
        assertEquals(2, context.itemCount());
        assertFalse(context.itemStartsInlineInParent());
        assertFalse(context.itemStartsMidLine());
    }

    @Test
    void testRecognizesConstructorOwnedNestedCreationContext()
    {
        String source =
                """
                class Test
                {
                    Object run(Object value, Object other)
                    {
                        return new Holder(new Nested(value, other),
                                other);
                    }

                    record Holder(Object value, Object other) {}

                    record Nested(Object value, Object other) {}
                }
                """;

        SourceModel sourceModel = SourceModel.create(source);
        ParenthesizedListLayoutModel layoutModel = ParenthesizedListLayoutModel.create(sourceModel);
        List<ClassInstanceCreation> creations = allNodes(sourceModel, ClassInstanceCreation.class);
        ClassInstanceCreation nested = creations.stream()
                .filter(node -> node.getType().toString().equals("Nested"))
                .findFirst()
                .orElseThrow();

        ParenthesizedListLayoutModel.ParenthesizedListContext context = layoutModel.contextFor(nested);

        assertNotNull(context);
        assertEquals(CLASS_INSTANCE_CREATION, context.parentOwnerKind());
        assertEquals(CONTINUATION_WITHOUT_FIRST, context.indentMode());
        assertEquals(0, context.itemIndex());
        assertEquals(2, context.itemCount());
        assertTrue(context.itemStartsInlineInParent());
        assertTrue(context.itemStartsMidLine());
        assertTrue(context.isFirstInlineInMultiItemList());
    }

    @Test
    void testCachesParenthesizedListLayoutModelPerSourceModel()
    {
        String source =
                """
                class Test
                {
                    void run(Object first, Object second) {}
                }
                """;

        SourceModel sourceModel = SourceModel.create(source);

        assertSame(ParenthesizedListLayoutModel.create(sourceModel), ParenthesizedListLayoutModel.create(sourceModel));
    }

    @Test
    void testReturnsNullForNodesOutsideParenthesizedLists()
    {
        String source =
                """
                class Test
                {
                    void run(Object first, Object second)
                    {
                    }
                }
                """;

        SourceModel sourceModel = SourceModel.create(source);
        ParenthesizedListLayoutModel layoutModel = ParenthesizedListLayoutModel.create(sourceModel);
        MethodDeclaration declaration = firstNode(sourceModel, MethodDeclaration.class);

        assertNull(layoutModel.contextFor(declaration));
    }

    private static <T extends ASTNode> T firstNode(SourceModel sourceModel, Class<T> nodeType)
    {
        return firstNode(sourceModel, nodeType, _ -> true);
    }

    private static <T extends ASTNode> T firstNode(SourceModel sourceModel, Class<T> nodeType, Predicate<T> predicate)
    {
        return allNodes(sourceModel, nodeType).stream()
                .filter(predicate)
                .findFirst()
                .orElseThrow();
    }

    private static <T extends ASTNode> List<T> allNodes(SourceModel sourceModel, Class<T> nodeType)
    {
        List<T> result = new ArrayList<>();
        sourceModel.compilationUnit().accept(new ASTVisitor()
        {
            @Override
            public void preVisit(ASTNode node)
            {
                if (nodeType.isInstance(node)) {
                    result.add(nodeType.cast(node));
                }
            }
        });
        return result;
    }
}
