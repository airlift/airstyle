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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Formats Java source files using Airstyle.
 * <p>
 * This mojo formats all Java files in the project's source directories, applying the Airstyle formatting rules.
 */
@Mojo(name = "format", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class FormatMojo
        extends AbstractFormattingMojo
{
    @Override
    public void execute()
            throws MojoExecutionException
    {
        if (isSkip()) {
            getLog().info("Airstyle formatting skipped");
            return;
        }

        List<Path> directories = collectSourceDirectories();

        if (directories.isEmpty()) {
            getLog().info("No source directories found");
            return;
        }

        List<Path> javaFiles = collectAllJavaFiles();
        if (javaFiles.isEmpty()) {
            getLog().info("No Java files found to format");
            return;
        }

        FileProcessor<FormatResult> processor = file -> new FormatResult(file, formatFile(new AirstyleFormatter(), file));
        List<FormatResult> results = processFiles(javaFiles, "formatting", processor);

        int filesFormatted = 0;
        for (FormatResult result : results) {
            if (result.formatted()) {
                filesFormatted++;
                getLog().info("Formatted: " + result.file());
            }
        }

        getLog().info("Processed " + javaFiles.size() + " files, formatted " + filesFormatted);
    }

    /**
     * Formats a single file, returning true if the file was modified.
     */
    protected boolean formatFile(AirstyleFormatter formatter, Path file)
            throws IOException
    {
        String original = Files.readString(file, StandardCharsets.UTF_8);
        String formatted = formatter.format(original);

        if (!original.equals(formatted)) {
            Files.writeString(file, formatted, StandardCharsets.UTF_8);
            return true;
        }

        return false;
    }

    private record FormatResult(Path file, boolean formatted) {}
}
