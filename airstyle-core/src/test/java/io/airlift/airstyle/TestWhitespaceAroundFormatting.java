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
package io.airlift.airstyle;

import org.junit.jupiter.api.Test;

import static io.airlift.airstyle.FormatterAssertions.assertCanonicalFormatting;
import static io.airlift.airstyle.FormatterAssertions.assertFormatsOldToNew;

public class TestWhitespaceAroundFormatting
{
    @Test
    void testFormatterFixesWhitespaceAroundOperators()
    {
        String oldCode =
                """
                class Test {
                    int run(int a,int b)
                    {
                        if(a>0&&b>0) {
                            return a+b;
                        }
                        return a-b;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    int run(int a, int b)
                    {
                        if (a > 0 && b > 0) {
                            return a + b;
                        }
                        return a - b;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWhitespaceAfterUnaryMinusInArrayInitializer()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return new Object[] {
                                - 2147483648,
                                - 9223372036854775808L,
                        };
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return new Object[] {
                                -2147483648,
                                -9223372036854775808L,
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testBinaryGreaterThanBeforeOpenParenPreservesSpace()
    {
        String code =
                """
                class Test
                {
                    boolean check(double sum, double x)
                    {
                        return sum > (1.0 - x) * 2.0;
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testPostfixDecrementBeforeGreaterThanPreservesSpace()
    {
        String code =
                """
                class Test
                {
                    void countDown(int qty)
                    {
                        while (qty-- > 0) {
                            doWork();
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testWhenPatternKeywordBeforeOpenParenPreservesSpace()
    {
        String code =
                """
                class Test
                {
                    String name(Object o)
                    {
                        return switch (o) {
                            case String s when (s.isEmpty()) -> "empty";
                            case String s -> s;
                            default -> "none";
                        };
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testWhenAsMethodIdentifierStaysAdjacentToOpenParen()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        when (service.call()).thenReturn(result);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        when(service.call()).thenReturn(result);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testWhenKeywordInSwitchGuardKeepsSpaceBeforeOpenParen()
    {
        String code =
                """
                class Test
                {
                    String run(Object value)
                    {
                        return switch (value) {
                            case String s when (s.isEmpty()) -> "empty";
                            default -> "none";
                        };
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testTypeUseAnnotationOnArrayDimensionGetsSpaceBeforeBracket()
    {
        String oldCode =
                """
                class Test
                {
                    Slice @Nullable[] objectPath()
                    {
                        return null;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Slice @Nullable [] objectPath()
                    {
                        return null;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesRareTypeUseAnnotationPositions()
    {
        String oldCode =
                """
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Target;
                import java.util.List;
                import java.util.Map;

                class Test{private @TypeMark String @TypeMark[]@TypeMark[] matrix;private Map<@TypeMark String,? extends @TypeMark Number> values;<@TypeMark T extends @TypeMark Number&@TypeMark Comparable<T>>@TypeMark T convert(
                @TypeMark Test this,
                @TypeMark T value,
                List<@TypeMark ? super @TypeMark T> sink)throws @TypeMark Exception{sink.add(value);return value;}@TypeMark String @TypeMark[]@TypeMark[] matrix(){return matrix;}Map<@TypeMark String,? extends @TypeMark Number> values(){return values;}@Target({ElementType.TYPE_USE,ElementType.TYPE_PARAMETER})
                @interface TypeMark{}}
                """;

        String newCode =
                """
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Target;
                import java.util.List;
                import java.util.Map;

                class Test
                {
                    private @TypeMark String @TypeMark [] @TypeMark [] matrix;
                    private Map<@TypeMark String, ? extends @TypeMark Number> values;

                    <@TypeMark T extends @TypeMark Number & @TypeMark Comparable<T>> @TypeMark T convert(
                            @TypeMark Test this,
                            @TypeMark T value,
                            List<@TypeMark ? super @TypeMark T> sink)
                            throws @TypeMark Exception
                    {
                        sink.add(value);
                        return value;
                    }

                    @TypeMark
                    String @TypeMark [] @TypeMark [] matrix()
                    {
                        return matrix;
                    }

                    Map<@TypeMark String, ? extends @TypeMark Number> values()
                    {
                        return values;
                    }

                    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
                    @interface TypeMark {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }
}
