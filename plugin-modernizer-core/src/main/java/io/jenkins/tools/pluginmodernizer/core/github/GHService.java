package io.jenkins.tools.pluginmodernizer.core.github;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.PluginProcessingException;
import io.jenkins.tools.pluginmodernizer.core.utils.JWTUtils;
import io.jenkins.tools.pluginmodernizer.core.utils.TemplateUtils;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.git.transport.GitSshdSessionFactory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.*;
import org.kohsuke.github.GHApp;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GHBranchSync;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "false positive")
public class GHService {

    private static final Logger LOG = LoggerFactory.getLogger(GHService.class);

    // TODO: Use unique branch name (with prefix ?) to avoid conflicts
    private static final String BRANCH_NAME = "plugin-modernizer-tool";

    @Inject
    private Config config;

    /**
     * The GitHub client
     */
    private GitHub github;

    /**
     * The GitHub App if connected by GitHub App
     */
    private GHApp app;

    /**
     * If the authentication is done using SSH key
     */
    private boolean sshKeyAuth = false;

    /**
     * Validate the configuration of the GHService
     */
    public void validate() {
        if (config.isFetchMetadataOnly()) {
            return;
        }
        setSshKeyAuth();
        if (Settings.GITHUB_TOKEN == null
                && (config.getGithubAppId() == null
                        || config.getGithubAppSourceInstallationId() == null
                        || config.getGithubAppTargetInstallationId() == null)) {
            throw new ModernizerException("Please set GH_TOKEN, GITHUB_TOKEN or configure GitHub app authentication.");
        }
        if (getGithubOwner() == null) {
            throw new ModernizerException(
                    "GitHub owner (username/organization) is not set. Please set GH_OWNER or GITHUB_OWNER environment variable. Or use --github-owner if running from CLI");
        }
        if (config.getGithubAppId() != null && config.getGithubAppSourceInstallationId() != null) {
            if (Settings.GITHUB_APP_PRIVATE_KEY_FILE == null) {
                throw new ModernizerException("GitHub App not configured. Please set GH_APP_PRIVATE_KEY_FILE");
            }
        }
    }

    public boolean isConnected() {
        return github != null;
    }

    /**
     * Connect to GitHub using the GitHub auth token
     */
    public void connect() {
        if (isConnected()) {
            return;
        }
        if (Settings.GITHUB_TOKEN == null
                && (config.getGithubAppId() == null
                        || config.getGithubAppSourceInstallationId() == null
                        || config.getGithubAppTargetInstallationId() == null)) {
            throw new ModernizerException("Please set GH_TOKEN, GITHUB_TOKEN or configure GitHub app authentication.");
        }
        try {

            // Connect with GitHub App
            if (config.getGithubAppId() != null
                    && config.getGithubAppSourceInstallationId() != null
                    && config.getGithubAppTargetInstallationId() != null) {
                LOG.debug("Connecting to GitHub using GitHub App...");
                LOG.debug("GitHub App ID: {}", config.getGithubAppId());
                LOG.debug("GitHub App Source Installation ID: {}", config.getGithubAppSourceInstallationId());
                LOG.debug("GitHub App Target Installation ID: {}", config.getGithubAppTargetInstallationId());
                LOG.debug("Private key file: {}", Settings.GITHUB_APP_PRIVATE_KEY_FILE);
                String jwtToken = JWTUtils.getJWT(config, Settings.GITHUB_APP_PRIVATE_KEY_FILE);

                // Get the GitHub App
                this.app = new GitHubBuilder().withJwtToken(jwtToken).build().getApp();
                GHAppInstallationToken appInstallationToken = this.app
                        .getInstallationById(config.getGithubAppSourceInstallationId())
                        .createToken()
                        .create();
                github = new GitHubBuilder()
                        .withEndpoint(config.getGithubApiUrl().toString())
                        .withAppInstallationToken(appInstallationToken.getToken())
                        .build();
                LOG.debug("Connected to GitHub using GitHub App");
            }
            // Connect with token
            else {
                LOG.debug("Connecting to GitHub using token...");
                github = new GitHubBuilder()
                        .withEndpoint(config.getGithubApiUrl().toString())
                        .withOAuthToken(Settings.GITHUB_TOKEN)
                        .build();
            }
            GHUser user = getCurrentUser();
            if (user == null) {
                throw new ModernizerException("Failed to get current user. Cannot use GitHub/SCM integration");
            }
            String email = getPrimaryEmail(user);
            if (email == null) {
                throw new ModernizerException(
                        "Email is not set in GitHub account. Please set email in GitHub account.");
            }
            LOG.debug("Connected to GitHub as {} <{}>", user.getName() != null ? user.getName() : user.getId(), email);

        } catch (IOException e) {
            throw new ModernizerException("Failed to connect to GitHub. Cannot use GitHub/SCM integration", e);
        }
        // Ensure to set up SSH client for Git operations
        setSshKeyAuth();
        if (sshKeyAuth) {
            try {
                SshClient client = SshClient.setUpDefaultClient();
                FileKeyPairProvider keyPairProvider =
                        new FileKeyPairProvider(Collections.singletonList(config.getSshPrivateKey()));
                client.setKeyIdentityProvider(keyPairProvider);
                GitSshdSessionFactory sshdFactory = new GitSshdSessionFactory(client);
                SshSessionFactory.setInstance(sshdFactory);
            } catch (Exception e) {
                throw new ModernizerException("Failed to set up SSH client for Git operations", e);
            }
        }
    }

