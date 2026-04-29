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

public class TestAvoidStarImportNormalizer
{
    @Test
    void testFormatterFixesStarImports()
    {
        String oldCode =
                """
                import static java.util.Collections.*;
                import java.util.*;

                class Test {
                    List<String> values = emptyList();
                }
                """;

        String newCode =
                """
                import java.util.List;

                import static java.util.Collections.emptyList;

                class Test
                {
                    List<String> values = emptyList();
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsUnresolvedSingleStarImportWhenNamesCannotBeDeterminedSafely()
    {
        String code =
                """
                import foo.*;

                @Deprecated
                class Test {}
                """;

        assertCanonicalFormatting(code);
    }

    @Test
    void testFormatterKeepsUnresolvedStarImportStableAfterExpandingResolvableStarImport()
    {
        String oldCode =
                """
                import foo.*;
                import java.util.*;

                @Deprecated
                class Test
                {
                    List<String> values;
                }
                """;

        String newCode =
                """
                import foo.*;

                import java.util.List;

                @Deprecated
                class Test
                {
                    List<String> values;
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFormatterKeepsUnresolvedSingleStaticStarImportWhenNamesCannotBeDeterminedSafely()
    {
        String code =
                """
                import static foo.Util.*;

                class Test
                {
                    void run()
                    {
                        call();
                    }
                }
                """;

        assertCanonicalFormatting(code);
    }
}
