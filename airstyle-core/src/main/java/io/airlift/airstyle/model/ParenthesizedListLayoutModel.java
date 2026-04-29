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
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ParenthesizedListLayoutModel
{
    public enum OwnerKind
    {
        METHOD_INVOCATION,
        SUPER_METHOD_INVOCATION,
        CLASS_INSTANCE_CREATION,
        CONSTRUCTOR_INVOCATION,
        SUPER_CONSTRUCTOR_INVOCATION,
        ENUM_CONSTANT_DECLARATION,
        METHOD_DECLARATION,
        RECORD_DECLARATION,
        NORMAL_ANNOTATION,
        SINGLE_MEMBER_ANNOTATION,
        OTHER;

        public static OwnerKind from(ASTNode node)
        {
            return switch (node) {
                case MethodInvocation _ -> METHOD_INVOCATION;
                case SuperMethodInvocation _ -> SUPER_METHOD_INVOCATION;
                case ClassInstanceCreation _ -> CLASS_INSTANCE_CREATION;
                case ConstructorInvocation _ -> CONSTRUCTOR_INVOCATION;
                case SuperConstructorInvocation _ -> SUPER_CONSTRUCTOR_INVOCATION;
                case EnumConstantDeclaration _ -> ENUM_CONSTANT_DECLARATION;
                case MethodDeclaration _ -> METHOD_DECLARATION;
                case RecordDeclaration _ -> RECORD_DECLARATION;
                case NormalAnnotation _ -> NORMAL_ANNOTATION;
                case SingleMemberAnnotation _ -> SINGLE_MEMBER_ANNOTATION;
                default -> OTHER;
            };
        }
    }

    public enum ParenthesizedListIndentMode
    {
        CONTINUATION_WITHOUT_FIRST,
        SMART_CALL_SITE;

        public static ParenthesizedListIndentMode from(ASTNode owner)
        {
            return switch (owner) {
                case MethodInvocation _, SuperMethodInvocation _, MethodDeclaration _ -> SMART_CALL_SITE;
                default -> CONTINUATION_WITHOUT_FIRST;
            };
        }
    }

    public record ParenthesizedListContext(
            ASTNode parentOwner,
            OwnerKind parentOwnerKind,
            ParenthesizedListIndentMode indentMode,
            int itemIndex,
            int itemCount,
            boolean itemStartsInlineInParent,
            boolean itemStartsMidLine)
    {
        boolean isFirstInlineInMultiItemList()
        {
            return itemStartsInlineInParent && itemCount > 1 && itemIndex == 0;
        }
    }

    private final SourceModel sourceModel;
    private final Map<ASTNode, ParenthesizedListContext> contextCache = new IdentityHashMap<>();
    private final Map<ASTNode, Boolean> missingContextCache = new IdentityHashMap<>();

    private ParenthesizedListLayoutModel(SourceModel sourceModel)
    {
        this.sourceModel = sourceModel;
    }

    public static ParenthesizedListLayoutModel create(SourceModel sourceModel)
    {
        return sourceModel.cachedModel(ParenthesizedListLayoutModel.class, () -> new ParenthesizedListLayoutModel(sourceModel));
    }

    public static ParenthesizedListContext contextFor(SourceModel sourceModel, ASTNode item)
    {
        return create(sourceModel).contextFor(item);
    }

    public ParenthesizedListContext contextFor(ASTNode item)
    {
        ParenthesizedListContext cached = contextCache.get(item);
        if (cached != null) {
            return cached;
        }
        if (missingContextCache.containsKey(item)) {
            return null;
        }

        ParenthesizedListContext created = createContext(item);
        if (created == null) {
            missingContextCache.put(item, Boolean.TRUE);
            return null;
        }

        contextCache.put(item, created);
        return created;
    }

    private ParenthesizedListContext createContext(ASTNode item)
    {
        ASTNode parentOwner = item.getParent();
        if (parentOwner == null) {
            return null;
        }

        List<?> items = parenthesizedListItems(parentOwner);
        if (items == null || items.isEmpty()) {
            return null;
        }

        int itemIndex = indexOfIdentity(items, item);
        if (itemIndex < 0) {
            return null;
        }

        int openParen = parenthesizedListOpenParen(parentOwner);
        if (openParen < 0) {
            return null;
        }

        int itemStart = item.getStartPosition();
        boolean itemStartsInlineInParent = itemStart > openParen
                && !sourceModel.containsLineBreak(openParen + 1, itemStart);

        return new ParenthesizedListContext(
                parentOwner,
                OwnerKind.from(parentOwner),
                ParenthesizedListIndentMode.from(parentOwner),
                itemIndex,
                items.size(),
                itemStartsInlineInParent,
                sourceModel.startsMidLine(itemStart));
    }

    private static List<?> parenthesizedListItems(ASTNode owner)
    {
        return switch (owner) {
            case MethodInvocation methodInvocation -> methodInvocation.arguments();
            case SuperMethodInvocation superMethodInvocation -> superMethodInvocation.arguments();
            case ClassInstanceCreation classInstanceCreation -> classInstanceCreation.arguments();
            case ConstructorInvocation constructorInvocation -> constructorInvocation.arguments();
            case SuperConstructorInvocation superConstructorInvocation -> superConstructorInvocation.arguments();
            case EnumConstantDeclaration enumConstantDeclaration -> enumConstantDeclaration.arguments();
            case MethodDeclaration methodDeclaration -> methodDeclaration.parameters();
            case RecordDeclaration recordDeclaration -> recordDeclaration.recordComponents();
            case NormalAnnotation normalAnnotation -> normalAnnotation.values();
            case SingleMemberAnnotation singleMemberAnnotation -> List.of(singleMemberAnnotation.getValue());
            default -> null;
        };
    }

    private int parenthesizedListOpenParen(ASTNode owner)
    {
        String source = sourceModel.source();
        int searchStart;
        int searchEnd = owner.getStartPosition() + owner.getLength();
        switch (owner) {
            case MethodInvocation methodInvocation -> searchStart = methodInvocation.getName().getStartPosition() + methodInvocation.getName().getLength();
            case SuperMethodInvocation superMethodInvocation -> searchStart = superMethodInvocation.getName().getStartPosition() + superMethodInvocation.getName().getLength();
            case ClassInstanceCreation classInstanceCreation -> searchStart = classInstanceCreation.getStartPosition();
            case ConstructorInvocation constructorInvocation -> searchStart = constructorInvocation.getStartPosition();
            case SuperConstructorInvocation superConstructorInvocation -> searchStart = superConstructorInvocation.getStartPosition();
            case EnumConstantDeclaration enumConstantDeclaration -> searchStart = enumConstantDeclaration.getName().getStartPosition() + enumConstantDeclaration.getName().getLength();
            case MethodDeclaration methodDeclaration -> searchStart = methodDeclaration.getName().getStartPosition() + methodDeclaration.getName().getLength();
            case RecordDeclaration recordDeclaration -> searchStart = recordDeclaration.getName().getStartPosition() + recordDeclaration.getName().getLength();
            case NormalAnnotation normalAnnotation -> searchStart = normalAnnotation.getTypeName().getStartPosition() + normalAnnotation.getTypeName().getLength();
            case SingleMemberAnnotation singleMemberAnnotation -> searchStart = singleMemberAnnotation.getTypeName().getStartPosition() + singleMemberAnnotation.getTypeName().getLength();
            default -> {
                return -1;
            }
        }

        searchStart = Math.clamp(searchStart, 0, source.length());
        searchEnd = Math.clamp(searchEnd, searchStart, source.length());
        return sourceModel.findOpeningParen(searchStart, searchEnd);
    }

    private static int indexOfIdentity(List<?> items, Object target)
    {
        for (int index = 0; index < items.size(); index++) {
            if (Objects.equals(items.get(index), target)) {
                return index;
            }
        }
        return -1;
    }
}
