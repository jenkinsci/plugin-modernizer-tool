package io.jenkins.tools.pluginmodernizer.cli.options;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import java.net.URL;
import picocli.CommandLine;

/**
 * Option for the tool environment configuration (like URLS, paths, etc)
 */
@CommandLine.Command(
        synopsisHeading = "%nUsage:%n",
        descriptionHeading = "%nDescription:%n",
        parameterListHeading = "%nParameters:%n",
        optionListHeading = "%nOptions:%n",
        commandListHeading = "%nCommands:%n")
public class EnvOptions implements IOption {

    @CommandLine.Option(
            names = "--jenkins-update-center",
            description =
                    "Sets main update center; will override JENKINS_UC environment variable. If not set via CLI option or environment variable, will use default update center url.")
    public URL jenkinsUpdateCenter = Settings.DEFAULT_UPDATE_CENTER_URL;

    @CommandLine.Option(
            names = "--jenkins-plugin-info",
            description =
                    "Sets jenkins plugin version; will override JENKINS_PLUGIN_INFO environment variable. If not set via CLI option or environment variable, will use default plugin info")
    public URL jenkinsPluginVersions = Settings.DEFAULT_PLUGIN_VERSIONS;

    @CommandLine.Option(
            names = "--plugin-health-score",
            description =
                    "Sets the plugin health score URL; will override JENKINS_PHS environment variable. If not set via CLI option or environment variable, will use default health score url.")
    public URL pluginHealthScore = Settings.DEFAULT_HEALTH_SCORE_URL;

    @CommandLine.Option(
            names = "--jenkins-plugins-stats-installations-url",
            description =
                    "Sets the Jenkins stats top plugins URL; will override JENKINS_PLUGINS_STATS_INSTALLATIONS_URL environment variable. If not set via CLI option or environment variable, will use default Jenkins stats top plugins url.")
    public URL jenkinsPluginsStatsInstallationsUrl = Settings.DEFAULT_PLUGINS_STATS_INSTALLATIONS_URL;

    @CommandLine.Option(
            names = {"--github-api-url"},
            description = "GitHub API URL. Default to https://api.github.com")
    public URL githubApiUrl = Settings.GITHUB_API_URL;

    @Override
    public void config(Config.Builder builder) {
        builder.withJenkinsUpdateCenter(jenkinsUpdateCenter)
                .withJenkinsPluginVersions(jenkinsPluginVersions)
                .withPluginHealthScore(pluginHealthScore)
                .withPluginStatsInstallations(jenkinsPluginsStatsInstallationsUrl)
                .withGithubApiUrl(githubApiUrl);
    }
}
