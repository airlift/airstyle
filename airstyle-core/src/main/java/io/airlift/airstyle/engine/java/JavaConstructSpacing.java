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

import io.airlift.airstyle.engine.Spacing;
import org.eclipse.jdt.core.dom.ASTNode;

final class JavaConstructSpacing
{
    private static final Spacing SAME_LINE_BEFORE_BLOCK = Spacing.createSpacing(1, 1, 0, false, 0);
    private static final Spacing NEXT_LINE = Spacing.createSpacing(0, 0, 1, false, 0);
    private static final Spacing NEXT_LINE_KEEP_ONE_BLANK = Spacing.createSpacing(0, 0, 1, true, 1);

    private JavaConstructSpacing() {}

    static Spacing sameLineBeforeBlock()
    {
        return SAME_LINE_BEFORE_BLOCK;
    }

    static Spacing nextLine()
    {
        return NEXT_LINE;
    }

    static Spacing nextLineKeepingOneBlank()
    {
        return NEXT_LINE_KEEP_ONE_BLANK;
    }

    static Spacing betweenControlHeaderAndBody(ASTNode body)
    {
        return body instanceof org.eclipse.jdt.core.dom.Block
                ? SAME_LINE_BEFORE_BLOCK
                : NEXT_LINE;
    }

    static Spacing betweenElseAndBody(boolean hasComment, ASTNode body)
    {
        if (hasComment || !(body instanceof org.eclipse.jdt.core.dom.Block)) {
            return NEXT_LINE;
        }
        return SAME_LINE_BEFORE_BLOCK;
    }
}