    /**
     * Refresh the JWT token for the GitHub app. Only for GitHub App authentication
     * @param installationId The installation ID
     */
    public void refreshToken(Long installationId) {
        if (installationId == null) {
            LOG.debug("Installation ID is not set. Skipping token refresh");
            return;
        }
        if (github == null) {
            throw new ModernizerException("GitHub client must be connected.");
        }
        try {
            String jwtToken = JWTUtils.getJWT(config, Settings.GITHUB_APP_PRIVATE_KEY_FILE);
            GHApp app = new GitHubBuilder().withJwtToken(jwtToken).build().getApp();
            GHAppInstallationToken appInstallationToken =
                    app.getInstallationById(installationId).createToken().create();
            github = new GitHubBuilder()
                    .withAppInstallationToken(appInstallationToken.getToken())
                    .build();
            this.app = app;
            LOG.debug("Refreshed token for GitHub App installation ID {}", installationId);
        } catch (IOException e) {
            throw new ModernizerException("Failed to refresh token", e);
        }
    }

    /**
     * Get the repository object for a plugin
     * @param plugin The plugin to get the repository for
     * @return The GHRepository object
     */
    public GHRepository getRepository(Plugin plugin) {
        try {
            return github.getRepository(Settings.ORGANIZATION + "/" + plugin.getRepositoryName());
        } catch (IOException e) {
            throw new PluginProcessingException("Failed to get repository", e, plugin);
        }
    }

    /**
     * Get a plugin repository to the organization or personal account
     * @param plugin The plugin
     * @return The GHRepository object
     */
    public GHRepository getRepositoryFork(Plugin plugin) {
        if (config.isDryRun()) {
            throw new PluginProcessingException("Cannot get fork repository in dry-run mode", plugin);
        }
        try {
            return github.getRepository(getGithubOwner() + "/" + plugin.getRepositoryName());
        } catch (IOException e) {
            throw new PluginProcessingException("Failed to get repository", e, plugin);
        }
    }

    /**
     * Check if the plugin repository is forked to the organization or personal account
     * @param plugin The plugin to check
     * @return True if the repository is forked
     */
    public boolean isForked(Plugin plugin) {
        try {
            GHOrganization organization = getOrganization();
            if (organization != null) {
                return isRepositoryForked(organization, plugin.getRepositoryName());
            }
            return isRepositoryForked(plugin.getRepositoryName());
        } catch (IOException e) {
            throw new PluginProcessingException("Failed to check if repository is forked", e, plugin);
        }
    }

    /**
     * Check if the plugin repository is archived
     * @param plugin The plugin to check
     * @return True if the repository is archived
     */
    public boolean isArchived(Plugin plugin) {
        return plugin.getRemoteRepository(this).isArchived();
    }

