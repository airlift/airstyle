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

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestFormatterMutationRecovery
{
    private static final int MUTATION_ITERATIONS = 1_000;
    private static final int MAX_MUTATION_ATTEMPTS = 5_000;
    private static final long MUTATION_SEED = 0x5A17_57A1L;

    private final AirstyleFormatter formatter = new AirstyleFormatter();

    @Test
    void testFormatterRestoresMutatedControlFlowFormatting()
    {
        assertMutationRestores("control flow",
                """
                class Test
                {
                    void run(boolean ready)
                    {
                        if (ready) {
                            work();
                        }
                        else {
                            skip();
                        }
                    }
                }
                """);
    }

    @Test
    void testFormatterRestoresMutatedImportOrdering()
    {
        assertMutationRestores("imports",
                """
                import com.google.common.collect.ImmutableList;

                import java.util.List;
                import java.util.Map;

                import static java.util.Objects.requireNonNull;

                class Test
                {
                    ImmutableList<String> values = ImmutableList.of();
                    Map<String, String> map;

                    void run(List<String> input)
                    {
                        requireNonNull(input);
                    }
                }
                """);
    }

    @Test
    void testFormatterRestoresMutatedWrappedCallFormatting()
    {
        assertMutationRestores("wrapped call",
                """
                class Test
                {
                    Object run(Object first, Object second)
                    {
                        return service.call(
                                        first,
                                        second)
                                .stream()
                                .toList();
                    }
                }
                """);
    }

    @Test
    void testFormatterRestoresMutatedWrappedLambdaFormatting()
    {
        assertMutationRestores("wrapped lambda",
                """
                class Test
                {
                    void run(Object body)
                    {
                        client.call(
                                value -> value
                                        .setA("a")
                                        .setB("b"),
                                body);
                    }
                }
                """);
    }

    @Test
    void testFormatterRestoresMutatedTypeHierarchyFormatting()
    {
        assertMutationRestores("type hierarchy",
                """
                public sealed interface Fruit<T extends Comparable<T>>
                        permits Apple,
                                Banana,
                                Orange {}

                final class Apple
                        implements Fruit<String> {}

                final class Banana
                        implements Fruit<String> {}

                final class Orange
                        implements Fruit<String> {}
                """);
    }

    @Test
    void testFormatterRestoresMutatedCommentPreservingFormatting()
    {
        assertMutationRestores("comments",
                """
                class Test
                {
                    void run()
                    {
                        execute(
                                // keep-first
                                first, // keep-inline
                                second);
                    }
                }
                """);
    }

    @Test
    void testFormatterRestoresMutatedNestedTernaryFormatting()
    {
        assertMutationRestores("nested ternary",
                """
                class Test
                {
                    JsonNode run(JsonNode fieldNode, JsonNode objectNode, JsonNode arrayNode)
                    {
                        return !fieldNode.isMissingNode() ? fieldNode
                                : !objectNode.isMissingNode() ? objectNode
                                  : !arrayNode.isMissingNode() ? arrayNode
                                    : fieldNode;
                    }
                }
                """);
    }

    @Test
    void testFormatterRestoresMutatedJavadocAndMethodFormatting()
    {
        assertMutationRestores("javadoc",
                """
                class Test
                {
                    /**
                     * Example:
                     * <pre>{@code
                     * @alpha
                     *   body();
                     * }</pre>
                     *
                     * @return the result
                     */
                    String run()
                    {
                        return "ok";
                    }
                }
                """);
    }

    private void assertMutationRestores(String label, String canonical)
    {
        RandomGenerator random = new Random(MUTATION_SEED);
        int noChangeAttempts = 0;
        int syntaxErrorAttempts = 0;
        int restoredCount = 0;

        for (int attempt = 0; restoredCount < MUTATION_ITERATIONS && attempt < MAX_MUTATION_ATTEMPTS; attempt++) {
            MutatedSource mutation = WhitespaceMutationFuzzer.mutate(canonical, random);
            if (mutation.mutationCount() == 0 || mutation.source().equals(canonical)) {
                noChangeAttempts++;
                continue;
            }
            assertNotEquals(canonical, mutation.source(), label + " attempt " + attempt + " must change the source");
            if (formatter.hasSyntaxErrors(mutation.source())) {
                syntaxErrorAttempts++;
                continue;
            }

            String restored = formatter.format(mutation.source());
            assertEquals(canonical, restored, label + " attempt " + attempt + " should restore canonical output");
            assertEquals(restored, formatter.format(restored), label + " attempt " + attempt + " restored output should be idempotent");

            restoredCount++;
        }

        assertEquals(MUTATION_ITERATIONS, restoredCount, label + " should produce enough mutated inputs");
        assertTrue(noChangeAttempts + syntaxErrorAttempts < MAX_MUTATION_ATTEMPTS - MUTATION_ITERATIONS, label + " should not exhaust attempts on discarded mutations");
    }

    private record MutatedSource(String source, int mutationCount) {}

    private record Token(int type, int start, int endExclusive) {}

    private record WhitespaceGap(int previousToken, int nextToken, int start, int endExclusive, boolean containsLineBreak) {}

    private record Edit(int start, int endExclusive, String replacement) {}

    private static final class WhitespaceMutationFuzzer
    {
        private static final int MAX_EDITS_PER_MUTATION = 8;
        private static final int MAX_SINGLE_LINE_SPACES = 12;
        private static final int MAX_INDENT_SPACES = 32;

        private static MutatedSource mutate(String source, RandomGenerator random)
        {
            List<WhitespaceGap> gaps = whitespaceGaps(source);
            if (gaps.isEmpty()) {
                return new MutatedSource(source, 0);
            }

            List<Edit> edits = new ArrayList<>();
            int editCount = 1 + random.nextInt(Math.min(MAX_EDITS_PER_MUTATION, gaps.size()));
            for (int index = 0; index < editCount; index++) {
                WhitespaceGap gap = gaps.get(random.nextInt(gaps.size()));
                String current = source.substring(gap.start(), gap.endExclusive());
                String replacement = replacementFor(current, gap, random);
                if (!replacement.equals(current)) {
                    edits.add(new Edit(gap.start(), gap.endExclusive(), replacement));
                }
            }

            if (edits.isEmpty()) {
                return new MutatedSource(source, 0);
            }

            edits.sort(Comparator.comparingInt(Edit::start).reversed());
            StringBuilder mutated = new StringBuilder(source);
            int previousStart = source.length() + 1;
            int applied = 0;
            for (Edit edit : edits) {
                if (edit.endExclusive() > previousStart) {
                    continue;
                }
                mutated.replace(edit.start(), edit.endExclusive(), edit.replacement());
                previousStart = edit.start();
                applied++;
            }

            return new MutatedSource(mutated.toString(), applied);
        }

        private static List<WhitespaceGap> whitespaceGaps(String source)
        {
            List<Token> tokens = scanTokens(source);
            if (tokens.size() < 2) {
                return List.of();
            }

            List<WhitespaceGap> gaps = new ArrayList<>();
            for (int index = 1; index < tokens.size(); index++) {
                Token previous = tokens.get(index - 1);
                Token current = tokens.get(index);
                int start = previous.endExclusive();
                int end = current.start();
                if (start >= end || !containsOnlyWhitespace(source, start, end)) {
                    continue;
                }
                gaps.add(new WhitespaceGap(previous.type(), current.type(), start, end, source.indexOf('\n', start) >= 0 && source.indexOf('\n', start) < end));
            }
            return gaps;
        }

        private static List<Token> scanTokens(String source)
        {
            IScanner scanner = ToolFactory.createScanner(
                    false,
                    false,
                    false,
                    JavaLanguageSupport.latestJavaVersion(),
                    JavaLanguageSupport.latestJavaVersion(),
                    true);
            scanner.setSource(source.toCharArray());
            if (!source.isEmpty()) {
                scanner.resetTo(0, source.length() - 1);
            }

            List<Token> tokens = new ArrayList<>();
            try {
                while (true) {
                    int token = scanner.getNextToken();
                    if (token == ITerminalSymbols.TokenNameEOF) {
                        return List.copyOf(tokens);
                    }
                    tokens.add(new Token(token, scanner.getCurrentTokenStartPosition(), scanner.getCurrentTokenEndPosition() + 1));
                }
            }
            catch (InvalidInputException _) {
                return List.of();
            }
        }

        private static String replacementFor(String current, WhitespaceGap gap, RandomGenerator random)
        {
            if (gap.containsLineBreak()) {
                return replaceIndentAfterLastLineBreak(current, random);
            }

            return switch (random.nextInt(3)) {
                case 0 -> current.length() > 1 && canCollapseToSingleSpace(gap) ? " " : "  ";
                case 1 -> " ";
                default -> " ".repeat(2 + random.nextInt(MAX_SINGLE_LINE_SPACES - 1));
            };
        }

        private static boolean canCollapseToSingleSpace(WhitespaceGap gap)
        {
            return gap.previousToken() != ITerminalSymbols.TokenNameDOT
                    && gap.nextToken() != ITerminalSymbols.TokenNameDOT;
        }

        private static String replaceIndentAfterLastLineBreak(String current, RandomGenerator random)
        {
            int lastLineBreak = current.lastIndexOf('\n');
            String prefix = current.substring(0, lastLineBreak + 1);
            return prefix + switch (random.nextInt(4)) {
                case 0 -> "";
                case 1 -> " ".repeat(random.nextInt(MAX_INDENT_SPACES + 1));
                case 2 -> "\t".repeat(1 + random.nextInt(3));
                default -> " \t ".repeat(1 + random.nextInt(3));
            };
        }

        private static boolean containsOnlyWhitespace(String source, int start, int endExclusive)
        {
            for (int index = start; index < endExclusive; index++) {
                if (!Character.isWhitespace(source.charAt(index))) {
                    return false;
                }
            }
            return true;
        }
    }
}
