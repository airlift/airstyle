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

public class TestExtendsImplementsLineFormatting
{
    @Test
    void testFormatterFixesExtendsAndImplementsLinePlacement()
    {
        String oldCode =
                """
                class Test extends Base implements Orange, Banana, Apple {
                    void run()
                    {
                    }
                }
                """;

        String newCode =
                """
                class Test
                        extends Base
                        implements Apple,
                                   Banana,
                                   Orange
                {
                    void run() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesPermitsClauseOrderingAndAlignment()
    {
        String oldCode =
                """
                public sealed interface Fruit permits Orange, Banana, Apple, Grape {
                    String name();
                }
                """;

        String newCode =
                """
                public sealed interface Fruit
                        permits Apple,
                                Banana,
                                Grape,
                                Orange
                {
                    String name();
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsTwoItemImplementsSortedInline()
    {
        String oldCode =
                """
                class Test implements Runnable, Closeable {
                }
                """;

        String newCode =
                """
                class Test
                        implements Closeable, Runnable {}
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsTwoItemImplementsInline()
    {
        String oldCode =
                """
                class Test implements Orange, Apple {
                }
                """;

        String newCode =
                """
                class Test
                        implements Apple, Orange {}
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsAlreadyWrappedTwoItemPermitsWrapped()
    {
        String oldCode =
                """
                public sealed interface Fruit
                        permits Orange,
                                Apple
                {
                }
                """;

        String newCode =
                """
                public sealed interface Fruit
                        permits Apple,
                                Orange {}
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsTwoItemPermitsInline()
    {
        String oldCode =
                """
                public sealed interface Fruit permits Orange, Apple {
                }
                """;

        String newCode =
                """
                public sealed interface Fruit
                        permits Apple, Orange {}
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsAlreadyWrappedTwoItemExtendsWrapped()
    {
        String oldCode =
                """
                interface Test
                        extends Orange,
                                Apple
                {
                }
                """;

        String newCode =
                """
                interface Test
                        extends Apple,
                                Orange {}
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsAlreadyWrappedTwoItemImplementsWrapped()
    {
        String oldCode =
                """
                class Test
                        implements Orange,
                                   Apple
                {
                }
                """;

        String newCode =
                """
                class Test
                        implements Apple,
                                   Orange {}
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsTwoItemExtendsInline()
    {
        String oldCode =
                """
                interface Test extends Orange, Apple {
                }
                """;

        String newCode =
                """
                interface Test
                        extends Apple, Orange {}
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsTwoItemExtendsOrderWhenClauseAlreadyWrapped()
    {
        String code =
                """
                public interface StreamingResponse
                        extends Response, Closeable
                {
                    void close();
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsTwoItemImplementsOrderWhenClauseAlreadyWrapped()
    {
        String code =
                """
                class HttpClientLoggingListener
                        implements Response.Listener, Request.Listener
                {
                    int value;
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesImplementsClauseWhenCommentContainsKeyword()
    {
        String oldCode =
                """
                class Test implements /* implements marker */ Orange, Banana, Apple {
                }
                """;

        String newCode =
                """
                class Test
                        /* implements marker */
                        implements Apple,
                                   Banana,
                                   Orange {}
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesExtendsClauseWhenCommentContainsKeyword()
    {
        String oldCode =
                """
                interface Test extends /* extends marker */ Orange, Banana, Apple {
                }
                """;

        String newCode =
                """
                interface Test
                        /* extends marker */
                        extends Apple,
                                Banana,
                                Orange {}
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesPermitsClauseWhenCommentContainsKeyword()
    {
        String oldCode =
                """
                public sealed interface Fruit permits /* permits marker */ Orange, Banana, Apple {
                }
                """;

        String newCode =
                """
                public sealed interface Fruit
                        /* permits marker */
                        permits Apple,
                                Banana,
                                Orange {}
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsGenericExtendsBoundWhenFixingClassExtendsClause()
    {
        String oldCode =
                """
                abstract class Test<T extends Enum<T>> extends Base {
                }
                """;

        String newCode =
                """
                abstract class Test<T extends Enum<T>>
                        extends Base {}
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsGenericExtendsBoundWhenFixingInterfaceExtendsClause()
    {
        String oldCode =
                """
                interface Test<T extends Enum<T>> extends Orange, Banana, Apple {
                }
                """;

        String newCode =
                """
                interface Test<T extends Enum<T>>
                        extends Apple,
                                Banana,
                                Orange {}
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testAnnotationThenClassWithWrappedImplementsStaysIndentedCorrectly()
    {
        String code =
                """
                @Provider
                public class Foo
                        implements Bar
                {
                    Foo() {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testMultipleAnnotationsThenClassWithWrappedImplementsStaysIndentedCorrectly()
    {
        String code =
                """
                @Priority(0)
                @Provider
                @Consumes("text/json")
                public class JaxrsMapper
                        implements MessageBodyReader<Object>, MessageBodyWriter<Object>
                {
                    JaxrsMapper() {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testJavadocThenClassWithWrappedImplementsStaysIndentedCorrectly()
    {
        String code =
                """
                /**
                 * My class
                 */
                public final class AutoCloseableCloser
                        implements AutoCloseable
                {
                    AutoCloseableCloser() {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testSealedInterfaceAnnotatedKeepsExpectedIndent()
    {
        String code =
                """
                @ApiPolyResource
                public sealed interface SimpleRecursive
                        permits SimpleRecursive.NameAndAge,
                                SimpleRecursive.Schedule
                {
                    int id();
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testAnnotatedInnerStaticClassKeepsItsIndent()
    {
        String code =
                """
                class Outer
                {
                    @SuppressWarnings("CloneableClassWithoutClone")
                    private static class BoundedMap<K, V>
                            extends LinkedHashMap<K, V>
                    {
                        BoundedMap() {}
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsSpacesInsideTypeUseAnnotationArguments()
    {
        String code =
                """
                import jakarta.inject.Named;

                class Test
                        implements @Named("a  b") Alpha,
                                   @Named("c   d") Beta {}

                interface Alpha {}

                interface Beta {}
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsTypeHierarchyCommentsAndGenericBounds()
    {
        String code =
                """
                public sealed interface Fruit<T extends Comparable<T>>
                        /* permits marker */
                        permits Apple,
                                Banana,
                                Orange {}

                final class Apple
                        implements Fruit<String> {}

                final class Banana
                        implements Fruit<String> {}

                final class Orange
                        implements Fruit<String> {}
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsTrailingTypeClauseComment()
    {
        String code =
                """
                class Test<T extends Comparable<T>>
                        extends Base<T>
                        implements java.io.Serializable // required by caller
                {
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsTrailingLineCommentBetweenPermitsEntries()
    {
        String code =
                """
                public sealed interface Fruit
                        permits Apple,
                                Banana, // note
                                Orange {}
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsTrailingLineCommentBetweenImplementsEntries()
    {
        String code =
                """
                class Test
                        implements Apple,
                                   Banana, // note
                                   Orange {}
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsTrailingLineCommentBetweenExtendsEntries()
    {
        String code =
                """
                interface Test
                        extends Apple,
                                Banana, // note
                                Orange {}
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsClassMethodOrderWithWrappedImplementsClause()
    {
        String code =
                """
                import java.util.Map;

                class Actors
                        implements Handler
                {
                    public Actors()
                    {
                        init();
                    }

                    private static void createInjector()
                    {
                        work();
                    }

                    public static void main(String[] args)
                    {
                        run();
                    }

                    @Override
                    public String handleRequest(Map<String, Object> event, Object context)
                    {
                        done();
                        return "Success";
                    }
                }

                interface Handler
                {
                    String handleRequest(Map<String, Object> event, Object context);
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsInterfaceMethodOrderWithWrappedPermitsClause()
    {
        String code =
                """
                sealed interface PrincipalInfo
                        permits ServiceAccountInfo,
                                UserInfo
                {
                    String principalType();

                    static PrincipalInfo fromIdentity(Object identity)
                    {
                        return null;
                    }

                    default Object toIdentity()
                    {
                        return null;
                    }
                }

                final class ServiceAccountInfo
                        implements PrincipalInfo
                {
                    @Override
                    public String principalType()
                    {
                        return "a";
                    }
                }

                final class UserInfo
                        implements PrincipalInfo
                {
                    @Override
                    public String principalType()
                    {
                        return "b";
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }
}