    /**
     * Fork a plugin repository to the organization or personal account
     * @param plugin The plugin to fork
     */
    public void fork(Plugin plugin) {
        if (config.isDryRun()) {
            LOG.info("Skipping forking plugin {} in dry-run mode", plugin);
            return;
        }
        if (config.isFetchMetadataOnly()) {
            LOG.info("Skipping forking plugin {} in fetch-metadata-only mode", plugin);
            return;
        }
        if (plugin.isArchived(this)) {
            LOG.info("Plugin {} is archived. Not forking", plugin);
            return;
        }
        LOG.info("Forking plugin {} locally from repo {}...", plugin, plugin.getRepositoryName());
        try {
            GHRepository fork = forkPlugin(plugin);
            LOG.debug("Forked repository: {}", fork.getHtmlUrl());
        } catch (IOException | InterruptedException e) {
            plugin.addError("Failed to fork the repository", e);
            plugin.raiseLastError();
        }
    }

    /**
     * Fork a plugin repository to the organization or personal account
     * @param plugin The plugin to fork
     * @throws IOException Forking the repository failed due to I/O error
     * @throws InterruptedException Forking the repository failed due to interruption
     */
    private GHRepository forkPlugin(Plugin plugin) throws IOException, InterruptedException {
        GHOrganization organization = getOrganization();
        GHRepository originalRepo = plugin.getRemoteRepository(this);
        if (organization != null) {
            if (isRepositoryForked(organization, originalRepo.getName())) {
                LOG.debug("Repository already forked to organization {}", organization.getLogin());
                GHRepository fork = getRepositoryFork(organization, originalRepo.getName());
                checkSameParentRepository(plugin, originalRepo, fork);
                return fork;
            } else {
                GHRepository fork = forkRepository(originalRepo, organization);
                Thread.sleep(5000); // Wait for the fork to be ready
                return fork;
            }
        } else {
            if (isRepositoryForked(originalRepo.getName())) {
                LOG.debug(
                        "Repository already forked to personal account {}",
                        getCurrentUser().getLogin());
                GHRepository fork = getRepositoryFork(originalRepo.getName());
                checkSameParentRepository(plugin, originalRepo, fork);
                return fork;
            } else {
                GHRepository fork = forkRepository(originalRepo);
                Thread.sleep(5000); // Wait for the fork to be ready
                return fork;
            }
        }
    }

    /**
     * Fork the repository
     * @param originalRepo The original repository to fork
     * @param organization The organization to fork the repository to. Can be null for personal account
     * @return The forked repository
     * @throws IOException If the fork operation failed
     * @throws InterruptedException If the fork operation was interrupted
     */
    private GHRepository forkRepository(GHRepository originalRepo, GHOrganization organization)
            throws IOException, InterruptedException {
        if (organization == null) {
            LOG.info(
                    "Forking the repository to personal account {}...",
                    getCurrentUser().getLogin());
            return originalRepo.fork();
        } else {
            LOG.info("Forking the repository to organisation {}...", organization.getLogin());
            return originalRepo.forkTo(organization);
        }
    }

    /**
     * Fork the repository to the personal account
     * @param originalRepo The original repository to fork
     * @return The forked repository
     * @throws IOException If the fork operation failed
     * @throws InterruptedException If the fork operation was interrupted
     */
    private GHRepository forkRepository(GHRepository originalRepo) throws IOException, InterruptedException {
        return forkRepository(originalRepo, null);
    }

    /**
     * Get the organization object for the given owner or null if the owner is not an organization
     * @return The GHOrganization object or null
     * @throws IOException If the organization access failed
     */
    private GHOrganization getOrganization() throws IOException {
        try {
            return github.getOrganization(getGithubOwner());
        } catch (GHFileNotFoundException e) {
            LOG.debug("Owner is not an organization: {}", config.getGithubOwner());
            return null;
        }
    }

    /**
     * Check if the repository is forked on the given organization
     * @param organization The organization to check
     * @param repoName The name of the repository
     * @return True if the repository is forked
     * @throws IOException If the repository access failed
     */
    private boolean isRepositoryForked(GHOrganization organization, String repoName) throws IOException {
        if (organization == null) {
            return false;
        }
        return getRepositoryFork(organization, repoName) != null;
    }

    /**
     * Get the forked repository on the given organization
     * @param organization The organization to check
     * @param repoName The name of the repository
     * @return The forked repository
     * @throws IOException If the repository access failed
     */
    private GHRepository getRepositoryFork(GHOrganization organization, String repoName) throws IOException {
        return organization.getRepository(repoName);
    }

