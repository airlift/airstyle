# Airstyle

Java code formatter and Maven plugin for Airlift code style.

The formatter produces output according to the [formatter style](docs/FORMATTER_STYLE.md),
which starts from the [Airlift.xml](airstyle-core/src/main/resources/Airlift.xml) IntelliJ code style configuration
and extends it with additional rules, including wrapping, imports, comments, Javadoc, and Checkstyle compatibility.

## Goals

The Airstyle plugin has two goals:

* `check` verifies that Java sources are properly formatted, and fails the build if any files need formatting.
* `format` rewrites Java sources in place to be properly formatted.

## Usage

Add the plugin to your build to run `check` in the default `validate` phase:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.airlift</groupId>
            <artifactId>airstyle-maven-plugin</artifactId>
            <version><!-- current plugin version --></version>
            <executions>
                <execution>
                    <goals>
                        <goal>check</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

After configuring the plugin, run your normal build to execute `check`:

```bash
mvn validate
```

Rewrite source files with the configured plugin:

```bash
mvn airstyle:format
```

If you just want to try the plugin without configuring it, you can run the goals directly:

```bash
mvn io.airlift:airstyle-maven-plugin:RELEASE:check
mvn io.airlift:airstyle-maven-plugin:RELEASE:format
```

## Parameters

### Common Parameters

These parameters are shared by `check` and `format`:

| Parameter                 | Description                                                                                                                          |
|---------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| `<sourceDirectories>`     | Source directories to process.<br>**Default:** compile source roots<br>**User Property:** `airstyle.sourceDirectories`               |
| `<testSourceDirectories>` | Test source directories to process.<br>**Default:** test compile source roots<br>**User Property:** `airstyle.testSourceDirectories` |
| `<includes>`              | Ant-style patterns for included files.<br>**Default:** `**/*.java`<br>**User Property:** `airstyle.includes`                         |
| `<excludes>`              | Ant-style patterns for excluded files.<br>**Default:** no exclusions<br>**User Property:** `airstyle.excludes`                       |
| `<includeTestSources>`    | Include test source roots.<br>**Default:** `true`<br>**User Property:** `airstyle.includeTestSources`                                |
| `<parallel>`              | Process files in parallel.<br>**Default:** `true`<br>**User Property:** `airstyle.parallel`                                          |
| `<skip>`                  | Skip goal execution.<br>**Default:** `false`<br>**User Property:** `airstyle.skip`                                                   |

### `check` Parameters

| Parameter | Description |
|---|---|
| `<failOnViolation>` | Fail the build when files needing formatting are found.<br>**Default:** `true`<br>**User Property:** `airstyle.failOnViolation` |

## License

See [LICENSE](LICENSE). Airstyle is licensed under the Apache License 2.0.
