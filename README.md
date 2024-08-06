<div align="center">
<h1>Plugin Modernizer Tool</h1>
<h3>
Using OpenRewrite Recipes for Plugin Modernization or Automation Plugin Build Metadata Updates
</h3>
</div>

[![Build Status](https://ci.jenkins.io/job/Tools/job/plugin-modernizer-tool/job/main/badge/icon)](https://ci.jenkins.io/job/Tools/job/plugin-modernizer-tool/job/main/)
[![Coverage](https://ci.jenkins.io/job/Tools/job/plugin-modernizer-tool/job/main/badge/icon?status=${instructionCoverage}&subject=coverage&color=${colorInstructionCoverage})](https://ci.jenkins.io/job/Tools/job/plugin-modernizer-tool/job/main)
[![LOC](https://ci.jenkins.io/job/Tools/job/plugin-modernizer-tool/job/main/badge/icon?job=test&status=${lineOfCode}&subject=line%20of%20code&color=blue)](https://ci.jenkins.io/job/Tools/job/plugin-modernizer-tool/job/main)
[![GitHub license](https://img.shields.io/github/license/jenkinsci/plugin-modernizer-tool)](https://github.com/jenkinsci/plugin-modernizer-tool/blob/main/LICENSE)

> [!WARNING]
> This tool is currently in development. Please avoid opening issues until the end of the GSoC 2024 period.
>
> It's configuration and APIs might change in future releases

## About

Plugin Modernizer is a generic CLI tool designed to automate the modernization of Jenkins plugins. It utilizes OpenRewrite recipes, JDOM, and refaster to apply transformations to the plugin, validating the applied transformations and creating pull requests with the results.

Learn more at [this project page](https://www.jenkins.io/projects/gsoc/2024/projects/using-openrewrite-recipes-for-plugin-modernization-or-automation-plugin-build-metadata-updates/).

## Usage

- [Getting Started](#getting-started)
    - [Requirements to build](#requirements-to-build)
    - [Build](#build)
    - [Using the tool](#using-the-tool)
    - [Setup](#setup)
- [CLI Options](#cli-options)
- [Plugin Input Format](#plugin-input-format)
    - [Plugin option](#plugin-option)
    - [Plugin file option](#plugin-file-option)
- [Configuring Environmental Variables](#configuring-environmental-variables)
- [Examples](#examples)
    - [without dry-run](#without-dry-run)
    - [with dry-run](#with-dry-run)
    - [with export-datatables](#with-export-datatables)

## Getting Started

> [!Note]
> Releases are paused until the end of the GSoC period. To use the latest version of the tool, clone this project locally and build it using the following commands.

### Requirements to build
- Maven version 3.9.7 or later, or mvnd
- Java 17 or Java 21

### Build

```shell
mvn clean install
```

### Using the tool

After building, the JAR file will be available at [plugin-modernizer-cli/target/jenkins-plugin-modernizer-999999-SNAPSHOT.jar](plugin-modernizer-cli/target/jenkins-plugin-modernizer-999999-SNAPSHOT.jar)

```shell
java -jar plugin-modernizer-cli/target/jenkins-plugin-modernizer-999999-SNAPSHOT.jar --help
```

### Setup
This tool requires forking repositories from GitHub, so you need to set the GitHub token and GitHub owner as environment variables.
Use either `GH_TOKEN` or `GITHUB_TOKEN` for the GitHub token, and either `GH_OWNER` or `GITHUB_OWNER` for the GitHub owner.
Alternatively, you can pass the GitHub owner through the CLI option `-g` or `--github-owner`.

> [!Note]
> The GitHub owner can be either a personal account or an organization.
>

## CLI Options
- `--plugins` or `-p`: (optional) Name(s) of plugin directory cloned inside the `test-plugins` directory.

- `--recipes` or `-r`: (required) Name(s) of recipes to apply to the plugins.

- `--plugin-file` or `-f`: (optional) Path to the text file that contains a list of plugins. (see example [plugin file](docs/example-plugins.txt))

- `--list-recipes` or `-l`: (optional) Displays the list of available recipes.

- `--dry-run` or `-n`: (optional) Enables dry run mode, generating patch files instead of applying changes. The patch files will be generated at `target/rewrite/rewrite.patch` inside the plugin directory if any change is made.

- `--skip-push` (optional) Skips pushing changes to the remote repository. Always enabled in dry-run mode.

- `--skip-pull-request` (optional) Skips creating pull requests in the remote repository. Always enabled in dry-run mode.

- `--clean-local-data` (optional) Deletes the local plugin directory before and after the process is completed.

- `--clean-forks` (optional) Remove forked repositories before and after the modernization process. Might cause data loss if you have other changes pushed on those forks. Forks with open pull request targeting original repo are not removed to prevent closing unmerged pull requests.

- `--export-datatables` or `-e`: (optional) Creates a report or summary of the changes made through OpenRewrite in CSV format. The report will be generated at `target/rewrite/datatables` inside the plugin directory.

- `--debug` or `-d`: (optional) Enables debug mode.

- `--jenkins-update-center`: (optional) Sets main update center; will override JENKINS_UC environment variable. If not set via CLI option or environment variable, will default to url in [properties file](plugin-modernizer-core/src/main/resources/update_center.properties)

- `--cache-path` or `-c`: (optional) Custom path to the cache directory. Defaults to `${user.home}/.cache/jenkins-plugin-modernizer-cli`.

- `--github-owner` or `-g`: (optional) GitHub owner for forked repositories. Can also be set via the environment variable `GH_OWNER` or `GITHUB_OWNER`.

- `--maven-home` or `-m`: (optional) Path to the Maven home directory. Required if both `MAVEN_HOME` and `M2_HOME` environment variables are not set. The minimum required version is 3.9.7.

- `--source-java-major-version`: (optional) Java major version to use before applying the recipes. Defaults to 8.

- `--target-java-major-version`: (optional) Java major version to use to apply the recipes and the plugin after applying the recipes. Defaults to 17.

- `--version` or `-v`: (optional) Displays the version of the Plugin Modernizer tool.

## Plugin Input Format

Plugins can be passed to the CLI tool in two ways:
- Plugin option
- Plugin file option

### Plugin option

Pass the plugin names directly using the `-p` or `--plugins option`. The expected input format for plugins is `artifact ID`.

```shell
java -jar plugin-modernizer-cli/target/jenkins-plugin-modernizer-999999-SNAPSHOT.jar --plugins git,git-client,jobcacher --recipes AddPluginsBom,AddCodeOwner
```
Here, `git`, `git-client`, and `jobcacher` are plugin artifact IDs (also known as plugin names), while `AddPluginsBom` and `AddCodeOwners` are recipe names. For more details about available recipes, refer to the [recipe_data.yaml](plugin-modernizer-core/src/main/resources/recipe_data.yaml) file.

### Plugin file option

Pass the path to a file that contains plugin names. The expected input format for plugins in the .txt file is `artifact ID` or `artifact ID:version`.
See example [plugin file](docs/example-plugins.txt)

```shell
java -jar plugin-modernizer-cli/target/jenkins-plugin-modernizer-999999-SNAPSHOT.jar --plugin-file path/to/plugin-file --recipes AddPluginsBom,AddCodeOwner
```

## Configuring Environmental Variables
- `GITHUB_TOKEN` or `GH_TOKEN`: (required) GitHub Token.

- `GITHUB_OWNER` or `GH_OWNER`: (required) GitHub username or organization name. Can also be passed through the CLI option `-g` or `--github-owner`.

- `JENKINS_UC`: (optional) Update Center URL. Can also be passed through the CLI option `--jenkins-update-center`.

- `MAVEN_HOME` or `M2_HOME`: (required) Path to Maven home directory. Can also be passed through the CLI option `-m` or `--maven-home`.

- `CACHE_DIR`: (optional) Path to cache directory. Can also be passed through the CLI option `-c` or `--cache-path`.

## Examples

### without dry-run

```shell
java -jar plugin-modernizer-cli/target/jenkins-plugin-modernizer-999999-SNAPSHOT.jar --plugins git,git-client,jobcacher --recipes AddPluginsBom,AddCodeOwner
```
The above command creates pull requests in the respective remote repositories after applying the changes.

### with dry-run

```shell
java -jar plugin-modernizer-cli/target/jenkins-plugin-modernizer-999999-SNAPSHOT.jar --plugins git,git-client,jobcacher --recipes AddPluginsBom,AddCodeOwner --dry-run
```

The above command generates patch files instead of applying changes directly. These patch files are saved in `/target/rewrite/rewrite.patch` inside each plugin directory. No pull requests will be created.

> [!Note]
> Enable dry-run to avoid opening pull requests in the remote repositories.

### with export-datatables

```shell
java -jar plugin-modernizer-cli/target/jenkins-plugin-modernizer-999999-SNAPSHOT.jar --plugins git,git-client,jobcacher --recipes AddPluginsBom,AddCodeOwner --export-datatables
```

The above command creates a report of the changes made through OpenRewrite in csv format. The report will be generated in `target/rewrite/datatables` inside the plugin directory.

See example generated files:
- [RecipeRunStats.csv](docs/example-rewrite-datatable/org.openrewrite.table.RecipeRunStats.csv)
- [SourcesFileResults.csv](docs/example-rewrite-datatable/org.openrewrite.table.SourcesFileResults.csv)

More about [Openrewrite Data Tables](https://docs.openrewrite.org/running-recipes/data-tables)

## Reproducibility

The maven build should be reproducible

See

- https://maven.apache.org/guides/mini/guide-reproducible-builds.html
- https://reproducible-builds.org
- https://github.com/jenkinsci/incrementals-tools/issues/103 for the support on the Jenkins incrementals tools and [JEP-229](https://github.com/jenkinsci/jep/blob/master/jep/229/README.adoc)

[Reproducible Builds](https://reproducible-builds.org/) for more information.

Ensure the repository is clean before running the following commands (otherwise you can pass the `-Dignore.dirty` flag to the maven command).

```shell
mvn -Dset.changelist clean install
mvn -Dset.changelist -Dreference.repo=central clean verify artifact:compare
```

The property `project.build.outputTimestamp` will be set with the timestamp of the latest commit.

If you are using a mirror for `central` you should adapt the `reference.repo` property accordingly to match the id of the mirror in your `settings.xml`.

## References

- [GSoC 2024 Project Proposal](https://docs.google.com/document/d/1e1QkprPN6fLpFXk_QqBUQlJhZrAl9RvXbOXOiJ-gAuY/edit?usp=sharing)
- [Project Slack Channel](https://cdeliveryfdn.slack.com/archives/C071YTZ807)
- [OpenRewrite Jenkins Recipes](https://docs.openrewrite.org/recipes/jenkins/)
- [OpenRewrite LST](https://docs.openrewrite.org/concepts-explanations/lossless-semantic-trees)