    /**
     * Return if the repository is forked on current access
     * @param repoName The name of the repository
     * @return True if the repository is forked
     * @throws IOException If the repository access failed
     */
    private boolean isRepositoryForked(String repoName) throws IOException {
        return getRepositoryFork(repoName) != null;
    }

    /**
     * Get the forked repository on the personal account
     * @param repoName The name of the repository
     * @return The forked repository
     * @throws IOException If the repository access failed
     */
    private GHRepository getRepositoryFork(String repoName) throws IOException {
        return getCurrentUser().getRepository(repoName);
    }

    /**
     * Sync a fork repository from its original upstream. Only the main branch is synced in case multiple branches exist.
     * @param plugin The plugin to sync
     */
    public void sync(Plugin plugin) {
        if (config.isDryRun()) {
            LOG.info("Skipping sync plugin {} in dry-run mode", plugin);
            return;
        }
        if (config.isFetchMetadataOnly()) {
            LOG.info("Skipping sync plugin {} in fetch-metadata-only mode", plugin);
            return;
        }
        if (!isForked(plugin)) {
            LOG.info("Plugin {} is not forked. Not attempting sync", plugin);
            return;
        }
        try {
            syncRepository(getRepositoryFork(plugin));
            LOG.info("Synced the forked repository for plugin {}", plugin);
        } catch (IOException e) {
            plugin.addError("Failed to sync the repository", e);
            plugin.raiseLastError();
        }
    }

    /**
     * Sync a fork repository from its original upstream. Only the main branch is synced in case multiple branches exist.
     * @param forkedRepo Forked repository
     * @throws IOException if an error occurs while syncing the repository
     */
    private GHBranchSync syncRepository(GHRepository forkedRepo) throws IOException {
        LOG.debug("Syncing the forked repository {}", forkedRepo.getFullName());
        return forkedRepo.sync(forkedRepo.getDefaultBranch());
    }

    /**
     * Delete a plugin repository fork to the organization or personal account
     * @param plugin The plugin of the fork to delete
     */
    public void deleteFork(Plugin plugin) {
        if (config.isDryRun()) {
            LOG.info("Skipping delete fork for plugin {} in dry-run mode", plugin);
            return;
        }
        if (config.isFetchMetadataOnly()) {
            LOG.info("Skipping delete for for plugin {} in fetch-metadata-only mode", plugin);
            return;
        }
        if (!isForked(plugin)) {
            LOG.info("Plugin {} is not forked. Not attempting delete", plugin);
            return;
        }
        if (hasAnyPullRequestFrom(plugin)) {
            LOG.warn("Skipping delete fork for plugin {} as it has open pull requests", plugin);
            return;
        }
        GHRepository repository = getRepositoryFork(plugin);
        if (!repository.isFork()) {
            LOG.warn("Repository {} is not a fork. Not attempting delete", repository.getHtmlUrl());
            return;
        }
        if (repository.getOwnerName().equals(Settings.ORGANIZATION)) {
            LOG.warn("Not attempting to delete fork from organization {}", repository.getHtmlUrl());
            return;
        }
        if (config.isDebug()) {
            LOG.debug("Deleting fork for plugin {} from repo {}...", plugin, repository.getHtmlUrl());
        } else {
            LOG.info("Deleting fork for plugin {}...", plugin);
        }
        try {
            repository.delete();
            plugin.withoutCommits();
            plugin.withoutChangesPushed();
        } catch (IOException e) {
            plugin.addError("Failed to delete the fork", e);
            plugin.raiseLastError();
        }
    }

    /**
     * Fetch a plugin repository code from the fork or original repo in dry-run mode
     * @param plugin The plugin to fork
     */
    public void fetch(Plugin plugin) {
        GHRepository repository = config.isDryRun() || config.isFetchMetadataOnly() || plugin.isArchived(this)
                ? getRepository(plugin)
                : getRepositoryFork(plugin);

        if (config.isDebug()) {
            LOG.debug(
                    "Fetch plugin code {} from {} into directory {}...",
                    plugin,
                    repository.getHtmlUrl(),
                    plugin.getRepositoryName());
        } else {
            LOG.info("Fetching plugin code locally {}...", plugin);
        }
        try {
            fetchRepository(plugin);
            LOG.debug(
                    "Fetched repository from {}",
                    sshKeyAuth ? repository.getSshUrl() : repository.getHttpTransportUrl());
        } catch (GitAPIException | URISyntaxException e) {
            LOG.error("Failed to fetch the repository", e);
            plugin.addError("Failed to fetch the repository", e);
            plugin.raiseLastError();
        }
    }

