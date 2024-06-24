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

> [!NOTE]
> Currently, this tool does not support checking out plugins from remote repositories. Therefore, one must manually check out the plugin repository into a new directory named `test-plugins`.

### Build

```shell
mvn clean install
```

Execute the following command outside the `test-plugins` directory:

```shell
java -jar plugin-modernizer-cli/target/jenkins-plugin-modernizer-999999-SNAPSHOT.jar --plugins plugin1,plugin2 --recipes AddPluginsBom,AddCodeOwner
```

Here, `plugin1` and `plugin2` are the names of plugin directories, and `AddPluginsBom` and `AddCodeOwners` are recipe names.  For more details about available recipes, refer to the [recipe_data.yaml](plugin-modernizer-core/src/main/resources/recipe_data.yaml) file.

### CLI Options
- `--plugins` or `-p`: (required) Name(s) of plugin directory cloned inside the `test-plugins` directory.

- `--recipes` or `-r`: (required) Name(s) of recipes to apply to the plugins.

- `--dry-run` or `-n`: (optional) Enables dry run mode, generating patch files instead of applying changes.

- `--debug` or `-d`: (optional) Enables debug mode.

- `--cache-path` or `-c`: (optional) Custom path to the cache directory

- `--maven-home` or `-m`: (optional) Path to the Maven home directory. Required if both `MAVEN_HOME` and `M2_HOME` environment variables are not set.

- `--version` or `-v`: (optional) Displays the version of the Plugin Modernizer tool.

## References

- [GSoC 2024 Project Proposal](https://docs.google.com/document/d/1e1QkprPN6fLpFXk_QqBUQlJhZrAl9RvXbOXOiJ-gAuY/edit?usp=sharing)
- [Project Slack Channel](https://cdeliveryfdn.slack.com/archives/C071YTZ807)
- [OpenRewrite Jenkins Recipes](https://docs.openrewrite.org/recipes/jenkins/)
- [OpenRewrite LST](https://docs.openrewrite.org/concepts-explanations/lossless-semantic-trees)
