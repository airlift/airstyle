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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.airlift.airstyle.FormatterAssertions.assertCanonicalFormatting;
import static io.airlift.airstyle.FormatterAssertions.assertFormatsOldToNew;

/// Tests for modern Java features (Java 21-25+).
///
/// These tests verify that the formatter correctly handles syntax from
/// recent Java versions, including:
///
///   - Records and record patterns (Java 21)
///   - Sealed classes and interfaces (Java 17+)
///   - Pattern matching for switch (Java 21)
///   - Text blocks (Java 15+)
///   - Switch expressions (Java 14+)
///   - Unnamed variables and patterns (Java 22+)
///   - Primitive types in patterns (Java 23+)
///   - Flexible constructor bodies (Java 22+)
///   - Module import declarations (Java 23+)
@DisplayName("Modern Java Features (21-25+)")
public class TestModernJavaFeatures
{
    // =========================================================================
    // Records (Java 16+, finalized Java 21)
    // =========================================================================

    @Nested
    @DisplayName("Records")
    class RecordTests
    {
        @Test
        @DisplayName("Simple record declaration")
        void testSimpleRecord()
        {
            String input =
                    """
                    public record Point(int x, int y)
                    {
                    }
                    """;

            String expected =
                    """
                    public record Point(int x, int y) {}
                    """;

            assertFormatsOldToNew(input, expected);
        }