    /**
     * Fetch the repository code into local directory of the plugin
     * @param plugin The plugin to fetch
     * @throws GitAPIException If the fetch operation failed
     */
    private void fetchRepository(Plugin plugin) throws GitAPIException, URISyntaxException {
        LOG.debug("Fetching {}", plugin.getName());
        GHRepository repository = config.isDryRun() || config.isFetchMetadataOnly() || plugin.isArchived(this)
                ? getRepository(plugin)
                : getRepositoryFork(plugin);

        // Get the correct URI
        URIish remoteUri =
                sshKeyAuth ? new URIish(repository.getSshUrl()) : new URIish(repository.getHttpTransportUrl());

        // Ensure to set port 22 if not set on remote URL to work with apache mina sshd
        if (sshKeyAuth) {
            if (remoteUri.getScheme() == null) {
                remoteUri = remoteUri.setScheme("ssh");
                LOG.debug("Setting scheme ssh for remote URI {}", remoteUri);
            }
            if (remoteUri.getPort() == -1) {
                remoteUri = remoteUri.setPort(22);
                LOG.debug("Setting port 22 for remote URI {}", remoteUri);
            }
        }
        // Fetch latest changes
        if (Files.isDirectory(plugin.getLocalRepository())) {
            // Ensure to set the correct remote, reset changes and pull
            try (Git git = Git.open(plugin.getLocalRepository().toFile())) {
                String defaultBranch = config.isDryRun() || config.isFetchMetadataOnly() || plugin.isArchived(this)
                        ? plugin.getRemoteRepository(this).getDefaultBranch()
                        : plugin.getRemoteForkRepository(this).getDefaultBranch();
                git.remoteSetUrl()
                        .setRemoteName("origin")
                        .setRemoteUri(remoteUri)
                        .call();
                git.fetch()
                        .setCredentialsProvider(getCredentialProvider())
                        .setRemote("origin")
                        .call();
                LOG.debug("Resetting changes and pulling latest changes from {}", remoteUri);
                git.reset()
                        .setMode(ResetCommand.ResetType.HARD)
                        .setRef("origin/" + defaultBranch)
                        .call();
                git.clean().setCleanDirectories(true).setDryRun(false).call();
                Ref ref = git.checkout()
                        .setCreateBranch(false)
                        .setName(defaultBranch)
                        .call();
                git.pull()
                        .setCredentialsProvider(getCredentialProvider())
                        .setRemote("origin")
                        .setRemoteBranchName(defaultBranch)
                        .call();
                LOG.info("Fetched repository from {} to branch {}", remoteUri, ref.getName());
            } catch (IOException e) {
                plugin.addError("Failed fetch repository", e);
                plugin.raiseLastError();
            }
        }
        // Clone the repository
        else {
            try (Git git = Git.cloneRepository()
                    .setCredentialsProvider(getCredentialProvider())
                    .setRemote("origin")
                    .setURI(remoteUri.toString())
                    .setDirectory(plugin.getLocalRepository().toFile())
                    .call()) {
                LOG.debug("Clone successfully from {}", remoteUri);
            }
        }
    }

    /**
     * Checkout the branch for the plugin. Creates the branch if not exists
     * @param plugin The plugin to checkout branch for
     */
    public void checkoutBranch(Plugin plugin) {
        try (Git git = Git.open(plugin.getLocalRepository().toFile())) {
            try {
                git.checkout().setCreateBranch(true).setName(BRANCH_NAME).call();
            } catch (RefAlreadyExistsException e) {
                String defaultBranch = config.isDryRun() || config.isFetchMetadataOnly() || plugin.isArchived(this)
                        ? plugin.getRemoteRepository(this).getDefaultBranch()
                        : plugin.getRemoteForkRepository(this).getDefaultBranch();
                LOG.debug("Branch already exists. Checking out the branch");
                git.checkout().setName(BRANCH_NAME).call();
                git.reset()
                        .setMode(ResetCommand.ResetType.HARD)
                        .setRef(defaultBranch)
                        .call();
                LOG.debug("Reseted the branch to Checking out the branch to default branch {}", defaultBranch);
            }
        } catch (IOException | GitAPIException e) {
            plugin.addError("Failed to checkout branch", e);
            plugin.raiseLastError();
        }
    }

