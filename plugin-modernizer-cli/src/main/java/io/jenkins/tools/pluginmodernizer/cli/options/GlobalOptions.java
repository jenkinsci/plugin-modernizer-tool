package io.jenkins.tools.pluginmodernizer.cli.options;

import io.jenkins.tools.pluginmodernizer.cli.VersionProvider;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import java.net.URL;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class GlobalOptions {

    /**
     * Logger
     */
    private static Logger LOG = LoggerFactory.getLogger(GlobalOptions.class);

    @CommandLine.Option(
            names = {"-d", "--debug"},
            description = "Enable debug logging.")
    public boolean debug;

    @CommandLine.Option(
            names = {"-g", "--github-owner"},
            description = "GitHub owner for forked repositories.")
    private String githubOwner = Settings.GITHUB_OWNER;

    @CommandLine.Option(
            names = {"--github-app-id"},
            description =
                    "GitHub App ID. If set you will need to set GH_APP_CLIENT_ID, GH_APP_CLIENT_SECRET, GH_APP_PRIVATE_KEY_FILE as environment variables to use JWT authentication. The app installation must be done on the given github owner (personal or organization).")
    public Long githubAppId;

    @CommandLine.Option(
            names = {"--github-app-source-installation-id"},
            description =
                    "GitHub App Installation ID for the source repositories. If set, the app installation must be done on the given github owner (personal or organization).")
    public Long githubAppSourceInstallationId;

    @CommandLine.Option(
            names = {"--github-app-target-installation-id"},
            description =
                    "GitHub App Installation ID for the target repositories. If set, the app installation must be done on the given github owner (personal or organization).")
    public Long githubAppTargetInstallationId;

    @CommandLine.Option(
            names = {"--draft"},
            description = "Open a draft pull request.")
    public boolean draft;

    @CommandLine.Option(
            names = {"--skip-push"},
            description = "Skip pushing changes to the forked repositories. Always true if --dry-run is set.")
    public boolean skipPush;

    @CommandLine.Option(
            names = {"--skip-build"},
            description = "Skip building the plugins before and after modernization.")
    public boolean skipBuild;

    @CommandLine.Option(
            names = {"--skip-pull-request"},
            description = "Skip creating pull requests but pull changes to the fork. Always true if --dry-run is set.")
    public boolean skipPullRequest;

    @CommandLine.Option(
            names = {"--clean-local-data"},
            description = "Remove local plugin data before and after the modernization process.")
    public boolean removeLocalData;

    @CommandLine.Option(
            names = {"--clean-forks"},
            description =
                    "Remove forked repositories before and after the modernization process. Might cause data loss if you have other changes pushed on those forks. Forks with open pull request targeting original repo are not removed to prevent closing unmerged pull requests.")
    public boolean removeForks;

    @CommandLine.Option(
            names = {"-e", "--export-datatables"},
            description = "Creates a report or summary of the changes made through OpenRewrite.")
    public boolean exportDatatables;

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

    @CommandLine.Option(
            names = {"-c", "--cache-path"},
            description = "Path to the cache directory.")
    public Path cachePath = Settings.DEFAULT_CACHE_PATH;

    @CommandLine.Option(
            names = {"-m", "--maven-home"},
            description = "Path to the Maven Home directory.")
    public Path mavenHome = Settings.DEFAULT_MAVEN_HOME;

    /**
     * Create a new config build for the global options
     * @return Config.Builder for global options
     */
    public Config.Builder getBuilderForOptions() {
        Config.DEBUG = debug;
        return Config.builder()
                .withVersion(getVersion())
                .withGitHubOwner(githubOwner)
                .withGitHubAppId(githubAppId)
                .withGitHubAppSourceInstallationId(githubAppSourceInstallationId)
                .withGitHubAppTargetInstallationId(githubAppTargetInstallationId)
                .withSkipPush(skipPush)
                .withSkipBuild(skipBuild)
                .withSkipPullRequest(skipPullRequest)
                .withDraft(draft)
                .withRemoveLocalData(removeLocalData)
                .withRemoveForks(removeForks)
                .withExportDatatables(exportDatatables)
                .withJenkinsUpdateCenter(jenkinsUpdateCenter)
                .withJenkinsPluginVersions(jenkinsPluginVersions)
                .withPluginHealthScore(pluginHealthScore)
                .withPluginStatsInstallations(jenkinsPluginsStatsInstallationsUrl)
                .withGithubApiUrl(githubApiUrl)
                .withCachePath(
                        !cachePath.endsWith(Settings.CACHE_SUBDIR)
                                ? cachePath.resolve(Settings.CACHE_SUBDIR)
                                : cachePath)
                .withMavenHome(mavenHome);
    }

    /**
     * Get the version from the pom.properties
     * @return Version string
     */
    private String getVersion() {
        try {
            return new VersionProvider().getMavenVersion();
        } catch (Exception e) {
            LOG.error("Error getting version from pom.properties", e);
            return "unknown";
        }
    }
}
