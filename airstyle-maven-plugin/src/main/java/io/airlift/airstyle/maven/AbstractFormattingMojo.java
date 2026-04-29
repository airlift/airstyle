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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

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
}
