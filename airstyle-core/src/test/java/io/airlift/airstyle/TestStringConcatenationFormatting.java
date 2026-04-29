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

public class TestStringConcatenationFormatting
{
    @Test
    void testFormatterFixesWrappedStringConcatenationContinuationInMethodArgument()
    {
        String oldCode =
                """
                class Test
                {
                    void run(String tableName)
                    {
                        executeQuery(
                                "CREATE TABLE " + tableName + "( " +
                                "  a_boolean boolean, " +
                                "  a_bigint bigint)");
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(String tableName)
                    {
                        executeQuery(
                                "CREATE TABLE " + tableName + "( " +
                                        "  a_boolean boolean, " +
                                        "  a_bigint bigint)");
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedStringConcatenationContinuationInCallArgument()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(query("INSERT INTO t VALUES "
                                        + "(1),"
                                        + "(2)"))
                                .check();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(query("INSERT INTO t VALUES "
                                + "(1),"
                                + "(2)"))
                                .check();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedNestedFormatStringConcatenationContinuationInMethodArgument()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        assertQuery(
                                format("SELECT table_schema, table_name"
                                        + " FROM information_schema.tables"
                                        + " WHERE table_catalog='%s' AND table_schema = '%s' AND table_name='%s'",
                                        catalog(),
                                        schema(),
                                        table()),
                                ok());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        assertQuery(
                                format("SELECT table_schema, table_name"
                                                + " FROM information_schema.tables"
                                                + " WHERE table_catalog='%s' AND table_schema = '%s' AND table_name='%s'",
                                        catalog(),
                                        schema(),
                                        table()),
                                ok());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesTrailingOperatorStringConcatenationInsideNestedShortFormatCall()
    {
        String oldCode =
                """
                class Test
                {
                    void run(Object left, Object right)
                    {
                        check(
                                format("left %s " +
                                        "right %s",
                                        left,
                                        right));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(Object left, Object right)
                    {
                        check(
                                format("left %s " +
                                                "right %s",
                                        left,
                                        right));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedStringConcatenationContinuationInNestedShortCall()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return assertThat(count(
                                plan("SELECT * " +
                                                "FROM t"),
                                table()))
                                .isEqualTo(1);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return assertThat(count(
                                plan("SELECT * " +
                                        "FROM t"),
                                table()))
                                .isEqualTo(1);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedNestedFormatStringConcatenationInsideConstructorArgument()
    {
        String oldCode =
                """
                class Test
                {
                    Object run(java.util.Optional<Object> optional)
                    {
                        return optional.orElseGet(() -> new Failure(
                                "Authentication required",
                                format("Bearer x_redirect_server=\\"http://localhost:%s\\", " +
                                        "x_token_server=\\"http://localhost:%s\\"",
                                        first(),
                                        second())));
                    }

                    record Failure(String message, String detail) {}
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run(java.util.Optional<Object> optional)
                    {
                        return optional.orElseGet(() -> new Failure(
                                "Authentication required",
                                format("Bearer x_redirect_server=\\"http://localhost:%s\\", " +
                                                "x_token_server=\\"http://localhost:%s\\"",
                                        first(),
                                        second())));
                    }

                    record Failure(String message, String detail) {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedEmptySeedStringConcatenationInNestedFormatCall()
    {
        String oldCode =
                """
                class Test
                {
                    void run(Object session, Object joinOperator)
                    {
                        assertThat(query(session, format("" +
                                        "SELECT c.name, n.name " +
                                        "FROM (SELECT * FROM customer WHERE acctbal > 8000) c " +
                                        "%s nation n ON c.custkey = n.nationkey", joinOperator)))
                                .check();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(Object session, Object joinOperator)
                    {
                        assertThat(query(session, format("" +
                                "SELECT c.name, n.name " +
                                "FROM (SELECT * FROM customer WHERE acctbal > 8000) c " +
                                "%s nation n ON c.custkey = n.nationkey", joinOperator)))
                                .check();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedStringConcatenationInNonFirstOuterArgument()
    {
        String oldCode =
                """
                class Test
                {
                    void run(Object fileSystem, Object key)
                    {
                        throw new RuntimeException(
                                "error",
                                format("Writes are not enabled on the %1$s filesystem in order to avoid eventual data corruption which may be caused by concurrent data modifications on the table. " +
                                                "Writes to the %1$s filesystem can be however enabled with the '%2$s' configuration property.", fileSystem, key));
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(Object fileSystem, Object key)
                    {
                        throw new RuntimeException(
                                "error",
                                format("Writes are not enabled on the %1$s filesystem in order to avoid eventual data corruption which may be caused by concurrent data modifications on the table. " +
                                        "Writes to the %1$s filesystem can be however enabled with the '%2$s' configuration property.", fileSystem, key));
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsShallowInlineStringConcatenationInLaterArgumentOfInlineCall()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        assertQuery("" +
                                "first", "" +
                                "second");
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesWrappedConditionalStringConcatenationInTopLevelMultiArgumentCall()
    {
        String oldCode =
                """
                class Test
                {
                    void run(String tableName, boolean enabled, int checkpoint)
                    {
                        assertUpdate("CREATE TABLE " + tableName
                                        + (enabled ? format(" WITH (checkpoint_interval = %s)", checkpoint) : "")
                                        + " AS SELECT * FROM t",
                                25);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(String tableName, boolean enabled, int checkpoint)
                    {
                        assertUpdate(
                                "CREATE TABLE " + tableName
                                        + (enabled ? format(" WITH (checkpoint_interval = %s)", checkpoint) : "")
                                        + " AS SELECT * FROM t",
                                25);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsInlineSingleArgumentWrappedStringConcatenation()
    {
        String code =
                """
                class Test
                {
                    void run(String tableName)
                    {
                        assertUpdate("CREATE TABLE " + tableName
                                + " WITH ("
                                + "   partitioned_by = ARRAY['regionkey']"
                                + ")"
                                + "AS SELECT * FROM tpch.sf1.nation");
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsWrappedStringConcatenationContinuationInTopLevelMultiArgumentCall()
    {
        String oldCode =
                """
                class Test
                {
                    void run(String tableName)
                    {
                        assertUpdate("CREATE TABLE " + tableName
                                + " WITH ("
                                + "   partitioned_by = ARRAY['regionkey']"
                                + ")"
                                + "AS SELECT * FROM tpch.sf1.nation",
                                25);
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run(String tableName)
                    {
                        assertUpdate(
                                "CREATE TABLE " + tableName
                                        + " WITH ("
                                        + "   partitioned_by = ARRAY['regionkey']"
                                        + ")"
                                        + "AS SELECT * FROM tpch.sf1.nation",
                                25);
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedStringConcatenationContinuationBeforeTrailingSelectors()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        handle.createQuery(
                                "SELECT reltuples FROM pg_class " +
                                        "WHERE relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = :schema) " +
                                        "AND relname = :table_name")
                                .bind("schema", schema())
                                .bind("table_name", table());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        handle.createQuery(
                                        "SELECT reltuples FROM pg_class " +
                                                "WHERE relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = :schema) " +
                                                "AND relname = :table_name")
                                .bind("schema", schema())
                                .bind("table_name", table());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedStringConcatenationContinuationInSelectorLineInvocation()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        target()
                                .first()
                                .message("left" +
                                "right");
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
                                .first()
                                .message("left" +
                                        "right");
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesQualifiedSingleArgumentStringConcatenationContinuationBeforeTrailingSelectors()
    {
        String oldCode =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(assertions.expression("array_sort(a, (x, y) -> CASE " +
                                "WHEN month(x) > month(y) THEN 1 " +
                                "ELSE -1 END)")
                                .binding("a", "value"))
                                .matches("ok");
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(assertions.expression("array_sort(a, (x, y) -> CASE " +
                                        "WHEN month(x) > month(y) THEN 1 " +
                                        "ELSE -1 END)")
                                .binding("a", "value"))
                                .matches("ok");
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsQualifiedSingleArgumentStringConcatenationContinuationBeforeSingleTrailingSelector()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(assertions.query("WITH RECURSIVE t(n) AS (" +
                                "          SELECT 1" +
                                "          )" +
                                "          SELECT * from t"))
                                .matches("ok");
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsQualifiedSingleArgumentStringConcatenationContinuationBeforeWrappedSelectorsAfterInlineSelector()
    {
        String code =
                """
                class Test
                {
                    void run()
                    {
                        assertThat(assertions.query("INSERT INTO t VALUES " +
                                "(1)," +
                                "(2)"))
                                .failure()
                                .hasErrorCode(code())
                                .hasMessage("bad");
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterFixesInlineSingleArgumentWrappedStringConcatenationBeforeTrailingSelectors()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return handle.createQuery("" +
                                "SELECT x " +
                                "FROM y")
                                .bind("schema", schema())
                                .bind("table_name", table());
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return handle.createQuery("" +
                                        "SELECT x " +
                                        "FROM y")
                                .bind("schema", schema())
                                .bind("table_name", table());
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterFixesWrappedStringConcatenationOnlyArgumentBeforeTrailingSelectors()
    {
        String oldCode =
                """
                class Test
                {
                    Object run()
                    {
                        return handle.createQuery("" +
                                "SELECT sum(rows) row_count " +
                                "FROM sys.partitions " +
                                "WHERE object_id = :object_id " +
                                "AND index_id IN (0, 1)")
                                .bind("object_id", value())
                                .mapTo(Long.class)
                                .one();
                    }
                }
                """;

        String newCode =
                """
                class Test
                {
                    Object run()
                    {
                        return handle.createQuery("" +
                                        "SELECT sum(rows) row_count " +
                                        "FROM sys.partitions " +
                                        "WHERE object_id = :object_id " +
                                        "AND index_id IN (0, 1)")
                                .bind("object_id", value())
                                .mapTo(Long.class)
                                .one();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }
}