    /**
     * Commit all changes in the plugin directory
     * @param plugin The plugin to commit changes for
     */
    public void commitChanges(Plugin plugin) {
        if (config.isDryRun()) {
            LOG.info("Skipping commits changes for plugin {} in dry-run mode", plugin);
            return;
        }
        if (plugin.isArchived(this)) {
            LOG.info("Plugin {} is archived. Not committing changes", plugin);
            return;
        }
        try (Git git = Git.open(plugin.getLocalRepository().toFile())) {
            git.getRepository().scanForRepoChanges();
            String commitMessage = TemplateUtils.renderCommitMessage(plugin, config.getRecipe());
            LOG.debug("Commit message: {}", commitMessage);
            Status status = git.status().call();
            if (status.hasUncommittedChanges()) {
                LOG.debug("Changed files before commit: {}", status.getChanged());
                LOG.debug("Untracked before commit: {}", status.getUntracked());
                LOG.debug("Missing before commit {}", status.getMissing());
                // Stage deleted file
                for (String file : status.getMissing()) {
                    git.rm().addFilepattern(file).call();
                }
                // Add the rest of the files
                git.add().addFilepattern(".").call();
                status = git.status().call();
                LOG.debug("Added files after staging: {}", status.getAdded());
                LOG.debug("Changed files to after staging: {}", status.getChanged());
                LOG.debug("Removed files to after staging: {}", status.getRemoved());
                GHUser user = getCurrentUser();
                String email = getPrimaryEmail(user);
                git.commit()
                        .setAuthor(user.getName() != null ? user.getName() : String.valueOf(user.getId()), email)
                        .setMessage(commitMessage)
                        .setSign(false) // Maybe a new option to sign commit?
                        .call();
                LOG.debug("Changes committed for plugin {}", plugin.getName());
                plugin.withCommits();
            } else {
                LOG.debug("No changes to commit for plugin {}", plugin.getName());
            }
        } catch (IOException | IllegalArgumentException | GitAPIException e) {
            plugin.addError("Failed to commit changes", e);
            plugin.raiseLastError();
        }
    }

    /**
     * Get the current user
     * @return The current user
     */
    public GHUser getCurrentUser() {
        if (!isConnected()) {
            LOG.debug("Not able to get current user. GitHub client is not connected");
            return null;
        }
        try {
            // Get myself
            if (config.getGithubAppId() == null) {
                LOG.debug("Getting current user using token...");
                return github.getMyself();
            }
            // Get the bot user
            else {
                LOG.debug("Getting current user using GitHub App...");
                LOG.debug("GitHub App name: {}", app.getName());
                return github.getUser("%s[bot]".formatted(app.getName()));
            }
        } catch (IOException e) {
            throw new ModernizerException("Failed to get current user", e);
        }
    }

    /**
     * Get the primary email of the user
     * @param user The user to get the primary email for
     * @return The primary email
     */
    public String getPrimaryEmail(GHUser user) {
        try {
            // User
            if (user instanceof GHMyself myself && myself.getType().equalsIgnoreCase("user")) {
                return "%s@users.noreply.github.com".formatted(user.getLogin());
            }
            // Bot
            else if (app != null && user.getType().equalsIgnoreCase("bot")) {
                return "%s+%s@users.noreply.github.com".formatted(user.getId(), user.getLogin());
            }
            throw new ModernizerException("Unknown user type %s".formatted(user.getType()));
        } catch (IOException e) {
            throw new ModernizerException("Failed to get primary email", e);
        }
    }

