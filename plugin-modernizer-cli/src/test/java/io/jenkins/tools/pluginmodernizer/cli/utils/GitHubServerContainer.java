package io.jenkins.tools.pluginmodernizer.cli.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sparsick.testcontainers.gitserver.GitServerVersions;
import com.github.sparsick.testcontainers.gitserver.plain.GitServerContainer;
import com.github.sparsick.testcontainers.gitserver.plain.SshIdentity;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import io.jenkins.tools.pluginmodernizer.core.model.HealthScoreData;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import io.jenkins.tools.pluginmodernizer.core.model.PluginVersionData;
import io.jenkins.tools.pluginmodernizer.core.model.UpdateCenterData;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.ExecConfig;
import org.testcontainers.utility.MountableFile;

/**
 * The GitHub server container accessible by SSH
 */
public class GitHubServerContainer extends GitServerContainer {

    /**
     * The logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(GitHubServerContainer.class);

    /**
     * The plugin name
     */
    private final String plugin;

    /**
     * The WireMock runtime info
     */
    private final WireMockRuntimeInfo wmRuntimeInfo;

    /**
     * The branch
     */
    private final String branch;

    /**
     * The path containers the SSH
     */
    private final Path keysPath;

    /**
     * Create a GitHub server container
     */
    public GitHubServerContainer(WireMockRuntimeInfo wmRuntimeInfo, Path keysPath, String plugin, String branch) {
        super(GitServerVersions.V2_45.getDockerImageName());
        this.plugin = plugin;
        this.wmRuntimeInfo = wmRuntimeInfo;
        this.branch = branch;
        this.keysPath = keysPath;
        withSshKeyAuth();
        withGitRepo(plugin);
    }

    @Override
    public void start() {
        super.start();
        setupGitContainer();
        setupMock();
    }

    /**
     * Setup mocks for integration tests with WireMock and Testcontainer git
     */
    private void setupMock() {

        // Setup responses
        PluginStatsApiResponse pluginStatsApiResponse = new PluginStatsApiResponse(Map.of(plugin, 1));
        UpdateCenterApiResponse updateCenterApiResponse = new UpdateCenterApiResponse(
                Map.of(
                        plugin,
                        new UpdateCenterData.UpdateCenterPlugin(
                                plugin,
                                "1",
                                this.getGitRepoURIAsSSH().toString(),
                                branch,
                                "io.jenkins.plugins:%s".formatted(plugin),
                                null)),
                Map.of());
        PluginVersionsApiResponse pluginVersionsApiResponse = new PluginVersionsApiResponse(
                Map.of(plugin, Map.of("1", new PluginVersionData.PluginVersionPlugin(plugin, "1"))));
        HealthScoreApiResponse pluginHealthScoreApiResponse =
                new HealthScoreApiResponse(Map.of(plugin, new HealthScoreData.HealthScorePlugin(100d)));

        // Setup mocks
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        wireMock.register(WireMock.get(WireMock.urlEqualTo("/api/user"))
                .willReturn(WireMock.jsonResponse(new UserApiResponse("fake-owner", "User"), 200)));
        wireMock.register(WireMock.get(WireMock.urlEqualTo("/api/repos/jenkinsci/%s".formatted(plugin)))
                .willReturn(WireMock.jsonResponse(
                        new RepoApiResponse(
                                "main",
                                "%s/%s/%s".formatted(wmRuntimeInfo.getHttpBaseUrl(), "fake-owner", plugin),
                                this.getGitRepoURIAsSSH().toString()),
                        200)));
        wireMock.register(WireMock.get(WireMock.urlEqualTo("/update-center.json"))
                .willReturn(WireMock.jsonResponse(updateCenterApiResponse, 200)));
        wireMock.register(WireMock.get(WireMock.urlEqualTo("/plugin-versions.json"))
                .willReturn(WireMock.jsonResponse(pluginVersionsApiResponse, 200)));
        wireMock.register(WireMock.get(WireMock.urlEqualTo("/scores"))
                .willReturn(WireMock.jsonResponse(pluginHealthScoreApiResponse, 200)));
        wireMock.register(WireMock.get(WireMock.urlEqualTo("/jenkins-stats/svg/202406-plugins.csv"))
                .willReturn(WireMock.jsonResponse(pluginStatsApiResponse, 200)));

        // Setup SSH key to access the container
        SshIdentity sshIdentity = this.getSshClientIdentity();
        byte[] privateKey = sshIdentity.getPrivateKey();
        try {
            Files.write(keysPath.resolve(plugin), privateKey);
            LOG.info("Private key: {}", keysPath.resolve(plugin));
        } catch (IOException e) {
            throw new ModernizerException("Error writing private key", e);
        }
    }

