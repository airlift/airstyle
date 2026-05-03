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

import io.takari.maven.testing.TestResources5;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenRuntime.MavenRuntimeBuilder;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenPluginTest;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/// Integration tests for the formatter Maven plugin mojos.
@MavenVersions({"3.9.11", "3.9.15"})
class TestFormatterIntegration
{
    @RegisterExtension
    private final TestResources5 resources = new TestResources5();

    private final MavenRuntime maven;

    TestFormatterIntegration(MavenRuntimeBuilder mavenBuilder)
            throws Exception
    {
        List<String> cliOptions = new ArrayList<>(List.of("-B", "-U"));

        String localRepository = System.getProperty("maven.repo.local");
        if (localRepository != null && !localRepository.isBlank()) {
            cliOptions.add("-Dmaven.repo.local=" + localRepository);
        }

        this.maven = mavenBuilder
                .withCliOptions(cliOptions.toArray(String[]::new))
                .build();
    }

    @MavenPluginTest
    void testFormatMojoFormatsProjectSources()
            throws Exception
    {
        File basedir = resources.getBasedir("format-simple");
        Path sourceFile = basedir.toPath().resolve("src/main/java/test/Simple.java");
        String original = Files.readString(sourceFile);

        maven.forProject(basedir)
                .execute("process-sources")
                .assertErrorFreeLog();

        String formatted = Files.readString(sourceFile);
        assertThat(formatted).isNotEqualTo(original);
        assertThat(formatted).isEqualTo(
                """
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
                package test;

                public class Simple
                {
                    public void method()
                    {
                        if (true) {
                            System.out.println("hello");
                        }
                    }
                }
                """);
    }

    @MavenPluginTest
    void testCheckMojoPassesFormattedProject()
            throws Exception
    {
        File basedir = resources.getBasedir("check-passes");

        maven.forProject(basedir)
                .execute("verify")
                .assertErrorFreeLog()
                .assertLogText("All 1 files are already formatted");
    }

    @MavenPluginTest
    void testCheckMojoFailsProjectWithViolations()
            throws Exception
    {
        File basedir = resources.getBasedir("check-fails");

        maven.forProject(basedir)
                .execute("verify")
                .assertLogText("Found 1 file(s) that need formatting. Run `mvn airstyle:format` to apply the required changes.")
                .assertLogText("BadlyFormatted.java")
                .assertNoLogText("BUILD SUCCESS");
    }

    @MavenPluginTest
    void testCheckMojoCanWarnWithoutFailing()
            throws Exception
    {
        File basedir = resources.getBasedir("check-warns");

        maven.forProject(basedir)
                .execute("verify")
                .assertErrorFreeLog()
                .assertLogText("Found 1 file(s) that need formatting")
                .assertLogText("BadlyFormatted.java");
    }
}
