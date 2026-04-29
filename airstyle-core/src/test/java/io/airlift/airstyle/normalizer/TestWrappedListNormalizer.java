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

import org.junit.jupiter.api.Test;

import static io.airlift.airstyle.FormatterAssertions.assertCanonicalFormatting;
import static io.airlift.airstyle.FormatterAssertions.assertFormatsOldToNew;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestWrappedListNormalizer
{
    @Test
    void testFormatterFixesMethodInvocationMixedWrappedArgumentsWithFirstInline()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        execute(
                                arg1, arg2,
                                arg3);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        execute(arg1,
                                arg2,
                                arg3);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesMethodInvocationMixedWrappedArgumentsWithFirstWrapped()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        executeAgain(
                                arg1, arg2,
                                arg3);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        executeAgain(
                                arg1,
                                arg2,
                                arg3);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedInvocationWhenInlineFirstArgumentReachesIndentThreshold()
    {
        String oldCode =
                """
                class Test
                {
                    void run(Object runner)
                    {
                        assertFunctionFails(runner,
                                "x",
                                "y");
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(Object runner)
                    {
                        assertFunctionFails(
                                runner,
                                "x",
                                "y");
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsInlineFirstArgumentBelowIndentThreshold()
    {
        String code =
                """
                class Test
                {
                    void run(Object runner)
                    {
                        rendered(runner,
                                "x",
                                "y");
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesInlineFirstArgumentExactlyAtIndentThreshold()
    {
        String oldCode =
                """
                class Test
                {
                    void run(Object runner)
                    {
                        threshold(runner,
                                "x",
                                "y");
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(Object runner)
                    {
                        threshold(
                                runner,
                                "x",
                                "y");
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesInlineFirstArgumentPastIndentThreshold()
    {
        String oldCode =
                """
                class Test
                {
                    void run(Object runner)
                    {
                        thresholds(runner,
                                "x",
                                "y");
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(Object runner)
                    {
                        thresholds(
                                runner,
                                "x",
                                "y");
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsWrappedFirstArgumentWhenOwnerIsRendered()
    {
        String code =
                """
                class Test
                {
                    void run(Object runner)
                    {
                        rendered(
                                runner,
                                "x",
                                "y");
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesWrappedFirstArgumentInlineWhenOwnerIsShort()
    {
        String oldCode =
                """
                class Test
                {
                    void run(Object runner)
                    {
                        special(
                                runner,
                                "x",
                                "y");
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(Object runner)
                    {
                        special(runner,
                                "x",
                                "y");
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsMethodInvocationArgumentsWrappedTogetherOnNextLine()
    {
        String code =
                """
                class Test
                {
                    Object run(Object a, Object b, Object c)
                    {
                        return execute(
                                a, b, c);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsFactoryArgumentsWrappedTogetherOnNextLine()
    {
        String code =
                """
                import java.util.List;

                class Test
                {
                    List<String> run()
                    {
                        return List.of(
                                "abc", "foo", "xyz");
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesLambdaMethodInvocationArgumentsWithFirstInline()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        execute(
                                value -> value + 1, fallback,
                                retry);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        execute(value -> value + 1,
                                fallback,
                                retry);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesLambdaMethodInvocationArgumentsWithFirstWrapped()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        executeAgain(
                                value -> value + 1, fallback,
                                retry);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        executeAgain(
                                value -> value + 1,
                                fallback,
                                retry);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesLambdaMethodInvocationArgumentsWhenLambdaIsNotFirst()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        execute(
                                first, value -> value + 1,
                                retry);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        execute(first,
                                value -> value + 1,
                                retry);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesLambdaBlockBodyMethodInvocationArguments()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        executeAgain(
                                value -> {
                                    return value + 1;
                                }, fallback,
                                retry);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        executeAgain(
                                value -> {
                                    return value + 1;
                                },
                                fallback,
                                retry);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesLambdaBodyCallWrappedArguments()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        executeAgain(
                                value -> {
                                    return transform(
                                            first, second,
                                            third);
                                }, fallback,
                                retry);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        executeAgain(
                                _ -> {
                                    return transform(
                                            first,
                                            second,
                                            third);
                                },
                                fallback,
                                retry);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesChainedMethodInvocationArgumentsWithFirstWrapped()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        client.executor().execute(
                                arg1, arg2,
                                arg3);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        client.executor().execute(
                                arg1,
                                arg2,
                                arg3);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesChainedMethodInvocationArgumentsWithLongSelector()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        client.veryLongExecutorSelector().execute(
                                arg1, arg2,
                                arg3);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        client.veryLongExecutorSelector().execute(
                                arg1,
                                arg2,
                                arg3);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsCanonicalWrappedChainedMethodInvocationArguments()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        client.executor().execute(
                                arg1,
                                arg2,
                                arg3);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsLambdaBodyCallUnwrappedArguments()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        executeAgain(
                                _ -> {
                                    return transform(first, second, third);
                                },
                                fallback,
                                retry);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsInlineArgumentsWhenLambdaBodyIsSecondArgument()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        executeAgain(abc, what -> {
                            IO.println(what);
                        });
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterWrapsArgumentsWhenLambdaBodyIsFirstArgument()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        executeAgain(what -> {
                            IO.println(what);
                        }, abc);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsInlineSuperMethodInvocationArgumentsWhenFirstIsMultilineLambda()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        super.addListener(() -> {
                            if (super.isCancelled()) {
                                delegate.cancel(super.wasInterrupted());
                            }
                        }, directExecutor());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsInlineMethodInvocationArgumentsWhenFirstIsMultilineLambda()
    {
        String code =
                """
                class Test
                {
                    void run(Source source, Destination destination, boolean mayInterruptIfRunning)
                    {
                        source.addListener(() -> {
                            if (source.isCancelled()) {
                                destination.cancel(mayInterruptIfRunning);
                            }
                        }, directExecutor());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsInlineQualifiedInvocationArgumentsWhenSecondIsCommentedBlockLambda()
    {
        String code =
                """
                class Test
                {
                    Object run(Cache cache, Object key, Object value)
                    {
                        return cache.computeIfAbsent(key, current -> {
                            // keep the computed wrapper stable across chained selectors
                            String prefix = current.toString();
                            return input -> prefix + input;
                        }).apply(value);
                    }

                    interface Cache
                    {
                        java.util.function.Function<Object, Object> computeIfAbsent(Object key, java.util.function.Function<Object, java.util.function.Function<Object, Object>> loader);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesWrappedInvocationWhenFirstArgumentIsMultilineLambda()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        java.util.concurrent.CompletableFuture.runAsync(() -> {
                            System.out.println();
                        },
                                Runnable::run);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        java.util.concurrent.CompletableFuture.runAsync(
                                () -> {
                                    System.out.println();
                                },
                                Runnable::run);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsInlineMethodInvocationArgumentsWhenMiddleArgumentIsMultilineAnonymousClass()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        execute(first, new Callback()
                        {
                            @Override
                            public void run()
                            {
                                process();
                            }
                        }, third);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsInlineSingleMethodInvocationArgumentWhenLambdaExpressionBodyIsMultiline()
    {
        String code =
                """
                class Test
                {
                    void run(Object jdbi)
                    {
                        String version = jdbi.withHandle(handle ->
                                handle.createQuery("SELECT version()")
                                        .mapTo(String.class)
                                        .one());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsInlineSingleMethodInvocationArgumentWhenLambdaExpressionBodyIsMultilineBooleanExpression()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(item.requests())
                                .anyMatch(req -> req.permission() == LIST
                                        && req.itemIds() != null
                                        && req.itemIds().contains(TEST_PARENT_UUID)
                                        && req.itemTypes().contains(ItemType.DATABASE));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsInlineSingleClassInstanceCreationArgumentWhenArgumentIsMultilineExpression()
    {
        String code =
                """
                class Test
                {
                    Object run(Object requests)
                    {
                        return new CompoundItem(requests.stream()
                                .map(Request::inDeletedItems)
                                .collect(Collectors.toSet()));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsNestedChainedMethodIndentationInSingleMethodArgument()
    {
        String code =
                """
                class Test
                {
                    void run(Throwable throwable)
                    {
                        assertThat(ImmutableSet.copyOf(
                                Arrays.stream(throwable.getSuppressed())
                                        .map(Throwable::getCause)
                                        .map(Throwable::getMessage)
                                        .collect(Collectors.toSet())))
                                .isEqualTo(ImmutableSet.of("one", "two"));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsNestedChainedMethodIndentationInLambdaArgument()
    {
        String code =
                """
                class Test
                {
                    <V> Object run(Object allDoneFuture, Object futures)
                    {
                        return unmodifiableFuture(allDoneFuture.thenApply(_ ->
                                futures.stream()
                                        .map(CompletableFuture::join)
                                        .collect(Collectors.<V>toList())));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsNestedChainedMethodIndentationInSuperConstructorLambdaArgument()
    {
        String code =
                """
                class Base
                {
                    Base(Runnable task) {}
                }

                class Test
                        extends Base
                {
                    Test(HttpClient httpClient)
                    {
                        super(() -> {
                            httpClient.getDestinations().stream()
                                    .filter(HttpDestination.class::isInstance)
                                    .map(HttpDestination.class::cast)
                                    .forEach(_ -> {});
                        });
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsInlineBlockCommentsWithFollowingArgumentsInWrappedSuperCall()
    {
        String oldCode =
                """
                class Test
                        extends Throwable
                {
                    Test()
                    {
                        super(
                                "Request was cancelled",
                                /* cause */ null,
                                /* enable suppression */ false,
                                /* writable stack trace */ false);
                    }
                }
                """;

        String newCode =
                """
                class Test
                        extends Throwable
                {
                    Test()
                    {
                        super("Request was cancelled",
                                /* cause */ null,
                                /* enable suppression */ false,
                                /* writable stack trace */ false);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsFirstArgumentInlineAtContinuationBoundary()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        execute(
                                arg1,
                                arg2);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        execute(arg1,
                                arg2);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsCanonicalWrappedMethodInvocation()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        execute(arg1,
                                arg2,
                                arg3);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsFirstMethodArgumentInlineWhenWithinThreshold()
    {
        String code =
                """
                class Test
                {
                    void run(ApiServiceWithMethod apiServiceWithMethod, ContainerRequestContext containerRequestContext)
                    {
                        log.info("API call request: Service Name: [%s], Service Id: [%s], Service Version: [%s], Method Type: [%s], Custom Verb: [%s], Http Path: [%s]",
                                apiServiceWithMethod.service().service().name(),
                                apiServiceWithMethod.service().service().type().id(),
                                apiServiceWithMethod.service().service().type().version(),
                                apiServiceWithMethod.method().methodType(),
                                apiServiceWithMethod.method().customVerb().orElse(""),
                                containerRequestContext.getUriInfo().getRequestUri());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedFirstMethodArgumentWhenWithinThreshold()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        logger.info(
                                arg1, arg2,
                                arg3);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        logger.info(
                                arg1,
                                arg2,
                                arg3);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesMethodDeclarationMixedWrappedParameters()
    {
        String oldCode =
                """
                class Test
                {
                    void run(
                            String a, String b,
                            String c) {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(
                            String a,
                            String b,
                            String c)
                    {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesMethodDeclarationMixedWrappedParametersWithAnnotationsAndVarargs()
    {
        String oldCode =
                """
                @interface Nullable {}

                class Test
                {
                    void run(
                            @Nullable String a, final String b,
                            String... rest) {}
                }
                """;

        String newCode =
                """
                @interface Nullable {}

                class Test
                {
                    void run(
                            @Nullable String a,
                            final String b,
                            String... rest)
                    {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesConstructorDeclarationMixedWrappedParametersWithFirstInline()
    {
        String oldCode =
                """
                class Test
                {
                    Test(
                            String a, String b,
                            String c) {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    Test(String a,
                            String b,
                            String c)
                    {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesConstructorDeclarationMixedWrappedParametersWithFirstWrapped()
    {
        String oldCode =
                """
                class Test
                {
                    public Test(
                            String a, String b,
                            String c) {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    public Test(
                            String a,
                            String b,
                            String c)
                    {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesRecordComponentListMixedWrapped()
    {
        String oldCode =
                """
                record Pair(
                        String left, String right,
                        int code) {}
                """;

        String newCode =
                """
                record Pair(
                        String left,
                        String right,
                        int code) {}
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesAnnotationArgumentsMixedWrappedWithFirstInline()
    {
        String oldCode =
                """
                @interface Tag
                {
                    String a();

                    String b();

                    String c();
                }

                @Tag(
                        a = "x", b = "y",
                        c = "z")
                class Test {}
                """;

        String newCode =
                """
                @interface Tag
                {
                    String a();

                    String b();

                    String c();
                }

                @Tag(a = "x",
                        b = "y",
                        c = "z")
                class Test {}
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesAnnotationArgumentsMixedWrappedWithFirstWrapped()
    {
        String oldCode =
                """
                @interface VeryLongAnnotationName
                {
                    String a();

                    String b();

                    String c();
                }

                @VeryLongAnnotationName(
                        a = "x", b = "y",
                        c = "z")
                class Test {}
                """;

        String newCode =
                """
                @interface VeryLongAnnotationName
                {
                    String a();

                    String b();

                    String c();
                }

                @VeryLongAnnotationName(
                        a = "x",
                        b = "y",
                        c = "z")
                class Test {}
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesAllocationArgumentsMixedWrappedWithFirstWrapped()
    {
        String oldCode =
                """
                class Pair
                {
                    Pair(String left, String right, int code) {}
                }

                class Test
                {
                    Pair make()
                    {
                        return new Pair(
                                left, right,
                                code);
                    }
                }
                """;

        String newCode =
                """
                class Pair
                {
                    Pair(String left, String right, int code) {}
                }

                class Test
                {
                    Pair make()
                    {
                        return new Pair(
                                left,
                                right,
                                code);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesNestedInvocationAndAllocationArguments()
    {
        String oldCode =
                """
                class Pair
                {
                    Pair(String left, String right) {}
                }

                class Test
                {
                    void run()
                    {
                        handle(
                                run(a, b), new Pair(c, d),
                                e);
                    }
                }
                """;

        String newCode =
                """
                class Pair
                {
                    Pair(String left, String right) {}
                }

                class Test
                {
                    void run()
                    {
                        handle(run(a, b),
                                new Pair(c, d),
                                e);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesCommentedArgumentsMixedWrapped()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        execute(
                                arg1, // first
                                arg2, arg3);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        execute(arg1, // first
                                arg2,
                                arg3);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterNormalizesWrappedThisCallFirstArgumentInline()
    {
        String oldCode =
                """
                class Test
                {
                    Test()
                    {
                        this(
                                arg1,
                                arg2);
                    }

                    Test(String arg1, String arg2) {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    Test()
                    {
                        this(arg1,
                                arg2);
                    }

                    Test(String arg1, String arg2) {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesThisCallMixedWrapped()
    {
        String oldCode =
                """
                class Test
                {
                    Test()
                    {
                        this(
                                arg1, arg2,
                                arg3);
                    }

                    Test(String arg1, String arg2, String arg3) {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    Test()
                    {
                        this(arg1,
                                arg2,
                                arg3);
                    }

                    Test(String arg1, String arg2, String arg3) {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterNormalizesWrappedSuperCallFirstArgumentInline()
    {
        String oldCode =
                """
                class Base
                {
                    Base(String a, String b) {}
                }

                class Child
                        extends Base
                {
                    Child()
                    {
                        super(
                                arg1,
                                arg2);
                    }
                }
                """;

        String newCode =
                """
                class Base
                {
                    Base(String a, String b) {}
                }

                class Child
                        extends Base
                {
                    Child()
                    {
                        super(arg1,
                                arg2);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesSuperCallMixedWrapped()
    {
        String oldCode =
                """
                class Base
                {
                    Base(String a, String b, String c) {}
                }

                class Child
                        extends Base
                {
                    Child()
                    {
                        super(
                                arg1, arg2,
                                arg3);
                    }
                }
                """;

        String newCode =
                """
                class Base
                {
                    Base(String a, String b, String c) {}
                }

                class Child
                        extends Base
                {
                    Child()
                    {
                        super(arg1,
                                arg2,
                                arg3);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterUnwrapsSingleArgumentWhenFirstShouldBeInline()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        execute(
                                arg1);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        execute(arg1);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsSingleWrappedArgumentWhenFirstShouldWrap()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        executeAgain(
                                arg1);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsFirstArgumentInlineWhenOnlyLastArgumentIsMultiline()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        var value = new Something("abc", Policy.builder()
                                .setName("name")
                                .setEnabled(true)
                                .build());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterWrapsNonLastArgumentsWhenOnlyLastArgumentIsMultilineAndListIsWrapped()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(RequestContext principal, ItemType itemType)
                    {
                        return api
                                .runWithTransaction(
                                        principal, writeArchivedItem(itemId, itemType), ALL, handle ->
                                                handle.attach(daoClass).getDeletedById(itemId))
                                .orThrow(NotFoundException::new, NotFoundException::new);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(RequestContext principal, ItemType itemType)
                    {
                        return api
                                .runWithTransaction(
                                        principal,
                                        writeArchivedItem(itemId, itemType),
                                        ALL,
                                        handle ->
                                                handle.attach(daoClass).getDeletedById(itemId))
                                .orThrow(NotFoundException::new, NotFoundException::new);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedFirstArgumentIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(User user)
                    {
                        return api
                                .runTransaction(
                                user, PERMISSION)
                                .orElseThrow();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(User user)
                    {
                        return api
                                .runTransaction(
                                        user, PERMISSION)
                                .orElseThrow();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsWrappedArgumentsTogetherOnNextLineForConstructorInvocation()
    {
        String code =
                """
                class Test
                {
                    void run(Object redirectHandler, Object poller, Object knownTokenCache, Object timeout)
                    {
                        ExternalClient authenticator = new ExternalClient(
                                redirectHandler, poller, knownTokenCache.create(), timeout);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedArgumentsTogetherOnNextLineWithTrailingBlockLambda()
    {
        String code =
                """
                class Scratch
                {
                    OptionalLike runIfAllowed(Object principal, Object targetItem, Object combinationRule, Supplier supplier)
                    {
                        return null;
                    }

                    interface Supplier
                    {
                        boolean get();
                    }

                    static class OptionalLike
                    {
                        boolean orElse(boolean fallback)
                        {
                            return fallback;
                        }
                    }

                    Object principal;
                    Object targetItem;
                    Object combinationRule;
                    AllowedAction authorizedFunc;

                    static class AllowedAction
                    {
                        void run() {}
                    }

                    public boolean main()
                    {
                        return runIfAllowed(
                                principal, targetItem, combinationRule, () -> {
                                    authorizedFunc.run();
                                    return true;
                                })
                                .orElse(false);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterWrapsFirstArgumentWhenFormatterWrapsLaterAnonymousClassArgument()
    {
        String oldCode =
                """
                class Test
                {
                    void run(Object future)
                    {
                        Futures.addCallback(future, new FutureCallback<Object>()
                                {
                                    @Override
                                    public void onSuccess(Object value)
                                    {
                                        finish();
                                    }

                                    @Override
                                    public void onFailure(Throwable t)
                                    {
                                        fail(t);
                                    }
                                },
                                directExecutor());
                    }

                    interface FutureCallback<T>
                    {
                        void onSuccess(T value);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(Object future)
                    {
                        Futures.addCallback(
                                future,
                                new FutureCallback<Object>()
                                {
                                    @Override
                                    public void onSuccess(Object value)
                                    {
                                        finish();
                                    }

                                    @Override
                                    public void onFailure(Throwable t)
                                    {
                                        fail(t);
                                    }
                                },
                                directExecutor());
                    }

                    interface FutureCallback<T>
                    {
                        void onSuccess(T value);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesExpressionLambdaBodyContinuationIndentation()
    {
        String oldCode =
                """
                class T
                {
                    Object f(Object a, Object b)
                    {
                        return api
                                .m(a, b, c, h ->
                                           k(
                                                   () -> one(a),
                                                   () -> two(b)))
                                .n();
                    }
                }
                """;

        String newCode =
                """
                class T
                {
                    Object f(Object a, Object b)
                    {
                        return api
                                .m(a, b, c, _ ->
                                        k(() -> one(a),
                                                () -> two(b)))
                                .n();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesExpressionLambdaBodyWrappedArgumentsIndentation()
    {
        String oldCode =
                """
                class T
                {
                    Object f(Object a, Object b)
                    {
                        return api
                                .m(a, b, c, h ->
                                           withDuplicateHandling(
                                                   () -> one(a),
                                                   () -> two(b)))
                                .n();
                    }
                }
                """;

        String newCode =
                """
                class T
                {
                    Object f(Object a, Object b)
                    {
                        return api
                                .m(a, b, c, _ ->
                                        withDuplicateHandling(
                                                () -> one(a),
                                                () -> two(b)))
                                .n();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsFirstArgumentInlineForMultilineLambdaArguments()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        Thread thread = new Thread(() -> {
                            try {
                                stop();
                            }
                            catch (Exception e) {
                                log.error(e, "Failed while stopping lifecycle");
                            }
                        }, "cleanup");
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsFirstInlineArgumentChainIndentationWhenAlreadyIndented()
    {
        String code =
                """
                class Test
                {
                    void run(String path)
                    {
                        assertFailsValidation(new NodeConfig()
                                        .setAnnotations("team=a,region=b")
                                        .setAnnotationFile(path),
                                "configurationValid",
                                "only one of node.annotations or node.annotation-file can be set");
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsFirstInlineArgumentChainIndentationForNestedInvocation()
    {
        String code =
                """
                class Test
                {
                    void run(int[] entries, int numberOfEntries)
                    {
                        checkState(Ordering.from(comparingInt(e -> decodeBucketIndex((Integer) e)))
                                        .isOrdered(Ints.asList(Arrays.copyOf(entries, numberOfEntries))),
                                "entries are not sorted");
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsFirstInlineArgumentChainIndentationInValidationCall()
    {
        String code =
                """
                class Test
                {
                    void run(String path)
                    {
                        assertFailsValidation(new NodeConfig()
                                        .setAnnotations("team=a,region=b")
                                        .setAnnotationFile(path),
                                "configurationValid",
                                "only one of node.annotations or node.annotation-file can be set",
                                AssertTrue.class);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesUnindentedChainedInvocation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object a, Object b)
                    {
                        return api
                .call(a, b)
                                .orElseThrow();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object a, Object b)
                    {
                        return api
                                .call(a, b)
                                .orElseThrow();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverindentedSingleChainedInvocationSelector()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object a, Object b)
                    {
                        return client.call(a, b)
                                  .map(x -> x);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object a, Object b)
                    {
                        return client.call(a, b)
                                .map(x -> x);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderindentedSingleChainedInvocationSelector()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object a, Object b)
                    {
                        return client.call(a, b)
                          .map(x -> x);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object a, Object b)
                    {
                        return client.call(a, b)
                                .map(x -> x);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverindentedSingleChainedInvocationSelectorInAssignment()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(java.util.List<String> values)
                    {
                        Object result = values.stream()
                                  .collect(java.util.stream.Collectors.toList());
                        return result;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(java.util.List<String> values)
                    {
                        Object result = values.stream()
                                .collect(java.util.stream.Collectors.toList());
                        return result;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverindentedSelectorAfterArgumentlessIntermediateChain()
    {
        String oldCode =
                """
                import static com.google.common.collect.ImmutableMap.toImmutableMap;
                import static java.util.function.Function.identity;

                class Test
                {
                    void run()
                    {
                        methods = builder.values().stream()
                                  .collect(toImmutableMap(ThriftMethodMetadata::getName, identity()));
                    }
                }
                """;

        String newCode =
                """
                import static com.google.common.collect.ImmutableMap.toImmutableMap;
                import static java.util.function.Function.identity;

                class Test
                {
                    void run()
                    {
                        methods = builder.values().stream()
                                .collect(toImmutableMap(ThriftMethodMetadata::getName, identity()));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesLambdaBlockStatementIndentation()
    {
        String oldCode =
                """
                class T
                {
                    Object f(Object a)
                    {
                        return api
                                .m(a, b, c, h -> {
                                    x();
                                     return y();
                                })
                                .n();
                    }
                }
                """;

        String newCode =
                """
                class T
                {
                    Object f(Object a)
                    {
                        return api
                                .m(a, b, c, _ -> {
                                    x();
                                    return y();
                                })
                                .n();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedMethodArgumentChainedSelectorIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(String key, Object bucket, String content, String acl)
                    {
                        return new BucketObject(
                                key,
                                BucketObjectArgs.builder()
                                .bucket(bucket)
                                .key(key)
                                .content(content)
                                .acl(acl)
                                .build(),
                                CustomResourceOptions.builder()
                                        .provider(provider())
                                        .build());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(String key, Object bucket, String content, String acl)
                    {
                        return new BucketObject(
                                key,
                                BucketObjectArgs.builder()
                                        .bucket(bucket)
                                        .key(key)
                                        .content(content)
                                        .acl(acl)
                                        .build(),
                                CustomResourceOptions.builder()
                                        .provider(provider())
                                        .build());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedMethodArgumentChainedSelectorIndentationWhenBuilderAlreadyWrapped()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object bucket, String key)
                    {
                        executeWithVeryLongMethodName(
                                BucketObjectArgs.builder()
                                .bucket(bucket)
                                .key(key)
                                .build(),
                                "done");
                        return null;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object bucket, String key)
                    {
                        executeWithVeryLongMethodName(
                                BucketObjectArgs.builder()
                                        .bucket(bucket)
                                        .key(key)
                                        .build(),
                                "done");
                        return null;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedMethodArgumentChainedSelectorIndentationWithStandaloneCommentLine()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(String name, Object vpc)
                    {
                        return new Cluster(
                                name,
                                ClusterArgs.builder()
                                        .vpcId(vpc.id())
                                // use testing instance
                                .instanceType(TESTING_INSTANCE_TYPE)
                                        .build());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(String name, Object vpc)
                    {
                        return new Cluster(
                                name,
                                ClusterArgs.builder()
                                        .vpcId(vpc.id())
                                        // use testing instance
                                        .instanceType(TESTING_INSTANCE_TYPE)
                                        .build());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedMethodArgumentChainedSelectorIndentationWithIndentedStandaloneCommentLine()
    {
        String oldCode =
                """
                class Test
                {
                    void run(Object vpc)
                    {
                        ClusterArgs.builder()
                                .vpcId(vpc.id())
                            // use testing instance
                            .instanceType(TESTING_INSTANCE_TYPE)
                            .build();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(Object vpc)
                    {
                        ClusterArgs.builder()
                                .vpcId(vpc.id())
                                // use testing instance
                                .instanceType(TESTING_INSTANCE_TYPE)
                                .build();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedMethodArgumentChainedSelectorIndentationWithCommentContainingDot()
    {
        String oldCode =
                """
                class Test
                {
                    void run(Object vpc)
                    {
                        ClusterArgs.builder()
                                .vpcId(vpc.id())
                            // use testing instance: .instanceType(TESTING_INSTANCE_TYPE)
                            .instanceType(TESTING_INSTANCE_TYPE)
                            .build();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(Object vpc)
                    {
                        ClusterArgs.builder()
                                .vpcId(vpc.id())
                                // use testing instance: .instanceType(TESTING_INSTANCE_TYPE)
                                .instanceType(TESTING_INSTANCE_TYPE)
                                .build();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesSingleInlineArgumentChainedSelectorIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(String appName)
                    {
                        return CredentialArgs.builder()
                                .metadata(ObjectMetaArgs.builder()
                                .name("app-service-account")
                                .labels(ImmutableMap.of("appClass", appName))
                                .build())
                                .build();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(String appName)
                    {
                        return CredentialArgs.builder()
                                .metadata(ObjectMetaArgs.builder()
                                        .name("app-service-account")
                                        .labels(ImmutableMap.of("appClass", appName))
                                        .build())
                                .build();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedSingleArgumentChainedSelectorIndentationWhenBuilderAlreadyWrapped()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(String appName)
                    {
                        return CredentialArgs.builder()
                                .metadata(
                                        ObjectMetaArgs.builder()
                                        .name("app-service-account")
                                        .labels(ImmutableMap.of("appClass", appName))
                                        .build())
                                .build();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(String appName)
                    {
                        return CredentialArgs.builder()
                                .metadata(
                                        ObjectMetaArgs.builder()
                                                .name("app-service-account")
                                                .labels(ImmutableMap.of("appClass", appName))
                                                .build())
                                .build();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsFirstInlineArgumentChainIndentationWhenAlreadyIndentedInLambda()
    {
        String code =
                """
                class Test
                {
                    Object run(String path)
                    {
                        return withSupplier(() ->
                                validate(new NodeConfig()
                                                .setAnnotations("team=a,region=b")
                                                .setAnnotationFile(path),
                                        "configurationValid",
                                        "only one of node.annotations or node.annotation-file can be set"));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsFirstInlineArgumentChainIndentationForNestedInvocationInLambda()
    {
        String code =
                """
                class Test
                {
                    Object run(int[] entries, int numberOfEntries)
                    {
                        return withSupplier(() ->
                                checkState(Ordering.from(comparingInt(e -> decodeBucketIndex((Integer) e)))
                                                .isOrdered(Ints.asList(Arrays.copyOf(entries, numberOfEntries))),
                                        "entries are not sorted"));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsFirstInlineArgumentChainIndentationInValidationCallInLambda()
    {
        String code =
                """
                class Test
                {
                    Object run(String path)
                    {
                        return withSupplier(() ->
                                validate(new NodeConfig()
                                                .setAnnotations("team=a,region=b")
                                                .setAnnotationFile(path),
                                        "configurationValid",
                                        "only one of node.annotations or node.annotation-file can be set",
                                        AssertTrue.class));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsExpressionLambdaBodyIndentationForWrappedFlatMapFollowedBySelector()
    {
        String code =
                """
                class Test
                {
                    Object run(java.util.Optional<Object> sessionController, Object request)
                    {
                        return sessionController.flatMap(controller ->
                                        optionalSessionId(request).flatMap(_ -> controller.toString()))
                                .orElse("fallback");
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsExpressionLambdaBodyIndentationForWrappedInvocationFollowedBySelector()
    {
        String code =
                """
                class Test
                {
                    Object run(Database database, Object sessionId, Object key)
                    {
                        return database.withConnection(connection ->
                                        internalGetValue(connection, sessionId, key))
                                .map(Object::toString);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesUnindentedChainedInvocationInLambda()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object a, Object b)
                    {
                        return withSupplier(() -> api
                .call(a, b)
                                .orElseThrow());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object a, Object b)
                    {
                        return withSupplier(() -> api
                                .call(a, b)
                                .orElseThrow());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverindentedSingleChainedInvocationSelectorInLambda()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object a, Object b)
                    {
                        return withSupplier(() -> client.call(a, b)
                                  .map(x -> x));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object a, Object b)
                    {
                        return withSupplier(() -> client.call(a, b)
                                .map(x -> x));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderindentedSingleChainedInvocationSelectorInLambda()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object a, Object b)
                    {
                        return withSupplier(() -> client.call(a, b)
                          .map(x -> x));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object a, Object b)
                    {
                        return withSupplier(() -> client.call(a, b)
                                .map(x -> x));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedMethodArgumentChainedSelectorIndentationInLambda()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(String key, Object bucket, String content, String acl)
                    {
                        return withSupplier(() -> create(
                                key,
                                BucketObjectArgs.builder()
                                .bucket(bucket)
                                .key(key)
                                .content(content)
                                .acl(acl)
                                .build(),
                                CustomResourceOptions.builder()
                                        .provider(provider())
                                        .build()));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(String key, Object bucket, String content, String acl)
                    {
                        return withSupplier(() -> create(
                                key,
                                BucketObjectArgs.builder()
                                        .bucket(bucket)
                                        .key(key)
                                        .content(content)
                                        .acl(acl)
                                        .build(),
                                CustomResourceOptions.builder()
                                        .provider(provider())
                                        .build()));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedMethodArgumentChainedSelectorIndentationWhenBuilderAlreadyWrappedInLambda()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object bucket, String key)
                    {
                        return withSupplier(() -> executeWithVeryLongMethodName(
                                BucketObjectArgs.builder()
                                .bucket(bucket)
                                .key(key)
                                .build(),
                                "done"));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object bucket, String key)
                    {
                        return withSupplier(() -> executeWithVeryLongMethodName(
                                BucketObjectArgs.builder()
                                        .bucket(bucket)
                                        .key(key)
                                        .build(),
                                "done"));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesSingleInlineArgumentChainedSelectorIndentationInLambda()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(String appName)
                    {
                        return withSupplier(() -> CredentialArgs.builder()
                                .metadata(ObjectMetaArgs.builder()
                                .name("app-service-account")
                                .labels(ImmutableMap.of("appClass", appName))
                                .build())
                                .build());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(String appName)
                    {
                        return withSupplier(() -> CredentialArgs.builder()
                                .metadata(ObjectMetaArgs.builder()
                                        .name("app-service-account")
                                        .labels(ImmutableMap.of("appClass", appName))
                                        .build())
                                .build());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsExpressionLambdaReturnedChainIndentationInsideSelectorLineArgument()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return Outer.builder()
                                .value(source.apply(_ -> Inner.builder()
                                        .setA("a")
                                        .build()))
                                .build();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesExpressionLambdaReturnedChainIndentationInsideSelectorLineArgumentWhenLambdaOwnerHasTrailingSelector()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return Outer.builder()
                                .value(source.apply(v -> Inner.builder()
                                        .setA("a")
                                        .setB("b")
                                        .build()
                                        .toValue())
                                        .map(this::convert))
                                .build();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return Outer.builder()
                                .value(source
                                        .apply(_ -> Inner.builder()
                                                .setA("a")
                                                .setB("b")
                                                .build()
                                                .toValue())
                                        .map(this::convert))
                                .build();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedFirstSelectorForMultilineExpressionLambdaWithTrailingSelector()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return policy(user.applyValue(v -> Builder.create()
                                        .setA("a")
                                        .setB("b")
                                        .build()
                                        .toValue())
                                .orElse(fallback));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return policy(user
                                .applyValue(_ -> Builder.create()
                                        .setA("a")
                                        .setB("b")
                                        .build()
                                        .toValue())
                                .orElse(fallback));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedFirstSelectorForMultilineExpressionLambdaWithTrailingSelectorInMethodArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return execute(source.map(v -> Builder.create()
                                        .setA("a")
                                        .build()
                                        .toValue())
                                .orElse(fallback));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return execute(source
                                .map(_ -> Builder.create()
                                        .setA("a")
                                        .build()
                                        .toValue())
                                .orElse(fallback));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedNonBaseSelectorForMultilineExpressionLambdaWithTrailingSelector()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return policy(user.id().map(v -> Builder.create()
                                        .setA("a")
                                        .build()
                                        .toValue())
                                .orElse(fallback));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return policy(user.id()
                                .map(_ -> Builder.create()
                                        .setA("a")
                                        .build()
                                        .toValue())
                                .orElse(fallback));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsInlineFirstSelectorForMultilineExpressionLambdaWithoutTrailingSelector()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return policy(user.applyValue(_ -> Builder.create()
                                .setA("a")
                                .build()
                                .toValue()));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsAlreadyWrappedFirstSelectorForMultilineExpressionLambdaWithTrailingSelector()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return policy(user
                                .applyValue(_ -> Builder.create()
                                        .setA("a")
                                        .build()
                                        .toValue())
                                .orElse(fallback));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesWrappedSingleArgumentChainedSelectorIndentationWhenBuilderAlreadyWrappedInLambda()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(String appName)
                    {
                        return withSupplier(() -> CredentialArgs.builder()
                                .metadata(
                                        ObjectMetaArgs.builder()
                                        .name("app-service-account")
                                        .labels(ImmutableMap.of("appClass", appName))
                                        .build())
                                .build());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(String appName)
                    {
                        return withSupplier(() -> CredentialArgs.builder()
                                .metadata(
                                        ObjectMetaArgs.builder()
                                                .name("app-service-account")
                                                .labels(ImmutableMap.of("appClass", appName))
                                                .build())
                                .build());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverindentedSingleChainedInvocationSelectorInBlockLambda()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object a, Object b)
                    {
                        return withSupplier(() -> {
                            return client.call(a, b)
                                      .map(x -> x);
                        });
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object a, Object b)
                    {
                        return withSupplier(() -> {
                            return client.call(a, b)
                                    .map(x -> x);
                        });
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderindentedSingleChainedInvocationSelectorInBlockLambda()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object a, Object b)
                    {
                        return withSupplier(() -> {
                            return client.call(a, b)
                              .map(x -> x);
                        });
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object a, Object b)
                    {
                        return withSupplier(() -> {
                            return client.call(a, b)
                                    .map(x -> x);
                        });
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesInconsistentMultiChainedInvocationSelectorIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object id)
                    {
                        return service.getSession()
                                .lookup(id)
                                      .map(this::toDto)
                            .orElseThrow();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object id)
                    {
                        return service.getSession()
                                .lookup(id)
                                .map(this::toDto)
                                .orElseThrow();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesInconsistentMultiChainedInvocationSelectorIndentationInLambda()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object id)
                    {
                        return withSupplier(() -> service.getSession()
                                .lookup(id)
                                      .map(this::toDto)
                            .orElseThrow());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object id)
                    {
                        return withSupplier(() -> service.getSession()
                                .lookup(id)
                                .map(this::toDto)
                                .orElseThrow());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsInlinePrefixedNestedChainIndentationInLambdaExpression()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(java.util.Optional<String> queryParameter)
                    {
                        Object value = queryParameter.map(spec -> Splitter.on(',').trimResults().omitEmptyStrings().splitToList(spec)
                                        .stream()
                                        .map(sort -> {
                                            return sort;
                                        })
                                        .collect(toImmutableList()))
                                .orElseGet(List::of);
                        return value;
                    }
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterKeepsInlinePrefixedLambdaBlockIndentationInChainedInvocation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return source.call(value -> {
                                            a();
                                            return b();
                                        })
                                        .stream()
                                        .map(this::convert);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return source.call(_ -> {
                                    a();
                                    return b();
                                })
                                .stream()
                                .map(this::convert);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsInlinePrefixedWrappedSelectorIndentationInReturn()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object value)
                    {
                        return call(value).first().second()
                                        .third()
                                        .fourth();
                    }
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterKeepsInlinePrefixedWrappedSelectorIndentationInAssignment()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object value)
                    {
                        Object result = call(value).first().second()
                                        .third()
                                        .fourth();
                        return result;
                    }
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterKeepsInlinePrefixedWrappedSelectorIndentationInTernaryBranch()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(boolean enabled, Object value)
                    {
                        return enabled
                                ? call(value).first().second()
                                  .third()
                                  .fourth()
                                : fallback();
                    }
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterFixesInlinePrefixedWrappedSelectorIndentationInWrappedArguments()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object value, Object fallback)
                    {
                        return execute(
                                call(value).first().second()
                                                .third()
                                                .fourth(),
                                fallback);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object value, Object fallback)
                    {
                        return execute(
                                call(value).first().second()
                                        .third()
                                        .fourth(),
                                fallback);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesInconsistentInlinePrefixedWrappedSelectorIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object value)
                    {
                        return call(value).first().second()
                                        .third()
                                                  .fourth();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object value)
                    {
                        return call(value).first().second()
                                .third()
                                .fourth();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsOperatorPrefixedWrappedSelectorIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        Object result
                                = new Builder()
                                .first()
                                .second();
                        return result;
                    }
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterKeepsWrappedFirstArgumentIndentationInChainedInvocation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object options)
                    {
                        return fetch(
                                Request.builder()
                                        .setA("a")
                                        .setB("b")
                                        .build(),
                                options)
                                .map(this::convert);
                    }
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterKeepsInlineQualifiedInvocationWrappedArgumentsWithSingleLineFirstArgument()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return api.call(
                                a,
                                b,
                                c).orElse(fallback);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesSelectorAttachedWrappedInvocationArgumentsBackToOnePerLine()
    {
        String oldCode =
                """
                class Test
                {
                    boolean run(Object a, Object b)
                    {
                        return java.util.List.of(a,
                                b)
                                .isEmpty();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    boolean run(Object a, Object b)
                    {
                        return java.util.List.of(
                                        a,
                                        b)
                                .isEmpty();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsInlineQualifiedInvocationWrappedArgumentsWithTrailingBlockLambda()
    {
        String code =
                """
                public class Test
                {
                    static Object main()
                    {
                        return workflowApi.runIfAllowed(
                                principal,
                                roleIds,
                                ANY,
                                (_, _) -> {
                                    if (setDefault) {
                                        apply(handle, selection);
                                    }

                                    return result(selection)
                                            .cookie(cookie(principal, selection))
                                            .build();
                                }).orElse(fallback());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsMidLineSecondArgumentWrappedSelectorIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object name, Object provider)
                    {
                        return build(name, Request.builder()
                                .setA("a")
                                .build(), Options.builder()
                                .setProvider(provider)
                                .build());
                    }
                }
                """;

        assertCanonicalFormatting(oldCode);
    }

    @Test
    void testFormatterFixesNestedBuilderChainIndentationInSelectorLineArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return Outer.builder()
                                .put("x", Inner.builder()
                                .setA("a")
                                .setB("b")
                                .build())
                                .build();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return Outer.builder()
                                .put("x", Inner.builder()
                                        .setA("a")
                                        .setB("b")
                                        .build())
                                .build();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesNestedBuilderChainIndentationInSelectorLineArgumentInsideLambdaBlock()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return call(v -> {
                            return Outer.builder()
                                    .put("x", Inner.builder()
                                    .setA("a")
                                    .setB("b")
                                    .build())
                                    .build();
                        });
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return call(_ -> {
                            return Outer.builder()
                                    .put("x", Inner.builder()
                                            .setA("a")
                                            .setB("b")
                                            .build())
                                    .build();
                        });
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesMidLineWrappedArgumentsAtSingleContinuationIndent()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object first, Object second)
                    {
                        return execute(
                                        first,
                                        second)
                                .map(this::convert);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object first, Object second)
                    {
                        return execute(
                                first,
                                second)
                                .map(this::convert);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesMidLineInlineFirstArgumentSelectorIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return invoke(Request.builder()
                                        .setName("x")
                                        .build())
                                .map(this::convert);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return invoke(Request.builder()
                                .setName("x")
                                .build())
                                .map(this::convert);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesQualifiedInvocationInlineFirstArgumentSelectorIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object client)
                    {
                        return client.execute(Request.builder()
                                .setA("a")
                                .setB("b")
                                .build())
                                .stream();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object client)
                    {
                        return client.execute(Request.builder()
                                        .setA("a")
                                        .setB("b")
                                        .build())
                                .stream();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesQualifiedInvocationWrappedFirstArgumentIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object client, Object provider)
                    {
                        return client.lookup(
                                        Request.builder()
                                                .setName("x")
                                                .build(),
                                        Options.builder()
                                                .setProvider(provider)
                                                .build());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object client, Object provider)
                    {
                        return client.lookup(
                                Request.builder()
                                        .setName("x")
                                        .build(),
                                Options.builder()
                                        .setProvider(provider)
                                        .build());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesQualifiedInvocationWrappedArgumentsIndentationInChainedInvocation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object provider)
                    {
                        return Api.fetch(
                                Request.builder()
                                        .setName("x")
                                        .build(),
                                Options.builder()
                                        .setProvider(provider)
                                        .build())
                                .map(this::convert);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object provider)
                    {
                        return Api.fetch(
                                        Request.builder()
                                                .setName("x")
                                                .build(),
                                        Options.builder()
                                                .setProvider(provider)
                                                .build())
                                .map(this::convert);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsWrappedBuilderArgumentsWithWrappedNestedInvocations()
    {
        String code =
                """
                class Test
                {
                    Object run(Object labels)
                    {
                        return Builder.create()
                                .meta(Metadata.builder()
                                        .name("x")
                                        .labels(labels)
                                        .build())
                                .items(Item.builder()
                                                .name("a")
                                                .value("1")
                                                .build(),
                                        Item.builder()
                                                .name("b")
                                                .value("2")
                                                .build(),
                                        Item.builder()
                                                .name("c")
                                                .value("3")
                                                .build())
                                .build();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesWrappedBuilderArgumentsWithWrappedNestedInvocations()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object labels)
                    {
                        return Builder.create()
                                .meta(Metadata.builder()
                                        .name("x")
                                        .labels(labels)
                                        .build())
                                .items(Item.builder()
                                        .name("a")
                                        .value("1")
                                        .build(),
                                        Item.builder()
                                                .name("b")
                                                .value("2")
                                                .build(),
                                        Item.builder()
                                                .name("c")
                                                .value("3")
                                                .build())
                                .build();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object labels)
                    {
                        return Builder.create()
                                .meta(Metadata.builder()
                                        .name("x")
                                        .labels(labels)
                                        .build())
                                .items(Item.builder()
                                                .name("a")
                                                .value("1")
                                                .build(),
                                        Item.builder()
                                                .name("b")
                                                .value("2")
                                                .build(),
                                        Item.builder()
                                                .name("c")
                                                .value("3")
                                                .build())
                                .build();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsWrappedBuilderArgumentsWithMinimalSecondArgument()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return Builder.create()
                                .items(Item.builder()
                                                .build(),
                                        value)
                                .build();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesWrappedBuilderArgumentsWithMinimalSecondArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return Builder.create()
                                .items(Item.builder()
                                        .build(),
                                        value)
                                .build();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return Builder.create()
                                .items(Item.builder()
                                                .build(),
                                        value)
                                .build();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsWrappedBuilderArgumentsWithMinimalWrappedArguments()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return Builder.create()
                                .items(Item.builder()
                                                .build(),
                                        Item.builder()
                                                .build())
                                .build();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesWrappedBuilderArgumentsWithMinimalWrappedArguments()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return Builder.create()
                                .items(Item.builder()
                                        .build(),
                                        Item.builder()
                                                .build())
                                .build();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return Builder.create()
                                .items(Item.builder()
                                                .build(),
                                        Item.builder()
                                                .build())
                                .build();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesNestedSelectorWrappedArgumentsIndentationInsideConstructorArgument()
    {
        String oldCode =
                """
                class Test
                {
                    void run(Object bucket, Object context, Object customResourceOptions)
                    {
                        new BucketPolicy(
                                "logsPolicy",
                                BucketPolicyArgs.builder()
                                        .bucket(bucket.id())
                                        .policy(IamFunctions.getPolicyDocument(
                                                            GetPolicyDocumentArgs.builder()
                                                                    .conditions(
                                                                                    Condition.builder()
                                                                                            .value(bucket.arn().applyValue(List::of))
                                                                                            .build(),
                                                                                    Condition.builder()
                                                                                            .value(accountId())
                                                                                            .build())
                                                                    .build(),
                                                            InvokeOptions.builder()
                                                                    .provider(context.awsProvider())
                                                                    .build())
                                                .applyValue(Result::json)
                                                .applyValue(Either::ofLeft))
                                        .build(),
                                customResourceOptions);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(Object bucket, Object context, Object customResourceOptions)
                    {
                        new BucketPolicy(
                                "logsPolicy",
                                BucketPolicyArgs.builder()
                                        .bucket(bucket.id())
                                        .policy(IamFunctions.getPolicyDocument(
                                                        GetPolicyDocumentArgs.builder()
                                                                .conditions(
                                                                        Condition.builder()
                                                                                .value(bucket.arn().applyValue(List::of))
                                                                                .build(),
                                                                        Condition.builder()
                                                                                .value(accountId())
                                                                                .build())
                                                                .build(),
                                                        InvokeOptions.builder()
                                                                .provider(context.awsProvider())
                                                                .build())
                                                .applyValue(Result::json)
                                                .applyValue(Either::ofLeft))
                                        .build(),
                                customResourceOptions);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesSelectorLineWrappedArgumentsAtSelectorIndent()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return Builder.create()
                                .item(Item.builder()
                                        .parts(
                                                        Part.builder()
                                                                .build(),
                                                        Part.builder()
                                                                .build())
                                        .build())
                                .build();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return Builder.create()
                                .item(Item.builder()
                                        .parts(
                                                Part.builder()
                                                        .build(),
                                                Part.builder()
                                                        .build())
                                        .build())
                                .build();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsNestedBuilderSelectorsWithMixedWrappedArguments()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        execute(Outer.builder()
                                .selectors(ListBuilder.<SelectorArgs>builder()
                                        .add(SelectorArgs.builder()
                                                .name("one")
                                                .fieldSelectors(FieldSelector.builder()
                                                        .field("first")
                                                        .build())
                                                .build())
                                        .add(SelectorArgs.builder()
                                                .name("two")
                                                .fieldSelectors(FieldSelector.builder()
                                                                .field("first")
                                                                .build(),
                                                        FieldSelector.builder()
                                                                .field("second")
                                                                .build())
                                                .build())
                                        .build())
                                .build());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesNestedWrappedSelectorIndentationInSinglePass()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        use("x", Outer.builder()
                                .value(Inner.builder()
                                        .fieldSelectors(FieldSelector.builder()
                                                .name("a")
                                                .build())
                                        .build())
                                .build(),
                                options);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        use("x",
                                Outer.builder()
                                        .value(Inner.builder()
                                                .fieldSelectors(FieldSelector.builder()
                                                        .name("a")
                                                        .build())
                                                .build())
                                        .build(),
                                options);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedSingleWrappedSelectorArgumentWithTrailingSelector()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        use("x",
                                Outer.builder()
                                        .value(Inner.builder()
                                                .fieldSelectors(
                                                FieldSelector.builder()
                                                        .name("a")
                                                        .build())
                                                .build())
                                        .build(),
                                options);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        use("x",
                                Outer.builder()
                                        .value(Inner.builder()
                                                .fieldSelectors(
                                                        FieldSelector.builder()
                                                                .name("a")
                                                                .build())
                                                .build())
                                        .build(),
                                options);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesNestedSelectorWrappedArgumentsIndentationInsideNestedBuilderArgument()
    {
        String oldCode =
                """
                public class Test
                {
                    void test()
                    {
                        new BucketPolicy(
                                "logsPolicy",
                                BucketPolicyArgs.builder()
                                        .bucket(logsBucket.id())
                                        .policy(IamFunctions.getPolicyDocument(
                                                        GetPolicyDocumentArgs.builder()
                                                                .statements(GetPolicyDocumentStatementArgs.builder()
                                                                        .effect("Allow")
                                                                        .principals(GetPolicyDocumentStatementPrincipalArgs.builder()
                                                                                .type("Service")
                                                                                .identifiers("logging.s3.amazonaws.com")
                                                                                .build())
                                                                        .actions("s3:PutObject")
                                                                        .resources(logsBucket.arn().applyValue(arn -> List.of(arn + "/s3-access-logs/*")))
                                                                        .conditions(
                                                                                        GetPolicyDocumentStatementConditionArgs.builder()
                                                                                                .test("ArnLike")
                                                                                                .variable("aws:SourceArn")
                                                                                                .values(appDataBucket.arn().applyValue(List::of))
                                                                                                .build(),
                                                                                        GetPolicyDocumentStatementConditionArgs.builder()
                                                                                                .test("StringEquals")
                                                                                                .variable("aws:SourceAccount")
                                                                                                .values(accountId())
                                                                                                .build())
                                                                        .build())
                                                                .build(),
                                                        InvokeOptions.builder()
                                                                .provider(context.awsProvider())
                                                                .build())
                                                .applyValue(GetPolicyDocumentResult::json)
                                                .applyValue(Either::ofLeft))
                                        .build(),
                                customResourceOptions);
                    }
                }
                """;

        String newCode =
                """
                public class Test
                {
                    void test()
                    {
                        new BucketPolicy(
                                "logsPolicy",
                                BucketPolicyArgs.builder()
                                        .bucket(logsBucket.id())
                                        .policy(IamFunctions.getPolicyDocument(
                                                        GetPolicyDocumentArgs.builder()
                                                                .statements(GetPolicyDocumentStatementArgs.builder()
                                                                        .effect("Allow")
                                                                        .principals(GetPolicyDocumentStatementPrincipalArgs.builder()
                                                                                .type("Service")
                                                                                .identifiers("logging.s3.amazonaws.com")
                                                                                .build())
                                                                        .actions("s3:PutObject")
                                                                        .resources(logsBucket.arn().applyValue(arn -> List.of(arn + "/s3-access-logs/*")))
                                                                        .conditions(
                                                                                GetPolicyDocumentStatementConditionArgs.builder()
                                                                                        .test("ArnLike")
                                                                                        .variable("aws:SourceArn")
                                                                                        .values(appDataBucket.arn().applyValue(List::of))
                                                                                        .build(),
                                                                                GetPolicyDocumentStatementConditionArgs.builder()
                                                                                        .test("StringEquals")
                                                                                        .variable("aws:SourceAccount")
                                                                                        .values(accountId())
                                                                                        .build())
                                                                        .build())
                                                                .build(),
                                                        InvokeOptions.builder()
                                                                .provider(context.awsProvider())
                                                                .build())
                                                .applyValue(GetPolicyDocumentResult::json)
                                                .applyValue(Either::ofLeft))
                                        .build(),
                                customResourceOptions);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesQualifiedChainHeadInsideSelectorArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object options)
                    {
                        return Builder.create()
                                .value(Api.fetch(
                                                Request.builder()
                                                        .parts(
                                                                        Part.builder()
                                                                                .build(),
                                                                        Part.builder()
                                                                                .build())
                                                        .build(),
                                                options)
                                        .map(this::convert))
                                .build();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object options)
                    {
                        return Builder.create()
                                .value(Api.fetch(
                                                Request.builder()
                                                        .parts(
                                                                Part.builder()
                                                                        .build(),
                                                                Part.builder()
                                                                        .build())
                                                        .build(),
                                                options)
                                        .map(this::convert))
                                .build();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsSingleWrappedQualifiedArgumentWithTrailingSelectors()
    {
        String code =
                """
                class Test
                {
                    Object run(Object a, Object b)
                    {
                        return Builder.create()
                                .value(Output.tuple(
                                                a,
                                                b)
                                        .applyValue(v -> v))
                                .build();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsNestedSingleWrappedQualifiedArgumentWithTrailingSelectors()
    {
        String code =
                """
                class Test
                {
                    Object run(Object a, Object b)
                    {
                        return Builder.create()
                                .item(Item.builder()
                                        .value(Output.tuple(
                                                        a,
                                                        b)
                                                .applyValue(v -> v))
                                        .build())
                                .build();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesSelectorLineWrappedArgumentsWithScalarSibling()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object value)
                    {
                        return Builder.create()
                                .item(Item.builder()
                                        .parts(
                                                        Part.builder()
                                                                .build(),
                                                        value)
                                        .build())
                                .build();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object value)
                    {
                        return Builder.create()
                                .item(Item.builder()
                                        .parts(
                                                Part.builder()
                                                        .build(),
                                                value)
                                        .build())
                                .build();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesQualifiedChainHeadBeforeSelectorLineWrappedArguments()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object value)
                    {
                        return Builder.create()
                                .item(Item.builder()
                                        .value(Api.fetch(
                                                        Request.builder()
                                                                .parts(
                                                                                Part.builder()
                                                                                        .build(),
                                                                                value)
                                                                .build())
                                                .map(this::convert))
                                        .build())
                                .build();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object value)
                    {
                        return Builder.create()
                                .item(Item.builder()
                                        .value(Api.fetch(
                                                        Request.builder()
                                                                .parts(
                                                                        Part.builder()
                                                                                .build(),
                                                                        value)
                                                                .build())
                                                .map(this::convert))
                                        .build())
                                .build();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsWrappedTupleArgumentsInSelectorInvocation()
    {
        String code =
                """
                class Test
                {
                    Object run(Object image, Object logs, Object url, Object user)
                    {
                        return Builder.create()
                                .containerDefinitions(Output.tuple(
                                                image,
                                                logs,
                                                url,
                                                user)
                                        .applyValue(v -> v))
                                .build();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesNestedMapFactoryArgumentIndentationAtSingleContinuationLevel()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object bootstrap)
                    {
                        return bootstrap.setOptionalConfigurationProperties(Map.of(
                                        "a", "1",
                                        "b", "2"));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object bootstrap)
                    {
                        return bootstrap.setOptionalConfigurationProperties(Map.of(
                                "a", "1",
                                "b", "2"));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesNestedCollectionFactoryArgumentIndentationAtSingleContinuationLevel()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object values)
                    {
                        return assertThat(values).isEqualTo(ImmutableSet.of(
                                        "x",
                                        "y"));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object values)
                    {
                        return assertThat(values).isEqualTo(ImmutableSet.of(
                                "x",
                                "y"));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsSingleInlineArgumentSelectorIndentationWhenInvocationIsNotChained()
    {
        String code =
                """
                class Test
                {
                    Object run(Object value)
                    {
                        return Factory.get(new Holder<Object>() {}
                                .configure(value)
                                .build());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesInconsistentMultiChainedInvocationSelectorIndentationInBlockLambda()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object id)
                    {
                        return withSupplier(() -> {
                            return service.getSession()
                                    .lookup(id)
                                          .map(this::toDto)
                                .orElseThrow();
                        });
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object id)
                    {
                        return withSupplier(() -> {
                            return service.getSession()
                                    .lookup(id)
                                    .map(this::toDto)
                                    .orElseThrow();
                        });
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedArgumentWithWrappedChainIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object id, String name, int timeout)
                    {
                        return execute(
                                id,
                                      Policy.builder()
                                              .setName(name)
                                              .setEnabled(true)
                                              .build(),
                                timeout);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object id, String name, int timeout)
                    {
                        return execute(
                                id,
                                Policy.builder()
                                        .setName(name)
                                        .setEnabled(true)
                                        .build(),
                                timeout);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedArgumentWithWrappedChainIndentationInLambda()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object id, String name, int timeout)
                    {
                        return withSupplier(() -> execute(
                                id,
                                      Policy.builder()
                                              .setName(name)
                                              .setEnabled(true)
                                              .build(),
                                timeout));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object id, String name, int timeout)
                    {
                        return withSupplier(() -> execute(
                                id,
                                Policy.builder()
                                        .setName(name)
                                        .setEnabled(true)
                                        .build(),
                                timeout));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedArgumentWithWrappedChainIndentationInBlockLambda()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object id, String name, int timeout)
                    {
                        return withSupplier(() -> {
                            return execute(
                                    id,
                                          Policy.builder()
                                                  .setName(name)
                                                  .setEnabled(true)
                                                  .build(),
                                    timeout);
                        });
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object id, String name, int timeout)
                    {
                        return withSupplier(() -> {
                            return execute(
                                    id,
                                    Policy.builder()
                                            .setName(name)
                                            .setEnabled(true)
                                            .build(),
                                    timeout);
                        });
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedChainSelectorIndentationInAssignment()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object id)
                    {
                        var result = repository
                              .findById(id)
                                    .map(this::convert)
                           .orElse(null);
                        return result;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object id)
                    {
                        var result = repository
                                .findById(id)
                                .map(this::convert)
                                .orElse(null);
                        return result;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedChainSelectorIndentationInAssignmentWithLambdaExpression()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object id)
                    {
                        var result = withSupplier(() -> repository
                              .findById(id)
                                    .map(this::convert)
                           .orElse(null));
                        return result;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object id)
                    {
                        var result = withSupplier(() -> repository
                                .findById(id)
                                .map(this::convert)
                                .orElse(null));
                        return result;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedChainSelectorIndentationInAssignmentWithBlockLambda()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object id)
                    {
                        var result = withSupplier(() -> {
                            return repository
                                  .findById(id)
                                        .map(this::convert)
                               .orElse(null);
                        });
                        return result;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object id)
                    {
                        var result = withSupplier(() -> {
                            return repository
                                    .findById(id)
                                    .map(this::convert)
                                    .orElse(null);
                        });
                        return result;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesConstructorArgumentAndSelectorIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object status, Object left, Object right, Object metadata, String traceId)
                    {
                        return new Response(
                                status,
                                       compute(
                                               left,
                                               right),
                                  metadata)
                              .withTrace(traceId);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object status, Object left, Object right, Object metadata, String traceId)
                    {
                        return new Response(
                                status,
                                compute(
                                        left,
                                        right),
                                metadata)
                                .withTrace(traceId);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesConstructorArgumentAndSelectorIndentationInLambda()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object status, Object left, Object right, Object metadata, String traceId)
                    {
                        return withSupplier(() -> new Response(
                                status,
                                       compute(
                                               left,
                                               right),
                                  metadata)
                              .withTrace(traceId));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object status, Object left, Object right, Object metadata, String traceId)
                    {
                        return withSupplier(() -> new Response(
                                status,
                                compute(
                                        left,
                                        right),
                                metadata)
                                .withTrace(traceId));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesConstructorArgumentAndSelectorIndentationInBlockLambda()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object status, Object left, Object right, Object metadata, String traceId)
                    {
                        return withSupplier(() -> {
                            return new Response(
                                    status,
                                           compute(
                                                   left,
                                                   right),
                                      metadata)
                                  .withTrace(traceId);
                        });
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object status, Object left, Object right, Object metadata, String traceId)
                    {
                        return withSupplier(() -> {
                            return new Response(
                                    status,
                                    compute(
                                            left,
                                            right),
                                    metadata)
                                    .withTrace(traceId);
                        });
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesNestedConstructorFirstArgumentIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    void run(Object request, Throwable cause)
                    {
                        throw new Failure(new Detail(
                                        "INVALID_ARGUMENTS",
                                        "Invalid arguments for '%s'".formatted(request)), cause);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(Object request, Throwable cause)
                    {
                        throw new Failure(new Detail(
                                "INVALID_ARGUMENTS",
                                "Invalid arguments for '%s'".formatted(request)), cause);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesNestedInvocationFirstArgumentIndentationInConstructor()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object left, Object right, Object metadata)
                    {
                        return new Response(build(
                                        left,
                                        right), metadata);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object left, Object right, Object metadata)
                    {
                        return new Response(build(
                                left,
                                right), metadata);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedArgumentsExpressionLambdaBodyIndentationInLambda()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object user, Object permission, Object item)
                    {
                        return withSupplier(() -> api
                                .authorize(user, permission, ALL, handle ->
                                           withDuplicateHandling(
                                                   () -> name(item),
                                                   () -> handle.attach(Dao.class).update(item)))
                                .orThrow(NotFoundException::new, ForbiddenException::new));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object user, Object permission, Object item)
                    {
                        return withSupplier(() -> api
                                .authorize(user, permission, ALL, handle ->
                                        withDuplicateHandling(
                                                () -> name(item),
                                                () -> handle.attach(Dao.class).update(item)))
                                .orThrow(NotFoundException::new, ForbiddenException::new));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedArgumentsExpressionLambdaBodyIndentationInBlockLambda()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object user, Object permission, Object item)
                    {
                        return withSupplier(() -> {
                            return api
                                    .authorize(user, permission, ALL, handle ->
                                               withDuplicateHandling(
                                                       () -> name(item),
                                                       () -> handle.attach(Dao.class).update(item)))
                                    .orThrow(NotFoundException::new, ForbiddenException::new);
                        });
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object user, Object permission, Object item)
                    {
                        return withSupplier(() -> {
                            return api
                                    .authorize(user, permission, ALL, handle ->
                                            withDuplicateHandling(
                                                    () -> name(item),
                                                    () -> handle.attach(Dao.class).update(item)))
                                    .orThrow(NotFoundException::new, ForbiddenException::new);
                        });
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesParenthesizedRootChainedSelectorIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object source)
                    {
                        return (source)
                              .toString()
                                     .trim()
                           .toLowerCase();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object source)
                    {
                        return (source)
                                .toString()
                                .trim()
                                .toLowerCase();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesCastRootChainedSelectorIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object source)
                    {
                        return ((String) source)
                                      .trim()
                                      .toLowerCase()
                                      .substring(1);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object source)
                    {
                        return ((String) source)
                                .trim()
                                .toLowerCase()
                                .substring(1);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesParenthesizedTernaryRootChainedSelectorIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(boolean enabled, Object source, Object fallback)
                    {
                        return (enabled
                                     ? source.toString()
                                     : fallback.toString())
                                  .trim()
                            .toLowerCase();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(boolean enabled, Object source, Object fallback)
                    {
                        return (enabled
                                ? source.toString()
                                : fallback.toString())
                                .trim()
                                .toLowerCase();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesParenthesizedBinaryRootChainedSelectorIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return (first()
                                     + second())
                                  .toString()
                                             .trim();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return (first()
                                + second())
                                .toString()
                                .trim();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesSuperInvocationWrappedArgumentChainedSelectorIndentation()
    {
        String oldCode =
                """
                class Base
                {
                    Base(Object left, Object right) {}
                }

                class Test
                        extends Base
                {
                    Test(Object left, Object right)
                    {
                        super(
                                Builder.create()
                                          .setLeft(left)
                                          .setRight(right)
                                          .build(),
                                right);
                    }
                }
                """;

        String newCode =
                """
                class Base
                {
                    Base(Object left, Object right) {}
                }

                class Test
                        extends Base
                {
                    Test(Object left, Object right)
                    {
                        super(
                                Builder.create()
                                        .setLeft(left)
                                        .setRight(right)
                                        .build(),
                                right);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesThisInvocationWrappedArgumentChainedSelectorIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    Test(Object left, Object right)
                    {
                        this(
                                Builder.create()
                                          .setLeft(left)
                                          .setRight(right)
                                          .build(),
                                right,
                                false);
                    }

                    Test(Object left, Object right, boolean enabled) {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    Test(Object left, Object right)
                    {
                        this(Builder.create()
                                        .setLeft(left)
                                        .setRight(right)
                                        .build(),
                                right,
                                false);
                    }

                    Test(Object left, Object right, boolean enabled) {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedMapOfArgumentsInPairs()
    {
        String oldCode =
                """
                import java.util.Map;

                class Test
                {
                    static final Map<String, String> FRUITS = Map.of(
                            "best",
                            "apple",
                            "worst",
                            "grapefruit");
                }
                """;

        String newCode =
                """
                import java.util.Map;

                class Test
                {
                    static final Map<String, String> FRUITS = Map.of(
                            "best", "apple",
                            "worst", "grapefruit");
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedImmutableMapOfArgumentsInPairs()
    {
        String oldCode =
                """
                import com.google.common.collect.ImmutableMap;

                class Test
                {
                    static final ImmutableMap<String, String> FRUITS = ImmutableMap.of(
                            "best",
                            "apple",
                            "worst",
                            "grapefruit");
                }
                """;

        String newCode =
                """
                import com.google.common.collect.ImmutableMap;

                class Test
                {
                    static final ImmutableMap<String, String> FRUITS = ImmutableMap.of(
                            "best", "apple",
                            "worst", "grapefruit");
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedImmutableListMultimapOfArgumentsInPairs()
    {
        String oldCode =
                """
                import com.google.common.collect.ImmutableListMultimap;

                class Test
                {
                    static final ImmutableListMultimap<String, String> FRUITS = ImmutableListMultimap.of(
                            "best",
                            "apple",
                            "worst",
                            "grapefruit");
                }
                """;

        String newCode =
                """
                import com.google.common.collect.ImmutableListMultimap;

                class Test
                {
                    static final ImmutableListMultimap<String, String> FRUITS = ImmutableListMultimap.of(
                            "best", "apple",
                            "worst", "grapefruit");
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedAttributesOfArgumentsInPairs()
    {
        String oldCode =
                """
                import io.opentelemetry.api.common.Attributes;

                class Test
                {
                    static final Object FRUITS = Attributes.of(
                            "best",
                            "apple",
                            "worst",
                            "grapefruit");
                }
                """;

        String newCode =
                """
                import io.opentelemetry.api.common.Attributes;

                class Test
                {
                    static final Object FRUITS = Attributes.of(
                            "best", "apple",
                            "worst", "grapefruit");
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedQualifiedAttributesOfArgumentsInPairs()
    {
        String oldCode =
                """
                class Test
                {
                    static final Object FRUITS = io.opentelemetry.api.common.Attributes.of(
                            "best",
                            "apple",
                            "worst",
                            "grapefruit");
                }
                """;

        String newCode =
                """
                class Test
                {
                    static final Object FRUITS = io.opentelemetry.api.common.Attributes.of(
                            "best", "apple",
                            "worst", "grapefruit");
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedQualifiedImmutableMapOfArgumentsInPairs()
    {
        String oldCode =
                """
                class Test
                {
                    static final Object FRUITS = com.example.ImmutableMap.of(
                            "best",
                            "apple",
                            "worst",
                            "grapefruit");
                }
                """;

        String newCode =
                """
                class Test
                {
                    static final Object FRUITS = com.example.ImmutableMap.of(
                            "best", "apple",
                            "worst", "grapefruit");
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedAttributesOfTextBlockValueWithoutPairWrapping()
    {
        String oldCode =
                """
                import io.opentelemetry.api.common.Attributes;

                class Test
                {
                    static final Object VALUES = Attributes.of(
                            "hello", \"\"\"
                            text block here
                            \"\"\".formatted(name),
                            "bye",
                            "value");
                }
                """;

        String newCode =
                """
                import io.opentelemetry.api.common.Attributes;

                class Test
                {
                    static final Object VALUES = Attributes.of(
                            "hello",
                            \"\"\"
                            text block here
                            \"\"\".formatted(name),
                            "bye", "value");
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedMapOfTextBlockValueWithoutPairWrapping()
    {
        String oldCode =
                """
                import java.util.Map;

                class Test
                {
                    static final Map<String, String> VALUES = Map.of(
                            "hello", \"\"\"
                            text block here
                            \"\"\");
                }
                """;

        String newCode =
                """
                import java.util.Map;

                class Test
                {
                    static final Map<String, String> VALUES = Map.of(
                            "hello",
                            \"\"\"
                            text block here
                            \"\"\");
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedImmutableMapOfFormattedTextBlockValueWithoutPairWrapping()
    {
        String oldCode =
                """
                import com.google.common.collect.ImmutableMap;

                class Test
                {
                    static final ImmutableMap<String, String> VALUES = ImmutableMap.of(
                            "hello", \"\"\"
                            text block here
                            \"\"\".formatted(name),
                            "bye",
                            "value");
                }
                """;

        String newCode =
                """
                import com.google.common.collect.ImmutableMap;

                class Test
                {
                    static final ImmutableMap<String, String> VALUES = ImmutableMap.of(
                            "hello",
                            \"\"\"
                            text block here
                            \"\"\".formatted(name),
                            "bye", "value");
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsSingleLineMapOfArgumentsUnwrapped()
    {
        String code =
                """
                import java.util.Map;

                class Test
                {
                    static final Map<String, String> FRUITS = Map.of("best", "apple", "worst", "grapefruit");
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesNestedWrappedListsInSinglePass()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Resolver resolver)
                    {
                        return new Node(
                                A,
                                B,
                                ImmutableList.of(
                                    new Call(
                                            resolver.op(X, ImmutableList.of(C, D)),
                                            ImmutableList.of(left(E, F), right(G, H))),
                                    new Node(
                                            A,
                                            B,
                                            ImmutableList.of(
                                                new Call(
                                                        resolver.op(X, ImmutableList.of(C, D)),
                                                        ImmutableList.of(left(E, F), right(G, H))),
                                                new Node(
                                                        A,
                                                        B,
                                                        ImmutableList.of(
                                                            new Call(
                                                                    resolver.op(X, ImmutableList.of(C, D)),
                                                                    ImmutableList.of(left(E, F), right(G, H))),
                                                            new Node(
                                                                    A,
                                                                    B,
                                                                    ImmutableList.of(
                                                                        new Call(
                                                                                resolver.op(X, ImmutableList.of(C, D)),
                                                                                ImmutableList.of(left(E, F), right(G, H))),
                                                                        new Call(
                                                                                resolver.op(X, ImmutableList.of(C, D)),
                                                                                ImmutableList.of(left(E, F), right(G, H)))), ImmutableList.of())), ImmutableList.of())), ImmutableList.of())),
                                ImmutableList.of());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Resolver resolver)
                    {
                        return new Node(
                                A,
                                B,
                                ImmutableList.of(
                                        new Call(
                                                resolver.op(X, ImmutableList.of(C, D)),
                                                ImmutableList.of(left(E, F), right(G, H))),
                                        new Node(
                                                A,
                                                B,
                                                ImmutableList.of(
                                                        new Call(
                                                                resolver.op(X, ImmutableList.of(C, D)),
                                                                ImmutableList.of(left(E, F), right(G, H))),
                                                        new Node(
                                                                A,
                                                                B,
                                                                ImmutableList.of(
                                                                        new Call(
                                                                                resolver.op(X, ImmutableList.of(C, D)),
                                                                                ImmutableList.of(left(E, F), right(G, H))),
                                                                        new Node(
                                                                                A,
                                                                                B,
                                                                                ImmutableList.of(
                                                                                        new Call(
                                                                                                resolver.op(X, ImmutableList.of(C, D)),
                                                                                                ImmutableList.of(left(E, F), right(G, H))),
                                                                                        new Call(
                                                                                                resolver.op(X, ImmutableList.of(C, D)),
                                                                                                ImmutableList.of(left(E, F), right(G, H)))),
                                                                                ImmutableList.of())),
                                                                ImmutableList.of())),
                                                ImmutableList.of())),
                                ImmutableList.of());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsWrappedArgumentsAtSingleContinuationLevelInsidePlainInvocation()
    {
        String code =
                """
                public class Test
                {
                    static void main()
                    {
                        assertThat(
                                client.fetch(
                                        user,
                                        access(testCase.permission(), item.itemId(), NOTEBOOK),
                                        ALL,
                                        () -> true).done())
                                .isTrue();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedSingleArgumentInTopLevelQualifiedChainWithTrailingSelector()
    {
        String code =
                """
                public class Test
                {
                    static void main()
                    {
                        Output<String> value = Client.get(Request.builder()
                                .setId(id)
                                .setNested(Nested.builder()
                                        .setName(name)
                                        .build())
                                .build()).map(Result::id);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsIdentityStoreStyleQualifiedInvocationInlineFirstArgumentSelectorIndentation()
    {
        String code =
                """
                class Scratch
                {
                    public static void main(String[] args)
                    {
                        Output<String> userId = IdentitystoreFunctions.getUser(GetUserArgs.builder()
                                .identityStoreId(identityStoreId)
                                .alternateIdentifier(GetUserAlternateIdentifierArgs.builder()
                                        .uniqueAttribute(GetUserAlternateIdentifierUniqueAttributeArgs.builder()
                                                .attributePath("UserName")
                                                .attributeValue(userShortName + "@example.com")
                                                .build())
                                        .build())
                                .build()).applyValue(GetUserResult::userId);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedArgumentsInTopLevelQualifiedChainWithTrailingSelector()
    {
        String code =
                """
                public class Test
                {
                    static void main()
                    {
                        Output<List<String>> deliveryStreamArn = Output.format(
                                        "arn:aws:firehose:%s:%s:deliverystream/%s",
                                        context.awsRegion(),
                                        accountId,
                                        deliveryStreamName)
                                .applyValue(ImmutableList::of);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedBuilderArgumentsInTopLevelQualifiedChainWithTrailingSelector()
    {
        String code =
                """
                class Test
                {
                    Object run(Object context)
                    {
                        return Client.fetch(
                                        Request.builder().build(),
                                        Options.builder().provider(context.awsProvider()).build())
                                .map(Result::id);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesStaticQualifiedSingleWrappedBuilderArgumentWithTrailingSelector()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return Ec2Functions.getInstanceTypeOffering(GetInstanceTypeOfferingArgs.builder()
                                .filters(GetInstanceTypeOfferingFilterArgs.builder()
                                        .name("instance-type")
                                        .values(preferredInstanceTypes)
                                        .build())
                                .preferredInstanceTypes(preferredInstanceTypes)
                                .build())
                                .applyValue(GetInstanceTypeOfferingResult::instanceType);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return Ec2Functions.getInstanceTypeOffering(GetInstanceTypeOfferingArgs.builder()
                                        .filters(GetInstanceTypeOfferingFilterArgs.builder()
                                                .name("instance-type")
                                                .values(preferredInstanceTypes)
                                                .build())
                                        .preferredInstanceTypes(preferredInstanceTypes)
                                        .build())
                                .applyValue(GetInstanceTypeOfferingResult::instanceType);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesStaticQualifiedSingleWrappedBuilderArgumentWithWrappedNestedArgumentsAndTrailingSelector()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return Ec2Functions.getSubnets(GetSubnetsArgs.builder()
                                .filters(
                                        GetSubnetsFilterArgs.builder()
                                                .name("vpc-id")
                                                .values(vpcIdOutput.applyValue(List::of))
                                                .build(),
                                        GetSubnetsFilterArgs.builder()
                                                .name("map-public-ip-on-launch")
                                                .values(List.of("false"))
                                                .build())
                                .build())
                                .applyValue(GetSubnetsResult::ids);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return Ec2Functions.getSubnets(GetSubnetsArgs.builder()
                                        .filters(
                                                GetSubnetsFilterArgs.builder()
                                                        .name("vpc-id")
                                                        .values(vpcIdOutput.applyValue(List::of))
                                                        .build(),
                                                GetSubnetsFilterArgs.builder()
                                                        .name("map-public-ip-on-launch")
                                                        .values(List.of("false"))
                                                        .build())
                                        .build())
                                .applyValue(GetSubnetsResult::ids);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsWrappedSingleTextBlockArgumentInTopLevelQualifiedChainWithTrailingSelector()
    {
        String code =
                """
                class Test
                {
                    Object run(Object db)
                    {
                        return db.query(
                                        \"\"\"
                                        select 1
                                        \"\"\")
                                .list();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedSingleTextBlockArgumentInLambdaQualifiedChainWithTrailingSelector()
    {
        String code =
                """
                class Test
                {
                    Object run(Object jdbi, Object sourceIntegrationId)
                    {
                        return jdbi.withHandle(handle -> handle.createQuery(
                                        \"\"\"
                                        SELECT DISTINCT resource_type
                                        FROM entities_latest
                                        WHERE integration_id = :integrationId
                                        \"\"\")
                                .bind("integrationId", sourceIntegrationId)
                                .mapTo(String.class)
                                .list());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsDeepWrappedSingleStringArgumentInQualifiedInvocationWithTrailingSelector()
    {
        String code =
                """
                class Test
                {
                    Object run(Object handle)
                    {
                        return handle.createQuery(
                                        "SELECT version()")
                                .mapTo(String.class)
                                .one();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedSingleTextBlockArgumentInQualifiedInvocationInsideConstructorArgument()
    {
        String code =
                """
                class Test
                {
                    void run(Object systemdUnitsBuilder)
                    {
                        systemdUnitsBuilder.add(new SystemdUnit("setup-ephemeral-disk.service", true, Optional.of(
                                \"\"\"
                                [Unit]
                                Before=mnt-ephemeral.mount
                                \"\"\")));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesWrappedSingleTextBlockArgumentInQualifiedInvocationInsideConstructorArgumentInChain()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        systemdUnitsBuilder.add(new SystemdUnit("setup-ephemeral-disk.service", true, Optional.of(
                                \"\"\"
                                [Unit]
                                Before=mnt-ephemeral.mount
                                \"\"\")))
                                .add(new SystemdUnit("mnt-ephemeral.mount", true, Optional.of(
                                        \"\"\"
                                        [Unit]
                                        Before=setup-ephemeral-directories.service
                                        \"\"\")));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        systemdUnitsBuilder.add(new SystemdUnit("setup-ephemeral-disk.service", true, Optional.of(
                                        \"\"\"
                                        [Unit]
                                        Before=mnt-ephemeral.mount
                                        \"\"\")))
                                .add(new SystemdUnit("mnt-ephemeral.mount", true, Optional.of(
                                        \"\"\"
                                        [Unit]
                                        Before=setup-ephemeral-directories.service
                                        \"\"\")));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedSingleLongTextBlockArgumentInQualifiedInvocationInsideConstructorArgumentInChain()
    {
        String oldCode =
                """
                import java.util.Optional;

                class Test
                {
                    void run()
                    {
                        systemdUnitsBuilder.add(new SystemdUnit("setup-ephemeral-disk.service", true, Optional.of(
                                \"\"\"
                                [Unit]
                                Before=mnt-ephemeral.mount
                                After=setup-oem.service extend-filesystems.service
                                Requires=setup-oem.service extend-filesystems.service
                                [Service]
                                Type=oneshot
                                ExecStart=/opt/bin/setup-ephemeral-disk
                                RemainAfterExit=yes
                                [Install]
                                WantedBy=multi-user.target
                                \"\"\")))
                                .add(new SystemdUnit("mnt-ephemeral.mount", true, Optional.of(
                                        \"\"\"
                                        [Unit]
                                        Before=setup-ephemeral-directories.service
                                        \"\"\")));
                    }
                }
                """;

        String newCode =
                """
                import java.util.Optional;

                class Test
                {
                    void run()
                    {
                        systemdUnitsBuilder.add(new SystemdUnit("setup-ephemeral-disk.service", true, Optional.of(
                                        \"\"\"
                                        [Unit]
                                        Before=mnt-ephemeral.mount
                                        After=setup-oem.service extend-filesystems.service
                                        Requires=setup-oem.service extend-filesystems.service
                                        [Service]
                                        Type=oneshot
                                        ExecStart=/opt/bin/setup-ephemeral-disk
                                        RemainAfterExit=yes
                                        [Install]
                                        WantedBy=multi-user.target
                                        \"\"\")))
                                .add(new SystemdUnit("mnt-ephemeral.mount", true, Optional.of(
                                        \"\"\"
                                        [Unit]
                                        Before=setup-ephemeral-directories.service
                                        \"\"\")));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesIgnitionConfigGeneratorMethodExcerpt()
    {
        String oldCode =
                """
                import java.util.List;
                import java.util.Optional;

                class Test
                {
                    public static String generateIgnitionConfig(MachineImage machineImage)
                    {
                        ImmutableList.Builder<SystemdUnit> systemdUnitsBuilder = ImmutableList.<SystemdUnit>builder()
                                .add(new SystemdUnit("docker.service", true, Optional.empty()));

                        systemdUnitsBuilder.add(new SystemdUnit("setup-ephemeral-disk.service", true, Optional.of(
                                \"\"\"
                                [Unit]
                                Before=mnt-ephemeral.mount
                                After=setup-oem.service extend-filesystems.service
                                Requires=setup-oem.service extend-filesystems.service
                                [Service]
                                Type=oneshot
                                ExecStart=/opt/bin/setup-ephemeral-disk
                                RemainAfterExit=yes
                                [Install]
                                WantedBy=multi-user.target
                                \"\"\")))
                                .add(new SystemdUnit("mnt-ephemeral.mount", true, Optional.of(
                                        \"\"\"
                                        [Unit]
                                        Before=setup-ephemeral-directories.service
                                        After=setup-ephemeral-disk.service
                                        Requires=setup-ephemeral-disk.service
                                        [Mount]
                                        What=/dev/md0
                                        Where=/mnt/ephemeral
                                        Type=ext4
                                        [Install]
                                        RequiredBy=docker.service
                                        \"\"\")))
                                .add(new SystemdUnit("setup-ephemeral-directories.service", true, Optional.of(
                                        \"\"\"
                                        [Unit]
                                        Before=docker.service
                                        Requires=mnt-ephemeral.mount
                                        [Service]
                                        Type=oneshot
                                        ExecStart=/bin/sh -c '/usr/bin/mkdir -p /mnt/ephemeral/containers /mnt/ephemeral/app-data /var/lib/docker && /usr/bin/ln -sf /mnt/ephemeral/containers /var/lib/docker/containers'
                                        [Install]
                                        RequiredBy=docker.service
                                        \"\"\")))
                                .addAll(containersToSystemd(machineImage.containers()));

                        return systemdUnitsBuilder.build().toString();
                    }
                }
                """;

        String newCode =
                """
                import java.util.Optional;

                class Test
                {
                    public static String generateIgnitionConfig(MachineImage machineImage)
                    {
                        ImmutableList.Builder<SystemdUnit> systemdUnitsBuilder = ImmutableList.<SystemdUnit>builder()
                                .add(new SystemdUnit("docker.service", true, Optional.empty()));

                        systemdUnitsBuilder.add(new SystemdUnit("setup-ephemeral-disk.service", true, Optional.of(
                                        \"\"\"
                                        [Unit]
                                        Before=mnt-ephemeral.mount
                                        After=setup-oem.service extend-filesystems.service
                                        Requires=setup-oem.service extend-filesystems.service
                                        [Service]
                                        Type=oneshot
                                        ExecStart=/opt/bin/setup-ephemeral-disk
                                        RemainAfterExit=yes
                                        [Install]
                                        WantedBy=multi-user.target
                                        \"\"\")))
                                .add(new SystemdUnit("mnt-ephemeral.mount", true, Optional.of(
                                        \"\"\"
                                        [Unit]
                                        Before=setup-ephemeral-directories.service
                                        After=setup-ephemeral-disk.service
                                        Requires=setup-ephemeral-disk.service
                                        [Mount]
                                        What=/dev/md0
                                        Where=/mnt/ephemeral
                                        Type=ext4
                                        [Install]
                                        RequiredBy=docker.service
                                        \"\"\")))
                                .add(new SystemdUnit("setup-ephemeral-directories.service", true, Optional.of(
                                        \"\"\"
                                        [Unit]
                                        Before=docker.service
                                        Requires=mnt-ephemeral.mount
                                        [Service]
                                        Type=oneshot
                                        ExecStart=/bin/sh -c '/usr/bin/mkdir -p /mnt/ephemeral/containers /mnt/ephemeral/app-data /var/lib/docker && /usr/bin/ln -sf /mnt/ephemeral/containers /var/lib/docker/containers'
                                        [Install]
                                        RequiredBy=docker.service
                                        \"\"\")))
                                .addAll(containersToSystemd(machineImage.containers()));

                        return systemdUnitsBuilder.build().toString();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesLambdaBodyStatementIndentationInWrappedQualifiedChain()
    {
        String oldCode =
                """
                class Test
                {
                    void run(Object module, Object timeout)
                    {
                        install(module
                                .withConfigDefaults(config -> {
                                    config.setIdleTimeout(timeout);
                                       config.setRequestTimeout(timeout);
                                }));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(Object module, Object timeout)
                    {
                        install(module
                                .withConfigDefaults(config -> {
                                    config.setIdleTimeout(timeout);
                                    config.setRequestTimeout(timeout);
                                }));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesLambdaBodyStatementIndentationInWrappedQualifiedChainWithTrailingSelector()
    {
        String oldCode =
                """
                class Test
                {
                    Builder module()
                    {
                        return null;
                    }

                    interface Builder
                    {
                        Builder withConfigDefaults(java.util.function.Consumer<Config> consumer);

                        Object build();
                    }

                    interface Config
                    {
                        void setIdleTimeout(Object timeout);

                        void setRequestTimeout(Object timeout);
                    }

                    void run(Object timeout)
                    {
                        install(module()
                                .withConfigDefaults(config -> {
                                    config.setIdleTimeout(timeout);
                                       config.setRequestTimeout(timeout);
                                }).build());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Builder module()
                    {
                        return null;
                    }

                    interface Builder
                    {
                        Builder withConfigDefaults(java.util.function.Consumer<Config> consumer);

                        Object build();
                    }

                    interface Config
                    {
                        void setIdleTimeout(Object timeout);

                        void setRequestTimeout(Object timeout);
                    }

                    void run(Object timeout)
                    {
                        install(module()
                                .withConfigDefaults(config -> {
                                    config.setIdleTimeout(timeout);
                                    config.setRequestTimeout(timeout);
                                }).build());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesLambdaBlockBodyIndentationAfterWrappedNestedInvocationSelector()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        java.util.List.of(
                                1,
                                2,
                                Integer.valueOf(
                                        Integer.valueOf(3))).stream().map(value -> {
                                            String a = value.toString();
                                            return a;
                                        });
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        java.util.List.of(
                                1,
                                2,
                                Integer.valueOf(
                                        Integer.valueOf(3))).stream().map(value -> {
                            String a = value.toString();
                            return a;
                        });
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsWrappedInvocationLambdaBodyIndentationWithAnonymousClassArgument()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        setup(Integer.valueOf(
                                        1),
                                _ -> {
                                    bind(new java.util.Comparator<String>()
                                    {
                                        @Override
                                        public int compare(String left, String right)
                                        {
                                            return 0;
                                        }
                                    });
                                });
                    }

                    void setup(Integer value, java.util.function.Consumer<String> consumer) {}

                    void bind(java.util.Comparator<String> comparator) {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedQualifiedInvocationLambdaBodyIndentationWithAnonymousClassArgument()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        Helper.create(Integer.valueOf(
                                        2),
                                _ -> {
                                    use(new Runnable()
                                    {
                                        @Override
                                        public void run() {}
                                    });
                                });
                    }

                    static void use(Runnable runnable) {}

                    static class Helper
                    {
                        static void create(Integer value, java.util.function.Consumer<String> consumer) {}
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsNestedSwitchIndentationInLambdaBlockAfterWrappedNestedInvocationSelector()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        var values = java.util.List.of(
                                1,
                                2,
                                Integer.valueOf(
                                        Integer.valueOf(3))).stream().map(value -> {
                            switch (value) {
                                case 1:
                                    return 1;
                                default:
                                    return 0;
                            }
                        });
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesNestedSwitchIndentationInLambdaBlockAfterSelectorLineInvocation()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        var values = java.util.List.of(
                                1,
                                2,
                                Integer.valueOf(
                                        Integer.valueOf(3)))
                                .stream()
                                .map(value -> {
                                    switch (value) {
                                        case 1:
                                            return 1;
                                        default:
                                            return 0;
                                    }
                                });
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        var values = java.util.List.of(
                                        1,
                                        2,
                                        Integer.valueOf(
                                                Integer.valueOf(3)))
                                .stream()
                                .map(value -> {
                                    switch (value) {
                                        case 1:
                                            return 1;
                                        default:
                                            return 0;
                                    }
                                });
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesIgnitionConfigGeneratorFileContextWrapping()
    {
        String oldCode =
                """
                import java.util.Optional;

                class Test
                {
                    private static final String setupEphemeralDiskScript =
                            \"\"\"
                            #!/bin/bash

                            # format the raid array
                            mkfs.ext4 -F /dev/md0
                            \"\"\";

                    public static String generateIgnitionConfig(MachineImage machineImage)
                    {
                        ImmutableList.Builder<SystemdUnit> systemdUnitsBuilder = ImmutableList.<SystemdUnit>builder()
                                .add(new SystemdUnit("docker.service", true, Optional.empty()));

                        systemdUnitsBuilder.add(new SystemdUnit("setup-ephemeral-disk.service", true, Optional.of(
                                \"\"\"
                                [Unit]
                                Before=mnt-ephemeral.mount
                                After=setup-oem.service extend-filesystems.service
                                Requires=setup-oem.service extend-filesystems.service
                                [Service]
                                Type=oneshot
                                ExecStart=/opt/bin/setup-ephemeral-disk
                                RemainAfterExit=yes
                                [Install]
                                WantedBy=multi-user.target
                                \"\"\")))
                                .add(new SystemdUnit("mnt-ephemeral.mount", true, Optional.of(
                                        \"\"\"
                                        [Unit]
                                        Before=setup-ephemeral-directories.service
                                        After=setup-ephemeral-disk.service
                                        Requires=setup-ephemeral-disk.service
                                        [Mount]
                                        What=/dev/md0
                                        Where=/mnt/ephemeral
                                        Type=ext4
                                        [Install]
                                        RequiredBy=docker.service
                                        \"\"\")))
                                .add(new SystemdUnit("setup-ephemeral-directories.service", true, Optional.of(
                                        \"\"\"
                                        [Unit]
                                        Before=docker.service
                                        Requires=mnt-ephemeral.mount
                                        [Service]
                                        Type=oneshot
                                        ExecStart=/bin/sh -c '/usr/bin/mkdir -p /mnt/ephemeral/containers /mnt/ephemeral/app-data /var/lib/docker && /usr/bin/ln -sf /mnt/ephemeral/containers /var/lib/docker/containers'
                                        [Install]
                                        RequiredBy=docker.service
                                        \"\"\")))
                                .addAll(containersToSystemd(machineImage.containers()));

                        return setupEphemeralDiskScript + systemdUnitsBuilder.build();
                    }
                }
                """;

        String newCode =
                """
                import java.util.Optional;

                class Test
                {
                    private static final String setupEphemeralDiskScript =
                            \"\"\"
                            #!/bin/bash

                            # format the raid array
                            mkfs.ext4 -F /dev/md0
                            \"\"\";

                    public static String generateIgnitionConfig(MachineImage machineImage)
                    {
                        ImmutableList.Builder<SystemdUnit> systemdUnitsBuilder = ImmutableList.<SystemdUnit>builder()
                                .add(new SystemdUnit("docker.service", true, Optional.empty()));

                        systemdUnitsBuilder.add(new SystemdUnit("setup-ephemeral-disk.service", true, Optional.of(
                                        \"\"\"
                                        [Unit]
                                        Before=mnt-ephemeral.mount
                                        After=setup-oem.service extend-filesystems.service
                                        Requires=setup-oem.service extend-filesystems.service
                                        [Service]
                                        Type=oneshot
                                        ExecStart=/opt/bin/setup-ephemeral-disk
                                        RemainAfterExit=yes
                                        [Install]
                                        WantedBy=multi-user.target
                                        \"\"\")))
                                .add(new SystemdUnit("mnt-ephemeral.mount", true, Optional.of(
                                        \"\"\"
                                        [Unit]
                                        Before=setup-ephemeral-directories.service
                                        After=setup-ephemeral-disk.service
                                        Requires=setup-ephemeral-disk.service
                                        [Mount]
                                        What=/dev/md0
                                        Where=/mnt/ephemeral
                                        Type=ext4
                                        [Install]
                                        RequiredBy=docker.service
                                        \"\"\")))
                                .add(new SystemdUnit("setup-ephemeral-directories.service", true, Optional.of(
                                        \"\"\"
                                        [Unit]
                                        Before=docker.service
                                        Requires=mnt-ephemeral.mount
                                        [Service]
                                        Type=oneshot
                                        ExecStart=/bin/sh -c '/usr/bin/mkdir -p /mnt/ephemeral/containers /mnt/ephemeral/app-data /var/lib/docker && /usr/bin/ln -sf /mnt/ephemeral/containers /var/lib/docker/containers'
                                        [Install]
                                        RequiredBy=docker.service
                                        \"\"\")))
                                .addAll(containersToSystemd(machineImage.containers()));

                        return setupEphemeralDiskScript + systemdUnitsBuilder.build();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsWrappedMultilineArgumentsInTopLevelQualifiedChainWithTrailingSelector()
    {
        String code =
                """
                public class Test
                {
                    static void main()
                    {
                        Output<String> value = Api.fetch(
                                        Request.builder()
                                                .items(
                                                        Item.builder()
                                                                .value("a")
                                                                .build(),
                                                        Item.builder()
                                                                .value("b")
                                                                .build())
                                                .build(),
                                        Options.builder()
                                                .provider(provider)
                                                .build())
                                .map(Result::value);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedArgumentsWithCommentInTopLevelQualifiedChain()
    {
        String code =
                """
                public class Test
                {
                    static void main()
                    {
                        values = Stream.concat(
                                        // first values
                                        Stream.of("a", "b"),
                                        loadValues(input).stream()
                                                .map(Value::name))
                                .toList();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedArgumentsInQualifiedChainReceiverWithTrailingSelectors()
    {
        String code =
                """
                public class Test
                {
                    static void main()
                    {
                        return client.get().fetch(Request.builder()
                                        .setId(id)
                                        .build())
                                .items().stream()
                                .toList();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesTopLevelQualifiedChainSingleWrappedBuilderArgumentIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object client)
                    {
                        return client.fetch(Request.builder()
                                .setId(id)
                                .setEnabled(true)
                                .build())
                                .stream();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object client)
                    {
                        return client.fetch(Request.builder()
                                        .setId(id)
                                        .setEnabled(true)
                                        .build())
                                .stream();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesTopLevelQualifiedChainMultipleWrappedScalarArguments()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        Output<String> value = Api.fetch(
                                Request.builder().build(),
                                Options.builder().build())
                                .map(Result::id);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        Output<String> value = Api.fetch(
                                        Request.builder().build(),
                                        Options.builder().build())
                                .map(Result::id);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesTopLevelQualifiedChainMultipleWrappedArgumentsNestedAsOuterArgument()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        use(Stream.concat(
                                left.stream(),
                                right.stream())
                                .toList());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        use(Stream.concat(
                                        left.stream(),
                                        right.stream())
                                .toList());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesTopLevelQualifiedChainWrappedArgumentsWithLambda()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return workflowApi.runIfAllowed(
                                principal,
                                permission,
                                ALL,
                                () -> action())
                                .orElseThrow();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return workflowApi.runIfAllowed(
                                        principal,
                                        permission,
                                        ALL,
                                        () -> action())
                                .orElseThrow();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesExpressionLambdaBodyIndentationInWrappedArguments()
    {
        String oldCode =
                """
                public class Test
                {
                    static void main()
                    {
                        client.call(value ->
                                        value
                                                .setA("a")
                                                .setB("b"),
                                body);
                    }
                }
                """;

        String newCode =
                """
                public class Test
                {
                    static void main()
                    {
                        client.call(
                                value -> value
                                        .setA("a")
                                        .setB("b"),
                                body);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesInlineCommentInsideWrappedArguments()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        schedule(
                                "pipeline",
                                "0 30 * * * ?", // Every hour at :30
                                        true,
                                        true,
                                sourceId,
                                null,
                                "{}");
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        schedule(
                                "pipeline",
                                "0 30 * * * ?", // Every hour at :30
                                true,
                                true,
                                sourceId,
                                null,
                                "{}");
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesExpressionLambdaSelectorContinuationIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object ids)
                    {
                        return api.call(ids -> service.load(name).stream()
                                        .filter(item -> keep(ids, item))
                                        .toList());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object ids)
                    {
                        return api.call(ids -> service.load(name).stream()
                                .filter(item -> keep(ids, item))
                                .toList());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedNestedOutputAllChainInWrappedBuilderArgument()
    {
        String oldCode =
                """
                class Test
                {
                    void run(Args arguments, Object option)
                    {
                        createResource(
                                "Name",
                                Builder.builder()
                                        .name("app-private")
                                        .subnetIds(Output.all(arguments.values().orElseThrow().stream()
                                                        .map(Object::toString)
                                                        .toList()))
                                        .tags(java.util.Map.of("environment", "prod"))
                                        .build(),
                                option);
                    }

                    Object createResource(Object a, Object b, Object c)
                    {
                        return null;
                    }

                    record Args(java.util.Optional<java.util.List<Object>> values) {}

                    static class Builder
                    {
                        static Builder builder()
                        {
                            return new Builder();
                        }

                        Builder name(String value)
                        {
                            return this;
                        }

                        Builder subnetIds(Object value)
                        {
                            return this;
                        }

                        Builder tags(Object value)
                        {
                            return this;
                        }

                        Object build()
                        {
                            return null;
                        }
                    }

                    static class Output
                    {
                        static Object all(Object value)
                        {
                            return null;
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(Args arguments, Object option)
                    {
                        createResource(
                                "Name",
                                Builder.builder()
                                        .name("app-private")
                                        .subnetIds(Output.all(arguments.values().orElseThrow().stream()
                                                .map(Object::toString)
                                                .toList()))
                                        .tags(java.util.Map.of("environment", "prod"))
                                        .build(),
                                option);
                    }

                    Object createResource(Object a, Object b, Object c)
                    {
                        return null;
                    }

                    record Args(java.util.Optional<java.util.List<Object>> values) {}

                    static class Builder
                    {
                        static Builder builder()
                        {
                            return new Builder();
                        }

                        Builder name(String value)
                        {
                            return this;
                        }

                        Builder subnetIds(Object value)
                        {
                            return this;
                        }

                        Builder tags(Object value)
                        {
                            return this;
                        }

                        Object build()
                        {
                            return null;
                        }
                    }

                    static class Output
                    {
                        static Object all(Object value)
                        {
                            return null;
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesReportCaseTopLevelQualifiedChainSingleWrappedBuilderArgumentWithTrailingSelectors()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object client)
                    {
                        return client.listTypesPaginator(ListTypesRequest.builder()
                                .type(type)
                                .visibility(visibility)
                                .build())
                                .typeSummaries().stream()
                                .toList();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object client)
                    {
                        return client.listTypesPaginator(ListTypesRequest.builder()
                                        .type(type)
                                        .visibility(visibility)
                                        .build())
                                .typeSummaries().stream()
                                .toList();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesReportCaseExpressionLambdaSelectorContinuationIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return values.applyValue(value -> "x:%s"
                                        .formatted(value));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return values.applyValue(value -> "x:%s"
                                .formatted(value));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsShallowFormattedArgumentsInsideLambdaWrappedSelectorArgument()
    {
        String code =
                """
                import java.util.List;

                class Test
                {
                    Object run(List<Object> values)
                    {
                        return root()
                                .fields(values.stream()
                                        .map(value -> "$left.%s == $right.%s".formatted(
                                                left(value),
                                                right(value)))
                                        .toList());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsShallowFormattedArgumentsInsideLambdaWrappedTrailingArgument()
    {
        String code =
                """
                import java.util.List;

                class Test
                {
                    Object run(List<Object> values)
                    {
                        return format(
                                kind(),
                                values.stream()
                                        .map(value -> "$left.%s == $right.%s".formatted(
                                                left(value),
                                                right(value)))
                                        .toList());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsShallowWrappedStaticFactoryArgumentsInsideLambdaSelectorChain()
    {
        String code =
                """
                class Test
                {
                    Object run(Object freshness)
                    {
                        return freshness
                                .map(instant -> LongTimestampWithTimeZone.fromEpochSecondsAndFraction(
                                        first(instant),
                                        second(instant),
                                        UTC))
                                .orElse(null);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsShallowWrappedStaticFactoryArgumentsInsideLambdaSelectorChainInWrappedCall()
    {
        String code =
                """
                class Test
                {
                    Object run(Object freshness, Object definition)
                    {
                        return result(
                                freshness.flatMap(getLastKnownFreshTime())
                                        .map(instant -> LongTimestampWithTimeZone.fromEpochSecondsAndFraction(
                                                first(instant),
                                                second(instant),
                                                UTC))
                                        .orElse(null),
                                definition);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesNestedWrappedConstructorArgumentIndentation()
    {
        String oldCode =
                """
                import java.util.List;

                class Test
                {
                    Object run(Object first, Object second, Object other)
                    {
                        return List.of(new Entry(
                                first,
                                second),
                                other);
                    }

                    record Entry(Object first, Object second) {}
                }
                """;

        String newCode =
                """
                import java.util.List;

                class Test
                {
                    Object run(Object first, Object second, Object other)
                    {
                        return List.of(new Entry(
                                        first,
                                        second),
                                other);
                    }

                    record Entry(Object first, Object second) {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesReportCaseNestedWrappedConstructorFirstElementIndentation()
    {
        String oldCode =
                """
                import java.util.List;

                class Test
                {
                    Object run(Object first, Object second, Object other)
                    {
                        return keep(List.of(new Pattern(
                                first,
                                second),
                                other));
                    }

                    record Pattern(Object first, Object second) {}
                }
                """;

        String newCode =
                """
                import java.util.List;

                class Test
                {
                    Object run(Object first, Object second, Object other)
                    {
                        return keep(List.of(new Pattern(
                                        first,
                                        second),
                                other));
                    }

                    record Pattern(Object first, Object second) {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesNestedWrappedInvocationArgumentIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object first, Object second, Object other)
                    {
                        return wrap(build(
                                first,
                                second),
                                other);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object first, Object second, Object other)
                    {
                        return wrap(build(
                                        first,
                                        second),
                                other);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsQualifiedNestedWrappedInvocationArgumentsShallow()
    {
        String code =
                """
                class Scratch
                {
                    public static void main(String[] args)
                    {
                        SampleLogAnalyzer.AnalysisResult analysisResult = SampleLogAnalyzer.analyze(List.of(
                                LogPattern.checkedType("AWS::S3::Bucket", true),
                                LogPattern.checkedType("AWS::ApiGateway::Deployment", false),
                                LogPattern.fetchFailureWithSkipHint("AWS::ApiGateway::Deployment"),
                                LogPattern.fetchFailure("AWS::ApiGateway::Deployment"),
                                "this line should be ignored"), 2);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesReportCaseFirstWrappedMultilineInvocationArgumentWithTrailingSelector()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object client, Object uri, Object handler)
                    {
                        return client.send(
                                        Request.builder()
                                                .uri(uri)
                                                .build(),
                                        handler).body();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object client, Object uri, Object handler)
                    {
                        return client.send(
                                Request.builder()
                                        .uri(uri)
                                        .build(),
                                handler).body();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesNestedFirstWrappedMultilineInvocationArgumentWithTrailingSelector()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return this.fromJson(this.send(
                                        Request.builder()
                                                .build(),
                                        null).body());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return this.fromJson(this.send(
                                Request.builder()
                                        .build(),
                                null).body());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesQualifiedOuterInvocationWithTrailingSelectorAndNestedWrappedFirstArgument()
    {
        String oldCode =
                """
                class Test
                {
                    java.util.function.Function<Object, String> mapper;

                    Object run()
                    {
                        return mapper.apply(send(
                                Builder.create()
                                        .build(),
                                null).toString())
                                .trim();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    java.util.function.Function<Object, String> mapper;

                    Object run()
                    {
                        return mapper.apply(send(
                                        Builder.create()
                                                .build(),
                                        null).toString())
                                .trim();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesNestedWrappedMultiItemInvocationArgumentWithOuterTrailingSelector()
    {
        String oldCode =
                """
                class Test
                {
                    void run(Object a, Object b, Object c)
                    {
                        Object value = java.util.Optional.of(java.util.List.of(
                                a,
                                b,
                                String.valueOf(c)))
                                .map(values -> values);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(Object a, Object b, Object c)
                    {
                        Object value = java.util.Optional.of(java.util.List.of(
                                        a,
                                        b,
                                        String.valueOf(c)))
                                .map(values -> values);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsSingleWrappedInvocationArgumentIndentedAsIfOuterCallWereWrappedWithTrailingSelector()
    {
        String code =
                """
                class Test
                {
                    Object run(Object first, Object second)
                    {
                        return target.wrap(
                                        build(first,
                                                second))
                                .finish();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesBlockLambdaBodyIndentationInBuilderChain()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return Registry.builder()
                                .add("row", Object.class, Object.class, (type, value) -> {
                        Object result = map(value);
                        return result;
                    })
                                .build();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return Registry.builder()
                                .add("row", Object.class, Object.class, (_, value) -> {
                                    Object result = map(value);
                                    return result;
                                })
                                .build();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesFirstWrappedMultilineInvocationArgumentIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object client, Object requestId, Object handler)
                    {
                        return client.send(
                                        Request.builder()
                                                .setId(requestId)
                                                .build(),
                                        handler);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object client, Object requestId, Object handler)
                    {
                        return client.send(
                                Request.builder()
                                        .setId(requestId)
                                        .build(),
                                handler);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesReportCaseBlockLambdaBodyIndentationInWrappedQualifiedCallArguments()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object principal, Object permission)
                    {
                        return workflowApi.runIfAllowed(
                                        principal,
                                        permission,
                                        ALL,
                                        (handle, selection) -> {
                                    ItemDao itemDao = handle.attach(ItemDao.class);
                                    return load(itemDao, selection);
                                })
                                .orElseThrow();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object principal, Object permission)
                    {
                        return workflowApi.runIfAllowed(
                                        principal,
                                        permission,
                                        ALL,
                                        (handle, selection) -> {
                                            ItemDao itemDao = handle.attach(ItemDao.class);
                                            return load(itemDao, selection);
                                        })
                                .orElseThrow();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsCommentedBlockLambdaBodyInWrappedQualifiedCallArguments()
    {
        String code =
                """
                class Test
                {
                    Object run(Object factories, Object partitionSpec, Object partitionData, Object structType)
                    {
                        return factories.computeIfAbsent(
                                        partitionSpec,
                                        key -> {
                                            // creating the template wrapper is expensive, reuse it for all partitions of the same spec
                                            // reuse is only safe because we only use the copyFor method which is thread safe
                                            Object templateWrapper = createWrapper(structType);
                                            return createPartitionKey(key, templateWrapper, partitionData);
                                        })
                                .apply(partitionData);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesCommentedBlockLambdaBodyIndentationInWrappedQualifiedCallArguments()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object factories, Object partitionSpec, Object partitionData, Object structType)
                    {
                        return factories.computeIfAbsent(
                                partitionSpec,
                                key -> {
                                        // creating the template wrapper is expensive, reuse it for all partitions of the same spec
                                        // reuse is only safe because we only use the copyFor method which is thread safe
                                        Object templateWrapper = createWrapper(structType);
                                        return createPartitionKey(key, templateWrapper, partitionData);
                                    })
                                .apply(partitionData);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object factories, Object partitionSpec, Object partitionData, Object structType)
                    {
                        return factories.computeIfAbsent(
                                        partitionSpec,
                                        key -> {
                                            // creating the template wrapper is expensive, reuse it for all partitions of the same spec
                                            // reuse is only safe because we only use the copyFor method which is thread safe
                                            Object templateWrapper = createWrapper(structType);
                                            return createPartitionKey(key, templateWrapper, partitionData);
                                        })
                                .apply(partitionData);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedMultilineLambdaArgumentIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object api, Object x)
                    {
                        return api.call(x,
                                (a, b) -> {
                                    run();
                                })
                                .done();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object api, Object x)
                    {
                        return api.call(x,
                                        (_, _) -> {
                                            run();
                                        })
                                .done();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsAssignmentPrefixedSelectorChainInLambdaBlockAtContinuationIndent()
    {
        String oldCode =
                """
                class Test
                {
                    Object userInfo;
                    Object targetItem;
                    Object ANY;
                    Object filter;

                    Object run(Object workflowApi)
                    {
                        return workflowApi.runWithTransactionAndFilter(
                                userInfo,
                                targetItem,
                                ANY,
                                (_, _) -> {
                                    Set<ItemType> itemTypeFilter = filter.typeFilter().stream()
                                                                    .map(ItemTypeFilter::toItemType)
                                                                    .collect(Collectors.toUnmodifiableSet());

                                    Set<RuleCategory> ruleCategoryFilter = filter.typeFilter().stream()
                                                                    .map(ItemTypeFilter::toRuleCategory)
                                                                    .filter(Objects::nonNull)
                                                                    .collect(Collectors.toUnmodifiableSet());

                                    String orderByClause = buildOrderByClause(filter.sortBy(), filter.sortOrder());

                                    return orderByClause;
                                });
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object userInfo;
                    Object targetItem;
                    Object ANY;
                    Object filter;

                    Object run(Object workflowApi)
                    {
                        return workflowApi.runWithTransactionAndFilter(
                                userInfo,
                                targetItem,
                                ANY,
                                (_, _) -> {
                                    Set<ItemType> itemTypeFilter = filter.typeFilter().stream()
                                            .map(ItemTypeFilter::toItemType)
                                            .collect(Collectors.toUnmodifiableSet());

                                    Set<RuleCategory> ruleCategoryFilter = filter.typeFilter().stream()
                                            .map(ItemTypeFilter::toRuleCategory)
                                            .filter(Objects::nonNull)
                                            .collect(Collectors.toUnmodifiableSet());

                                    String orderByClause = buildOrderByClause(filter.sortBy(), filter.sortOrder());

                                    return orderByClause;
                                });
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesExpressionLambdaSelectorContinuationInWrappedBuilderArgument()
    {
        String oldCode =
                """
                class Test
                {
                    void run(Object accountId, Object context, Object firehoseLogGroupName)
                    {
                        policy(Output.<String>listBuilder()
                                .add(accountId.applyValue(accountIdValue -> "arn:aws:logs:%s:%s:log-group:%s:*"
                                                .formatted(context.awsRegion(), accountIdValue, firehoseLogGroupName)))
                                .build());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(Object accountId, Object context, Object firehoseLogGroupName)
                    {
                        policy(Output.<String>listBuilder()
                                .add(accountId.applyValue(accountIdValue -> "arn:aws:logs:%s:%s:log-group:%s:*"
                                        .formatted(context.awsRegion(), accountIdValue, firehoseLogGroupName)))
                                .build());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesNestedConstructorFirstArgumentIndentationInWrappedList()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return List.of(new PatternWildcardedName(location, List.of(
                                new LiteralSegment(location, "E"),
                                new StarSegment(location),
                                new LiteralSegment(location, "m"),
                                new StarSegment(location))),
                                other);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return List.of(new PatternWildcardedName(location, List.of(
                                        new LiteralSegment(location, "E"),
                                        new StarSegment(location),
                                        new LiteralSegment(location, "m"),
                                        new StarSegment(location))),
                                other);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsNestedQualifiedConstructorArgumentIndentationWhenContainingConstructorIsNotFirstListElement()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return new DynamicLiteral(
                                location,
                                new JsonObject(location, List.of(
                                        jsonEntry("a", "b"),
                                        jsonEntry("c", "d"))),
                                List.of());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsBlankLineBetweenWrappedInvocationArguments()
    {
        // Authors use blank lines between wrapped args to group related
        // arguments (long stat-collector / constructor arg lists). Preserve
        // one blank line if the source had any.
        String code =
                """
                class Test
                {
                    void run()
                    {
                        call(
                                first,

                                second);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesRepeatedBlankLinesBetweenWrappedInvocationArgumentsBeforeEngine()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        call(
                                first,


                                second);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        call(
                                first,

                                second);
                    }
                }
                """;

        assertEquals(newCode, WrappedListNormalizer.normalize(oldCode));
        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesBlankLineInsideWrappedSelectorChain()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        builder
                                .setFirst(first)

                                .setSecond(second)
                                .build();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        builder
                                .setFirst(first)
                                .setSecond(second)
                                .build();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesTextBlockArgumentIndentationAtSingleContinuationIndent()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        query(
                                        \"""
                                        SELECT *
                                        FROM example
                                        \""");
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        query(
                                \"""
                                SELECT *
                                FROM example
                                \""");
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesAnonymousClassClosingBraceIndentationInWrappedInvocation()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        call(
                                new Listener()
                                {
                                    @Override
                                    public void run()
                                    {
                                        work();
                                    }
                                                },
                                other);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        call(
                                new Listener()
                                {
                                    @Override
                                    public void run()
                                    {
                                        work();
                                    }
                                },
                                other);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesAnonymousClassClosingBraceIndentationWithEmptyMethodBody()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        call(
                                new Listener()
                                {
                                    @Override
                                    public void run()
                                    { }
                                                },
                                other);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        call(
                                new Listener()
                                {
                                    @Override
                                    public void run() {}
                                },
                                other);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsSingleBlankLineAfterStandaloneCommentedWrappedChainItem()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        var value = new StringBuilder()
                                .append("a")

                                // Special item
                                .append("b")

                                .append("c");
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesRepeatedBlankLinesAfterStandaloneCommentedWrappedChainItem()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        var value = new StringBuilder()
                                .append("a")

                                // Special item
                                .append("b")

                                .append("c");
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        var value = new StringBuilder()
                                .append("a")

                                // Special item
                                .append("b")

                                .append("c");
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsCompactMultilineBlockLambdaInWrappedChain()
    {
        String oldCode =
                """
                class Test
                {
                    void run(java.util.List<String> fragments, java.util.Map<String, Integer> values)
                    {
                        sink(
                                fragments.stream()
                                        .map(fragment -> {
                                            Integer value = values.get(fragment);
                                            return value.toString(); })
                                        .toList());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(java.util.List<String> fragments, java.util.Map<String, Integer> values)
                    {
                        sink(
                                fragments.stream()
                                        .map(fragment -> {
                                            Integer value = values.get(fragment);
                                            return value.toString();
                                        })
                                        .toList());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsWrappedNonFirstBlockLambdaWithLeadingComments()
    {
        String code =
                """
                class Test
                {
                    Object run(Cache cache, Object key, Object data)
                    {
                        return cache.computeIfAbsent(
                                        key,
                                        _ -> {
                                            // creating the template wrapper is expensive, reuse it
                                            // reuse is only safe because copy is thread safe
                                            String prefix = "x";
                                            return input -> prefix + input;
                                        })
                                .apply(data);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedQualifiedCallBlockLambdaBodyIndentation()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        tester().assertStatsFor(
                                        enabled,
                                        pb -> {
                                            Object aggregatedOutput = pb.symbol("count_on_x", bigint);
                                            return pb.filter(
                                                    test(aggregatedOutput),
                                                    // Narrowing identity projection
                                                    pb.project(identity(aggregatedOutput),
                                                            pb.aggregation(ab -> ab
                                                                    .addAggregation(aggregatedOutput, aggregation("count", values(reference("x"))), values(bigint))
                                                                    .singleGroupingSet(pb.symbol("y", bigint))
                                                                    .source(pb.values(pb.symbol("x", bigint), pb.symbol("y", bigint)))
                                                                    .nodeId(aggregationId))));
                                        })
                                .withSourceStats(sourceStats)
                                .withSourceStats(aggregationId, stats(50))
                                .check();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesWrappedInvocationArgumentsInInlineExpressionLambdaSelectorCall()
    {
        String oldCode =
                """
                import com.google.common.collect.ImmutableList;

                class Test
                {
                    void run(Object sourceStats, Object approximationEnabled)
                    {
                        tester().assertStatsFor(approximationEnabled, planBuilder -> planBuilder.filter(
                                new Comparison(EQUAL, new Reference(DOUBLE, "y"), new Constant(DOUBLE, 1.0)),
                                planBuilder.aggregation(aggregationBuilder -> aggregationBuilder
                                        .addAggregation(planBuilder.symbol("count_on_x", DOUBLE), aggregation("count", ImmutableList.of(new Reference(DOUBLE, "x"))), ImmutableList.of(DOUBLE))
                                        .singleGroupingSet(planBuilder.symbol("y", DOUBLE))
                                        .source(planBuilder.values(planBuilder.symbol("x", DOUBLE), planBuilder.symbol("y", DOUBLE))))))
                                .withSourceStats(sourceStats)
                                .check(check -> check.outputRowsCount(100 * (1.0 / 10)));
                    }
                }
                """;

        String newCode =
                """
                import com.google.common.collect.ImmutableList;

                class Test
                {
                    void run(Object sourceStats, Object approximationEnabled)
                    {
                        tester().assertStatsFor(approximationEnabled, planBuilder -> planBuilder.filter(
                                        new Comparison(EQUAL, new Reference(DOUBLE, "y"), new Constant(DOUBLE, 1.0)),
                                        planBuilder.aggregation(aggregationBuilder -> aggregationBuilder
                                                .addAggregation(planBuilder.symbol("count_on_x", DOUBLE), aggregation("count", ImmutableList.of(new Reference(DOUBLE, "x"))), ImmutableList.of(DOUBLE))
                                                .singleGroupingSet(planBuilder.symbol("y", DOUBLE))
                                                .source(planBuilder.values(planBuilder.symbol("x", DOUBLE), planBuilder.symbol("y", DOUBLE))))))
                                .withSourceStats(sourceStats)
                                .check(check -> check.outputRowsCount(100 * (1.0 / 10)));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsShallowWrappedBuilderArgumentInsideInlineLambdaMapCall()
    {
        String code =
                """
                class Test
                {
                    Object run(Object argument)
                    {
                        return argument
                                .map(descriptor -> DescriptorArgument.builder()
                                        .descriptor(new Descriptor(descriptor.fields().stream()
                                                .map(field -> new Field(
                                                        field.name(),
                                                        field.type()))
                                                .toList()))
                                        .build())
                                .orElse(NULL_DESCRIPTOR);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsShallowWrappedBuilderArgumentInsideInlineLambdaMapCallInWrappedConstructorArgument()
    {
        String code =
                """
                class Test
                {
                    Object run(Object argument)
                    {
                        return new ArgumentAnalysis(
                                argument.getDescriptor()
                                        .map(descriptor -> DescriptorArgument.builder()
                                                .descriptor(new Descriptor(descriptor.getFields().stream()
                                                        .map(field -> new Descriptor.Field(
                                                                field.getName(),
                                                                field.getType().map(type -> lookup(type))))
                                                        .toList()))
                                                .build())
                                        .orElse(NULL_DESCRIPTOR),
                                java.util.Optional.empty());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesInlineExpressionLambdaSelectorIndentationInWrappedFirstArgument()
    {
        String oldCode =
                """
                import java.util.function.Function;

                class Test
                {
                    Object run()
                    {
                        return outer(
                                enabled(),
                                window(windowBuilder -> windowBuilder
                                        .addFunction(
                                                "count",
                                                windowFunction()),
                                        project()));
                    }

                    Object outer(Object left, Object right)
                    {
                        return null;
                    }

                    Object enabled()
                    {
                        return null;
                    }

                    Object window(Function<WindowBuilder, Object> builder, Object project)
                    {
                        return null;
                    }

                    Object windowFunction()
                    {
                        return null;
                    }

                    Object project()
                    {
                        return null;
                    }

                    interface WindowBuilder
                    {
                        Object addFunction(String name, Object value);
                    }
                }
                """;

        String newCode =
                """
                import java.util.function.Function;

                class Test
                {
                    Object run()
                    {
                        return outer(
                                enabled(),
                                window(windowBuilder -> windowBuilder
                                                .addFunction(
                                                        "count",
                                                        windowFunction()),
                                        project()));
                    }

                    Object outer(Object left, Object right)
                    {
                        return null;
                    }

                    Object enabled()
                    {
                        return null;
                    }

                    Object window(Function<WindowBuilder, Object> builder, Object project)
                    {
                        return null;
                    }

                    Object windowFunction()
                    {
                        return null;
                    }

                    Object project()
                    {
                        return null;
                    }

                    interface WindowBuilder
                    {
                        Object addFunction(String name, Object value);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesNestedQualifiedFirstArgumentIndentationInSmartCallSiteList()
    {
        String oldCode =
                """
                import com.google.common.collect.ImmutableList;
                import com.google.common.collect.ImmutableMap;

                class Test
                {
                    void run()
                    {
                        project(
                                ImmutableMap.of(
                                        "gid-filter-0", expression(new Comparison(EQUAL, new Reference(BIGINT, "group_id"), new Constant(BIGINT, 0L))),
                                        "gid-filter-1", expression(new Comparison(EQUAL, new Reference(BIGINT, "group_id"), new Constant(BIGINT, 1L)))),
                                aggregation(
                                        singleGroupingSet("b", "group_id"),
                                        ImmutableMap.of("non-distinct", aggregationFunction("sum", ImmutableList.of("a"))),
                                        groupId(ImmutableList.of(
                                                ImmutableList.of("a"),
                                                ImmutableList.of("b")),
                                                "group_id",
                                                values("a", "b"))));
                    }
                }
                """;

        String newCode =
                """
                import com.google.common.collect.ImmutableList;
                import com.google.common.collect.ImmutableMap;

                class Test
                {
                    void run()
                    {
                        project(
                                ImmutableMap.of(
                                        "gid-filter-0", expression(new Comparison(EQUAL, new Reference(BIGINT, "group_id"), new Constant(BIGINT, 0L))),
                                        "gid-filter-1", expression(new Comparison(EQUAL, new Reference(BIGINT, "group_id"), new Constant(BIGINT, 1L)))),
                                aggregation(
                                        singleGroupingSet("b", "group_id"),
                                        ImmutableMap.of("non-distinct", aggregationFunction("sum", ImmutableList.of("a"))),
                                        groupId(ImmutableList.of(
                                                        ImmutableList.of("a"),
                                                        ImmutableList.of("b")),
                                                "group_id",
                                                values("a", "b"))));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesBuilderLambdaSelectorIndentationInWrappedInvocation()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        tableFunction(
                                builder -> builder
                                                .name("json_table")
                                                .addTableArgument(
                                                        "input",
                                                        tableArgument(0)
                                                                .rowSemantics()
                                                                .passThroughColumns()
                                                                .passThroughSymbols(java.util.Set.of("json_col", "int_col")))
                                                .properOutputs(java.util.List.of("bigint_col", "varchar_col")),
                                project());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        tableFunction(
                                builder -> builder
                                        .name("json_table")
                                        .addTableArgument(
                                                "input",
                                                tableArgument(0)
                                                        .rowSemantics()
                                                        .passThroughColumns()
                                                        .passThroughSymbols(java.util.Set.of("json_col", "int_col")))
                                        .properOutputs(java.util.List.of("bigint_col", "varchar_col")),
                                project());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesNestedBuilderSelectorIndentInWrappedInvocationArgument()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        executor.invokeAll(ImmutableList.<Callable<Void>>builder()
                                .add(() -> {
                                    barrier();
                                    return null;
                                })
                                .add(() -> {
                                    barrier();
                                    return null;
                                })
                                .build())
                                .forEach(this::done);
                    }

                    void barrier() {}

                    void done(Object value) {}

                    Executor executor;

                    interface Executor
                    {
                        Iterable<Object> invokeAll(Object value);
                    }

                    interface Callable<T>
                    {
                        T call();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        executor.invokeAll(ImmutableList.<Callable<Void>>builder()
                                        .add(() -> {
                                            barrier();
                                            return null;
                                        })
                                        .add(() -> {
                                            barrier();
                                            return null;
                                        })
                                        .build())
                                .forEach(this::done);
                    }

                    void barrier() {}

                    void done(Object value) {}

                    Executor executor;

                    interface Executor
                    {
                        Iterable<Object> invokeAll(Object value);
                    }

                    interface Callable<T>
                    {
                        T call();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsNestedBuilderSelectorIndentationInSingleArgumentCallWithTrailingSelector()
    {
        String code =
                """
                import java.time.Duration;

                class Test
                {
                    void run()
                    {
                        try {
                            Failsafe.with(RetryPolicy.<Integer>builder()
                                            .withMaxAttempts(-1)
                                            .withMaxDuration(Duration.ofSeconds(4))
                                            .withBackoff(100, 500, MILLIS)
                                            .handleResultIf(code -> code >= HTTP_INTERNAL_ERROR)
                                            .build())
                                    .get(() -> {
                                        return 1;
                                    });
                        }
                        catch (Exception e) {
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsInlinePrefixedExpressionLambdaSelectorIndentation()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return tester().assertStatsFor(pb -> pb
                                        .aggregation(ab -> ab
                                                .add(value)
                                                .build()))
                                .check();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesInlinePrefixedExpressionLambdaSelectorIndentationInWrappedMultiArgumentInvocation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return filter(
                                condition(),
                                window(windowBuilder -> windowBuilder
                                        .addFunction(
                                                "count",
                                                function()),
                                        project()));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return filter(
                                condition(),
                                window(windowBuilder -> windowBuilder
                                                .addFunction(
                                                        "count",
                                                        function()),
                                        project()));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsSelectorLineExpressionLambdaStaticCallChainIndentation()
    {
        String code =
                """
                class Test
                {
                    int run(java.util.List<Object> values)
                    {
                        return values
                                .stream()
                                .mapToInt(value -> Searcher
                                        .searchFrom(value)
                                        .where(_ -> true)
                                        .findAll()
                                        .size())
                                .sum();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsSelectorLineNestedExpressionLambdaSelectorIndentation()
    {
        String code =
                """
                class Test
                {
                    Object run(Object value)
                    {
                        return registry()
                                .choice(choice -> choice
                                        .implementation(group -> group
                                                .first(value)
                                                .second(value)))
                                .done();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedArgumentsInInlineLambdaBodyOfMultiArgumentQualifiedCall()
    {
        String code =
                """
                class Test
                {
                    Object run(Object enabled, Object sourceStats)
                    {
                        return tester().assertStatsFor(enabled, pb -> pb.filter(
                                        comparison(),
                                        pb.aggregation(ab -> ab
                                                .add(value())
                                                .source(values()))))
                                .withSourceStats(sourceStats)
                                .check();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedArgumentIndentationInSelectorLineInvocation()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return builder()
                                .header().add(java.util.Map.<String, Object>ofEntries(
                                        entry("alg", "RS256"),
                                        entry("kid", "key"),
                                        entry("typ", "JWT")))
                                .done();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsBuilderSelectorIndentationInWrappedInvocationArgument()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return root()
                                .left(window(
                                        builder -> builder
                                                .first(spec())
                                                .second(function()),
                                        values()))
                                .done();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsNestedBuilderSelectorIndentationInsideBuilderLambdaInWrappedInvocationArgument()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return sink(join(
                                kind(),
                                builder -> builder
                                        .left(window(
                                                nested -> nested
                                                        .first(spec())
                                                        .second(function()),
                                                values()))
                                        .right(window(
                                                nested -> nested
                                                        .first(spec())
                                                        .second(function()),
                                                values()))));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsNestedBuilderSelectorChainInSelectorLineInvocationArgument()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return builder()
                                .header().add(ImmutableMap.<String, Object>builder()
                                        .put("alg", "RS256")
                                        .put("kid", "key")
                                        .put("typ", "JWT")
                                        .buildOrThrow())
                                .and();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsNestedBuilderSelectorChainInSelectorLineInvocationArgumentWithTrailingSelectors()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return newJwtBuilder()
                                .header().add(ImmutableMap.<String, Object>builder()
                                        .put("alg", "RS256")
                                        .put("kid", "key")
                                        .put("typ", "JWT")
                                        .buildOrThrow())
                                .and()
                                .claims(value());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesClassInstanceBackedSelectorChainInWrappedInvocationArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object invokeInputFunction)
                    {
                        return body().append(new ForLoop()
                                        .initialize(start())
                                        .condition(check())
                                        .update(next())
                                        .body(new IfStatement()
                                        .condition(check2())
                                        .ifFalse(invokeInputFunction)))
                                .ret();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object invokeInputFunction)
                    {
                        return body().append(new ForLoop()
                                        .initialize(start())
                                        .condition(check())
                                        .update(next())
                                        .body(new IfStatement()
                                                .condition(check2())
                                                .ifFalse(invokeInputFunction)))
                                .ret();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedClassInstanceBackedSelectorChainsInWrappedInvocationArguments()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object first, Object second)
                    {
                        return body().append(new ForLoop()
                                        .initialize(new BytecodeBlock()
                                                        .append(first())
                                                        .append(second()))
                                        .condition(new BytecodeBlock()
                                                        .append(first())
                                                        .append(second())
                                                        .invoke())
                                        .update(next())
                                        .body(new BytecodeBlock()
                                                        .append(apply(
                                                                first,
                                                                second))
                                                        .append(result())))
                                .ret();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object first, Object second)
                    {
                        return body().append(new ForLoop()
                                        .initialize(new BytecodeBlock()
                                                .append(first())
                                                .append(second()))
                                        .condition(new BytecodeBlock()
                                                .append(first())
                                                .append(second())
                                                .invoke())
                                        .update(next())
                                        .body(new BytecodeBlock()
                                                .append(apply(
                                                        first,
                                                        second))
                                                .append(result())))
                                .ret();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsShallowClassInstanceBackedChainInsideInvocationArgument()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return body().append(new IfStatement()
                                        .condition(flag())
                                        .ifTrue(new BytecodeBlock()
                                                .comment("output.appendNull();")
                                                .pop(value())
                                                .invoke(result())
                                                .pop())
                                        .ifFalse(other()))
                                .ret();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsShallowClassInstanceBackedChainInsideNestedInvocationArgument()
    {
        String code =
                """
                class Test
                {
                    Object run(Object wasNullVariable, Object valueJavaType)
                    {
                        return body().append(new IfStatement()
                                        .condition(wasNullVariable)
                                        .ifTrue(new BytecodeBlock()
                                                .comment("output.appendNull();")
                                                .pop(valueJavaType)
                                                .invokeInterface(BlockBuilder.class, "appendNull", BlockBuilder.class)
                                                .pop())
                                        .ifFalse(new BytecodeBlock()
                                                .comment("%s.%s(output, %s)", type(), methodName(), valueJavaType)
                                                .putVariable(tempValue())))
                                .ret();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsShallowClassInstanceBackedChainInsideNestedInvocationArgumentOfAssignmentPrefixedOuterChain()
    {
        String code =
                """
                class Test
                {
                    Object run(Object wasNullVariable, Object valueJavaType)
                    {
                        BytecodeBlock block = new BytecodeBlock()
                                .comment("if (wasNull)")
                                .append(new IfStatement()
                                        .condition(wasNullVariable)
                                        .ifTrue(new BytecodeBlock()
                                                .comment("output.appendNull();")
                                                .pop(valueJavaType)
                                                .invokeInterface(BlockBuilder.class, "appendNull", BlockBuilder.class)
                                                .pop())
                                        .ifFalse(new BytecodeBlock()
                                                .comment("%s.%s(output, %s)", type(), methodName(), valueJavaType)
                                                .putVariable(tempValue())));
                        return block;
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsShallowIfStatementChainInsideNestedBytecodeForLoopBody()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        BytecodeBlock computeHashLoop = new BytecodeBlock()
                                .append(flag())
                                .append(new ForLoop()
                                        .initialize(start())
                                        .condition(check())
                                        .update(next())
                                        .body(new BytecodeBlock()
                                                .append(position())
                                                .append(new IfStatement("if")
                                                        .condition(and(flag(), value()))
                                                        .ifTrue(hash())
                                                        .ifFalse(other()))
                                                .append(setHash())));
                        return computeHashLoop;
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesOverIndentedClassInstanceBackedSelectorChainsInAssignmentPrefixedInvocation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object first, Object second)
                    {
                        Loop loop = new Loop()
                                .initialize(new BytecodeBlock()
                                                .append(first())
                                                .append(second()))
                                .condition(new BytecodeBlock()
                                                .append(first())
                                                .append(second())
                                                .invoke())
                                .update(next())
                                .body(new BytecodeBlock()
                                                .append(apply(
                                                        first,
                                                        second))
                                                .append(result()));
                        return loop;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object first, Object second)
                    {
                        Loop loop = new Loop()
                                .initialize(new BytecodeBlock()
                                        .append(first())
                                        .append(second()))
                                .condition(new BytecodeBlock()
                                        .append(first())
                                        .append(second())
                                        .invoke())
                                .update(next())
                                .body(new BytecodeBlock()
                                        .append(apply(
                                                first,
                                                second))
                                        .append(result()));
                        return loop;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedBytecodeBuilderChainsInsideForLoopInitializers()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object loopBody)
                    {
                        Object rowsVariable = variable();
                        Object selectedPositionsArrayVariable = variable();
                        Object selectedPositionVariable = variable();
                        Object positionVariable = variable();
                        Object blockVariable = variable();

                        BytecodeBlock block = new BytecodeBlock()
                                .initializeVariable(rowsVariable)
                                .initializeVariable(selectedPositionVariable)
                                .initializeVariable(positionVariable);

                        ForLoop selectAllLoop = new ForLoop()
                                .initialize(new BytecodeBlock()
                                                .append(set(rowsVariable, invoke(mask())))
                                                .append(set(positionVariable, constantInt(0))))
                                .condition(lessThan(positionVariable, rowsVariable))
                                .update(new BytecodeBlock().incrementVariable(positionVariable, (byte) 1))
                                .body(generate(positionVariable));

                        ForLoop selectedPositionsLoop = new ForLoop()
                                .initialize(new BytecodeBlock()
                                                .append(set(rowsVariable, invokeSelected(mask())))
                                                .append(set(selectedPositionsArrayVariable, selected(mask())))
                                                .append(set(positionVariable, constantInt(0))))
                                .condition(lessThan(positionVariable, rowsVariable))
                                .update(new BytecodeBlock().incrementVariable(positionVariable, (byte) 1))
                                .body(new BytecodeBlock()
                                                .append(set(selectedPositionVariable, getElement(selectedPositionsArrayVariable, positionVariable)))
                                                .append(generate(selectedPositionVariable)));

                        IfStatement ifStatement = new IfStatement()
                                .condition(new BytecodeBlock()
                                                .append(blockVariable)
                                                .append(positionVariable)
                                                .invokeInterface())
                                .ifFalse(loopBody);

                        block.append(new ForLoop()
                                .initialize(set(positionVariable, constantInt(0)))
                                .condition(new BytecodeBlock()
                                                .append(positionVariable)
                                                .append(rowsVariable)
                                                .invokeStatic())
                                .update(new BytecodeBlock().incrementVariable(positionVariable, (byte) 1))
                                .body(ifStatement));

                        return block;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object loopBody)
                    {
                        Object rowsVariable = variable();
                        Object selectedPositionsArrayVariable = variable();
                        Object selectedPositionVariable = variable();
                        Object positionVariable = variable();
                        Object blockVariable = variable();

                        BytecodeBlock block = new BytecodeBlock()
                                .initializeVariable(rowsVariable)
                                .initializeVariable(selectedPositionVariable)
                                .initializeVariable(positionVariable);

                        ForLoop selectAllLoop = new ForLoop()
                                .initialize(new BytecodeBlock()
                                        .append(set(rowsVariable, invoke(mask())))
                                        .append(set(positionVariable, constantInt(0))))
                                .condition(lessThan(positionVariable, rowsVariable))
                                .update(new BytecodeBlock().incrementVariable(positionVariable, (byte) 1))
                                .body(generate(positionVariable));

                        ForLoop selectedPositionsLoop = new ForLoop()
                                .initialize(new BytecodeBlock()
                                        .append(set(rowsVariable, invokeSelected(mask())))
                                        .append(set(selectedPositionsArrayVariable, selected(mask())))
                                        .append(set(positionVariable, constantInt(0))))
                                .condition(lessThan(positionVariable, rowsVariable))
                                .update(new BytecodeBlock().incrementVariable(positionVariable, (byte) 1))
                                .body(new BytecodeBlock()
                                        .append(set(selectedPositionVariable, getElement(selectedPositionsArrayVariable, positionVariable)))
                                        .append(generate(selectedPositionVariable)));

                        IfStatement ifStatement = new IfStatement()
                                .condition(new BytecodeBlock()
                                        .append(blockVariable)
                                        .append(positionVariable)
                                        .invokeInterface())
                                .ifFalse(loopBody);

                        block.append(new ForLoop()
                                .initialize(set(positionVariable, constantInt(0)))
                                .condition(new BytecodeBlock()
                                        .append(positionVariable)
                                        .append(rowsVariable)
                                        .invokeStatic())
                                .update(new BytecodeBlock().incrementVariable(positionVariable, (byte) 1))
                                .body(ifStatement));

                        return block;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsNestedWrappedFirstArgumentIndentationInMultiArgumentInvocation()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return match(
                                groupId(ImmutableList.of(
                                                ImmutableList.of("a"),
                                                ImmutableList.of("b")),
                                        "group_id",
                                        values("a", "b")));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesNestedOnlyArgumentInvocationInSelectorLineInvocation()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        tester().assertThat(
                                        rule(
                                                        "name",
                                                        pattern(),
                                                        result()))
                                .on(plan());
                    }

                    Target tester()
                    {
                        return null;
                    }

                    Object rule(String name, Object pattern, Object result)
                    {
                        return null;
                    }

                    Object pattern()
                    {
                        return null;
                    }

                    Object result()
                    {
                        return null;
                    }

                    Object plan()
                    {
                        return null;
                    }

                    interface Target
                    {
                        Target assertThat(Object value);

                        void on(Object plan);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        tester().assertThat(
                                        rule(
                                                "name",
                                                pattern(),
                                                result()))
                                .on(plan());
                    }

                    Target tester()
                    {
                        return null;
                    }

                    Object rule(String name, Object pattern, Object result)
                    {
                        return null;
                    }

                    Object pattern()
                    {
                        return null;
                    }

                    Object result()
                    {
                        return null;
                    }

                    Object plan()
                    {
                        return null;
                    }

                    interface Target
                    {
                        Target assertThat(Object value);

                        void on(Object plan);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesNestedShortInvocationInSelectorLineInvocation()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        Object value = tester.assertThat(
                                        rule(
                                                        first(),
                                                        second(),
                                                        third()))
                                .done();
                    }

                    Target tester()
                    {
                        return null;
                    }

                    Object rule(Object first, Object second, Object third)
                    {
                        return null;
                    }

                    Object first()
                    {
                        return null;
                    }

                    Object second()
                    {
                        return null;
                    }

                    Object third()
                    {
                        return null;
                    }

                    interface Target
                    {
                        Target assertThat(Object value);

                        Object done();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        Object value = tester.assertThat(
                                        rule(
                                                first(),
                                                second(),
                                                third()))
                                .done();
                    }

                    Target tester()
                    {
                        return null;
                    }

                    Object rule(Object first, Object second, Object third)
                    {
                        return null;
                    }

                    Object first()
                    {
                        return null;
                    }

                    Object second()
                    {
                        return null;
                    }

                    Object third()
                    {
                        return null;
                    }

                    interface Target
                    {
                        Target assertThat(Object value);

                        Object done();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsNestedWrappedInvocationIndentationInsideConstructorArgumentWrapper()
    {
        String code =
                """
                class Test
                {
                    Test(Object featuresConfig, Object blockEncodingSerde, Object spillerStats, Object paths)
                    {
                        this(listeningDecorator(newFixedThreadPool(
                                        featuresConfig,
                                        daemonThreadsNamed("binary-spiller-%s"))),
                                blockEncodingSerde,
                                spillerStats,
                                paths);
                    }

                    Test(Object executor, Object blockEncodingSerde, Object spillerStats, Object paths) {}
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesNestedWrappedInvocationIndentationInsideConstructorArgumentWrapper()
    {
        String oldCode =
                """
                class Test
                {
                    Test(Object config, Object socksProxy, Object connectTimeout, Object readTimeout)
                    {
                        this(buildSslContext(
                                config,
                                key(),
                                password(),
                                trust(),
                                trustPassword()),
                                socksProxy,
                                connectTimeout,
                                readTimeout);
                    }

                    Test(Object sslContext, Object socksProxy, Object connectTimeout, Object readTimeout) {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    Test(Object config, Object socksProxy, Object connectTimeout, Object readTimeout)
                    {
                        this(buildSslContext(
                                        config,
                                        key(),
                                        password(),
                                        trust(),
                                        trustPassword()),
                                socksProxy,
                                connectTimeout,
                                readTimeout);
                    }

                    Test(Object sslContext, Object socksProxy, Object connectTimeout, Object readTimeout) {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesSingleArgumentWrapperNestedInvocationIndentation()
    {
        String oldCode =
                """
                class Test
                {
                    void run(Object queries, Object taskInfos)
                    {
                        assertThat(check(
                                queries,
                                ImmutableSet.of("q_1", "q_2"),
                                taskInfos)).isEqualTo(java.util.Optional.of(KillTarget.selectedTasks(
                                        ImmutableSet.of(
                                                taskId("q_1", 1),
                                                taskId("q_2", 6)))));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(Object queries, Object taskInfos)
                    {
                        assertThat(check(
                                queries,
                                ImmutableSet.of("q_1", "q_2"),
                                taskInfos)).isEqualTo(java.util.Optional.of(KillTarget.selectedTasks(
                                ImmutableSet.of(
                                        taskId("q_1", 1),
                                        taskId("q_2", 6)))));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsNestedWrappedInvocationArgumentsInSelectorLineInvocation()
    {
        String code =
                """
                class Test
                {
                    String run(java.util.List<Object> values)
                    {
                        return values.stream()
                                .collect(toImmutableMap(
                                        value -> value.toString(),
                                        value -> value))
                                .toString();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsNestedWrappedInvocationArgumentsInInlineLambdaBodySelectorChain()
    {
        String code =
                """
                class Test
                {
                    Object run(java.util.Optional<Object> optional)
                    {
                        return optional.map(info -> info.values().stream().flatMap(Test::expand)
                                        .collect(result(
                                                value -> value.toString(),
                                                value -> value)))
                                .orElse(other());
                    }

                    static java.util.stream.Stream<Object> expand(Object value)
                    {
                        return null;
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsNestedStaticCallArgumentsWithTrailingSelectorInWrappedInvocation()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return create(
                                Stream.of(new Item("a"),
                                                new Item("b"))
                                        .collect(result()),
                                other());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesUnderIndentedNestedStaticCallArgumentsWithTrailingSelectorInWrappedInvocation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return create(
                                Stream.of(new Item("a"),
                                        new Item("b"))
                                        .collect(result()),
                                other());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return create(
                                Stream.of(new Item("a"),
                                                new Item("b"))
                                        .collect(result()),
                                other());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedNestedStaticCallArgumentsWithTrailingSelectorInNonFirstWrappedInvocation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return create(
                                first(),
                                Stream.of(new Item("a"),
                                        new Item("b"))
                                        .collect(result()),
                                other());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return create(
                                first(),
                                Stream.of(new Item("a"),
                                                new Item("b"))
                                        .collect(result()),
                                other());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsWrappedMatcherChainInsideArgumentList()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return root()
                                .with(source().matching(
                                        // keep aligned with wrapped matcher argument
                                        node().capturedAs(first)
                                                .with(child().matching(
                                                        leaf().capturedAs(second)))));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesUnderIndentedWrappedMatcherChainInsideArgumentList()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return root()
                                .with(source().matching(
                                node().capturedAs(first)
                                        .with(child().matching(
                                                leaf().capturedAs(second)))));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return root()
                                .with(source().matching(
                                        node().capturedAs(first)
                                                .with(child().matching(
                                                        leaf().capturedAs(second)))));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedCommentedWrappedMatcherChainInsideArgumentList()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return root()
                                .with(source().matching(
                                // keep aligned with wrapped matcher argument
                                node().capturedAs(first)
                                        .with(child().matching(
                                                leaf().capturedAs(second)))));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return root()
                                .with(source().matching(
                                        // keep aligned with wrapped matcher argument
                                        node().capturedAs(first)
                                                .with(child().matching(
                                                        leaf().capturedAs(second)))));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedMultiInlineSelectorMatcherChainInsideWrappedArgumentList()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return limit()
                                .with(source().matching(
                                project().capturedAs(node).matching(Test::keep)
                                        .with(source().matching(
                                                sort().capturedAs(other)))));
                    }

                    static boolean keep(Object value)
                    {
                        return true;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return limit()
                                .with(source().matching(
                                        project().capturedAs(node).matching(Test::keep)
                                                .with(source().matching(
                                                        sort().capturedAs(other)))));
                    }

                    static boolean keep(Object value)
                    {
                        return true;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedWrappedMatcherChainInsideInlineLambdaBody()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return rule().or(
                                prev -> prev.with(right().matching(
                                exchange()
                                        .capturedAs(node))));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return rule().or(
                                prev -> prev.with(right().matching(
                                        exchange()
                                                .capturedAs(node))));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedNestedQualifiedChainInsideWrappedInvocationArgument()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        body().append(
                                value.set(
                                source().first(first)
                                        .second(second)
                                        .finish()));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        body().append(
                                value.set(
                                        source().first(first)
                                                .second(second)
                                                .finish()));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedNestedInvocationInsideInlineParentArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return result(node.replaceChildren(
                        ImmutableList.of(
                                first(),
                                second())));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return result(node.replaceChildren(
                                ImmutableList.of(
                                        first(),
                                        second())));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedNestedSingleArgumentInvocationInsideInlineParentArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return result(node.replaceChildren(
                        ImmutableList.of(new Item(
                                first(),
                                second()))));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return result(node.replaceChildren(
                                ImmutableList.of(new Item(
                                        first(),
                                        second()))));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedQualifiedSingleArgumentInvocationInsideInlineParentArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return keep(new Constraint(
                                Domain.wrap(
                                Helper.create(
                                        first(),
                                        second(),
                                        third()))));
                    }

                    Object keep(Object value)
                    {
                        return null;
                    }

                    record Constraint(Object value) {}

                    static class Domain
                    {
                        static Object wrap(Object value)
                        {
                            return null;
                        }
                    }

                    static class Helper
                    {
                        static Object create(Object first, Object second, Object third)
                        {
                            return null;
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return keep(new Constraint(
                                Domain.wrap(
                                        Helper.create(
                                                first(),
                                                second(),
                                                third()))));
                    }

                    Object keep(Object value)
                    {
                        return null;
                    }

                    record Constraint(Object value) {}

                    static class Domain
                    {
                        static Object wrap(Object value)
                        {
                            return null;
                        }
                    }

                    static class Helper
                    {
                        static Object create(Object first, Object second, Object third)
                        {
                            return null;
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedUnqualifiedInvocationInsideInlineParentArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return body().append(
                        generate(
                                first(),
                                second(),
                                third()));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return body().append(
                                generate(
                                        first(),
                                        second(),
                                        third()));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesSelectorChainInsideBooleanContinuation()
    {
        String oldCode =
                """
                import java.util.Collection;

                class Test
                {
                    boolean run(java.util.Collection<Object> partitioned, java.util.Map<String, Object> functions, Object child)
                    {
                        return partitioned.stream().anyMatch(this::contains)
                                || functions.values().stream()
                                                .map(Test::extract)
                                                .flatMap(Collection::stream)
                                                .anyMatch(this::contains);
                    }

                    static Collection<Object> extract(Object value)
                    {
                        return java.util.List.of();
                    }

                    boolean contains(Object value)
                    {
                        return true;
                    }
                }
                """;

        String newCode =
                """
                import java.util.Collection;

                class Test
                {
                    boolean run(java.util.Collection<Object> partitioned, java.util.Map<String, Object> functions, Object child)
                    {
                        return partitioned.stream().anyMatch(this::contains)
                                || functions.values().stream()
                                .map(Test::extract)
                                .flatMap(Collection::stream)
                                .anyMatch(this::contains);
                    }

                    static Collection<Object> extract(Object value)
                    {
                        return java.util.List.of();
                    }

                    boolean contains(Object value)
                    {
                        return true;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsSelectorChainInsideInlineOptionalMapExpression()
    {
        String code =
                """
                class Test
                {
                    Object run(java.util.Optional<Object> optional)
                    {
                        return optional.map(info -> info.stats().getDistribution()
                                        .entrySet().stream()
                                        .collect(result()))
                                .orElse(other());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsStandardWrappedIndentForOnlyArgumentInvocationWithTrailingSelector()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return assertThat(contains(
                                first(),
                                second()))
                                .isTrue();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesOverIndentedWrappedArgumentsInOnlyArgumentQualifiedInvocationWithTrailingSelector()
    {
        String oldCode =
                """
                class Test
                {
                    void run(Object[] expectedValues, int start, Object outputType, Object block)
                    {
                        assertThat(makeValidityAssertion(expectedValues[start]).apply(
                                        BlockAssertions.getOnlyValue(outputType, block),
                                        expectedValues[start]))
                                .isTrue();
                    }

                    Checker assertThat(boolean value)
                    {
                        return null;
                    }

                    Assertion makeValidityAssertion(Object expected)
                    {
                        return null;
                    }

                    interface Checker
                    {
                        void isTrue();
                    }

                    interface Assertion
                    {
                        boolean apply(Object left, Object right);
                    }

                    static class BlockAssertions
                    {
                        static Object getOnlyValue(Object outputType, Object block)
                        {
                            return null;
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(Object[] expectedValues, int start, Object outputType, Object block)
                    {
                        assertThat(makeValidityAssertion(expectedValues[start]).apply(
                                BlockAssertions.getOnlyValue(outputType, block),
                                expectedValues[start]))
                                .isTrue();
                    }

                    Checker assertThat(boolean value)
                    {
                        return null;
                    }

                    Assertion makeValidityAssertion(Object expected)
                    {
                        return null;
                    }

                    interface Checker
                    {
                        void isTrue();
                    }

                    interface Assertion
                    {
                        boolean apply(Object left, Object right);
                    }

                    static class BlockAssertions
                    {
                        static Object getOnlyValue(Object outputType, Object block)
                        {
                            return null;
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsSelectorChainInsideInfixContinuation()
    {
        String code =
                """
                class Test
                {
                    boolean run(Object left, Object parent, Object child)
                    {
                        return left(parent, child)
                                || parent.values().stream()
                                .map(Object::toString)
                                .flatMap(Test::stream)
                                .anyMatch(child::equals);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsNestedWrappedInvocationInMidLineCall()
    {
        String code =
                """
                class Test
                {
                    Object run(Object value, Object min, Object max, Object complement)
                    {
                        return process(and(
                                compare(value, min),
                                compare(value, max)), complement);
                    }

                    Object process(Object left, Object right)
                    {
                        return null;
                    }

                    Object and(Object left, Object right)
                    {
                        return null;
                    }

                    Object compare(Object left, Object right)
                    {
                        return null;
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsNestedWrappedConstructorInvocationInMidLineCall()
    {
        String code =
                """
                class Test
                {
                    Object run(Object table, Object column, Object exists, Object queryStateMachine)
                    {
                        return task().execute(new Drop(
                                table,
                                column,
                                exists), queryStateMachine);
                    }

                    Runner task()
                    {
                        return null;
                    }

                    record Drop(Object table, Object column, Object exists) {}

                    interface Runner
                    {
                        Object execute(Object a, Object b);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesUnderIndentedQualifiedInvocationInsideConstructorArgumentOfSelectorLineCall()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object session, Object tableHandle, Object tableSchemaColumn)
                    {
                        return metadata().applyFilter(session, tableHandle, new Constraint(TupleDomain.withColumnDomains(
                                ImmutableMap.of(tableSchemaColumn, Domain.singleValue(Type.VARCHAR, Slices.utf8Slice(""))))))
                                .map(Result::getHandle)
                                .map(Handle.class::cast)
                                .orElseThrow(AssertionError::new);
                    }

                    Metadata metadata()
                    {
                        return null;
                    }

                    record Constraint(Object value) {}

                    interface Metadata
                    {
                        Chain applyFilter(Object session, Object tableHandle, Object constraint);
                    }

                    interface Chain
                    {
                        Chain map(Object mapper);

                        Object orElseThrow();
                    }

                    interface Result
                    {
                        Object getHandle();
                    }

                    static class Handle {}

                    static class TupleDomain
                    {
                        static Object withColumnDomains(Object value)
                        {
                            return null;
                        }
                    }

                    static class ImmutableMap
                    {
                        static Object of(Object left, Object right)
                        {
                            return null;
                        }
                    }

                    static class Domain
                    {
                        static Object singleValue(Object type, Object value)
                        {
                            return null;
                        }
                    }

                    static class Type
                    {
                        static final Object VARCHAR = new Object();
                    }

                    static class Slices
                    {
                        static Object utf8Slice(String value)
                        {
                            return null;
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object session, Object tableHandle, Object tableSchemaColumn)
                    {
                        return metadata().applyFilter(session, tableHandle, new Constraint(TupleDomain.withColumnDomains(
                                        ImmutableMap.of(tableSchemaColumn, Domain.singleValue(Type.VARCHAR, Slices.utf8Slice(""))))))
                                .map(Result::getHandle)
                                .map(Handle.class::cast)
                                .orElseThrow(AssertionError::new);
                    }

                    Metadata metadata()
                    {
                        return null;
                    }

                    record Constraint(Object value) {}

                    interface Metadata
                    {
                        Chain applyFilter(Object session, Object tableHandle, Object constraint);
                    }

                    interface Chain
                    {
                        Chain map(Object mapper);

                        Object orElseThrow();
                    }

                    interface Result
                    {
                        Object getHandle();
                    }

                    static class Handle {}

                    static class TupleDomain
                    {
                        static Object withColumnDomains(Object value)
                        {
                            return null;
                        }
                    }

                    static class ImmutableMap
                    {
                        static Object of(Object left, Object right)
                        {
                            return null;
                        }
                    }

                    static class Domain
                    {
                        static Object singleValue(Object type, Object value)
                        {
                            return null;
                        }
                    }

                    static class Type
                    {
                        static final Object VARCHAR = new Object();
                    }

                    static class Slices
                    {
                        static Object utf8Slice(String value)
                        {
                            return null;
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedWideQualifiedInvocationInsideConstructorArgumentOfSelectorLineCall()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object session, Object tableHandle, Object tableSchemaColumn)
                    {
                        return metadata().applyFilter(session, tableHandle, new Constraint(TupleDomain.withColumnDomains(
                                ImmutableMap.of(tableSchemaColumn, Domain.singleValue(VARCHAR, Slices.utf8Slice(""))))))
                                .map(ConstraintApplicationResult::getHandle)
                                .map(InformationSchemaTableHandle.class::cast)
                                .orElseThrow(AssertionError::new);
                    }

                    Metadata metadata()
                    {
                        return null;
                    }

                    record Constraint(Object value) {}

                    interface Metadata
                    {
                        Chain applyFilter(Object session, Object tableHandle, Object constraint);
                    }

                    interface Chain
                    {
                        Chain map(Object mapper);

                        Object orElseThrow(Object mapper);
                    }

                    interface ConstraintApplicationResult
                    {
                        Object getHandle();
                    }

                    static class InformationSchemaTableHandle {}

                    static class TupleDomain
                    {
                        static Object withColumnDomains(Object value)
                        {
                            return null;
                        }
                    }

                    static class ImmutableMap
                    {
                        static Object of(Object left, Object right)
                        {
                            return null;
                        }
                    }

                    static class Domain
                    {
                        static Object singleValue(Object type, Object value)
                        {
                            return null;
                        }
                    }

                    static final Object VARCHAR = new Object();

                    static class Slices
                    {
                        static Object utf8Slice(String value)
                        {
                            return null;
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object session, Object tableHandle, Object tableSchemaColumn)
                    {
                        return metadata().applyFilter(session, tableHandle, new Constraint(TupleDomain.withColumnDomains(
                                        ImmutableMap.of(tableSchemaColumn, Domain.singleValue(VARCHAR, Slices.utf8Slice(""))))))
                                .map(ConstraintApplicationResult::getHandle)
                                .map(InformationSchemaTableHandle.class::cast)
                                .orElseThrow(AssertionError::new);
                    }

                    Metadata metadata()
                    {
                        return null;
                    }

                    record Constraint(Object value) {}

                    interface Metadata
                    {
                        Chain applyFilter(Object session, Object tableHandle, Object constraint);
                    }

                    interface Chain
                    {
                        Chain map(Object mapper);

                        Object orElseThrow(Object mapper);
                    }

                    interface ConstraintApplicationResult
                    {
                        Object getHandle();
                    }

                    static class InformationSchemaTableHandle {}

                    static class TupleDomain
                    {
                        static Object withColumnDomains(Object value)
                        {
                            return null;
                        }
                    }

                    static class ImmutableMap
                    {
                        static Object of(Object left, Object right)
                        {
                            return null;
                        }
                    }

                    static class Domain
                    {
                        static Object singleValue(Object type, Object value)
                        {
                            return null;
                        }
                    }

                    static final Object VARCHAR = new Object();

                    static class Slices
                    {
                        static Object utf8Slice(String value)
                        {
                            return null;
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedQualifiedInvocationInsidePlainSmartCallSite()
    {
        String oldCode =
                """
                class Test
                {
                    void run(Object real)
                    {
                        assertThat(applySaturatedCasts(
                                Domain.create(
                                        ValueSet.ofRanges(range(DOUBLE.value, 0.0, true, maxDouble(), true)),
                                        true),
                                real)).isEqualTo(Domain.create(
                                        ValueSet.ofRanges(range(REAL.value, zeroFloat(), true, maxFloat(), true)),
                                        true));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(Object real)
                    {
                        assertThat(applySaturatedCasts(
                                Domain.create(
                                        ValueSet.ofRanges(range(DOUBLE.value, 0.0, true, maxDouble(), true)),
                                        true),
                                real)).isEqualTo(Domain.create(
                                ValueSet.ofRanges(range(REAL.value, zeroFloat(), true, maxFloat(), true)),
                                true));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedQualifiedInvocationInsideInlinePlainSmartCallSite()
    {
        String oldCode =
                """
                class Test
                {
                    void run(Object original)
                    {
                        assertThat(applySaturatedCasts(original, REAL)).isEqualTo(Domain.create(
                        ValueSet.ofRanges(
                                lessThan(REAL, zero()),
                                range(REAL, zero(), false, one(), false),
                                range(REAL, two(), true, three(), true),
                                greaterThan(REAL, four())),
                        true));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(Object original)
                    {
                        assertThat(applySaturatedCasts(original, REAL)).isEqualTo(Domain.create(
                                ValueSet.ofRanges(
                                        lessThan(REAL, zero()),
                                        range(REAL, zero(), false, one(), false),
                                        range(REAL, two(), true, three(), true),
                                        greaterThan(REAL, four())),
                                true));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsWrappedOnlyArgumentInvocationInSelectorLineCall()
    {
        String code =
                """
                class Test
                {
                    Object run(Object binder, Object factory, Object arrayType, Object argsLength)
                    {
                        return chain()
                                .append(
                                        create(
                                                VeryLongStateType.class,
                                                loadConstant(binder, factory, Handle.class).invoke("invokeExact", Object.class),
                                                newArray(type(arrayType), argsLength).cast(Object.class)).ret());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedConstructorInvocationInTernaryBranch()
    {
        String code =
                """
                class Test
                {
                    Object run(boolean flag)
                    {
                        return flag
                                ? new Marker()
                                : new Marker(
                                first(),
                                second());
                    }

                    Object first()
                    {
                        return null;
                    }

                    Object second()
                    {
                        return null;
                    }

                    record Marker(Object a, Object b)
                    {
                        Marker()
                        {
                            this(null, null);
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesUnderIndentedNonFirstWrappedStaticFactoryArgumentInsideConstructorArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return new Holder(
                                null,
                                java.util.stream.Stream.of(new Item("a"),
                                        new Item("b"))
                                        .toList(),
                                other());
                    }

                    Object other()
                    {
                        return null;
                    }

                    record Holder(Object left, Object middle, Object right) {}

                    record Item(String value) {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return new Holder(
                                null,
                                java.util.stream.Stream.of(
                                                new Item("a"),
                                                new Item("b"))
                                        .toList(),
                                other());
                    }

                    Object other()
                    {
                        return null;
                    }

                    record Holder(Object left, Object middle, Object right) {}

                    record Item(String value) {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsSelectorChainInsideInlineOptionalMapLambda()
    {
        // Matches IntelliJ: lambda body on new line after `->` gets double
        // CONTINUATION from the statement (lambda's own CONTINUATION + the
        // outer chain's expanded enforce). Inner chain selectors wrap at
        // single CONTINUATION from the body line.
        String code =
                """
                class Test
                {
                    Object run(java.util.Optional<Object> optional)
                    {
                        return optional.map(value ->
                                        items(value).stream().flatMap(Test::expand)
                                                .collect(java.util.stream.Collectors.toSet()))
                                .orElse(java.util.Set.of());
                    }

                    java.util.List<Object> items(Object value)
                    {
                        return java.util.List.of(value);
                    }

                    static java.util.stream.Stream<Object> expand(Object value)
                    {
                        return java.util.stream.Stream.of(value);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesUnderIndentedNestedInvocationInsideOnlyArgumentOfInlineParentInvocation()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        body().append(size.set(
                        combine(
                                first(),
                                second())));
                    }

                    Target body()
                    {
                        return null;
                    }

                    Target size()
                    {
                        return null;
                    }

                    Object combine(Object left, Object right)
                    {
                        return null;
                    }

                    Object first()
                    {
                        return null;
                    }

                    Object second()
                    {
                        return null;
                    }

                    interface Target
                    {
                        Target append(Object value);

                        Object set(Object value);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        body().append(size.set(
                                combine(
                                        first(),
                                        second())));
                    }

                    Target body()
                    {
                        return null;
                    }

                    Target size()
                    {
                        return null;
                    }

                    Object combine(Object left, Object right)
                    {
                        return null;
                    }

                    Object first()
                    {
                        return null;
                    }

                    Object second()
                    {
                        return null;
                    }

                    interface Target
                    {
                        Target append(Object value);

                        Object set(Object value);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedMatcherChainInsideWrappedSelectorArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return exchange()
                                .with(source().matching(
                                node().matching(Test::matches)
                                        .with(child().matching(Test::other))
                                        .capturedAs(capture)));
                    }

                    static boolean matches(Object value)
                    {
                        return true;
                    }

                    static boolean other(Object value)
                    {
                        return true;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return exchange()
                                .with(source().matching(
                                        node().matching(Test::matches)
                                                .with(child().matching(Test::other))
                                                .capturedAs(capture)));
                    }

                    static boolean matches(Object value)
                    {
                        return true;
                    }

                    static boolean other(Object value)
                    {
                        return true;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedBlockLambdaBodiesInsideWrappedIfPresentOrElseArguments()
    {
        String oldCode =
                """
                class Test
                {
                    void run(java.util.Optional<Object> optional)
                    {
                        optional.ifPresentOrElse(
                                value -> {
                                first(value);
                                second(value);
                            },
                                () -> {
                                fallback();
                            });
                    }

                    void first(Object value) {}

                    void second(Object value) {}

                    void fallback() {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(java.util.Optional<Object> optional)
                    {
                        optional.ifPresentOrElse(
                                value -> {
                                    first(value);
                                    second(value);
                                },
                                () -> {
                                    fallback();
                                });
                    }

                    void first(Object value) {}

                    void second(Object value) {}

                    void fallback() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsSelectorChainInsideInlineOptionalMapLambdaWithNestedLambda()
    {
        // Matches IntelliJ: same pattern as the previous test but with a
        // nested lambda in `.flatMap(value -> expand(value).stream())`.
        // Lambda body head at double CONTINUATION, inner selector at +8.
        String code =
                """
                class Test
                {
                    Object run(java.util.Optional<Object> optional)
                    {
                        return optional.map(info ->
                                        values(info).stream().flatMap(value -> expand(value).stream())
                                                .collect(java.util.stream.Collectors.toSet()))
                                .orElse(java.util.Set.of());
                    }

                    java.util.List<Object> values(Object value)
                    {
                        return java.util.List.of(value);
                    }

                    java.util.List<Object> expand(Object value)
                    {
                        return java.util.List.of(value);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesUnderIndentedQualifiedInvocationInsideOnlyArgumentOfSelectorLineParentInvocation()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        body()
                                .append(block.set(
                                source().first(first)
                                        .second(second)
                                        .finish()));
                    }

                    Target body()
                    {
                        return null;
                    }

                    Target block()
                    {
                        return null;
                    }

                    Chain source()
                    {
                        return null;
                    }

                    Object first()
                    {
                        return null;
                    }

                    Object second()
                    {
                        return null;
                    }

                    interface Target
                    {
                        Target append(Object value);

                        Object set(Object value);
                    }

                    interface Chain
                    {
                        Chain first(Object value);

                        Chain second(Object value);

                        Object finish();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        body()
                                .append(block.set(
                                        source().first(first)
                                                .second(second)
                                                .finish()));
                    }

                    Target body()
                    {
                        return null;
                    }

                    Target block()
                    {
                        return null;
                    }

                    Chain source()
                    {
                        return null;
                    }

                    Object first()
                    {
                        return null;
                    }

                    Object second()
                    {
                        return null;
                    }

                    interface Target
                    {
                        Target append(Object value);

                        Object set(Object value);
                    }

                    interface Chain
                    {
                        Chain first(Object value);

                        Chain second(Object value);

                        Object finish();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedStaticInvocationInsideOnlyArgumentOfSelectorLineParentInvocation()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        body()
                                .append(size.set(
                                Helpers.combine(
                                        first(),
                                        second())));
                    }

                    Target body()
                    {
                        return null;
                    }

                    Target size()
                    {
                        return null;
                    }

                    Object first()
                    {
                        return null;
                    }

                    Object second()
                    {
                        return null;
                    }

                    interface Target
                    {
                        Target append(Object value);

                        Object set(Object value);
                    }

                    static class Helpers
                    {
                        static Object combine(Object left, Object right)
                        {
                            return null;
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        body()
                                .append(size.set(
                                        Helpers.combine(
                                                first(),
                                                second())));
                    }

                    Target body()
                    {
                        return null;
                    }

                    Target size()
                    {
                        return null;
                    }

                    Object first()
                    {
                        return null;
                    }

                    Object second()
                    {
                        return null;
                    }

                    interface Target
                    {
                        Target append(Object value);

                        Object set(Object value);
                    }

                    static class Helpers
                    {
                        static Object combine(Object left, Object right)
                        {
                            return null;
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedWideQualifiedInvocationInsidePlainSmartCallSite()
    {
        String oldCode =
                """
                class Test
                {
                    void run(Object real)
                    {
                        assertThat(applySaturatedCasts(
                                Domain.create(
                                        ValueSet.ofRanges(
                                                lessThan(REAL.value, zeroFloat()),
                                                range(REAL.value, zeroFloat(), false, oneFloat(), false),
                                                range(REAL.value, twoFloat(), true, threeFloat(), true),
                                                greaterThan(REAL.value, fourFloat())),
                                        true),
                                real)).isEqualTo(Domain.create(
                                        ValueSet.ofRanges(
                                                lessThan(REAL.value, zeroFloat()),
                                                range(REAL.value, zeroFloat(), false, oneFloat(), false),
                                                range(REAL.value, twoFloat(), true, threeFloat(), true),
                                                greaterThan(REAL.value, fourFloat())),
                                        true));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(Object real)
                    {
                        assertThat(applySaturatedCasts(
                                Domain.create(
                                        ValueSet.ofRanges(
                                                lessThan(REAL.value, zeroFloat()),
                                                range(REAL.value, zeroFloat(), false, oneFloat(), false),
                                                range(REAL.value, twoFloat(), true, threeFloat(), true),
                                                greaterThan(REAL.value, fourFloat())),
                                        true),
                                real)).isEqualTo(Domain.create(
                                ValueSet.ofRanges(
                                        lessThan(REAL.value, zeroFloat()),
                                        range(REAL.value, zeroFloat(), false, oneFloat(), false),
                                        range(REAL.value, twoFloat(), true, threeFloat(), true),
                                        greaterThan(REAL.value, fourFloat())),
                                true));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedUnqualifiedBuilderChainInsideWrappedOnlyArgumentCall()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(create(
                                        builder()
                                                .put(A, value())
                                                .put(B, value())
                                                .build()));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(create(
                                builder()
                                        .put(A, value())
                                        .put(B, value())
                                        .build()));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedCollectorSelectorInsideInlineMapLambdaWithNestedLambda()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(QueryInfo queryInfo)
                    {
                        return queryInfo.getStages().map(stagesInfo ->
                                stagesInfo.getStages().stream().flatMap(stageInfo -> stageInfo.tasks().stream())
                                                .collect(toImmutableMap(
                                                        taskInfo -> taskInfo.taskStatus().taskId(),
                                                        identity())))
                                .orElse(java.util.Map.of());
                    }

                    interface QueryInfo
                    {
                        java.util.Optional<StagesInfo> getStages();
                    }

                    interface StagesInfo
                    {
                        java.util.List<StageInfo> getStages();
                    }

                    interface StageInfo
                    {
                        java.util.List<TaskInfo> tasks();
                    }

                    interface TaskInfo
                    {
                        TaskStatus taskStatus();
                    }

                    interface TaskStatus
                    {
                        Object taskId();
                    }

                    static Object toImmutableMap(Object left, Object right)
                    {
                        return null;
                    }

                    static Object identity()
                    {
                        return null;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(QueryInfo queryInfo)
                    {
                        return queryInfo.getStages().map(stagesInfo ->
                                        stagesInfo.getStages().stream().flatMap(stageInfo -> stageInfo.tasks().stream())
                                                .collect(toImmutableMap(
                                                        taskInfo -> taskInfo.taskStatus().taskId(),
                                                        identity())))
                                .orElse(java.util.Map.of());
                    }

                    interface QueryInfo
                    {
                        java.util.Optional<StagesInfo> getStages();
                    }

                    interface StagesInfo
                    {
                        java.util.List<StageInfo> getStages();
                    }

                    interface StageInfo
                    {
                        java.util.List<TaskInfo> tasks();
                    }

                    interface TaskInfo
                    {
                        TaskStatus taskStatus();
                    }

                    interface TaskStatus
                    {
                        Object taskId();
                    }

                    static Object toImmutableMap(Object left, Object right)
                    {
                        return null;
                    }

                    static Object identity()
                    {
                        return null;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedSelectorChainInWrappedVerificationArgument()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        verify(stream()
                                .mapToInt(fieldAction -> switch (fieldAction) {
                                    case A a -> a.x();
                                    case B b -> b.x();
                                    case C ignore -> -1;
                                }).filter(a -> a >= 0)
                                .distinct()
                                .sum() == size(),
                                "ok");
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        verify(stream()
                                        .mapToInt(fieldAction -> switch (fieldAction) {
                                            case A a -> a.x();
                                            case B b -> b.x();
                                            case C ignore -> -1;
                                        }).filter(a -> a >= 0)
                                        .distinct()
                                        .sum() == size(),
                                "ok");
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedBuilderChainInsideSelectorPrefixedWrappedArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return assertThat(query())
                                .result().matches(resultBuilder()
                                .row(first())
                                .row(second())
                                .build());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return assertThat(query())
                                .result().matches(resultBuilder()
                                        .row(first())
                                        .row(second())
                                        .build());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedClassInstanceBuilderChainInsideWrappedArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return request()
                                .url(url())
                                .post(new FormBody.Builder()
                                .add("grant_type", value())
                                .add("audience", audience())
                                .build())
                                .build();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return request()
                                .url(url())
                                .post(new FormBody.Builder()
                                        .add("grant_type", value())
                                        .add("audience", audience())
                                        .build())
                                .build();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedNestedOnlyArgumentInvocationInsideWrappedFirstArgument()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(run(
                                wrap(
                                                and(
                                                        and(
                                                                equal(firstColumn(), null),
                                                                equal(secondColumn(), null)),
                                                        and(
                                                                equal(thirdColumn(), null),
                                                                equal(fourthColumn(), null)))),
                                other(),
                                last()));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(run(
                                wrap(
                                        and(
                                                and(
                                                        equal(firstColumn(), null),
                                                        equal(secondColumn(), null)),
                                                and(
                                                        equal(thirdColumn(), null),
                                                        equal(fourthColumn(), null)))),
                                other(),
                                last()));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedNestedVerificationLambdaInWrappedSelectorArgument()
    {
        String oldCode =
                """
                import java.util.function.Consumer;

                class Test
                {
                    void run(Holder handle)
                    {
                        values()
                                .distinct()
                                .peek(handle.getColumns().<Consumer<Object>>map(
                                        columns -> value -> verify(
                                                columns.contains(value),
                                                "bad",
                                                value,
                                                values()))
                                        .orElse(_ -> {}))
                                .forEach(this::add);
                    }
                }
                """;

        String newCode =
                """
                import java.util.function.Consumer;

                class Test
                {
                    void run(Holder handle)
                    {
                        values()
                                .distinct()
                                .peek(handle.getColumns().<Consumer<Object>>map(
                                                columns -> value -> verify(
                                                        columns.contains(value),
                                                        "bad",
                                                        value,
                                                        values()))
                                        .orElse(_ -> {}))
                                .forEach(this::add);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedStreamChainInsideConstructorArgument()
    {
        String oldCode =
                """
                import java.util.List;

                class Test
                {
                    Test(Entry entry)
                    {
                        this(entry.blocks()
                                .orElseGet(() -> List.of(new Block(List.of(), 0, entry.length())))
                                .stream()
                                .map(BlockLocation::new)
                                .toList(),
                                entry.location(),
                                false);
                    }

                    Test(Object blocks, Object location, boolean hidden) {}
                }
                """;

        String newCode =
                """
                import java.util.List;

                class Test
                {
                    Test(Entry entry)
                    {
                        this(entry.blocks()
                                        .orElseGet(() -> List.of(new Block(List.of(), 0, entry.length())))
                                        .stream()
                                        .map(BlockLocation::new)
                                        .toList(),
                                entry.location(),
                                false);
                    }

                    Test(Object blocks, Object location, boolean hidden) {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsSelectorChainAfterInlinePlusInsideWrappedFirstArgumentShallow()
    {
        String code =
                """
                class Test
                {
                    void run(String tableName)
                    {
                        assertUpdate("INSERT INTO " + tableName + " VALUES " + java.util.stream.IntStream.rangeClosed(1, 2)
                                        .mapToObj(i -> format(i))
                                        .collect(java.util.stream.Collectors.joining(", ")),
                                5);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsInlineSecondArgumentAfterWrappedVerificationChainShallow()
    {
        String code =
                """
                class Test
                {
                    void run(Object[] values)
                    {
                        verify(java.util.Arrays.stream(values)
                                .mapToInt(fieldAction -> switch (fieldAction) {
                                    case A a -> a.x();
                                    case B b -> b.x();
                                    case C ignore -> -1;
                                })
                                .filter(a -> a >= 0)
                                .distinct().count() == size(), "ok");
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsMultiWrappedSelectorChainInInlineLambdaBodyAtSingleContinuationIndent()
    {
        String code =
                """
                class Test
                {
                    Object run(java.util.List<Result> results)
                    {
                        return results.stream().map(result ->
                                        result.values().stream()
                                                .map(Test::wrap)
                                                .collect(java.util.stream.Collectors.toList()))
                                .flatMap(java.util.List::stream)
                                .collect(java.util.stream.Collectors.toList());
                    }

                    static Object wrap(Object value)
                    {
                        return value;
                    }

                    interface Result
                    {
                        java.util.List<Object> values();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesOverIndentedQualifiedSingleArgumentChainInsideWrappedFirstArgument()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        Object testDomain = Domain.create(
                                ValueSet.ofRanges(
                                        Range.lessThan(BIGINT, 4L)).intersect(
                                                        ValueSet.all(BIGINT)
                                                                .subtract(ValueSet.ofRanges(Range.equal(BIGINT, 1L), Range.equal(BIGINT, 2L), Range.equal(BIGINT, 3L)))),
                                false);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        Object testDomain = Domain.create(
                                ValueSet.ofRanges(
                                        Range.lessThan(BIGINT, 4L)).intersect(
                                        ValueSet.all(BIGINT)
                                                .subtract(ValueSet.ofRanges(Range.equal(BIGINT, 1L), Range.equal(BIGINT, 2L), Range.equal(BIGINT, 3L)))),
                                false);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedQualifiedOnlyArgumentChainInsideInlineQualifiedInvocation()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(build(
                                first(),
                                second())).isEqualTo(TupleDomain.withColumnDomains(ImmutableMap.builder()
                                                .put(A, value())
                                                .put(B, value())
                                                .buildOrThrow()));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(build(
                                first(),
                                second())).isEqualTo(TupleDomain.withColumnDomains(ImmutableMap.builder()
                                .put(A, value())
                                .put(B, value())
                                .buildOrThrow()));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedQualifiedInvocationInsideInlineQualifiedFirstArgumentOfWrappedCall()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        assertPredicateTranslates(
                                greaterThan(left(), cast(C_VARCHAR, TYPE)),
                                tupleDomain(
                                        C_VARCHAR,
                                        Domain.create(ValueSet.ofRanges(
                                                                Range.lessThan(TYPE, lower()),
                                                                Range.greaterThan(TYPE, upper())),
                                                false)),
                                greaterThan(left(), cast(C_VARCHAR, TYPE)));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        assertPredicateTranslates(
                                greaterThan(left(), cast(C_VARCHAR, TYPE)),
                                tupleDomain(
                                        C_VARCHAR,
                                        Domain.create(ValueSet.ofRanges(
                __DEEP__Range.lessThan(TYPE, lower()),
                __DEEP__Range.greaterThan(TYPE, upper())),
                                                false)),
                                greaterThan(left(), cast(C_VARCHAR, TYPE)));
                    }
                }
                """.replace("__DEEP__", "                                        ");

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedPairWrappedQualifiedOnlyArgumentInvocationInsideMultilineQualifiedCall()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(build(
                                first(),
                                second())).isEqualTo(ImmutableMap.of(
                                        left(), right(),
                                        next(), other()));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(build(
                                first(),
                                second())).isEqualTo(ImmutableMap.of(
                                left(), right(),
                                next(), other()));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsShallowPairWrappedMapFactoryInsideMultilineQualifiedOnlyArgumentInvocation()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(build(
                                first(),
                                second())).isEqualTo(ImmutableMap.of(
                                left(), right(),
                                next(), other()));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesOverIndentedPairWrappedMapFactoryInsideQualifiedInvocationAfterMultilineQualifiedExpression()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(extract(withColumnDomains(
                                ImmutableMap.<Object, Object>builder()
                                        .put(firstKey(), firstValue())
                                        .put(secondKey(), secondValue())
                                        .put(thirdKey(), thirdValue())
                                        .put(fourthKey(), fourthValue())
                                        .buildOrThrow())).get()).isEqualTo(ImmutableMap.of(
                                        secondKey(), present(),
                                        thirdKey(), absent()));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(extract(withColumnDomains(
                                ImmutableMap.<Object, Object>builder()
                                        .put(firstKey(), firstValue())
                                        .put(secondKey(), secondValue())
                                        .put(thirdKey(), thirdValue())
                                        .put(fourthKey(), fourthValue())
                                        .buildOrThrow())).get()).isEqualTo(ImmutableMap.of(
                                secondKey(), present(),
                                thirdKey(), absent()));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsShallowPairWrappedMapFactoryInsideInlineExpressionLambdaBodyInvocation()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return builder()
                                .withItems((_, _) -> ImmutableMap.of(
                                        first(), second(),
                                        third(), fourth()))
                                .done();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsShallowStreamChainInsideSingleInlineWrappedInvocationItem()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        consume(copy(transform(values().stream()
                                .map(this::item)
                                .collect(toList()))));
                    }

                    Object item(Object value)
                    {
                        return value;
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsShallowBuilderChainInsideSingleInlineWrappedInvocationItem()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(query()).matches(ImmutableList.builder()
                                .add(first())
                                .add(second())
                                .build());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsShallowBuilderChainInsideSingleInlineWrappedInvocationItemAfterMultilineReceiver()
    {
        String code =
                """
                class Test
                {
                    void run(Object value)
                    {
                        assertThat(source(value)
                                .map(this::item)
                                .toList()).isEqualTo(ImmutableList.builder()
                                .add(first())
                                .add(second())
                                .build());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsShallowQualifiedStreamChainInsideSingleInlineWrappedInvocationItem()
    {
        String code =
                """
                class Test
                {
                    void run(Object values)
                    {
                        consume(copy(project(values.stream()
                                .map(this::item)
                                .collect(toList()))));
                    }

                    Object item(Object value)
                    {
                        return value;
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsShallowMapFactoryInsideInlineExpressionLambdaBodyInvocationWithMultilineValue()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return builder()
                                .withItems((_, _) -> ImmutableMap.of(
                                        firstKey(), new Item(
                                                firstValue(),
                                                secondValue())))
                                .done();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsShallowStaticFactoryArgumentsInsideLambdaStreamChain()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return target()
                                .items(columns().stream()
                                        .flatMap(column -> Stream.of(
                                                first(column),
                                                second(column),
                                                third(column),
                                                fourth(column)))
                                        .toList())
                                .done();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsShallowBuilderChainInsideWrappedArgumentBeforeTrailingSelector()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return root()
                                .config(new Config()
                                        .setFirst(first())
                                        // keep note
                                        .setSecond(second()))
                                .done();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsShallowQualifiedStreamChainInsideNestedStaticInvocation()
    {
        String code =
                """
                class Test
                {
                    Object run(Object assignments)
                    {
                        return ImmutableSet.copyOf(projectParentColumns(assignments.values().stream()
                                .map(Item.class::cast)
                                .collect(toImmutableList())));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsShallowStaticFactoryArgumentsInsideWrappedConstructorCall()
    {
        String code =
                """
                class Test
                {
                    Object run(Object tableMetadata)
                    {
                        return new Stats(
                                tableMetadata.getColumns().stream()
                                        .filter(column -> !column.isHidden())
                                        .flatMap(column -> Stream.of(
                                                new Entry(column, first()),
                                                new Entry(column, second()),
                                                new Entry(column, third()),
                                                new Entry(column, fourth())))
                                        .collect(toImmutableSet()),
                                Set.of(rowCount()),
                                List.of());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsShallowMapFactoryInsideInlineExpressionLambdaBodyInvocationWithMultilineConstructorValue()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return builder()
                                .withGetViews((_, _) -> ImmutableMap.of(
                                        name("default", "test_view"), new ViewDefinition(
                                                "select",
                                                first(),
                                                second(),
                                                third(),
                                                fourth(),
                                                fifth(),
                                                sixth(),
                                                seventh())))
                                .done();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsShallowBuilderChainInsideWrappedArgumentBeforeTrailingSelectorWithLeadingWrappedSelector()
    {
        String code =
                """
                class Test
                {
                    Object run(Object ticker)
                    {
                        return cachingClientBuilder()
                                .ticker(ticker)
                                .config(new Config()
                                        .setFirst(first())
                                        .setSecond(second())
                                        .setThird(third())
                                        // keep note
                                        .setFourth(fourth()))
                                .build();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesOverIndentedGenericBuilderChainInsideQualifiedOnlyArgumentAfterMultilineQualifiedExpression()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(fromFixedValues(
                                ImmutableMap.<Object, Object>builder()
                                        .put(firstKey(), firstValue())
                                        .put(secondKey(), secondValue())
                                        .put(thirdKey(), thirdValue())
                                        .put(fourthKey(), fourthValue())
                                        .buildOrThrow())).isEqualTo(withColumnDomains(ImmutableMap.<Object, Object>builder()
                                                .put(firstKey(), singleFirst())
                                                .put(secondKey(), singleSecond())
                                                .put(thirdKey(), singleThird())
                                                .put(fourthKey(), onlyNull())
                                                .buildOrThrow()));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(fromFixedValues(
                                ImmutableMap.<Object, Object>builder()
                                        .put(firstKey(), firstValue())
                                        .put(secondKey(), secondValue())
                                        .put(thirdKey(), thirdValue())
                                        .put(fourthKey(), fourthValue())
                                        .buildOrThrow())).isEqualTo(withColumnDomains(ImmutableMap.<Object, Object>builder()
                                .put(firstKey(), singleFirst())
                                .put(secondKey(), singleSecond())
                                .put(thirdKey(), singleThird())
                                .put(fourthKey(), onlyNull())
                                .buildOrThrow()));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedStaticInvocationInsideNestedSelectorLineArgument()
    {
        String oldCode =
                """
                class Test
                {
                    void run(FieldDefinition stateField)
                    {
                        method()
                                .append(estimatedSize.set(
                                BytecodeExpressions.add(
                                        estimatedSize,
                                        method().getField(stateField).invoke("getEstimatedSize", long.class))));
                    }

                    Target method()
                    {
                        return null;
                    }

                    Target estimatedSize;

                    interface Target
                    {
                        Target append(Object value);

                        Object set(Object value);

                        Target getField(Object value);

                        Object invoke(String name, Object type);
                    }

                    interface FieldDefinition {}

                    static class BytecodeExpressions
                    {
                        static Object add(Object left, Object right)
                        {
                            return null;
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(FieldDefinition stateField)
                    {
                        method()
                                .append(estimatedSize.set(
                                        BytecodeExpressions.add(
                                                estimatedSize,
                                                method().getField(stateField).invoke("getEstimatedSize", long.class))));
                    }

                    Target method()
                    {
                        return null;
                    }

                    Target estimatedSize;

                    interface Target
                    {
                        Target append(Object value);

                        Object set(Object value);

                        Target getField(Object value);

                        Object invoke(String name, Object type);
                    }

                    interface FieldDefinition {}

                    static class BytecodeExpressions
                    {
                        static Object add(Object left, Object right)
                        {
                            return null;
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedQualifiedSelectorChainInsideNestedSelectorLineArgument()
    {
        String oldCode =
                """
                class Test
                {
                    void run(Object outputChannel, Object blockIndex)
                    {
                        appendToBody
                                .append(block.set(
                                thisVariable.getField(channelFields().get(outputChannel))
                                        .invoke("get", Object.class, blockIndex)
                                        .cast(Block.class)));
                    }

                    Target appendToBody;
                    Target block;
                    Chain thisVariable;

                    Lookup channelFields()
                    {
                        return null;
                    }

                    interface Target
                    {
                        Target append(Object value);

                        Object set(Object value);
                    }

                    interface Chain
                    {
                        Chain getField(Object value);

                        Chain invoke(String name, Object type, Object value);

                        Object cast(Object type);
                    }

                    interface Lookup
                    {
                        Object get(Object value);
                    }

                    static class Block {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(Object outputChannel, Object blockIndex)
                    {
                        appendToBody
                                .append(block.set(
                                        thisVariable.getField(channelFields().get(outputChannel))
                                                .invoke("get", Object.class, blockIndex)
                                                .cast(Block.class)));
                    }

                    Target appendToBody;
                    Target block;
                    Chain thisVariable;

                    Lookup channelFields()
                    {
                        return null;
                    }

                    interface Target
                    {
                        Target append(Object value);

                        Object set(Object value);
                    }

                    interface Chain
                    {
                        Chain getField(Object value);

                        Chain invoke(String name, Object type, Object value);

                        Object cast(Object type);
                    }

                    interface Lookup
                    {
                        Object get(Object value);
                    }

                    static class Block {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedBlockLambdaBodiesInsideWrappedIfPresentOrElseWithNestedBlocks()
    {
        String oldCode =
                """
                class Test
                {
                    void run(java.util.Optional<String> optional)
                    {
                        optional.ifPresentOrElse(
                                regexPattern -> {
                                check();
                                try {
                                    compile(regexPattern);
                                }
                                catch (RuntimeException e) {
                                    handle(regexPattern);
                                }
                                store(regexPattern);
                            },
                                () -> {
                                if (enabled()) {
                                    fallback();
                                }
                            });
                    }

                    void check() {}

                    void compile(String regexPattern) {}

                    void handle(String regexPattern) {}

                    void store(String regexPattern) {}

                    boolean enabled()
                    {
                        return true;
                    }

                    void fallback() {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(java.util.Optional<String> optional)
                    {
                        optional.ifPresentOrElse(
                                regexPattern -> {
                                    check();
                                    try {
                                        compile(regexPattern);
                                    }
                                    catch (RuntimeException e) {
                                        handle(regexPattern);
                                    }
                                    store(regexPattern);
                                },
                                () -> {
                                    if (enabled()) {
                                        fallback();
                                    }
                                });
                    }

                    void check() {}

                    void compile(String regexPattern) {}

                    void handle(String regexPattern) {}

                    void store(String regexPattern) {}

                    boolean enabled()
                    {
                        return true;
                    }

                    void fallback() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedFieldAccessSelectorChainInsideNestedSelectorLineArgument()
    {
        String oldCode =
                """
                class Test
                {
                    void run(Object outputChannel, Object blockIndex)
                    {
                        appendToBody.append(block.set(
                        thisVariable.getField(channelFields.get(outputChannel))
                                .invoke("get", Object.class, blockIndex)
                                .cast(Block.class)));
                    }

                    Target appendToBody;
                    Target block;
                    Chain thisVariable;
                    Lookup channelFields;

                    interface Target
                    {
                        Object append(Object value);

                        Object set(Object value);
                    }

                    interface Chain
                    {
                        Chain getField(Object value);

                        Chain invoke(String name, Object type, Object value);

                        Object cast(Object type);
                    }

                    interface Lookup
                    {
                        Object get(Object value);
                    }

                    static class Block {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(Object outputChannel, Object blockIndex)
                    {
                        appendToBody.append(block.set(
                                thisVariable.getField(channelFields.get(outputChannel))
                                        .invoke("get", Object.class, blockIndex)
                                        .cast(Block.class)));
                    }

                    Target appendToBody;
                    Target block;
                    Chain thisVariable;
                    Lookup channelFields;

                    interface Target
                    {
                        Object append(Object value);

                        Object set(Object value);
                    }

                    interface Chain
                    {
                        Chain getField(Object value);

                        Chain invoke(String name, Object type, Object value);

                        Object cast(Object type);
                    }

                    interface Lookup
                    {
                        Object get(Object value);
                    }

                    static class Block {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedMatcherChainWithLambdaInsideWrappedSelectorArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return exchange()
                                .with(source().matching(
                                topN().matching(value -> value != null)
                                        .with(child().matching(other -> other != null))
                                        .capturedAs(capture)));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return exchange()
                                .with(source().matching(
                                        topN().matching(value -> value != null)
                                                .with(child().matching(other -> other != null))
                                                .capturedAs(capture)));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedOnlyArgumentInvocationInSelectorLineCallWithTrailingSelector()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        body.append(
                                generateLambda(
                                                generatorContext,
                                                java.util.List.of(),
                                                compiledLambda,
                                                lambdaInterface))
                                .retObject();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        body.append(
                                        generateLambda(
                                                generatorContext,
                                                java.util.List.of(),
                                                compiledLambda,
                                                lambdaInterface))
                                .retObject();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedExpressionLambdaInsideOnlyArgumentInvocationBeforeTrailingSelector()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object value)
                    {
                        return verify(
                                        () -> factory(value)
                                                .create())
                                .check();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object value)
                    {
                        return verify(
                                () -> factory(value)
                                        .create())
                                .check();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedNestedPlainInvocationInsideOnlyArgumentBeforeWrappedTrailingSelector()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        target()
                                .planPattern(
                                        any(
                                                        node(
                                                                left(),
                                                                right())))
                                .children(done());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        target()
                                .planPattern(
                                        any(
                                                node(
                                                        left(),
                                                        right())))
                                .children(done());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedMatcherChainInsideNestedWrappedSelectorArgumentAfterCapturedAs()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return root()
                                .with(project()
                                        .capturedAs(value)
                                        .with(source().matching(node()
                                        .matching(Test::matches)
                                        .capturedAs(capture))));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return root()
                                .with(project()
                                        .capturedAs(value)
                                        .with(source().matching(node()
                                                .matching(Test::matches)
                                                .capturedAs(capture))));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedQualifiedBuilderInvocationInsideOnlyArgumentBeforeWrappedTrailingSelector()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return schema()
                                .mapDefault(ImmutableMap.<String, Integer>builder()
                                                .put("one", 1)
                                                .put("two", 2)
                                                .buildOrThrow())
                                .name("value");
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return schema()
                                .mapDefault(ImmutableMap.<String, Integer>builder()
                                        .put("one", 1)
                                        .put("two", 2)
                                        .buildOrThrow())
                                .name("value");
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedBuilderChainInsideQualifiedWrapperInvocation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return java.util.Optional.of(Helpers.combine(
                        Builders.<Object>builder()
                                .addAll(values)
                                .add(Helpers.or(otherValues))
                                .build()));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return java.util.Optional.of(Helpers.combine(
                                Builders.<Object>builder()
                                        .addAll(values)
                                        .add(Helpers.or(otherValues))
                                        .build()));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedNestedInvocationInsideQualifiedWrapperInvocation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return Result.ofPlanNode(node.replaceChildren(
                        java.util.List.of(new Item(
                                first(),
                                second()))));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return Result.ofPlanNode(node.replaceChildren(
                                java.util.List.of(new Item(
                                        first(),
                                        second()))));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedNestedInvocationInsideOnlyArgumentOfWrappedParentInvocation()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return child.replaceChildren(Lists.of(
                        parent.replaceChildren(
                                child.getSources())));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return child.replaceChildren(Lists.of(
                                parent.replaceChildren(
                                        child.getSources())));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedNestedInvocationArgumentsInsideOnlyArgumentOfPlainInvocationWithTrailingSelector()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return assertThat(estimator.getNextRetryMemoryRequirements(
                                        new Requirement(first()),
                                        second(),
                                        third(),
                                        fourth()))
                                .isEqualTo(value());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return assertThat(estimator.getNextRetryMemoryRequirements(
                                new Requirement(first()),
                                second(),
                                third(),
                                fourth()))
                                .isEqualTo(value());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedQualifiedInvocationArgumentsInsideOnlyArgumentOfPlainInvocationWithTrailingSelector()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return getOnlyElement(processor.process(
                                        first(),
                                        second(),
                                        third()))
                                .orElseThrow();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return getOnlyElement(processor.process(
                                first(),
                                second(),
                                third()))
                                .orElseThrow();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedQualifiedOnlyArgumentInvocationWithTrailingSelectorWhenWrappedFromParent()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return getOnlyElement(
                                        processor.process(
                                                first(),
                                                second(),
                                                third()))
                                .orElseThrow();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return getOnlyElement(
                                processor.process(
                                        first(),
                                        second(),
                                        third()))
                                .orElseThrow();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedNestedMultiArgumentInvocationInsideSingleArgumentAssertionCall()
    {
        String oldCode =
                """
                class Test
                {
                    void run(Object values)
                    {
                        assertThat(values).isEqualTo(ImmutableList.of(
                        Result.withItems(ImmutableMap.of(
                                key("a"), value(1))),
                        Result.withItems(ImmutableMap.of(
                                key("b"), value(2)))));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(Object values)
                    {
                        assertThat(values).isEqualTo(ImmutableList.of(
                                Result.withItems(ImmutableMap.of(
                                        key("a"), value(1))),
                                Result.withItems(ImmutableMap.of(
                                        key("b"), value(2)))));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedQualifiedOnlyArgumentInvocationWithoutTrailingSelector()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        body.append(self.setField(
                        descriptor.field(),
                        loadConstant(binder, descriptor.value(), Object.class)));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        body.append(self.setField(
                                descriptor.field(),
                                loadConstant(binder, descriptor.value(), Object.class)));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedQualifiedBuilderInvocationInsideSingleArgumentSelectorLineCall()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        Loader.fetch(Request.builder()
                                .state("ready")
                                .build())
                                .apply(result -> result);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        Loader.fetch(Request.builder()
                                        .state("ready")
                                        .build())
                                .apply(result -> result);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedCastQualifiedInvocationInsideQualifiedOnlyArgumentAfterMultilineQualifiedExpression()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        target()
                                .toBuilder()
                                .setValue(mapper.writeValueAsString(
                                                ((Node) source.copy())
                                                        .merge(details)))
                                .build();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        target()
                                .toBuilder()
                                .setValue(mapper.writeValueAsString(
                                        ((Node) source.copy())
                                                .merge(details)))
                                .build();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedSingleArgumentInvocationInsideInlineSelectorLineBuilderCall()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(query())
                                .matches(results(
                                                column("x", type(
                                                        left(),
                                                        right())))
                                        .addRow(value())
                                        .build());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(query())
                                .matches(results(
                                        column("x", type(
                                                left(),
                                                right())))
                                        .addRow(value())
                                        .build());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedExpressionLambdaInsideWrappedFilterCall()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return values.stream()
                                .filter(
                                                value ->
                                                value.isReady()
                                                        && value.isHealthy()
                                                        && value.isEnabled())
                                .toList();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return values.stream()
                                .filter(
                                        value ->
                                                value.isReady()
                                                        && value.isHealthy()
                                                        && value.isEnabled())
                                .toList();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedQualifiedBuilderInvocationInsideSingleArgumentSelectorLineCallWithBlockLambda()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        var value = Loader.fetch(Request.builder()
                                .state("ready")
                                .build())
                                .apply(result -> {
                                    return result;
                                });
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        var value = Loader.fetch(Request.builder()
                                        .state("ready")
                                        .build())
                                .apply(result -> {
                                    return result;
                                });
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedQualifiedBuilderInvocationInsideOnlyArgumentWithInlineTrailingSelector()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        client.describe(Request.builder()
                                        .key(KEY)
                                        .build()).value();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        client.describe(Request.builder()
                                .key(KEY)
                                .build()).value();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedQualifiedBuilderInvocationInsideWrappedStreamArgumentChain()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return StreamSupport.stream(
                                        client.list(Request.builder()
                                                .setFilter(join(
                                                        values.stream()
                                                                .map(value -> value)
                                                                .toList()))
                                                .build())
                                                .iterateAll()
                                                .spliterator(),
                                        false)
                                .collect(done());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return StreamSupport.stream(
                                        client.list(Request.builder()
                                                        .setFilter(join(
                                                                values.stream()
                                                                        .map(value -> value)
                                                                        .toList()))
                                                        .build())
                                                .iterateAll()
                                                .spliterator(),
                                        false)
                                .collect(done());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedQualifiedBuilderInvocationInsideWrappedStreamArgumentChainWithQualifiedJoin()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return StreamSupport.stream(
                                        client.list(Request.builder()
                                                .setFilter(Joiner.on(" ").join(
                                                        values.stream()
                                                                .map(value -> value)
                                                                .toList()))
                                                .build())
                                                .iterateAll()
                                                .spliterator(),
                                        false)
                                .collect(done());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return StreamSupport.stream(
                                        client.list(Request.builder()
                                                        .setFilter(Joiner.on(" ").join(
                                                                values.stream()
                                                                        .map(value -> value)
                                                                        .toList()))
                                                        .build())
                                                .iterateAll()
                                                .spliterator(),
                                        false)
                                .collect(done());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedSelectorLineNestedInvocationInsideWrappedSecondArgument()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        JsonNode node = mapper.createObjectNode()
                                .set("groups", mapper.createArrayNode()
                                        .add(mapper.createObjectNode()
                                                .set("members", mapper.createArrayNode()
                                                        .add(mapper.createObjectNode().put("id", "u1"))
                                                        .add(mapper.createObjectNode().put("id", "u2"))))
                                        .add(mapper.createObjectNode()
                                                .set("members", mapper.createArrayNode()
                                                                .add(mapper.createObjectNode().put("id", "u3")))));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        JsonNode node = mapper.createObjectNode()
                                .set("groups", mapper.createArrayNode()
                                        .add(mapper.createObjectNode()
                                                .set("members", mapper.createArrayNode()
                                                        .add(mapper.createObjectNode().put("id", "u1"))
                                                        .add(mapper.createObjectNode().put("id", "u2"))))
                                        .add(mapper.createObjectNode()
                                                .set("members", mapper.createArrayNode()
                                                        .add(mapper.createObjectNode().put("id", "u3")))));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedClassInstanceBackedSelectorChainInOnlyArgumentOfPlainInvocation()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        target(new BlockBuilder()
                                        .append(value()));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        target(new BlockBuilder()
                                .append(value()));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedMatcherChainInsideInlinePrefixedWrappedSelectorArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return root()
                                .with(subquery().matching(project()
                                        .capturedAs(value)
                                        .with(source().matching(node()
                                        .matching(Test::matches)
                                        .capturedAs(capture)))));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return root()
                                .with(subquery().matching(project()
                                        .capturedAs(value)
                                        .with(source().matching(node()
                                                .matching(Test::matches)
                                                .capturedAs(capture)))));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedBuilderChainInsideInlinePrefixedWrappedSelectorArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return schema().fields().name("map").type().map().values().intType().withDefault(new Builder()
                                        .put("one", 1)
                                        .put("two", 2)
                                        .build())
                                .name("next");
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return schema().fields().name("map").type().map().values().intType().withDefault(new Builder()
                                        .put("one", 1)
                                        .put("two", 2)
                                        .build())
                                .name("next");
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedNestedSecondArgumentInvocationInWrappedCall()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        target(
                                node(Object.class,
                                exchange(value())));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        target(
                                node(Object.class,
                                        exchange(value())));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedSelectorChainInsideOnlyArgumentInvocationUsedAsFirstWrappedArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return call(
                                identity(
                                values.stream()
                                        .filter(Test::matches)
                                        .toList()),
                                other());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return call(
                                identity(
                                        values.stream()
                                                .filter(Test::matches)
                                                .toList()),
                                other());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedQualifiedBuilderChainInsideInlinePrefixedWrappedSelectorArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return schema().fields().name("map").type().map().values().intType().withDefault(Builders.create()
                                        .put("one", 1)
                                        .put("two", 2)
                                        .build())
                                .name("next");
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return schema().fields().name("map").type().map().values().intType().withDefault(Builders.create()
                                        .put("one", 1)
                                        .put("two", 2)
                                        .build())
                                .name("next");
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedAggregationMatcherChainInsideInlinePrefixedWrappedSelectorArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return root()
                                .with(subquery().matching(project()
                                        .capturedAs(value)
                                        .with(source().matching(aggregation()
                                        .matching(Test::matches)
                                        .capturedAs(capture)))));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return root()
                                .with(subquery().matching(project()
                                        .capturedAs(value)
                                        .with(source().matching(aggregation()
                                                .matching(Test::matches)
                                                .capturedAs(capture)))));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedNestedSecondArgumentInvocationInsideSingleArgumentCallWithTrailingSelector()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return target()
                                .planPattern(
                                        node(Object.class,
                                        exchange(value())))
                                .children(done());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return target()
                                .planPattern(
                                        node(Object.class,
                                                exchange(value())))
                                .children(done());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedGenericBuilderChainInsideInlinePrefixedWrappedSelectorArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return schema().fields().name("map").type().map().values().intType().mapDefault(ImmutableMap.<String, Integer>builder()
                                        .put("one", 1)
                                        .put("two", 2)
                                        .buildOrThrow())
                                .name("next");
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return schema().fields().name("map").type().map().values().intType().mapDefault(ImmutableMap.<String, Integer>builder()
                                        .put("one", 1)
                                        .put("two", 2)
                                        .buildOrThrow())
                                .name("next");
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedSimpleBytecodeBlockSelectorChainInsideNestedWrappedInvocationArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return block().append(new Loop()
                                        .initialize(start())
                                        .condition(check())
                                        .update(next())
                                        .body(new BytecodeBlock()
                                                        .append(apply(value))))
                                .done();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return block().append(new Loop()
                                        .initialize(start())
                                        .condition(check())
                                        .update(next())
                                        .body(new BytecodeBlock()
                                                .append(apply(value))))
                                .done();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedAggregationMatcherChainInFieldInitializer()
    {
        String oldCode =
                """
                class Test
                {
                    Object pattern = root()
                            .with(subquery().matching(project()
                                    .capturedAs(value)
                                    .with(source().matching(aggregation()
                                    .matching(Test::matches)
                                    .capturedAs(capture)))));
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object pattern = root()
                            .with(subquery().matching(project()
                                    .capturedAs(value)
                                    .with(source().matching(aggregation()
                                            .matching(Test::matches)
                                            .capturedAs(capture)))));
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedVariablesMatcherChainInsideWrappedMatchingArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return root()
                                .with(arguments().matching(
                                variables()
                                        .matching(typeRule())
                                        .capturedAs(capture)));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return root()
                                .with(arguments().matching(
                                        variables()
                                                .matching(typeRule())
                                                .capturedAs(capture)));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedBytecodeBlockSelectorChainInsideNestedWrappedInvocationArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return loop()
                                .body(new Block()
                                                .append(value.call("evaluate")));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return loop()
                                .body(new Block()
                                        .append(value.call("evaluate")));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedBlockSelectorChainInsideNestedSelectorLineArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return target()
                                .append(new Loop()
                                        .first(value())
                                        .body(new Block()
                                                        .append(call())));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return target()
                                .append(new Loop()
                                        .first(value())
                                        .body(new Block()
                                                .append(call())));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedAggregationMatcherChainInsideWrappedMatchingArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return pattern()
                                .with(source().matching(aggregation()
                                .matching(Test::rule)
                                .capturedAs(capture)));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return pattern()
                                .with(source().matching(aggregation()
                                        .matching(Test::rule)
                                        .capturedAs(capture)));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedVariablesMatcherChainInsideSelectorLineMatchingArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return pattern()
                                .with(arguments().matching(
                                variables()
                                        .matching(types())
                                        .capturedAs(capture)));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return pattern()
                                .with(arguments().matching(
                                        variables()
                                                .matching(types())
                                                .capturedAs(capture)));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedProjectIdentityStreamInsideWrappedArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return project(
                                identity(
                                outputs.stream()
                                        .filter(Test::keep)
                                        .toList()),
                                window());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return project(
                                identity(
                                        outputs.stream()
                                                .filter(Test::keep)
                                                .toList()),
                                window());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedProjectIdentityNestedInvocationStreamInsideWrappedArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return project(
                                identity(
                                ImmutableList.of(first(), second()).stream()
                                        .filter(Test::keep)
                                        .toList()),
                                window());
                    }

                    static boolean keep(Object value)
                    {
                        return true;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return project(
                                identity(
                                        ImmutableList.of(first(), second()).stream()
                                                .filter(Test::keep)
                                                .toList()),
                                window());
                    }

                    static boolean keep(Object value)
                    {
                        return true;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedAdaptivePlanInsideLambdaWrappedCall()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return matcher()
                                .fragmentMatcher(fm -> fm
                                        .planPattern(
                                                any(
                                                                adaptivePlan(
                                                                        join(INNER, builder -> builder
                                                                                .left(left())
                                                                                .right(right())),
                                                                        join(INNER, builder -> builder
                                                                                .right(left())
                                                                                .left(right()))))));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return matcher()
                                .fragmentMatcher(fm -> fm
                                        .planPattern(
                                                any(
                                                        adaptivePlan(
                                                                join(INNER, builder -> builder
                                                                        .left(left())
                                                                        .right(right())),
                                                                join(INNER, builder -> builder
                                                                        .right(left())
                                                                        .left(right()))))));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsSingleContinuationLambdaBuilderChainInsideFragmentMatcherBeforeSiblingSelector()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return matcher()
                                .fragmentMatcher(fm -> fm
                                        .fragmentId(0)
                                        .planPattern(
                                                output(
                                                        node(Object.class,
                                                                exchange(
                                                                        remoteSource(values()))))))
                                .children(
                                        child -> child
                                                .fragmentMatcher(other -> other
                                                        .planPattern(
                                                                node(Object.class))));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsSingleContinuationLambdaBuilderChainInsideWrappedFragmentMatcherArgument()
    {
        String code =
                """
                class Test
                {
                    Object run(Object session)
                    {
                        return assertPlan(
                                session,
                                first(),
                                second(),
                                builder()
                                        .fragmentMatcher(fm -> fm
                                                .fragmentId(0)
                                                .planPattern(
                                                        output(
                                                                node(Object.class,
                                                                        exchange(
                                                                                remoteSource(values()))))))
                                        .children(
                                                child -> child
                                                        .fragmentMatcher(other -> other
                                                                .planPattern(
                                                                        node(Object.class))))
                                        .build(),
                                true);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsShallowLambdaReceiverChainBeforeWrappedSelectorInsideWrappedArgument()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return verify(
                                first(),
                                matcher()
                                        .fragmentMatcher(fm -> fm
                                                .fragmentId(0)
                                                .planPattern(
                                                        output(
                                                                node(Object.class,
                                                                        exchange(
                                                                                remoteSource(values()))))))
                                        .children(
                                                child -> child
                                                        .fragmentMatcher(other -> other
                                                                .planPattern(
                                                                        node(Object.class)))));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesUnderIndentedNodeExchangeInsideNestedLambdaMatcher()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return matcher()
                                .fragmentMatcher(fm -> fm
                                                .planPattern(
                                                        output(
                                                                        node(Object.class,
                                                                        exchange(
                                                                                remoteSource(value()))))));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return matcher()
                                .fragmentMatcher(fm -> fm
                                        .planPattern(
                                                output(
                                                        node(Object.class,
                                                                exchange(
                                                                        remoteSource(value()))))));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedNestedSecondArgumentInvocationInsideLambdaWrappedCall()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return matcher()
                                .fragmentMatcher(fm -> fm
                                        .planPattern(node(Object.class,
                                        adaptivePlan(
                                                join(INNER, builder -> builder
                                                        .left(left())
                                                        .right(right())),
                                                join(INNER, builder -> builder
                                                        .right(left())
                                                        .left(right()))))))
                                .children(
                                        child -> child
                                                .fragmentMatcher(fm -> fm
                                                        .planPattern(
                                                                node(Object.class,
                                                                exchange(
                                                                        remoteSource(value()))))));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return matcher()
                                .fragmentMatcher(fm -> fm
                                        .planPattern(node(Object.class,
                                                adaptivePlan(
                                                        join(INNER, builder -> builder
                                                                .left(left())
                                                                .right(right())),
                                                        join(INNER, builder -> builder
                                                                .right(left())
                                                                .left(right()))))))
                                .children(
                                        child -> child
                                                .fragmentMatcher(fm -> fm
                                                        .planPattern(
                                                                node(Object.class,
                                                                        exchange(
                                                                                remoteSource(value()))))));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedBuilderChainInsideSelectorLineMapDefaultArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return schema()
                                .mapDefault(Map.<String, Integer>builder()
                                                .put("one", 1)
                                                .put("two", 2)
                                                .build());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return schema()
                                .mapDefault(Map.<String, Integer>builder()
                                        .put("one", 1)
                                        .put("two", 2)
                                        .build());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedOnlyArgumentInvocationInsideAnyTree()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return root()
                                .right(
                                        anyTree(
                                                        assignUniqueId(
                                                                "source",
                                                                tableScan("target"))));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return root()
                                .right(
                                        anyTree(
                                                assignUniqueId(
                                                        "source",
                                                        tableScan("target"))));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedInlinePrefixedBuilderChainInsideWrappedArgumentWithInlineArguments()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return assertThat(query())
                                .result().matches(resultBuilder(session(), first(), second())
                                .row(third())
                                .row(fourth())
                                .build());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return assertThat(query())
                                .result().matches(resultBuilder(session(), first(), second())
                                        .row(third())
                                        .row(fourth())
                                        .build());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedClassInstanceBuilderChainInsideWrappedArgumentAfterPriorSelectors()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return request()
                                .url(url())
                                .addHeader(name(), value())
                                .post(new FormBody.Builder()
                                .add("grant_type", grantType())
                                .add("audience", audience())
                                .build())
                                .build();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return request()
                                .url(url())
                                .addHeader(name(), value())
                                .post(new FormBody.Builder()
                                        .add("grant_type", grantType())
                                        .add("audience", audience())
                                        .build())
                                .build();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedNestedOnlyArgumentInvocationInsideInlineFirstArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return outer(inner(
                                nested(
                                        first(),
                                        second())),
                                third(),
                                fourth());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return outer(inner(
                                        nested(
                                                first(),
                                                second())),
                                third(),
                                fourth());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsInlinePrefixedNestedFirstArgumentInsideWrappedMultiArgumentInvocation()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return accept(
                                outer(inner(
                                                value()),
                                        other()));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsInlinePrefixedComparisonArgumentInsideWrappedMatcherInvocation()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return output(
                                filter(new Comparison(EQUAL, left(), right()),
                                        values()));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedFirstArgumentInsideOnlyArgumentParentInvocation()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return anyTree(
                                node(
                                        Type.class,
                                        left(),
                                        right()));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedSingleArgumentNestedInvocationInsideOnlyArgumentParentInvocation()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return builder()
                                .left(
                                        anyTree(
                                                tableScan(
                                                        source())))
                                .done();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedSingleArgumentNestedInvocationWithTrailingSelectorInsideOnlyArgumentParentInvocation()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return builder()
                                .left(
                                        anyTree(
                                                tableScan(source())
                                                        .withAlias(name(), column())))
                                .done();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedSingleArgumentNestedInvocationInsideBuilderLambdaSelectorChain()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return anyTree(
                                join(INNER, builder -> builder
                                        .equiCriteria(left(), right())
                                        .left(
                                                anyTree(
                                                        tableScan(source()).withAlias(name(), column())))
                                        .right(
                                                anyTree(
                                                        tableScan(other()).withAlias(alias(), field())))));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsInlinePrefixedMultilineFirstArgumentInsideOnlyArgumentParentInvocation()
    {
        String code =
                """
                import java.util.Map;

                class Test
                {
                    Object run()
                    {
                        return builder()
                                .left(project(Map.of(
                                                "first", left(),
                                                "second", right()),
                                        source()))
                                .done();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsInlinePrefixedMultilineFirstArgumentInsideWrappedMultiArgumentInvocation()
    {
        String code =
                """
                import java.util.Map;

                class Test
                {
                    Object run()
                    {
                        return project(Map.of(
                                        "first", left(),
                                        "second", right()),
                                source());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsLeadingInlineArgumentsWhenOnlyLastArgumentIsMultiline()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return exchange(REMOTE, empty(),
                                node(
                                        left(),
                                        right()));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsSimpleFirstArgumentInlineBeforeMultipleMultilineArguments()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return join(condition(),
                                left(
                                        first(),
                                        second()),
                                right(
                                        third(),
                                        fourth()));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsInlinePrefixedMultilineFirstArgumentInsideOnlyArgumentParentInvocationBeforeMultilineSecondArgument()
    {
        String code =
                """
                import java.util.Map;

                class Test
                {
                    Object run()
                    {
                        return builder()
                                .left(project(Map.of(
                                                "first", left(),
                                                "second", right()),
                                        assign(
                                                third(),
                                                fourth())))
                                .done();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsLongSingleLineFirstArgumentInlineBeforeMultipleMultilineArguments()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return spatialJoin(new Call(CONTAINS, ImmutableList.of(left(), right())),
                                project(
                                        first(),
                                        second()),
                                project(
                                        third(),
                                        fourth()));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsLeadingInlineArgumentsInsideOnlyArgumentParentWhenOnlyLastArgumentIsMultiline()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return builder()
                                .left(exchange(REMOTE, empty(),
                                        node(
                                                left(),
                                                right())))
                                .done();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedLeadingArgumentsInsideOnlyArgumentOfPlainParentInvocation()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return anyTree(
                                match(first(),
                                        second(),
                                        third(),
                                        source(),
                                        node(
                                                left(),
                                                right())));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsSingleLineProjectArgumentsInsideWrappedMatchesCall()
    {
        String code =
                """
                import java.util.Map;

                class Test
                {
                    Object run()
                    {
                        return tester()
                                .matches(
                                        project(Map.of("output", expression(value())),
                                                values()));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedLeadingArgumentsInsideTopNMatcherCall()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return tester()
                                .matches(
                                        topN(1,
                                                sortKeys(),
                                                step(),
                                                tableScan(
                                                        first(),
                                                        second(),
                                                        third())));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedLeadingArgumentsInsideSemiJoinMatcherCall()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return tester()
                                .matches(
                                        semiJoin("leftKey",
                                                "rightKey",
                                                "match",
                                                values("leftKey"),
                                                strictProject(
                                                        projection(),
                                                        values("rightKey", "rightValue"))));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedProjectInsideBuilderLambdaSelectorChain()
    {
        String code =
                """
                import java.util.Map;

                class Test
                {
                    Object run()
                    {
                        return anyTree(
                                join(INNER, builder -> builder
                                        .equiCriteria(left(), right())
                                        .left(
                                                project(
                                                        Map.of(
                                                                "output", expression(value())),
                                                        values()))
                                        .right(
                                                anyTree(
                                                        tableScan(source())))));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedFilterInsideBuilderLambdaSelectorChain()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return anyTree(
                                join(INNER, builder -> builder
                                        .equiCriteria(left(), right())
                                        .left(
                                                filter(
                                                        compare(left(), right()),
                                                        tableScan(
                                                                source(),
                                                                column())))
                                        .right(
                                                anyTree(
                                                        tableScan(other())))));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedProjectInsideMatchesCall()
    {
        String code =
                """
                import java.util.Map;

                class Test
                {
                    Object run()
                    {
                        return tester()
                                .matches(
                                        project(
                                                Map.of("projectedA", expression(a()), "projectedB", expression(b())),
                                                limit(1, orderBy(), values("a", "b"))));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedFilterInsideMatchesCall()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return tester()
                                .matches(
                                        filter(
                                                condition(),
                                                values("a")));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsLeadingInlineArgumentsInsideNestedExchangeBeforeMultilineLastArgument()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return anyTree(
                                join(INNER, builder -> builder
                                        .left(exchange(REMOTE, REPARTITION, optional(),
                                                node(
                                                        left(),
                                                        right())))
                                        .right(
                                                anyTree(
                                                        tableScan(source())))));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsQualifiedIdentityStreamInsideWrappedFirstArgumentBeforeQualifiedSecondArgument()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return p.project(
                                Assignments.identity(
                                        outputs.stream()
                                                .filter(Test::keep)
                                                .toList()),
                                p.window());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsQualifiedBuilderChainInsideStreamMapLambdaWithQualifiedOuterBuilder()
    {
        String code =
                """
                class Test
                {
                    Object run(Object table)
                    {
                        return openLineage.newSchemaDatasetFacetBuilder()
                                .fields(
                                        table
                                                .getColumns()
                                                .stream()
                                                .map(field -> openLineage.newSchemaDatasetFacetFieldsBuilder()
                                                        .name(field.getColumn())
                                                        .build()).toList())
                                .build();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesOverIndentedWrappedFollowupArgumentsAfterInlineFirstArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return planner()
                                .registerRules(firstRule(),
                                                secondRule(),
                                                thirdRule())
                                .done();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return planner()
                                .registerRules(firstRule(),
                                        secondRule(),
                                        thirdRule())
                                .done();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedLongInlinePrefixedBuilderChainInsideWrappedArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return assertThat(query())
                                .result().matches(resultBuilder(TEST_SESSION, VARCHAR, VARCHAR, VARCHAR, VARCHAR)
                                .row("regionkey", "bigint", "", "")
                                .row("name", "varchar(25)", "", "")
                                .row("comment", "varchar(152)", "", "")
                                .build());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return assertThat(query())
                                .result().matches(resultBuilder(TEST_SESSION, VARCHAR, VARCHAR, VARCHAR, VARCHAR)
                                        .row("regionkey", "bigint", "", "")
                                        .row("name", "varchar(25)", "", "")
                                        .row("comment", "varchar(152)", "", "")
                                        .build());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedNestedBuilderChainInsideWrappedArgumentOfNestedBuilderChain()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return client()
                                .newCall(
                                        new Request.Builder()
                                                .url("https://localhost:" + getPort() + "/oauth2/token")
                                                .addHeader(name(), Credentials.basic(clientId, clientSecret))
                                                .post(new FormBody.Builder()
                                                .add("grant_type", grantType())
                                                .add("audience", audience())
                                                .build())
                                                .build())
                                .execute();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return client()
                                .newCall(
                                        new Request.Builder()
                                                .url("https://localhost:" + getPort() + "/oauth2/token")
                                                .addHeader(name(), Credentials.basic(clientId, clientSecret))
                                                .post(new FormBody.Builder()
                                                        .add("grant_type", grantType())
                                                        .add("audience", audience())
                                                        .build())
                                                .build())
                                .execute();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedStreamChainInsideFirstWrappedArgumentOfTwoArgumentCall()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return plan(
                                identity(
                                ImmutableList.of(leftKey, leftValue, rightKey, rightValue).stream()
                                        .filter(projectionFilter)
                                        .collect(toImmutableList())),
                                join());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return plan(
                                identity(
                                        ImmutableList.of(leftKey, leftValue, rightKey, rightValue).stream()
                                                .filter(projectionFilter)
                                                .collect(toImmutableList())),
                                join());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedTrailingArgumentsAfterInlineFirstArgumentInWrappedShortSelectorCall()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return planner()
                                .add(first(),
                                                second(),
                                                third())
                                .done();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return planner()
                                .add(first(),
                                        second(),
                                        third())
                                .done();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedQualifiedStreamChainInsideWrappedFirstArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return project(
                                Assignments.identity(
                                ImmutableList.of(first(), second()).stream()
                                        .filter(this::keep)
                                        .toList()),
                                source());
                    }

                    boolean keep(Object value)
                    {
                        return true;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return project(
                                Assignments.identity(
                                        ImmutableList.of(first(), second()).stream()
                                                .filter(this::keep)
                                                .toList()),
                                source());
                    }

                    boolean keep(Object value)
                    {
                        return true;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedNestedPatternChainInsideWrappedMatchingCall()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return root().with(source().matching(
                        writer().captured().with(source().matching(
                        project().captured().with(source().matching(
                                scan()))))));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return root().with(source().matching(
                                writer().captured().with(source().matching(
                                        project().captured().with(source().matching(
                                                scan()))))));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedInlinePrefixedBuilderChainInsideProjectedMatchesCall()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return query()
                                .projected(name())
                                .matches(resultBuilder(session(), type())
                                                .row("x")
                                                .build());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return query()
                                .projected(name())
                                .matches(resultBuilder(session(), type())
                                        .row("x")
                                        .build());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedInlinePrefixedBuilderChainInsideContainsAllCall()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return query()
                                .containsAll(resultBuilder(session())
                                                .row("x")
                                                .build());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return query()
                                .containsAll(resultBuilder(session())
                                        .row("x")
                                        .build());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedBuilderChainInsideLambdaMapCall()
    {
        String oldCode =
                """
                import java.util.List;

                class Test
                {
                    Object run(List<Object> values)
                    {
                        return values.stream()
                                .map(value -> builder()
                                .name(value)
                                .build())
                                .toList();
                    }
                }
                """;

        String newCode =
                """
                import java.util.List;

                class Test
                {
                    Object run(List<Object> values)
                    {
                        return values.stream()
                                .map(value -> builder()
                                        .name(value)
                                        .build())
                                .toList();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedTrailingArgumentsAfterInlineFirstArgumentInWrappedInlinePrefixedSelectorCall()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return planner()
                                .seed(value())
                                .add(first(),
                                                second(),
                                                third())
                                .done();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return planner()
                                .seed(value())
                                .add(first(),
                                        second(),
                                        third())
                                .done();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedTrailingArgumentsAfterWrappedFirstArgumentInBuilderAddCall()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return command(ImmutableList.<String>builder()
                                .add(
                                                javaHome() + "/bin/java",
                                                "-Xmx1g",
                                                // logging
                                                "-DProgressLoggingListener.enabled=false")
                                .addAll(options())
                                .build());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return command(ImmutableList.<String>builder()
                                .add(
                                        javaHome() + "/bin/java",
                                        "-Xmx1g",
                                        // logging
                                        "-DProgressLoggingListener.enabled=false")
                                .addAll(options())
                                .build());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedTrailingArgumentsAfterInlineFirstArgumentInBuilderAddCallNestedInConstructorArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return new Optimizer(
                                plannerContext,
                                ruleStats,
                                ImmutableSet.<Rule<?>>builder()
                                        .addAll(first().rules())
                                        .add(new PushPartialAggregationThroughExchange(plannerContext),
                                                        new PruneJoinColumns(),
                                                        new PruneJoinChildrenColumns(),
                                                        new RemoveRedundantIdentityProjections())
                                        .build());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return new Optimizer(
                                plannerContext,
                                ruleStats,
                                ImmutableSet.<Rule<?>>builder()
                                        .addAll(first().rules())
                                        .add(new PushPartialAggregationThroughExchange(plannerContext),
                                                new PruneJoinColumns(),
                                                new PruneJoinChildrenColumns(),
                                                new RemoveRedundantIdentityProjections())
                                        .build());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsWrappedBuilderAddCallInsideWithCommandArgument()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return root().withCommand(ImmutableList.<String>builder()
                                .add(
                                        javaHome() + "/bin/java",
                                        "-Xmx1g",
                                        // logging
                                        "-DProgressLoggingListener.enabled=false")
                                .addAll(options())
                                .add(
                                        "-jar",
                                        "/docker/test.jar",
                                        "--config",
                                        String.join(",", ImmutableList.<String>builder()
                                                .add("tempto-configuration.yaml")
                                                .add(config())
                                                .build()))
                                .build().toArray(new String[0]));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedBuilderAddCallInsideWithCommandAfterPriorSelectors()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return root()
                                .copy(first())
                                .copy(second())
                                .withCommand(ImmutableList.<String>builder()
                                        .add(
                                                javaHome() + "/bin/java",
                                                "-Xmx1g",
                                                "-DProgressLoggingListener.enabled=false")
                                        .addAll(options())
                                        .add(
                                                "-jar",
                                                "/docker/test.jar",
                                                "--config",
                                                String.join(",", ImmutableList.<String>builder()
                                                        .add("tempto-configuration.yaml")
                                                        .add(config())
                                                        .build()))
                                        .build().toArray(new String[0]));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedBuilderAddCallInsideWithCommandArgumentAfterPriorSelectorLines()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return container()
                                .withFile(first())
                                .withFile(second())
                                .withCommand(ImmutableList.<String>builder()
                                        .add(
                                                javaHome() + "/bin/java",
                                                "-Xmx1g",
                                                // logging
                                                "-DProgressLoggingListener.enabled=false")
                                        .addAll(options())
                                        .add(
                                                "-jar",
                                                "/docker/test.jar",
                                                "--config",
                                                String.join(",", ImmutableList.<String>builder()
                                                        .add(alpha())
                                                        .add(beta())
                                                        .build()))
                                        .addAll(arguments())
                                        .build().toArray(new String[0]));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesUnderIndentedQualifiedStreamChainInsideWrappedFirstArgumentOfSelectorCall()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return root()
                                .project(
                                        Assignments.identity(
                                        ImmutableList.of(first(), second()).stream()
                                                .filter(this::keep)
                                                .toList()),
                                        source());
                    }

                    boolean keep(Object value)
                    {
                        return true;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return root()
                                .project(
                                        Assignments.identity(
                                                ImmutableList.of(first(), second()).stream()
                                                        .filter(this::keep)
                                                        .toList()),
                                        source());
                    }

                    boolean keep(Object value)
                    {
                        return true;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedQualifiedStreamChainInsideQualifiedWrappedFirstArgumentBeforeMultilineSecondArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return project(
                                Assignments.identity(
                                ImmutableList.of(orderkey, custkey, totalprice).stream()
                                        .filter(this::keep)
                                        .collect(toImmutableList())),
                                indexSource(
                                        first(),
                                        second(),
                                        third()));
                    }

                    boolean keep(Object value)
                    {
                        return true;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return project(
                                Assignments.identity(
                                        ImmutableList.of(orderkey, custkey, totalprice).stream()
                                                .filter(this::keep)
                                                .collect(toImmutableList())),
                                indexSource(
                                        first(),
                                        second(),
                                        third()));
                    }

                    boolean keep(Object value)
                    {
                        return true;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedQualifiedStreamChainInsideQualifiedWrappedFirstArgumentBeforeQualifiedSecondArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return p.project(
                                Assignments.identity(
                                ImmutableList.of(leftKey, leftValue, rightKey, rightValue).stream()
                                        .filter(projectionFilter)
                                        .collect(toImmutableList())),
                                p.join(
                                        INNER,
                                        left(),
                                        right()));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return p.project(
                                Assignments.identity(
                                        ImmutableList.of(leftKey, leftValue, rightKey, rightValue).stream()
                                                .filter(projectionFilter)
                                                .collect(toImmutableList())),
                                p.join(
                                        INNER,
                                        left(),
                                        right()));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedBuilderChainInsideLambdaMapCallInsideWrappedSelectorArgument()
    {
        String oldCode =
                """
                import java.util.List;

                class Test
                {
                    Object run(List<Object> values)
                    {
                        return root()
                                .fields(values.stream()
                                        .map(value -> builder()
                                        .name(value)
                                        .build()).toList())
                                .done();
                    }
                }
                """;

        String newCode =
                """
                import java.util.List;

                class Test
                {
                    Object run(List<Object> values)
                    {
                        return root()
                                .fields(values.stream()
                                        .map(value -> builder()
                                                .name(value)
                                                .build()).toList())
                                .done();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedTrailingArgumentsAfterInlineFirstArgumentInWrappedBuilderChain()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return setBuilder()
                                .addAll(existing())
                                .add(first(),
                                                second(),
                                                third())
                                .build();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return setBuilder()
                                .addAll(existing())
                                .add(first(),
                                        second(),
                                        third())
                                .build();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedCollectChainInsideWrappedFirstArgumentOfQualifiedCall()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return plan().project(
                                identity(
                                ImmutableList.of(first(), second(), third()).stream()
                                        .filter(this::keep)
                                        .collect(toImmutableList())),
                                source());
                    }

                    boolean keep(Object value)
                    {
                        return true;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return plan().project(
                                identity(
                                        ImmutableList.of(first(), second(), third()).stream()
                                                .filter(this::keep)
                                                .collect(toImmutableList())),
                                source());
                    }

                    boolean keep(Object value)
                    {
                        return true;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedTrailingArgumentsInsideNestedBuilderArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return target(
                                first(),
                                second(),
                                ImmutableSet.<Object>builder()
                                        .addAll(existing())
                                        .add(first(),
                                                        second(),
                                                        third())
                                        .build());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return target(
                                first(),
                                second(),
                                ImmutableSet.<Object>builder()
                                        .addAll(existing())
                                        .add(first(),
                                                second(),
                                                third())
                                        .build());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedBuilderChainInsideLambdaMapCallInsideFieldsBuilder()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object table)
                    {
                        return root().schema(builder()
                                .fields(
                                        table
                                                .values()
                                                .stream()
                                                .map(value -> itemBuilder()
                                                .name(value)
                                                .build()).toList())
                                .build());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object table)
                    {
                        return root().schema(builder()
                                .fields(
                                        table
                                                .values()
                                                .stream()
                                                .map(value -> itemBuilder()
                                                        .name(value)
                                                        .build()).toList())
                                .build());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedTrailingArgumentsInsideNestedBuilderWithClassInstanceFirstArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object context)
                    {
                        return target(
                                first(),
                                builder()
                                        .addAll(existing())
                                        .add(new One(context),
                                                        new Two(),
                                                        new Three())
                                        .build());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object context)
                    {
                        return target(
                                first(),
                                builder()
                                        .addAll(existing())
                                        .add(new One(context),
                                                new Two(),
                                                new Three())
                                        .build());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedQualifiedStreamChainInsideWrappedFirstArgumentBeforeMultilineSecondArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return project(
                                identity(
                                values().stream()
                                        .filter(this::keep)
                                        .toList()),
                                indexSource(
                                        first(),
                                        second(),
                                        third()));
                    }

                    boolean keep(Object value)
                    {
                        return true;
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return project(
                                identity(
                                        values().stream()
                                                .filter(this::keep)
                                                .toList()),
                                indexSource(
                                        first(),
                                        second(),
                                        third()));
                    }

                    boolean keep(Object value)
                    {
                        return true;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedQualifiedStreamChainInsideCheckStateFirstArgument()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        checkState(SUPPORTED_PROPERTIES.containsAll(tableProperties.stream()
                        .map(PropertyMetadata::getName)
                        .collect(toImmutableList())),
                                "%s does not contain all supported properties",
                                SUPPORTED_PROPERTIES);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        checkState(SUPPORTED_PROPERTIES.containsAll(tableProperties.stream()
                                        .map(PropertyMetadata::getName)
                                        .collect(toImmutableList())),
                                "%s does not contain all supported properties",
                                SUPPORTED_PROPERTIES);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedQualifiedMapFactoryInsideOnlyArgumentInvocationAfterWrappedAssertionChain()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(split().getFileStatisticsDomain()).isEqualTo(TupleDomain.withColumnDomains(
                        ImmutableMap.of(
                                nationKey, Domain.create(ValueSet.ofRanges(Range.range(BIGINT, 0L, true, 24L, true)), false),
                                regionKey, Domain.create(ValueSet.ofRanges(Range.range(BIGINT, 0L, true, 4L, true)), false))));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(split().getFileStatisticsDomain()).isEqualTo(TupleDomain.withColumnDomains(
                                ImmutableMap.of(
                                        nationKey, Domain.create(ValueSet.ofRanges(Range.range(BIGINT, 0L, true, 24L, true)), false),
                                        regionKey, Domain.create(ValueSet.ofRanges(Range.range(BIGINT, 0L, true, 4L, true)), false))));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesUnderIndentedQualifiedMapFactoryInsideQualifiedOnlyArgumentInvocation()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(build(
                                first(),
                                second())).isEqualTo(TupleDomain.withColumnDomains(
                        ImmutableMap.of(
                                nationKey, Domain.create(ValueSet.ofRanges(Range.range(BIGINT, 0L, true, 24L, true)), false),
                                regionKey, Domain.create(ValueSet.ofRanges(Range.range(BIGINT, 0L, true, 4L, true)), false))));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(build(
                                first(),
                                second())).isEqualTo(TupleDomain.withColumnDomains(
                                ImmutableMap.of(
                                        nationKey, Domain.create(ValueSet.ofRanges(Range.range(BIGINT, 0L, true, 24L, true)), false),
                                        regionKey, Domain.create(ValueSet.ofRanges(Range.range(BIGINT, 0L, true, 4L, true)), false))));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedLongBuilderChainInsideStreamMapLambdaInWrappedFieldsBuilderCall()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object outputColumns)
                    {
                        return inputDatasetBuilder()
                                .schema(
                                        openLineage.newSchemaDatasetFacetBuilder()
                                                .fields(
                                                        outputColumns.stream()
                                                        .map(field -> openLineage.newSchemaDatasetFacetFieldsBuilder()
                                                                .name(field.getColumn())
                                                                .build()).toList())
                                                .build())
                                .done();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object outputColumns)
                    {
                        return inputDatasetBuilder()
                                .schema(
                                        openLineage.newSchemaDatasetFacetBuilder()
                                                .fields(
                                                        outputColumns.stream()
                                                                .map(field -> openLineage.newSchemaDatasetFacetFieldsBuilder()
                                                                        .name(field.getColumn())
                                                                        .build()).toList())
                                                .build())
                                .done();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsDeepBuilderChainInsideStreamMapLambdaAfterPriorWrappedSelector()
    {
        String code =
                """
                class Test
                {
                    Object run(Object table)
                    {
                        return root()
                                .dataSource(source(
                                        name(),
                                        uri()))
                                .schema(builder()
                                        .fields(
                                                table
                                                        .getColumns()
                                                        .stream()
                                                        .map(field -> newFieldBuilder()
                                                                .name(field)
                                                                .build()).toList())
                                        .build())
                                .done();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsDeepBuilderChainInsideStreamMapLambdaAfterPriorWrappedSelectorInAssignment()
    {
        String code =
                """
                class Test
                {
                    Object run(Object table)
                    {
                        Object facets = root()
                                .dataSource(source(
                                        name(),
                                        uri()))
                                .schema(builder()
                                        .fields(
                                                table
                                                        .getColumns()
                                                        .stream()
                                                        .map(field -> newFieldBuilder()
                                                                .name(field)
                                                                .build()).toList())
                                        .build());
                        return facets;
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsNestedStreamChainInsideWrappedQualifiedCallArgument()
    {
        String code =
                """
                class Test
                {
                    Object run(Object[] args)
                    {
                        return Lists.cartesianProduct(Arrays.stream(args)
                                        .map(this::copy)
                                        .toList())
                                .stream()
                                .toList();
                    }

                    Object copy(Object value)
                    {
                        return value;
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsNestedBuilderChainInsideWrappedInvocationArgument()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return Runner.builder(sessionBuilder()
                                        .setCatalog(catalog())
                                        .setSchema(schema())
                                        .build())
                                .done();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsNestedOptionalSelectorChainInsideWrappedQualifiedCallArgument()
    {
        String code =
                """
                class Test
                {
                    Object run(Object client, Object token, Object callback)
                    {
                        return Response.seeOther(client.fetch(token, callback)
                                        .orElse(callback))
                                .cookie(a())
                                .build();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsShallowNestedOptionalSelectorChainInsideWrappedUnqualifiedCallArgument()
    {
        String code =
                """
                class Test
                {
                    Object run(Object client, Object token, Object callback)
                    {
                        return wrap(client.fetch(token, callback)
                                .orElse(callback))
                                .first()
                                .done();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsShallowNestedQualifiedBindingChainInsideWrappedUnqualifiedCallArgument()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(assertions.expression("x")
                                .binding("a", "v"))
                                .hasType(type())
                                .matches("ok");
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsShallowNestedStreamChainInsideWrappedQualifiedStaticCallArgument()
    {
        String code =
                """
                class Test
                {
                    Object run(java.util.List<Object> outputs)
                    {
                        return Assignments.identity(
                                outputs.stream()
                                        .filter(this::accept)
                                        .toList());
                    }

                    boolean accept(Object value)
                    {
                        return true;
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsDeepNestedBuilderChainInsideWrappedGenericStaticCallArgumentBeforeTrailingSelector()
    {
        String code =
                """
                class Test
                {
                    java.util.Set<Object> run()
                    {
                        return newSetFromMap(SafeCaches.<Object, Boolean>buildNonEvictableCache(CacheBuilder.newBuilder()
                                        .expireAfterWrite(30, SECONDS))
                                .asMap());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsDeepNestedWrappedCallInsideWrappedSecondArgument()
    {
        String code =
                """
                class Test
                {
                    void run(Object tester, Object names, Object values)
                    {
                        tester.testRoundTrip(getInspector(names,
                                        asList(
                                                a(),
                                                b(),
                                                c())),
                                values);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesUnderIndentedNestedWrappedCallInsideWrappedSecondArgument()
    {
        String oldCode =
                """
                class Test
                {
                    void run(Object tester, Object names, Object values)
                    {
                        tester.testRoundTrip(getInspector(names,
                                asList(
                                        a(),
                                        b(),
                                        c())),
                                values);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(Object tester, Object names, Object values)
                    {
                        tester.testRoundTrip(getInspector(names,
                                        asList(
                                                a(),
                                                b(),
                                                c())),
                                values);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsShallowNestedBuilderChainInsideWrappedQualifiedStaticBuilderCallArgumentWithTrailingSelectors()
    {
        String code =
                """
                class Test
                {
                    Object run()
                    {
                        return DistributedQueryRunner.builder(testSessionBuilder()
                                        .setCatalog(catalog())
                                        .setSchema(schema())
                                        .build())
                                .setAdditionalSetup(value -> {
                                    use(value);
                                })
                                .build();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsShallowLaterArgumentStreamChainInsideQualifiedStaticCall()
    {
        String code =
                """
                class Test
                {
                    Object run(java.util.List<Object> left, java.util.List<Object> right)
                    {
                        return Streams
                                .zip(left.stream(),
                                        right.stream(),
                                        (a, b) -> rowBuilder()
                                                .addField("a", a)
                                                .addField("b", b)
                                                .build())
                                .toList();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesOverIndentedBuilderChainInsideStreamMapLambdaInWrappedFieldsCall()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object outputColumns)
                    {
                        return target()
                                .schema(
                                        builder()
                                                .fields(
                                                        outputColumns.stream()
                                                                .map(column -> openLineage.newSchemaDatasetFacetFieldsBuilder()
                                                                                .name(column)
                                                                                .type(column)
                                                                                .build())
                                                                .toList())
                                                .build())
                                .done();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object outputColumns)
                    {
                        return target()
                                .schema(
                                        builder()
                                                .fields(
                                                        outputColumns.stream()
                                                                .map(column -> openLineage.newSchemaDatasetFacetFieldsBuilder()
                                                                        .name(column)
                                                                        .type(column)
                                                                        .build())
                                                                .toList())
                                                .build())
                                .done();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedNestedInvocationLambdaInsideQualifiedCallWithTrailingSelector()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                            throws Exception
                    {
                        try (java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newCachedThreadPool()) {
                            executor.invokeAll(java.util.Collections.nCopies(4,
                                            () -> {
                                                testAssignUniqueId();
                                                return null;
                                            }))
                                    .forEach(this::done);
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                            throws Exception
                    {
                        try (java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newCachedThreadPool()) {
                            executor.invokeAll(java.util.Collections.nCopies(4,
                                            () -> {
                                                testAssignUniqueId();
                                                return null;
                                            }))
                                    .forEach(this::done);
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesOverIndentedNestedInvocationLambdaInsideUnqualifiedCallWithTrailingSelector()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                            throws Exception
                    {
                        try (ExecutorService executor = Executors.newCachedThreadPool()) {
                            executor.invokeAll(nCopies(4,
                                            () -> {
                                                testAssignUniqueId();
                                                return null;
                                            }))
                                    .forEach(this::done);
                        }
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                            throws Exception
                    {
                        try (ExecutorService executor = Executors.newCachedThreadPool()) {
                            executor.invokeAll(nCopies(4,
                                            () -> {
                                                testAssignUniqueId();
                                                return null;
                                            }))
                                    .forEach(this::done);
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsExpressionLambdaBodyWithTextBlockJoinedToArrow()
    {
        String code =
                """
                class Test
                {
                    String run(java.util.Optional<Object> resource)
                    {
                        return resource
                                .map(modifier ->
                                        \"""
                                        - %s
                                        \""".formatted(modifier))
                                .orElse("fallback");
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsChainInsideAnonymousClassIndentation()
    {
        String code =
                """
                class Test
                {
                    Object run(Object key)
                    {
                        return (Object)
                                TypeLiteral.get(new TypeToken<Object>() {}
                                        .where(new TypeParameter<>() {}, key)
                                        .getType());
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsSwitchExpressionLambdaArgumentIndentation()
    {
        String code =
                """
                class Test
                {
                    void run(Object a, Object b)
                    {
                        install(switchModule(
                                Config.class,
                                Config::getValue,
                                value -> switch (value) {
                                    case A -> a;
                                    case B -> b;
                                    default -> throw new RuntimeException("Not supported: " + value);
                                }));
                    }

                    static void install(Object module) {}

                    static Object switchModule(Object a, Object b, Object c)
                    {
                        return null;
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedArgumentsInsideLambdaExpressionBodyAnonymousClass()
    {
        String code =
                """
                class Test
                {
                    void test()
                    {
                        Supplier<Object> module = () -> new AbstractModule()
                        {
                            @Override
                            protected void setup()
                            {
                                install(SwitchModule.switchModule(
                                        Config.class,
                                        Config::getValue,
                                        value -> switch (value) {
                                            case A -> "a";
                                            case B -> "b";
                                            default -> throw new RuntimeException("bad");
                                        }));
                            }
                        };
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsBuilderChainIndentationInConstructorArgument()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        for (Object schema : schemas) {
                            for (Object table : tables) {
                                rows.add(new Row(ImmutableList.<Object>builder()
                                        .add(table.name())
                                        .add(table.doc())
                                        .build(),
                                        RowType.anonymous(List.of(VARCHAR))));
                            }
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsBuilderChainIndentationInChainSelectorArgument()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        return list.stream()
                                .findFirst()
                                .map(_ -> getSecret(
                                        Args.builder()
                                                .secretId(name)
                                                .build(),
                                        Options.builder()
                                                .provider(provider())
                                                .build()));
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsAnonymousClassBraceIndentationInWrappedArgument()
    {
        String code =
                """
                class Test
                {
                    Object cache = CacheBuilder.newBuilder().build(
                            new CacheLoader<Object, Object>()
                            {
                                @Override
                                public Object load(Object key)
                                {
                                    return key;
                                }
                            });
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testAnonymousClassBodyKeepsIndent()
    {
        String code =
                """
                class Test
                {
                    Runnable r = new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            doSomething();
                        }
                    };
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testConstructorSingleWrappedParamKeepsContinuationIndent()
    {
        String code =
                """
                class Outer
                {
                    class Log_args
                    {
                        public Log_args(
                                List<LogEntry> messages)
                        {
                            this.messages = messages;
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testLambdaBodyWrappingAfterArrowKeepsContinuation()
    {
        String code =
                """
                class Test
                {
                    Runnable r = () ->
                            doSomething();
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testBuilderChainAfterInlineAssignmentKeepsSingleContinuationOnSelectors()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        GenericBuilder<Integer, Double> builderObject =
                                new GenericBuilder.Builder<Integer, Double>()
                                        .setFirst(12345)
                                        .setSecond(1.2345)
                                        .build();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testInterfaceMethodSingleWrappedParamGetsContinuationIndent()
    {
        String oldCode =
                """
                interface Dao
                {
                    Optional<Entity> getByName(
                    String resourceName);
                }
                """;

        String newCode =
                """
                interface Dao
                {
                    Optional<Entity> getByName(
                            String resourceName);
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }
}