    /**
     * Run a command on the git container
     * @param command The command
     */
    private void runContainerGitCommand(String user, String command) {
        try {
            Container.ExecResult containerResult = this.execInContainer(ExecConfig.builder()
                    .user(user)
                    .command(command.split(" ")) // We assume no command contains space
                    .build());
            LOG.debug("Running command on container: {}", command);
            LOG.debug("Stdout: {}", containerResult.getStdout());
            LOG.debug("Stderr: {}", containerResult.getStderr());

            assertEquals(
                    0,
                    containerResult.getExitCode(),
                    "Command '%s' failed with status code '%s' and output '%s' and error '%s'"
                            .formatted(
                                    command,
                                    containerResult.getExitCode(),
                                    containerResult.getStdout(),
                                    containerResult.getStderr()));
        } catch (IOException | InterruptedException e) {
            throw new ModernizerException("Error running command: %s".formatted(command), e);
        }
    }

    /**
     * Setup the git container
     */
    private void setupGitContainer() {
        String gitRepoPath = String.format("/srv/git/%s.git", plugin);
        String sourceDirectory = "src/test/resources/%s".formatted(plugin);
        assertTrue(
                Files.exists(Path.of(sourceDirectory)),
                "Source directory %s does not exist".formatted(sourceDirectory));
        runContainerGitCommand("root", "rm -Rf %s".formatted(gitRepoPath));
        LOG.debug("Copying %s to %s".formatted(sourceDirectory, gitRepoPath));
        this.copyFileToContainer(MountableFile.forHostPath(sourceDirectory.formatted(plugin)), gitRepoPath);
        LOG.debug("Copied %s to %s".formatted(sourceDirectory, gitRepoPath));
        runContainerGitCommand("root", "chown -R git:git %s".formatted(gitRepoPath));
        runContainerGitCommand("git", "ls -la %s".formatted(gitRepoPath));
        runContainerGitCommand("git", "git config --global init.defaultBranch main");
        runContainerGitCommand("git", "git config --global --add safe.directory %s".formatted(gitRepoPath));
        runContainerGitCommand("git", "git config --global user.name Fake");
        runContainerGitCommand("git", "git config --global user.email fake-email@example.com");
        runContainerGitCommand("git", "git init %s".formatted(gitRepoPath));
        runContainerGitCommand("git", "git -C %s status".formatted(gitRepoPath));
        runContainerGitCommand("git", "git -C %s add .".formatted(gitRepoPath));
        runContainerGitCommand("git", "git -C %s status".formatted(gitRepoPath));
        runContainerGitCommand("git", "git -C %s commit -m init".formatted(gitRepoPath));
        runContainerGitCommand("git", "git -C %s status".formatted(gitRepoPath));
    }

    /**
     * Login API response
     */
    public record UserApiResponse(String login, String type) {}

    /**
     * Setup the mock
     * @param ssh_url the SSH URL
     */
    public record RepoApiResponse(String default_branch, String clone_url, String ssh_url) {}

    /**
     * Setup the mock
     * @param plugins
     */
    public record PluginStatsApiResponse(Map<String, Integer> plugins) {}

    /**
     * Update center API response
     * @param plugins the plugins
     * @param deprecations the deprecations
     */
    public record UpdateCenterApiResponse(
            Map<String, UpdateCenterData.UpdateCenterPlugin> plugins,
            Map<String, UpdateCenterData.DeprecatedPlugin> deprecations) {}

    /**
     * Plugin versions API response
     * @param plugins the plugins
     */
    public record PluginVersionsApiResponse(Map<String, Map<String, PluginVersionData.PluginVersionPlugin>> plugins) {}

    /**
     * Health score API response
     * @param plugins the plugins
     */
    public record HealthScoreApiResponse(Map<String, HealthScoreData.HealthScorePlugin> plugins) {}
}