    /**
     * Push the changes to the forked repository
     * @param plugin The plugin to push changes for
     */
    public void pushChanges(Plugin plugin) {
        if (config.isDryRun()) {
            LOG.info("Skipping push changes for plugin {} in dry-run mode", plugin);
            return;
        }
        if (config.isFetchMetadataOnly()) {
            LOG.info("Skipping push changes for plugin {} in fetch-metadata-only mode", plugin);
            return;
        }
        if (!plugin.hasCommits()) {
            LOG.info("No commits to push for plugin {}", plugin.getName());
            return;
        }
        if (plugin.isArchived(this)) {
            LOG.info("Plugin {} is archived. Not pushing changes", plugin);
            return;
        }
        try (Git git = Git.open(plugin.getLocalRepository().toFile())) {
            List<PushResult> results = StreamSupport.stream(
                            git.push()
                                    .setForce(true)
                                    .setRemote("origin")
                                    .setCredentialsProvider(getCredentialProvider())
                                    .setRefSpecs(new RefSpec(BRANCH_NAME + ":" + BRANCH_NAME))
                                    .call()
                                    .spliterator(),
                            false)
                    .toList();
            results.forEach(result -> {
                LOG.debug("Push result: {}", result.getMessages());
                if (result.getMessages().contains("error")) {
                    plugin.addError("Unexpected push error: %s".formatted(result.getMessages()));
                    plugin.raiseLastError();
                }
            });
            plugin.withoutCommits();
            plugin.withChangesPushed();
            LOG.info("Pushed changes to forked repository for plugin {}", plugin.getName());
        } catch (IOException | GitAPIException e) {
            plugin.addError("Failed to push changes", e);
            plugin.raiseLastError();
        }
    }

    /**
     * Open a pull request for the plugin
     * @param plugin The plugin to open a pull request for
     */
    public void openPullRequest(Plugin plugin) {

        // Ensure to refresh client to target installation
        refreshToken(config.getGithubAppTargetInstallationId());

        // Renders parts and log then even if dry-run
        String prTitle = TemplateUtils.renderPullRequestTitle(plugin, config.getRecipe());
        String prBody = TemplateUtils.renderPullRequestBody(plugin, config.getRecipe());
        LOG.debug("Pull request title: {}", prTitle);
        LOG.debug("Pull request body: {}", prBody);
        LOG.debug("Draft mode: {}", config.isDraft());

        if (config.isDryRun()) {
            LOG.info("Skipping pull request changes for plugin {} in dry-run mode", plugin);
            return;
        }
        if (config.isFetchMetadataOnly()) {
            LOG.info("Skipping pull request for plugin {} in fetch-metadata-only mode", plugin);
            return;
        }
        if (!plugin.hasChangesPushed()) {
            LOG.info("No changes pushed to open pull request for plugin {}", plugin.getName());
            return;
        }
        if (plugin.isArchived(this)) {
            LOG.info("Plugin {} is archived. Not opening pull request", plugin);
            return;
        }

        // Check if existing PR exists
        GHRepository repository = plugin.getRemoteRepository(this);
        Optional<GHPullRequest> existingPR = checkIfPullRequestExists(plugin);
        if (existingPR.isPresent()) {
            LOG.info("Pull request already exists: {}", existingPR.get().getHtmlUrl());
            return;
        }

        try {
            GHPullRequest pr = repository.createPullRequest(
                    prTitle,
                    getGithubOwner() + ":" + BRANCH_NAME,
                    repository.getDefaultBranch(),
                    prBody,
                    false,
                    config.isDraft());
            LOG.info("Pull request created: {}", pr.getHtmlUrl());
            plugin.withPullRequest();
            try {
                String[] tags = plugin.getTags().toArray(String[]::new);
                if (tags.length > 0) {
                    pr.addLabels(tags);
                }
            } catch (Exception e) {
                LOG.debug("Failed to add labels to pull request: {}. Probably missing permission.", e.getMessage());
            } finally {
                plugin.withoutTags();
            }
        } catch (IOException e) {
            plugin.addError("Failed to create pull request", e);
            plugin.raiseLastError();
        }
    }

    /**
     * Get the current credentials provider
     * @return The credentials provider
     */
    private CredentialsProvider getCredentialProvider() {
        return sshKeyAuth
                ? new SshCredentialsProvider()
                : new UsernamePasswordCredentialsProvider(Settings.GITHUB_TOKEN, "");
    }

