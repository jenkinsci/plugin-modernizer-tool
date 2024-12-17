package io.jenkins.tools.pluginmodernizer.cli.options;

import io.jenkins.tools.pluginmodernizer.cli.VersionProvider;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import java.nio.file.Path;
import picocli.CommandLine;

/**
 * Global options for all commands
 * TODO: Need cleanup and move some option to specific subcommand
 */
@CommandLine.Command(
        synopsisHeading = "%nUsage:%n",
        descriptionHeading = "%nDescription:%n",
        parameterListHeading = "%nParameters:%n",
        optionListHeading = "%nOptions:%n",
        commandListHeading = "%nCommands:%n")
public class GlobalOptions implements IOption {

    @CommandLine.Option(
            names = {"--debug"},
            description = "Enable debug logging.")
    public boolean debug;

    @CommandLine.Option(
            names = {"--cache-path"},
            description = "Path to the cache directory.")
    public Path cachePath = Settings.DEFAULT_CACHE_PATH;

    @CommandLine.Option(
            names = {"--maven-home"},
            description = "Path to the Maven Home directory.")
    public Path mavenHome = Settings.DEFAULT_MAVEN_HOME;

    @CommandLine.Option(
            names = {"--maven-local-repo"},
            description = "Path to the Maven local repository.")
    public Path mavenLocalRepo = Settings.DEFAULT_MAVEN_LOCAL_REPO;

    /**
     * Create a new config build for the global options
     */
    @Override
    public void config(Config.Builder builder) {
        Config.DEBUG = debug;
        builder.withVersion(getVersion())
                .withCachePath(
                        !cachePath.endsWith(Settings.CACHE_SUBDIR)
                                ? cachePath.resolve(Settings.CACHE_SUBDIR)
                                : cachePath)
                .withMavenHome(mavenHome)
                .withMavenLocalRepo(mavenLocalRepo);
    }

    /**
     * Get the version from the pom.properties
     * @return Version string
     */
    private String getVersion() {
        try {
            return new VersionProvider().getMavenVersion();
        } catch (Exception e) {
            throw new ModernizerException("Failed to get version from pom.properties", e);
        }
    }
}
