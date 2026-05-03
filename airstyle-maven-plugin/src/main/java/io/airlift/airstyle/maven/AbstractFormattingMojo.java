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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;

/**
 * Base class for Airstyle formatting mojos.
 */
public abstract class AbstractFormattingMojo
        extends AbstractMojo
{
    /**
     * Current Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /**
     * List of source directories to process. Defaults to the project's compile source roots.
     */
    @Parameter(property = "airstyle.sourceDirectories")
    protected List<String> sourceDirectories;

    /**
     * List of test source directories to process. Defaults to the project's test compile source roots.
     */
    @Parameter(property = "airstyle.testSourceDirectories")
    protected List<String> testSourceDirectories;

    /**
     * Ant-style file patterns to include.
     */
    @Parameter(property = "airstyle.includes", defaultValue = "**/*.java")
    protected Set<String> includes;

    /**
     * Ant-style file patterns to exclude.
     */
    @Parameter(property = "airstyle.excludes")
    protected Set<String> excludes;

    /**
     * Skip execution.
     */
    @Parameter(property = "airstyle.skip", defaultValue = "false")
    protected boolean skip;

    /**
     * Include test sources.
     */
    @Parameter(property = "airstyle.includeTestSources", defaultValue = "true")
    protected boolean includeTestSources;

    /**
     * Process files in parallel using the JVM common fork-join pool.
     */
    @Parameter(property = "airstyle.parallel", defaultValue = "true")
    protected boolean parallel;

    protected final boolean isSkip()
    {
        return skip;
    }

    protected final List<Path> collectSourceDirectories()
    {
        List<Path> directories = new ArrayList<>();

        addConfiguredOrDefaultDirectories(directories, sourceDirectories, project.getCompileSourceRoots());

        if (includeTestSources) {
            addConfiguredOrDefaultDirectories(directories, testSourceDirectories, project.getTestCompileSourceRoots());
        }

        return directories;
    }

    protected final List<Path> collectAllJavaFiles()
            throws MojoExecutionException
    {
        List<Path> allJavaFiles = new ArrayList<>();
        for (Path directory : collectSourceDirectories()) {
            if (!Files.isDirectory(directory)) {
                getLog().debug("Skipping non-existent directory: " + directory);
                continue;
            }

            allJavaFiles.addAll(collectJavaFiles(directory));
        }
        return allJavaFiles;
    }

    protected final List<Path> collectJavaFiles(Path directory)
            throws MojoExecutionException
    {
        try {
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir(directory.toFile());
            scanner.setIncludes(patternsOrDefault(includes).toArray(String[]::new));
            if (excludes != null && !excludes.isEmpty()) {
                scanner.setExcludes(excludes.toArray(String[]::new));
            }
            scanner.addDefaultExcludes();
            scanner.scan();

            return Arrays.stream(scanner.getIncludedFiles())
                    .map(directory::resolve)
                    .sorted()
                    .toList();
        }
        catch (IllegalStateException e) {
            throw new MojoExecutionException("Error scanning directory: " + directory, e);
        }
    }

    protected final <T> List<T> processFiles(List<Path> files, String operation, FileProcessor<T> processor)
            throws MojoExecutionException
    {
        if (files.isEmpty()) {
            return List.of();
        }
        if (!parallel || files.size() == 1) {
            List<T> results = new ArrayList<>(files.size());
            for (Path file : files) {
                results.add(processFile(file, operation, processor));
            }
            return results;
        }
        try {
            return files.parallelStream()
                    .map(file -> processFile(file, operation, processor))
                    .toList();
        }
        catch (RuntimeException e) {
            Throwable cause = unwrap(e);
            if (cause instanceof FileProcessingException fileProcessingException) {
                throw fileProcessingException.mojoExecutionException();
            }
            throw new MojoExecutionException("Error while %s files".formatted(operation), cause);
        }
    }

    private static <T> T processFile(Path file, String operation, FileProcessor<T> processor)
    {
        try {
            return processor.process(file);
        }
        catch (FileProcessingException e) {
            throw e;
        }
        catch (IOException e) {
            throw new FileProcessingException(new MojoExecutionException("Error %s file: %s".formatted(operation, file), e));
        }
        catch (RuntimeException e) {
            throw new FileProcessingException(new MojoExecutionException("Internal formatter error while %s file: %s".formatted(operation, file), e));
        }
    }

    private static Throwable unwrap(Throwable throwable)
    {
        Throwable current = throwable;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static Set<String> patternsOrDefault(Set<String> patterns)
    {
        if (patterns == null || patterns.isEmpty()) {
            return Set.of("**/*.java");
        }
        return patterns;
    }

    private static void addConfiguredOrDefaultDirectories(List<Path> directories, List<String> configuredDirectories, List<?> projectRoots)
    {
        if (configuredDirectories != null && !configuredDirectories.isEmpty()) {
            configuredDirectories.stream()
                    .map(Path::of)
                    .forEach(directories::add);
            return;
        }

        if (projectRoots != null) {
            projectRoots.stream()
                    .map(Object::toString)
                    .map(Path::of)
                    .forEach(directories::add);
        }
    }

    @FunctionalInterface
    protected interface FileProcessor<T>
    {
        T process(Path file)
                throws IOException;
    }

    private static final class FileProcessingException
            extends RuntimeException
    {
        private final MojoExecutionException mojoExecutionException;

        private FileProcessingException(MojoExecutionException mojoExecutionException)
        {
            super(mojoExecutionException);
            this.mojoExecutionException = mojoExecutionException;
        }

        private MojoExecutionException mojoExecutionException()
        {
            return mojoExecutionException;
        }
    }
}
