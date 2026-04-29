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

        AirstyleFormatter formatter = new AirstyleFormatter();
        int filesProcessed = 0;
        int filesFormatted = 0;

        for (Path directory : directories) {
            if (!Files.isDirectory(directory)) {
                getLog().debug("Skipping non-existent directory: " + directory);
                continue;
            }

            List<Path> javaFiles = collectJavaFiles(directory);

            for (Path file : javaFiles) {
                try {
                    filesProcessed++;
                    if (formatFile(formatter, file)) {
                        filesFormatted++;
                        getLog().info("Formatted: " + file);
                    }
                }
                catch (IOException e) {
                    throw new MojoExecutionException("Error formatting file: " + file, e);
                }
                catch (RuntimeException e) {
                    throw new MojoExecutionException("Internal formatter error while formatting file: " + file, e);
                }
            }
        }

        getLog().info("Processed " + filesProcessed + " files, formatted " + filesFormatted);
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
}
