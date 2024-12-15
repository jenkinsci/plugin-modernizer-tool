package io.jenkins.tools.pluginmodernizer.cli.options;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import picocli.CommandLine;

/**
 * Option for GitHub integration
 */
@CommandLine.Command(
        synopsisHeading = "%nUsage:%n",
        descriptionHeading = "%nDescription:%n",
        parameterListHeading = "%nParameters:%n",
        optionListHeading = "%nOptions:%n",
        commandListHeading = "%nCommands:%n")
public class GitHubOptions implements IOption {

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

    /**
     * Create a new config build for the global options
     */
    @Override
    public void config(Config.Builder builder) {
        builder.withGitHubOwner(githubOwner)
                .withGitHubAppId(githubAppId)
                .withGitHubAppSourceInstallationId(githubAppSourceInstallationId)
                .withGitHubAppTargetInstallationId(githubAppTargetInstallationId);
    }
}
