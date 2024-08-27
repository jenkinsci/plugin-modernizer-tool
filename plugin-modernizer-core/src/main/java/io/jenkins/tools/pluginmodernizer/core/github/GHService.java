package io.jenkins.tools.pluginmodernizer.core.github;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.PluginProcessingException;
import io.jenkins.tools.pluginmodernizer.core.utils.TemplateUtils;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.kohsuke.github.GHBranchSync;
import org.kohsuke.github.GHEmail;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "false positive")
public class GHService {

    private static final Logger LOG = LoggerFactory.getLogger(GHService.class);

    // TODO: Use unique branch name (with prefix ?) to avoid conflicts
    private static final String BRANCH_NAME = "plugin-modernizer-tool";

    private final Config config;
    private GitHub github;

    public GHService(Config config) {
        this.config = config;
        validate();
    }

    /**
     * Validate the configuration of the GHService
     */
    private void validate() {
        if (Settings.GITHUB_TOKEN == null) {
            throw new ModernizerException(
                    "GitHub token is not set. Please set GH_TOKEN or GITHUB_TOKEN environment variable.");
        }
        if (config.getGithubOwner() == null) {
            throw new ModernizerException(
                    "GitHub owner (username/organization) is not set. Please set GH_OWNER or GITHUB_OWNER environment variable.");
        }
    }

