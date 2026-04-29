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
package io.airlift.airstyle.maven;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TestAbstractFormattingMojo
{
    @Test
    void testCollectJavaFilesUsesMavenAntPatternsAndDefaultExcludes(@TempDir Path directory)
            throws Exception
    {
        createFile(directory.resolve("Root.java"));
        createFile(directory.resolve("nested/Nested.java"));
        createFile(directory.resolve("nested/Skip.java"));
        createFile(directory.resolve(".git/Ignored.java"));
        createFile(directory.resolve("NotJava.txt"));

        TestMojo mojo = new TestMojo();
        mojo.includes = Set.of("**/*.java");
        mojo.excludes = Set.of("**/Skip.java");

        assertThat(mojo.collectJavaFiles(directory))
                .containsExactlyInAnyOrder(
                        directory.resolve("Root.java"),
                        directory.resolve("nested/Nested.java"));
    }

    private static void createFile(Path file)
            throws Exception
    {
        Files.createDirectories(file.getParent());
        Files.writeString(file, "");
    }

    private static class TestMojo
            extends AbstractFormattingMojo
    {
        @Override
        public void execute() {}
    }
}
