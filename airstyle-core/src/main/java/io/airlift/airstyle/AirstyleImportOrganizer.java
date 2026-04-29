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

import io.airlift.airstyle.model.ImportBlockModel;
import io.airlift.airstyle.model.ImportBlockModel.ImportLine;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;

/// Organizes Java imports according to Airlift style.
///
/// Airlift import order (each group alphabetically sorted, blank line between groups):
///
///   * All other packages
///   * `javax.*` packages
///   * `java.*` packages
///   * Static imports
public final class AirstyleImportOrganizer
{
    private AirstyleImportOrganizer() {}

    /// Organizes imports in the source code according to Airlift style.
    ///
    /// @param source the Java source code
    /// @return source code with organized imports
    public static String organizeImports(String source)
    {
        ImportBlockModel importBlock = ImportBlockModel.create(source);

        // Skip when there are no imports, or when comments/other non-import
        // content is mixed in — reorganizing would drop that content.
        if (!importBlock.hasImports() || importBlock.hasUnsupportedContent()) {
            return source;
        }

        Map<Group, List<ImportLine>> groups = new EnumMap<>(Group.class);
        for (ImportLine line : importBlock.imports()) {
            groups.computeIfAbsent(Group.of(line), _ -> new ArrayList<>()).add(line);
        }

        String organizedImports = Stream.of(Group.values())
                .map(groups::get)
                .filter(Objects::nonNull)
                .map(lines -> lines.stream()
                        .sorted(comparing(AirstyleImportOrganizer::importSortKey))
                        .map(ImportLine::text)
                        .collect(joining("\n")))
                .collect(joining("\n\n"));
        return importBlock.replaceImportsText(organizedImports);
    }

    private static String importSortKey(ImportLine importLine)
    {
        String key = importLine.importName();
        if (importLine.isOnDemand()) {
            key += ".*";
        }
        return key;
    }

    /// Import groups in output order.
    private enum Group
    {
        OTHER,
        JAVAX,
        JAVA,
        STATIC;

        static Group of(ImportLine line)
        {
            if (line.isStatic()) {
                return STATIC;
            }
            if (line.isModuleImport()) {
                return OTHER;
            }
            String name = line.importName();
            if (name.startsWith("javax.")) {
                return JAVAX;
            }
            if (name.startsWith("java.")) {
                return JAVA;
            }
            return OTHER;
        }
    }
}