    /**
     * Connect to GitHub using the GitHub auth token
     */
    public void connect() {
        if (github != null) {
            throw new ModernizerException("GitHub client is already connected.");
        }
        try {
            github = GitHub.connectUsingOAuth(Settings.GITHUB_TOKEN);
        } catch (IOException e) {
            throw new ModernizerException("Failed to connect to GitHub. Cannot use GitHub/SCM integration", e);
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
            return github.getRepository(config.getGithubOwner() + "/" + plugin.getRepositoryName());
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
                return getRepositoryFork(organization, originalRepo.getName());
            } else {
                GHRepository fork = forkRepository(originalRepo, organization);
                Thread.sleep(5000); // Wait for the fork to be ready
                return fork;
            }
        } else {
            if (isRepositoryForked(originalRepo.getName())) {
                LOG.debug(
                        "Repository already forked to personal account {}",
                        github.getMyself().getLogin());
                return getRepositoryFork(originalRepo.getName());
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
                    github.getMyself().getLogin());
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
            return github.getOrganization(config.getGithubOwner());
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
        return github.getMyself().getRepository(repoName);
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
        } catch (IOException e) {
            plugin.addError("Failed to sync the repository", e);
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
            LOG.debug("Fetched repository from {}", repository.getHtmlUrl());
        } catch (GitAPIException e) {
            LOG.error("Failed to fetch the repository", e);
            plugin.addError("Failed to fetch the repository", e);
        }
    }

    /**
     * Fetch the repository code into local directory of the plugin
     * @param plugin The plugin to fetch
     * @throws GitAPIException If the fetch operation failed
     */
    private void fetchRepository(Plugin plugin) throws GitAPIException {
        LOG.debug("Fetching {}", plugin.getName());
        GHRepository repository = config.isDryRun() || config.isFetchMetadataOnly() || plugin.isArchived(this)
                ? getRepository(plugin)
                : getRepositoryFork(plugin);
        String remoteUrl = repository.getHttpTransportUrl();
        // Fetch latest changes
        if (Files.isDirectory(plugin.getLocalRepository())) {
            // Ensure to set the correct remote, reset changes and pull
            try (Git git = Git.open(plugin.getLocalRepository().toFile())) {
                git.remoteSetUrl()
                        .setRemoteName("origin")
                        .setRemoteUri(new URIish(repository.getHttpTransportUrl()))
                        .call();
                LOG.debug("Resetting changes and pulling latest changes from {}", remoteUrl);
                git.reset().setMode(ResetCommand.ResetType.HARD).call();
                git.pull().setRemote("origin").setRemoteBranchName(repository.getDefaultBranch());
            } catch (IOException | URISyntaxException e) {
                plugin.addError("Failed fetch repository", e);
            }
        }
        // Clone the repository
        else {
            try (Git git = Git.cloneRepository()
                    .setURI(remoteUrl)
                    .setDirectory(plugin.getLocalRepository().toFile())
                    .call()) {
                LOG.debug("Fetch successfully from {}", remoteUrl);
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
                String defaultBranch = config.isDryRun() || plugin.isArchived(this)
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
            String commitMessage = TemplateUtils.renderCommitMessage(plugin, config.getRecipes());
            LOG.debug("Commit message: {}", commitMessage);
            if (git.status().call().hasUncommittedChanges()) {
                git.add().addFilepattern(".").call();
                Optional<GHEmail> email = github.getMyself().getEmails2().stream()
                        .filter(GHEmail::isPrimary)
                        .findFirst();
                if (email.isEmpty()) {
                    throw new IllegalArgumentException("Primary email not found for GitHub user");
                }
                git.commit()
                        .setAuthor(github.getMyself().getName(), email.get().getEmail())
                        .setMessage(commitMessage)
                        .setSign(false) // Maybe a new option to sign commit?
                        .call();
                LOG.debug("Changes committed for plugin {}", plugin.getName());
                plugin.withCommits();
            } else {
                LOG.debug("No changes to commit for plugin {}", plugin.getName());
            }
        } catch (IOException | GitAPIException e) {
            plugin.addError("Failed to commit changes", e);
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
        if (config.isSkipPush()) {
            LOG.info("Skipping push changes for plugin {}", plugin);
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
            git.push()
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(Settings.GITHUB_TOKEN, ""))
                    .setRemote("origin")
                    .setRefSpecs(new RefSpec(BRANCH_NAME + ":" + BRANCH_NAME))
                    .call();
            plugin.withoutCommits();
            plugin.withChangesPushed();
            LOG.info("Pushed changes to forked repository for plugin {}", plugin.getName());
        } catch (IOException | GitAPIException e) {
            plugin.addError("Failed to push changes", e);
        }
    }

    /**
     * Open a pull request for the plugin
     * @param plugin The plugin to open a pull request for
     */
    public void openPullRequest(Plugin plugin) {

        // Renders parts and log then even if dry-run
        String prTitle = TemplateUtils.renderPullRequestTitle(plugin, config.getRecipes());
        String prBody = TemplateUtils.renderPullRequestBody(plugin, config.getRecipes());
        LOG.debug("Pull request title: {}", prTitle);
        LOG.debug("Pull request body: {}", prBody);

        if (config.isDryRun()) {
            LOG.info("Skipping pull request changes for plugin {} in dry-run mode", plugin);
            return;
        }
        if (config.isFetchMetadataOnly()) {
            LOG.info("Skipping pull request for plugin {} in fetch-metadata-only mode", plugin);
            return;
        }
        if (config.isSkipPullRequest() || config.isSkipPush()) {
            LOG.info("Skipping pull request for plugin {}", plugin);
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
                    prTitle, config.getGithubOwner() + ":" + BRANCH_NAME, repository.getDefaultBranch(), prBody);
            pr.addLabels(plugin.getTags().toArray(String[]::new));
            LOG.info("Pull request created: {}", pr.getHtmlUrl());
            plugin.withoutTags();
            plugin.withPullRequest();
        } catch (IOException e) {
            plugin.addError("Failed to create pull request", e);
        }
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
            boolean hasPullRequest = originalRepo.getPullRequests(GHIssueState.OPEN).stream()
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
            List<GHPullRequest> pullRequests = repository.getPullRequests(GHIssueState.OPEN);
            return pullRequests.stream()
                    .filter(pr -> pr.getHead().getRef().equals(BRANCH_NAME))
                    .findFirst();
        } catch (IOException e) {
            plugin.addError("Failed to check if pull request exists", e);
            return Optional.empty();
        }
    }
}