    /**
     * Return if the given repository has any pull request originating from it
     * Typically to avoid deleting fork with open pull requests
     * @param plugin The plugin to check
     * @return True if the repository has any pull request
     */
    private boolean hasAnyPullRequestFrom(Plugin plugin) {
        if (config.isDryRun()) {
            LOG.info("Skipping check for pull requests in dry-run mode");
            return false;
        }
        GHRepository originalRepo = plugin.getRemoteRepository(this);
        GHRepository forkRepo = plugin.getRemoteForkRepository(this);

        try {
            boolean hasPullRequest = originalRepo.queryPullRequests().state(GHIssueState.OPEN).list().toList().stream()
                    .peek(pr -> LOG.debug("Checking pull request: {}", pr.getHtmlUrl()))
                    .anyMatch(pr -> pr.getHead().getRepository().getFullName().equals(forkRepo.getFullName()));
            if (hasPullRequest) {
                LOG.debug("Found open pull request from {} to {}", forkRepo.getFullName(), originalRepo.getFullName());
                return true;
            }
        } catch (IOException e) {
            plugin.addError("Failed to check for pull requests", e);
            return false;
        }
        LOG.debug(
                "No open pull requests found for plugin {} targeting {}", plugin.getName(), originalRepo.getFullName());
        return false;
    }

    /**
     * Check if a pull request already exists for the branch to the target repo
     * @param plugin The plugin to check
     * @return The pull request if it exists
     */
    private Optional<GHPullRequest> checkIfPullRequestExists(Plugin plugin) {
        GHRepository repository = plugin.getRemoteRepository(this);
        try {
            List<GHPullRequest> pullRequests = repository
                    .queryPullRequests()
                    .state(GHIssueState.OPEN)
                    .list()
                    .toList();
            return pullRequests.stream()
                    .filter(pr -> pr.getHead().getRef().equals(BRANCH_NAME))
                    .findFirst();
        } catch (IOException e) {
            plugin.addError("Failed to check if pull request exists", e);
            return Optional.empty();
        }
    }

    /**
     * Determine the GitHub owner from config or using current token
     * @return The GitHub owner
     */
    public String getGithubOwner() {
        return config.getGithubOwner() != null
                ? config.getGithubOwner()
                : getCurrentUser().getLogin();
    }

    /**
     * Return if SSH auth is used
     * @return True if SSH key is used
     */
    public boolean isSshKeyAuth() {
        return sshKeyAuth;
    }

    /**
     * Ensure the forked reository correspond of the origin parent repository
     * @param originalRepo The original repository
     * @param fork The forked repository
     * @throws IOException If the check failed
     */
    private void checkSameParentRepository(Plugin plugin, GHRepository originalRepo, GHRepository fork)
            throws IOException {
        if (!fork.getParent().equals(originalRepo)) {
            LOG.warn(
                    "Forked repository {} is not forked from the original repository {}. Please remove forks if changing the source repo",
                    fork.getFullName(),
                    originalRepo.getFullName());
            throw new PluginProcessingException(
                    "Forked repository %s is not forked from the original repository %s but %s. Please remove forks if changing the source repo"
                            .formatted(
                                    fork.getFullName(),
                                    originalRepo.getFullName(),
                                    fork.getParent().getFullName()),
                    plugin);
        }
    }

    /**
     * Set the SSH key authentication if needed
     */
    private void setSshKeyAuth() {
        Path privateKey = config.getSshPrivateKey();
        if (Files.isRegularFile(privateKey)) {
            sshKeyAuth = true;
            LOG.debug("Using SSH private key for git operation: {}", privateKey);
        } else {
            sshKeyAuth = false;
            LOG.debug("SSH private key file {} does not exist. Will use GH_TOKEN for git operation", privateKey);
        }
    }

    /**
     * JGit expect a credential provider even if transport and authentication is none at transport level with
     * Apache Mina SSHD. This is therefor a dummy provider
     */
    private static class SshCredentialsProvider extends CredentialsProvider {
        @Override
        public boolean isInteractive() {
            return false;
        }

        @Override
        public boolean supports(CredentialItem... credentialItems) {
            return false;
        }

        @Override
        public boolean get(URIish uri, CredentialItem... credentialItems) throws UnsupportedCredentialItem {
            return false;
        }
    }
}
