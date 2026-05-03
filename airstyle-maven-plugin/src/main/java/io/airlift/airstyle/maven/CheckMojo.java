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

import io.airlift.airstyle.AirstyleFormatter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Checks that Java source files are already formatted with Airstyle.
 * <p>
 * This mojo validates that all Java files in the project's source directories already match Airstyle's formatting output.
 * If any files would be rewritten, the build fails.
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class CheckMojo
        extends AbstractFormattingMojo
{
    private static final String FORMAT_HINT = "Run `mvn airstyle:format` to apply the required changes.";

    /**
     * Whether to fail the build if files needing formatting are found.
     */
    @Parameter(property = "airstyle.failOnViolation", defaultValue = "true")
    private boolean failOnViolation;

    @Override
    public void execute()
            throws MojoExecutionException, MojoFailureException
    {
        if (isSkip()) {
            getLog().info("Airstyle check skipped");
            return;
        }

        List<Path> directories = collectSourceDirectories();

        if (directories.isEmpty()) {
            getLog().info("No source directories found");
            return;
        }

        List<Path> allJavaFiles = collectAllJavaFiles();

        if (allJavaFiles.isEmpty()) {
            getLog().info("No Java files found to check");
            return;
        }

        FileProcessor<CheckResult> processor = file -> new CheckResult(file, needsFormatting(new AirstyleFormatter(), file));
        List<Path> filesNeedingFormatting = processFiles(allJavaFiles, "checking", processor).stream()
                .filter(CheckResult::needsFormatting)
                .map(CheckResult::file)
                .toList();

        if (filesNeedingFormatting.isEmpty()) {
            getLog().info("All " + allJavaFiles.size() + " files are already formatted");
            return;
        }

        getLog().warn("Found " + filesNeedingFormatting.size() + " file(s) that need formatting:");
        for (Path file : filesNeedingFormatting) {
            getLog().warn("  " + file);
        }
        getLog().warn(FORMAT_HINT);

        if (failOnViolation) {
            throw new MojoFailureException("Found %s file(s) that need formatting. %s"
                    .formatted(filesNeedingFormatting.size(), FORMAT_HINT));
        }
    }

    private static boolean needsFormatting(AirstyleFormatter formatter, Path file)
            throws IOException
    {
        String original = Files.readString(file);
        String formatted = formatter.format(original);
        return !original.equals(formatted);
    }

    private record CheckResult(Path file, boolean needsFormatting) {}
}
