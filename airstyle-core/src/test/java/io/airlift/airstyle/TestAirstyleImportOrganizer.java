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

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Tests for [AirstyleImportOrganizer].
class TestAirstyleImportOrganizer
{
    @Test
    void testBasicImportOrdering()
    {
        String input =
                """
                package io.airlift.test;

                import java.util.List;
                import com.google.common.collect.ImmutableList;
                import javax.annotation.Nullable;

                public class Test {}
                """;

        String expected =
                """
                package io.airlift.test;

                import com.google.common.collect.ImmutableList;

                import javax.annotation.Nullable;

                import java.util.List;

                public class Test {}
                """;

        assertEquals(expected, AirstyleImportOrganizer.organizeImports(input));
    }

    @Test
    void testStaticImportsAtBottom()
    {
        String input =
                """
                package io.airlift.test;

                import static org.junit.jupiter.api.Assertions.assertEquals;
                import com.google.common.collect.ImmutableList;
                import java.util.List;

                public class Test {}
                """;

        String expected =
                """
                package io.airlift.test;

                import com.google.common.collect.ImmutableList;

                import java.util.List;

                import static org.junit.jupiter.api.Assertions.assertEquals;

                public class Test {}
                """;

        assertEquals(expected, AirstyleImportOrganizer.organizeImports(input));
    }

    @Test
    void testAlphabeticalSortingWithinGroups()
    {
        String input =
                """
                package io.airlift.test;

                import com.google.common.collect.ImmutableSet;
                import com.google.common.collect.ImmutableList;
                import com.google.common.collect.ImmutableMap;
                import java.util.Set;
                import java.util.List;
                import java.util.Map;

                public class Test {}
                """;

        String expected =
                """
                package io.airlift.test;

                import com.google.common.collect.ImmutableList;
                import com.google.common.collect.ImmutableMap;
                import com.google.common.collect.ImmutableSet;

                import java.util.List;
                import java.util.Map;
                import java.util.Set;

                public class Test {}
                """;

        assertEquals(expected, AirstyleImportOrganizer.organizeImports(input));
    }

    @Test
    void testScrambledImportsRestored()
    {
        // This simulates what ShuffleImportsMutator does
        String scrambled =
                """
                package io.airlift.test;

                import java.util.Map;
                import io.airlift.http.client.HttpClient;
                import java.security.PublicKey;
                import java.util.concurrent.atomic.AtomicReference;
                import io.airlift.units.Duration;
                import io.airlift.http.client.Response;
                import java.net.URI;
                import org.junit.jupiter.api.Test;
                import static org.assertj.core.api.Assertions.assertThat;

                public class Test {}
                """;

        String expected =
                """
                package io.airlift.test;

                import io.airlift.http.client.HttpClient;
                import io.airlift.http.client.Response;
                import io.airlift.units.Duration;
                import org.junit.jupiter.api.Test;

                import java.net.URI;
                import java.security.PublicKey;
                import java.util.Map;
                import java.util.concurrent.atomic.AtomicReference;

                import static org.assertj.core.api.Assertions.assertThat;

                public class Test {}
                """;

        assertEquals(expected, AirstyleImportOrganizer.organizeImports(scrambled));
    }

    @Test
    void testPreservesCodeAfterImports()
    {
        String input =
                """
                package io.airlift.test;

                import java.util.List;
                import com.google.common.collect.ImmutableList;

                public class Test
                {
                    public void method()
                    {
                        // Some code
                    }
                }
                """;

        String expected =
                """
                package io.airlift.test;

                import com.google.common.collect.ImmutableList;

                import java.util.List;

                public class Test
                {
                    public void method()
                    {
                        // Some code
                    }
                }
                """;

        assertEquals(expected, AirstyleImportOrganizer.organizeImports(input));
    }

    @Test
    void testNoImports()
    {
        String input =
                """
                package io.airlift.test;

                public class Test {}
                """;

        assertEquals(input, AirstyleImportOrganizer.organizeImports(input));
    }

    @Test
    void testOnlyJavaImports()
    {
        String input =
                """
                package io.airlift.test;

                import java.util.Set;
                import java.util.List;
                import java.util.Map;

                public class Test {}
                """;

        String expected =
                """
                package io.airlift.test;

                import java.util.List;
                import java.util.Map;
                import java.util.Set;

                public class Test {}
                """;

        assertEquals(expected, AirstyleImportOrganizer.organizeImports(input));
    }

    @Test
    void testOnlyOtherImports()
    {
        String input =
                """
                package io.airlift.test;

                import com.google.common.collect.ImmutableSet;
                import com.google.common.collect.ImmutableList;

                public class Test {}
                """;

        String expected =
                """
                package io.airlift.test;

                import com.google.common.collect.ImmutableList;
                import com.google.common.collect.ImmutableSet;

                public class Test {}
                """;

        assertEquals(expected, AirstyleImportOrganizer.organizeImports(input));
    }

    @Test
    void testJavaxImports()
    {
        String input =
                """
                package io.airlift.test;

                import java.util.List;
                import com.google.inject.Inject;
                import javax.annotation.Nullable;
                import javax.annotation.PostConstruct;

                public class Test {}
                """;

        String expected =
                """
                package io.airlift.test;

                import com.google.inject.Inject;

                import javax.annotation.Nullable;
                import javax.annotation.PostConstruct;

                import java.util.List;

                public class Test {}
                """;

        assertEquals(expected, AirstyleImportOrganizer.organizeImports(input));
    }

    @Test
    void testDoesNotRemoveImportsWhenSourceHasMalformedImport()
    {
        String input =
                """
                package io.airlift.test;

                import java.util.Map;
                import java.util.List
                import com.google.common.collect.ImmutableList;

                public class Test {}
                """;

        String expected =
                """
                package io.airlift.test;

                import com.google.common.collect.ImmutableList;

                import java.util.List
                import java.util.Map;

                public class Test {}
                """;

        assertEquals(expected, AirstyleImportOrganizer.organizeImports(input));
    }

    @Test
    void testKeepsImportSectionWithCommentUnchanged()
    {
        String input =
                """
                package io.airlift.test;

                import java.util.List;
                // keep this comment
                import com.google.common.collect.ImmutableList;

                public class Test {}
                """;

        assertEquals(input, AirstyleImportOrganizer.organizeImports(input));
    }
}