        @Test
        @DisplayName("Record with compact constructor")
        void testRecordWithCompactConstructor()
        {
            String input =
                    """
                    public record Person(String name, int age)
                    {
                        public Person
                        {
                            if (age < 0) {
                                throw new IllegalArgumentException("Age cannot be negative");
                            }
                        }
                    }
                    """;

            String expected =
                    """
                    public record Person(String name, int age)
                    {
                        public Person
                        {
                            if (age < 0) {
                                throw new IllegalArgumentException("Age cannot be negative");
                            }
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("Record with static members")
        void testRecordWithStaticMembers()
        {
            String input =
                    """
                    public record Config(String host, int port)
                    {
                        public static final Config DEFAULT = new Config("localhost", 8080);

                        public static Config fromEnv()
                        {
                            return DEFAULT;
                        }
                    }
                    """;

            String expected =
                    """
                    public record Config(String host, int port)
                    {
                        public static final Config DEFAULT = new Config("localhost", 8080);

                        public static Config fromEnv()
                        {
                            return DEFAULT;
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("Record field with trailing comment")
        void testFormatterKeepsTrailingLineCommentOnRecordField()
        {
            String input =
                    """
                    public record Config(String host, int port)
                    {
                        private static final int MIN_SIZE = 5 * 1024 * 1024; // required

                        public Config
                        {
                            if (port < 0) {
                                throw new IllegalArgumentException("port is negative");
                            }
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("Record header with consecutive trailing comments")
        void testFormatterKeepsConsecutiveCommentsBeforeRecordHeaderClose()
        {
            String input =
                    """
                    public record Config(
                            String host,
                            int port
                            // first
                            // second
                    ) {}
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("Nested records")
        void testNestedRecords()
        {
            String input =
                    """
                    public record Outer(Inner inner)
                    {
                        public record Inner(String value)
                        {
                        }
                    }
                    """;

            String expected =
                    """
                    public record Outer(Inner inner)
                    {
                        public record Inner(String value) {}
                    }
                    """;

            assertFormatsOldToNew(input, expected);
        }

        @Test
        @DisplayName("Record with generics")
        void testRecordWithGenerics()
        {
            String input =
                    """
                    public record Pair<T, U>(T first, U second)
                    {
                        public static <T, U> Pair<T, U> of(T first, U second)
                        {
                            return new Pair<>(first, second);
                        }
                    }
                    """;

            String expected =
                    """
                    public record Pair<T, U>(T first, U second)
                    {
                        public static <T, U> Pair<T, U> of(T first, U second)
                        {
                            return new Pair<>(first, second);
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }
    }

    // =========================================================================
    // Record Patterns (Java 21)
    // =========================================================================

    @Nested
    @DisplayName("Record Patterns")
    class RecordPatternTests
    {
        @Test
        @DisplayName("Simple record pattern in switch")
        void testSimpleRecordPattern()
        {
            String input =
                    """
                    class Test
                    {
                        String describe(Object obj)
                        {
                            return switch (obj) {
                                case Point(int x, int y) -> "Point at " + x + ", " + y;
                                case null, default -> "Unknown";
                            };
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        String describe(Object obj)
                        {
                            return switch (obj) {
                                case Point(int x, int y) -> "Point at " + x + ", " + y;
                                case null, default -> "Unknown";
                            };
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("Nested record patterns")
        void testNestedRecordPatterns()
        {
            String input =
                    """
                    class Test
                    {
                        String describe(Object obj)
                        {
                            return switch (obj) {
                                case Line(Point(int x1, int y1), Point(int x2, int y2)) ->
                                        "Line from (" + x1 + "," + y1 + ") to (" + x2 + "," + y2 + ")";
                                default -> "Unknown";
                            };
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        String describe(Object obj)
                        {
                            return switch (obj) {
                                case Line(Point(int x1, int y1), Point(int x2, int y2)) -> "Line from (" + x1 + "," + y1 + ") to (" + x2 + "," + y2 + ")";
                                default -> "Unknown";
                            };
                        }
                    }
                    """;

            assertFormatsOldToNew(input, expected);
        }

        @Test
        @DisplayName("Record pattern with when guard")
        void testRecordPatternWithGuard()
        {
            String input =
                    """
                    class Test
                    {
                        String quadrant(Point p)
                        {
                            return switch (p) {
                                case Point(int x, int y) when x > 0 && y > 0 -> "Q1";
                                case Point(int x, int y) when x < 0 && y > 0 -> "Q2";
                                case Point(int x, int y) when x < 0 && y < 0 -> "Q3";
                                case Point(int x, int y) when x > 0 && y < 0 -> "Q4";
                                default -> "Origin or axis";
                            };
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        String quadrant(Point p)
                        {
                            return switch (p) {
                                case Point(int x, int y) when x > 0 && y > 0 -> "Q1";
                                case Point(int x, int y) when x < 0 && y > 0 -> "Q2";
                                case Point(int x, int y) when x < 0 && y < 0 -> "Q3";
                                case Point(int x, int y) when x > 0 && y < 0 -> "Q4";
                                default -> "Origin or axis";
                            };
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("Record pattern in instanceof")
        void testRecordPatternInInstanceof()
        {
            String input =
                    """
                    class Test
                    {
                        void process(Object obj)
                        {
                            if (obj instanceof Point(int x, int y)) {
                                System.out.println("Point: " + x + ", " + y);
                            }
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        void process(Object obj)
                        {
                            if (obj instanceof Point(int x, int y)) {
                                System.out.println("Point: " + x + ", " + y);
                            }
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }
    }

    // =========================================================================
    // Sealed Classes (Java 17+)
    // =========================================================================

    @Nested
    @DisplayName("Sealed Classes")
    class SealedClassTests
    {
        @Test
        @DisplayName("Sealed class with permits")
        void testSealedClassWithPermits()
        {
            String input =
                    """
                    public sealed class Shape permits Circle, Rectangle, Square
                    {
                        public abstract double area();
                    }
                    """;

            String expected =
                    """
                    public sealed class Shape
                            permits Circle,
                                    Rectangle,
                                    Square
                    {
                        public abstract double area();
                    }
                    """;

            assertFormatsOldToNew(input, expected);
        }

        @Test
        @DisplayName("Final subclass of sealed")
        void testFinalSubclass()
        {
            String input =
                    """
                    public final class Circle extends Shape
                    {
                        private final double radius;

                        public Circle(double radius)
                        {
                            this.radius = radius;
                        }

                        @Override
                        public double area()
                        {
                            return Math.PI * radius * radius;
                        }
                    }
                    """;

            String expected =
                    """
                    public final class Circle
                            extends Shape
                    {
                        private final double radius;

                        public Circle(double radius)
                        {
                            this.radius = radius;
                        }

                        @Override
                        public double area()
                        {
                            return Math.PI * radius * radius;
                        }
                    }
                    """;

            assertFormatsOldToNew(input, expected);
        }

        @Test
        @DisplayName("Non-sealed subclass")
        void testNonSealedSubclass()
        {
            String input =
                    """
                    public non-sealed class Rectangle extends Shape
                    {
                        protected final double width;
                        protected final double height;

                        public Rectangle(double width, double height)
                        {
                            this.width = width;
                            this.height = height;
                        }

                        @Override
                        public double area()
                        {
                            return width * height;
                        }
                    }
                    """;

            String expected =
                    """
                    public non-sealed class Rectangle
                            extends Shape
                    {
                        protected final double width;
                        protected final double height;

                        public Rectangle(double width, double height)
                        {
                            this.width = width;
                            this.height = height;
                        }

                        @Override
                        public double area()
                        {
                            return width * height;
                        }
                    }
                    """;

            assertFormatsOldToNew(input, expected);
        }

        @Test
        @DisplayName("Sealed interface")
        void testSealedInterface()
        {
            String input =
                    """
                    public sealed interface Expr permits Const, Add, Mul
                    {
                        int eval();
                    }
                    """;

            String expected =
                    """
                    public sealed interface Expr
                            permits Add,
                                    Const,
                                    Mul
                    {
                        int eval();
                    }
                    """;

            assertFormatsOldToNew(input, expected);
        }
    }

    // =========================================================================
    // Pattern Matching for Switch (Java 21)
    // =========================================================================

    @Nested
    @DisplayName("Pattern Matching for Switch")
    class PatternMatchingSwitchTests
    {
        @Test
        @DisplayName("Type pattern in switch")
        void testTypePatternInSwitch()
        {
            String input =
                    """
                    class Test
                    {
                        String format(Object obj)
                        {
                            return switch (obj) {
                                case Integer i -> String.format("int %d", i);
                                case Long l -> String.format("long %d", l);
                                case Double d -> String.format("double %f", d);
                                case String s -> String.format("String %s", s);
                                default -> obj.toString();
                            };
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        String format(Object obj)
                        {
                            return switch (obj) {
                                case Integer i -> String.format("int %d", i);
                                case Long l -> String.format("long %d", l);
                                case Double d -> String.format("double %f", d);
                                case String s -> String.format("String %s", s);
                                default -> obj.toString();
                            };
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("Null case in switch")
        void testNullCaseInSwitch()
        {
            String input =
                    """
                    class Test
                    {
                        String describe(Object obj)
                        {
                            return switch (obj) {
                                case null -> "null";
                                case String s -> "string: " + s;
                                default -> "other";
                            };
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        String describe(Object obj)
                        {
                            return switch (obj) {
                                case null -> "null";
                                case String s -> "string: " + s;
                                default -> "other";
                            };
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("Combined null and default case")
        void testNullDefaultCase()
        {
            String input =
                    """
                    class Test
                    {
                        String describe(Object obj)
                        {
                            return switch (obj) {
                                case String s -> "string: " + s;
                                case null, default -> "other or null";
                            };
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        String describe(Object obj)
                        {
                            return switch (obj) {
                                case String s -> "string: " + s;
                                case null, default -> "other or null";
                            };
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("Guarded patterns with when")
        void testGuardedPatterns()
        {
            String input =
                    """
                    class Test
                    {
                        String describe(Integer i)
                        {
                            return switch (i) {
                                case Integer n when n < 0 -> "negative";
                                case Integer n when n == 0 -> "zero";
                                case Integer n when n > 0 -> "positive";
                                default -> "unreachable";
                            };
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        String describe(Integer i)
                        {
                            return switch (i) {
                                case Integer n when n < 0 -> "negative";
                                case Integer n when n == 0 -> "zero";
                                case Integer n when n > 0 -> "positive";
                                default -> "unreachable";
                            };
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }
    }

    // =========================================================================
    // Switch Expressions (Java 14+)
    // =========================================================================

    @Nested
    @DisplayName("Switch Expressions")
    class SwitchExpressionTests
    {
        @Test
        @DisplayName("Arrow switch expression")
        void testArrowSwitchExpression()
        {
            String input =
                    """
                    class Test
                    {
                        int numLetters(String day)
                        {
                            return switch (day) {
                                case "MONDAY", "FRIDAY", "SUNDAY" -> 6;
                                case "TUESDAY" -> 7;
                                case "THURSDAY", "SATURDAY" -> 8;
                                case "WEDNESDAY" -> 9;
                                default -> throw new IllegalArgumentException();
                            };
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        int numLetters(String day)
                        {
                            return switch (day) {
                                case "MONDAY", "FRIDAY", "SUNDAY" -> 6;
                                case "TUESDAY" -> 7;
                                case "THURSDAY", "SATURDAY" -> 8;
                                case "WEDNESDAY" -> 9;
                                default -> throw new IllegalArgumentException();
                            };
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("Yield in switch expression")
        void testYieldInSwitchExpression()
        {
            String input =
                    """
                    class Test
                    {
                        String describe(int value)
                        {
                            return switch (value) {
                                case 1 -> "one";
                                case 2 -> "two";
                                default -> {
                                    String result = "number: " + value;
                                    yield result;
                                }
                            };
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        String describe(int value)
                        {
                            return switch (value) {
                                case 1 -> "one";
                                case 2 -> "two";
                                default -> {
                                    String result = "number: " + value;
                                    yield result;
                                }
                            };
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("Switch expression as first method argument")
        void testFormatterKeepsSwitchExpressionAsFirstMethodArgument()
        {
            String code =
                    """
                    class Test
                    {
                        String format(Kind kind, Object value)
                        {
                            return "%s: %s".formatted(
                                    switch (kind) {
                                        case ONE -> "one";
                                        case TWO -> "two";
                                    },
                                    value);
                        }
                    }
                    """;

            assertCanonicalFormatting(code);
        }
    }

    // =========================================================================
    // Text Blocks (Java 15+)
    // =========================================================================

    @Nested
    @DisplayName("Text Blocks")
    class TextBlockTests
    {
        @Test
        @DisplayName("Simple text block")
        void testSimpleTextBlock()
        {
            String input =
                    """
                    class Test
                    {
                        String html = \"""
                                <html>
                                    <body>
                                        <p>Hello, World</p>
                                    </body>
                                </html>
                                \""";
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        String html =
                                \"""
                                <html>
                                    <body>
                                        <p>Hello, World</p>
                                    </body>
                                </html>
                                \""";
                    }
                    """;

            assertFormatsOldToNew(input, expected);
        }

        @Test
        @DisplayName("Text block with JSON")
        void testTextBlockWithJson()
        {
            String input =
                    """
                    class Test
                    {
                        String json = \"""
                                {
                                    "name": "John",
                                    "age": 30,
                                    "city": "New York"
                                }
                                \""";
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        String json =
                                \"""
                                {
                                    "name": "John",
                                    "age": 30,
                                    "city": "New York"
                                }
                                \""";
                    }
                    """;

            assertFormatsOldToNew(input, expected);
        }

        @Test
        @DisplayName("Text block with SQL")
        void testTextBlockWithSql()
        {
            String input =
                    """
                    class Test
                    {
                        String sql = \"""
                                SELECT id, name, email
                                FROM users
                                WHERE status = 'active'
                                ORDER BY created_at DESC
                                \""";
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        String sql =
                                \"""
                                SELECT id, name, email
                                FROM users
                                WHERE status = 'active'
                                ORDER BY created_at DESC
                                \""";
                    }
                    """;

            assertFormatsOldToNew(input, expected);
        }
    }

    // =========================================================================
    // Unnamed Variables and Patterns (Java 22+)
    // =========================================================================

    @Nested
    @DisplayName("Unnamed Variables (Java 22+)")
    class UnnamedVariableTests
    {
        @Test
        @DisplayName("Unnamed variable in try-with-resources")
        void testUnnamedVariableInTry()
        {
            String input =
                    """
                    class Test
                    {
                        void process()
                        {
                            try (var _ = ScopedContext.acquire()) {
                                doWork();
                            }
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        void process()
                        {
                            try (var _ = ScopedContext.acquire()) {
                                doWork();
                            }
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("Unnamed variable in lambda")
        void testUnnamedVariableInLambda()
        {
            String input =
                    """
                    class Test
                    {
                        void process(List<String> list)
                        {
                            list.forEach(_ -> count++);
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        void process(List<String> list)
                        {
                            list.forEach(_ -> count++);
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("Unnamed variable in catch")
        void testUnnamedVariableInCatch()
        {
            String input =
                    """
                    class Test
                    {
                        void process()
                        {
                            try {
                                riskyOperation();
                            }
                            catch (Exception _) {
                                handleError();
                            }
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        void process()
                        {
                            try {
                                riskyOperation();
                            }
                            catch (Exception _) {
                                handleError();
                            }
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("Unnamed pattern in switch")
        void testUnnamedPatternInSwitch()
        {
            String input =
                    """
                    class Test
                    {
                        void process(Box box)
                        {
                            switch (box) {
                                case Box(RedBall _) -> processRed();
                                case Box(BlueBall _) -> processBlue();
                                case Box(_) -> processOther();
                            }
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        void process(Box box)
                        {
                            switch (box) {
                                case Box(RedBall _) -> processRed();
                                case Box(BlueBall _) -> processBlue();
                                case Box(_) -> processOther();
                            }
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }
    }

    // =========================================================================
    // Primitive Types in Patterns (Java 23+)
    // =========================================================================

    @Nested
    @DisplayName("Primitive Types in Patterns (Java 23+)")
    class PrimitivePatternTests
    {
        @Test
        @DisplayName("Primitive type pattern in switch")
        void testPrimitiveTypePatternInSwitch()
        {
            String input =
                    """
                    class Test
                    {
                        String classify(int value)
                        {
                            return switch (value) {
                                case int i when i < 0 -> "negative";
                                case int i when i == 0 -> "zero";
                                case int i -> "positive: " + i;
                            };
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        String classify(int value)
                        {
                            return switch (value) {
                                case int i when i < 0 -> "negative";
                                case int i when i == 0 -> "zero";
                                case int i -> "positive: " + i;
                            };
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }

        @Test
        @DisplayName("Primitive pattern in instanceof")
        void testPrimitivePatternInInstanceof()
        {
            String input =
                    """
                    class Test
                    {
                        void process(Number n)
                        {
                            if (n instanceof int i) {
                                System.out.println("int: " + i);
                            }
                            else if (n instanceof long l) {
                                System.out.println("long: " + l);
                            }
                        }
                    }
                    """;

            String expected =
                    """
                    class Test
                    {
                        void process(Number n)
                        {
                            if (n instanceof int i) {
                                System.out.println("int: " + i);
                            }
                            else if (n instanceof long l) {
                                System.out.println("long: " + l);
                            }
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }
    }

    // =========================================================================
    // Flexible Constructor Bodies (Java 22+)
    // =========================================================================

    @Nested
    @DisplayName("Flexible Constructor Bodies (Java 22+)")
    class FlexibleConstructorTests
    {
        @Test
        @DisplayName("Statements before super()")
        void testStatementsBeforeSuper()
        {
            String input =
                    """
                    class PositiveBigInteger extends BigInteger
                    {
                        public PositiveBigInteger(long value)
                        {
                            if (value <= 0) {
                                throw new IllegalArgumentException("Value must be positive");
                            }
                            super(value);
                        }
                    }
                    """;

            String expected =
                    """
                    class PositiveBigInteger
                            extends BigInteger
                    {
                        public PositiveBigInteger(long value)
                        {
                            if (value <= 0) {
                                throw new IllegalArgumentException("Value must be positive");
                            }
                            super(value);
                        }
                    }
                    """;

            assertFormatsOldToNew(input, expected);
        }

        @Test
        @DisplayName("Validation before this()")
        void testStatementsBeforeThis()
        {
            String code =
                    """
                    class Rectangle
                    {
                        private final int width;
                        private final int height;

                        public Rectangle(int width, int height)
                        {
                            this.width = width;
                            this.height = height;
                        }

                        public Rectangle(int side)
                        {
                            if (side <= 0) {
                                throw new IllegalArgumentException("Side must be positive");
                            }
                            this(side, side);
                        }
                    }
                    """;

            assertCanonicalFormatting(code);
        }
    }

    // =========================================================================
    // Module Import Declarations (Java 23+)
    // =========================================================================

    @Nested
    @DisplayName("Module Import Declarations (Java 23+)")
    class ModuleImportTests
    {
        @Test
        @DisplayName("Import module declaration")
        void testImportModuleDeclaration()
        {
            String input =
                    """
                    import module java.base;

                    class Test
                    {
                        void process()
                        {
                            List<String> list = new ArrayList<>();
                        }
                    }
                    """;

            String expected =
                    """
                    import module java.base;

                    class Test
                    {
                        void process()
                        {
                            List<String> list = new ArrayList<>();
                        }
                    }
                    """;

            assertCanonicalFormatting(input);
        }
    }

    // =========================================================================
    // Implicitly Declared Classes (Java 23+)
    // =========================================================================

    @Nested
    @DisplayName("Implicitly Declared Classes (Java 23+)")
    class ImplicitlyDeclaredClassTests
    {
        @Test
        @DisplayName("Simple implicitly declared class")
        void testImplicitlyDeclaredClass()
        {
            // Note: Implicitly declared classes don't have an explicit class declaration
            // The file contains just methods at the top level
            String input =
                    """
                    void main()
                    {
                        System.out.println("Hello, World!");
                    }
                    """;

            String expected =
                    """
                    void main()
                    {
                    System.out.println("Hello, World!");
                    }
                    """;

            assertFormatsOldToNew(input, expected);
        }

        @Test
        @DisplayName("Implicitly declared class with helper methods")
        void testImplicitClassWithHelpers()
        {
            String input =
                    """
                    void main()
                    {
                        greet("World");
                    }

                    void greet(String name)
                    {
                        System.out.println("Hello, " + name + "!");
                    }
                    """;

            String expected =
                    """
                    void main()
                    {
                    greet("World");
                    }

                    void greet(String name)
                    {
                    System.out.println("Hello, " + name + "!");
                    }
                    """;

            assertFormatsOldToNew(input, expected);
        }
    }

    // =========================================================================
    // Complex Combinations
    // =========================================================================

    @Nested
    @DisplayName("Complex Feature Combinations")
    class ComplexCombinationTests
    {
        @Test
        @DisplayName("Sealed hierarchy with record patterns")
        void testSealedWithRecordPatterns()
        {
            String input =
                    """
                    sealed interface Expr permits Const, Add, Mul
                    {
                    }

                    record Const(int value) implements Expr
                    {
                    }

                    record Add(Expr left, Expr right) implements Expr
                    {
                    }

                    record Mul(Expr left, Expr right) implements Expr
                    {
                    }

                    class Calculator
                    {
                        int eval(Expr expr)
                        {
                            return switch (expr) {
                                case Const(int value) -> value;
                                case Add(Expr left, Expr right) -> eval(left) + eval(right);
                                case Mul(Expr left, Expr right) -> eval(left) * eval(right);
                            };
                        }
                    }
                    """;

            String expected =
                    """
                    sealed interface Expr
                            permits Add,
                                    Const,
                                    Mul {}

                    record Const(int value)
                            implements Expr {}

                    record Add(Expr left, Expr right)
                            implements Expr {}

                    record Mul(Expr left, Expr right)
                            implements Expr {}

                    class Calculator
                    {
                        int eval(Expr expr)
                        {
                            return switch (expr) {
                                case Const(int value) -> value;
                                case Add(Expr left, Expr right) -> eval(left) + eval(right);
                                case Mul(Expr left, Expr right) -> eval(left) * eval(right);
                            };
                        }
                    }
                    """;

            assertFormatsOldToNew(input, expected);
        }

        @Test
        @DisplayName("Records with text blocks and patterns")
        void testRecordsWithTextBlocksAndPatterns()
        {
            String input =
                    """
                    record HtmlTemplate(String title, String body)
                    {
                        String render()
                        {
                            return \"""
                                    <html>
                                        <head><title>%s</title></head>
                                        <body>%s</body>
                                    </html>
                                    \""".formatted(title, body);
                        }
                    }

                    class Processor
                    {
                        void process(Object template)
                        {
                            switch (template) {
                                case HtmlTemplate(String title, String body) when !title.isEmpty() ->
                                        System.out.println("Rendering: " + title);
                                case HtmlTemplate(_, _) ->
                                        System.out.println("Empty template");
                                default ->
                                        System.out.println("Unknown");
                            }
                        }
                    }
                    """;

            String expected =
                    """
                    record HtmlTemplate(String title, String body)
                    {
                        String render()
                        {
                            return \"""
                                   <html>
                                       <head><title>%s</title></head>
                                       <body>%s</body>
                                   </html>
                                   \""".formatted(title, body);
                        }
                    }

                    class Processor
                    {
                        void process(Object template)
                        {
                            switch (template) {
                                case HtmlTemplate(String title, String body) when !title.isEmpty() -> System.out.println("Rendering: " + title);
                                case HtmlTemplate(_, _) -> System.out.println("Empty template");
                                default -> System.out.println("Unknown");
                            }
                        }
                    }
                    """;

            assertFormatsOldToNew(input, expected);
        }
    }

    @Nested
    @DisplayName("Exact Output Coverage")
    class ExactOutputCoverageTests
    {
        @Test
        @DisplayName("Record compact constructor")
        void testFormatterFormatsRecordCompactConstructorExactly()
        {
            String oldCode =
                    """
                    public record Person(String name, int age){
                        public Person
                        {
                        if (age < 0) {
                        throw new IllegalArgumentException("Age cannot be negative");
                        }
                        }
                    }
                    """;

            String newCode =
                    """
                    public record Person(String name, int age)
                    {
                        public Person
                        {
                            if (age < 0) {
                                throw new IllegalArgumentException("Age cannot be negative");
                            }
                        }
                    }
                    """;

            assertFormatsOldToNew(oldCode, newCode);
        }

        @Test
        @DisplayName("Sealed interface permits clause")
        void testFormatterFormatsSealedInterfaceExactly()
        {
            String oldCode =
                    """
                    public sealed interface Expr permits Mul, Add, Const {
                        int eval();
                    }
                    """;

            String newCode =
                    """
                    public sealed interface Expr
                            permits Add,
                                    Const,
                                    Mul
                    {
                        int eval();
                    }
                    """;

            assertFormatsOldToNew(oldCode, newCode);
        }

        @Test
        @DisplayName("Text block")
        void testFormatterKeepsTextBlockExactly()
        {
            String code =
                    """
                    class Test
                    {
                        String json()
                        {
                            return \"""
                                   {
                                       "name": "x"
                                   }
                                   \""";
                        }
                    }
                    """;

            assertCanonicalFormatting(code);
        }

        @Test
        @DisplayName("Module import declaration")
        void testFormatterKeepsModuleImportExactly()
        {
            String code =
                    """
                    import module java.base;

                    class Test
                    {
                        void process()
                        {
                            List<String> list = new ArrayList<>();
                        }
                    }
                    """;

            assertCanonicalFormatting(code);
        }
    }

    @Test
    void testMultiLineRecordComponentsIndentedAtContinuation()
    {
        String code =
                """
                public record Point(
                        int x,
                        int y,
                        int z) {}
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testMultiLineRecordPatternInCaseLabel()
    {
        String code =
                """
                class Test
                {
                    void describe(Object o)
                    {
                        switch (o) {
                            case Point(
                                    int x,
                                    int y) -> handlePoint(x, y);
                            default -> handleOther();
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testParenthesizedSwitchAsChainReceiverDecomposesCases()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object type)
                    {
                        return (switch (type) {
                        case A -> a();
                        case B -> b();
                        case C -> c();
                        }).orElse(null);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object type)
                    {
                        return (switch (type) {
                            case A -> a();
                            case B -> b();
                            case C -> c();
                        }).orElse(null);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testMultiEnumConstantCaseLabelAlignsSubsequentConstantsAfterCaseKeyword()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object value)
                    {
                        return switch (value) {
                            case Kind.BOOL,
                            Kind.BOOLEAN -> Type.BOOL;
                            case Kind.DATETIME -> Type.DATETIME;
                            default -> null;
                        };
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object value)
                    {
                        return switch (value) {
                            case Kind.BOOL,
                                 Kind.BOOLEAN -> Type.BOOL;
                            case Kind.DATETIME -> Type.DATETIME;
                            default -> null;
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testMultiPatternCaseLabelAlignsSubsequentPatternsAfterCaseKeyword()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(Object o)
                    {
                        return switch (o) {
                            case Foo x,
                            Bar y,
                            Baz z -> empty();
                            default -> null;
                        };
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(Object o)
                    {
                        return switch (o) {
                            case Foo x,
                                 Bar y,
                                 Baz z -> empty();
                            default -> null;
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testMultiLineRecordPatternComponentsGetContinuationIndent()
    {
        String code =
                """
                class Test
                {
                    void describe(Object o)
                    {
                        switch (o) {
                            case Comparison(_,
                                    var operator,
                                    Dereference(_, NameReference(_, var leftBase), var leftField),
                                    Dereference(_, NameReference(_, var rightBase), var rightField)) -> handle(operator);
                            default -> other();
                        }
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsRecordCompactConstructorLayout()
    {
        String code =
                """
                record Value(String name)
                {
                    Value
                    {
                        validate(name);
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsSwitchArrowBlockComments()
    {
        String code =
                """
                class Test
                {
                    int run(int value)
                    {
                        return switch (value) {
                            case 1 -> {
                                {
                                    work();
                                }
                                // keep at block indent
                                yield 1;
                            }
                            default -> 0;
                        };
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsSingleLineSwitchRuleExpressions()
    {
        String code =
                """
                class Test
                {
                    Result run(int value)
                    {
                        return switch (value) {
                            case 1 -> Result.ONE;
                            case 2 -> Result.TWO;
                            default -> Result.OTHER;
                        };
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesNestedRecordPatternsWithGuards()
    {
        String oldCode =
                """
                class Test{sealed interface Shape permits Dot,Segment,Box{}record Dot(int x,int y)implements Shape{}record Segment(Dot start,Dot end)implements Shape{}non-sealed interface Box extends Shape{int width();int height();}String describe(Shape shape){return switch(shape){case Segment(Dot(int x1,int y1),Dot(int x2,int y2))when x1==x2->"vertical "+y1+":"+y2;case Segment(Dot(int x1,int y1),Dot(int x2,int y2))when y1==y2->"horizontal "+x1+":"+x2;case Segment(Dot(int x1,int y1),Dot(int x2,int y2))->"diagonal "+x1+","+y1+":"+x2+","+y2;case Dot(int x,int y)->"dot "+x+","+y;case Box box when box.width()==box.height()->"square";case Box box->"box "+box.width()+"x"+box.height();};}boolean startsAtOrigin(Object value){return value instanceof Segment(Dot(int x,int y),Dot end)&&x==0&&y==0&&end.x()>=x;}}
                """;

        String newCode =
                """
                class Test
                {
                    sealed interface Shape
                            permits Box,
                                    Dot,
                                    Segment {}

                    record Dot(int x, int y)
                            implements Shape {}

                    record Segment(Dot start, Dot end)
                            implements Shape {}

                    non-sealed interface Box
                            extends Shape
                    {
                        int width();

                        int height();
                    }

                    String describe(Shape shape)
                    {
                        return switch (shape) {
                            case Segment(Dot(int x1, int y1), Dot(int x2, int y2)) when x1 == x2 -> "vertical " + y1 + ":" + y2;
                            case Segment(Dot(int x1, int y1), Dot(int x2, int y2)) when y1 == y2 -> "horizontal " + x1 + ":" + x2;
                            case Segment(Dot(int x1, int y1), Dot(int x2, int y2)) -> "diagonal " + x1 + "," + y1 + ":" + x2 + "," + y2;
                            case Dot(int x, int y) -> "dot " + x + "," + y;
                            case Box box when box.width() == box.height() -> "square";
                            case Box box -> "box " + box.width() + "x" + box.height();
                        };
                    }

                    boolean startsAtOrigin(Object value)
                    {
                        return value instanceof Segment(Dot(int x, int y), Dot end) && x == 0 && y == 0 && end.x() >= x;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesLocalTypesInsideMethod()
    {
        String oldCode =
                """
                class Test{Object run(String input){record LocalRecord(String value){}
                enum LocalEnum{
                ONE,
                TWO,
                }
                class LocalClass{private final LocalRecord record;private final LocalEnum kind;LocalClass(LocalRecord record,LocalEnum kind){this.record=record;this.kind=kind;}Object describe(){return new Object(){@Override public String toString(){return kind+":"+record.value();}};}}return new LocalClass(new LocalRecord(input),LocalEnum.ONE).describe();}}
                """;

        String newCode =
                """
                class Test
                {
                    Object run(String input)
                    {
                        record LocalRecord(String value) {}

                        enum LocalEnum
                        {
                            ONE,
                            TWO,
                        }

                        class LocalClass
                        {
                            private final LocalRecord record;
                            private final LocalEnum kind;

                            LocalClass(LocalRecord record, LocalEnum kind)
                            {
                                this.record = record;
                                this.kind = kind;
                            }

                            Object describe()
                            {
                                return new Object()
                                {
                                    @Override
                                    public String toString()
                                    {
                                        return kind + ":" + record.value();
                                    }
                                };
                            }
                        }

                        return new LocalClass(new LocalRecord(input), LocalEnum.ONE).describe();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesEnumConstantBodiesAndSealedChildren()
    {
        String oldCode =
                """
                class Test{record Range(int start,int end){Range{if(end<start){throw new IllegalArgumentException("end before start");}}Range(int single){this(single,single);}}enum Operation{PLUS("+"){@Override int apply(int left,int right){return left+right;}},
                MINUS("-"){@Override int apply(int left,int right){return left-right;}};private final String symbol;Operation(String symbol){this.symbol=symbol;}abstract int apply(int left,int right);String symbol(){return symbol;}}sealed static class Parent
                permits ClosedChild,OpenChild{}final static class ClosedChild extends Parent{}non-sealed static class OpenChild extends Parent{}}
                """;

        String newCode =
                """
                class Test
                {
                    record Range(int start, int end)
                    {
                        Range
                        {
                            if (end < start) {
                                throw new IllegalArgumentException("end before start");
                            }
                        }

                        Range(int single)
                        {
                            this(single, single);
                        }
                    }

                    enum Operation
                    {
                        PLUS("+") {
                            @Override
                            int apply(int left, int right)
                            {
                                return left + right;
                            }
                        },
                        MINUS("-") {
                            @Override
                            int apply(int left, int right)
                            {
                                return left - right;
                            }
                        };

                        private final String symbol;

                        Operation(String symbol)
                        {
                            this.symbol = symbol;
                        }

                        abstract int apply(int left, int right);

                        String symbol()
                        {
                            return symbol;
                        }
                    }

                    static sealed class Parent
                            permits ClosedChild, OpenChild {}

                    static final class ClosedChild
                            extends Parent {}

                    static non-sealed class OpenChild
                            extends Parent {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }
}
