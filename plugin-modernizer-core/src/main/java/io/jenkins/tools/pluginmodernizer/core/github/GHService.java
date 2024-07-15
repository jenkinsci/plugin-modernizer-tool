package io.jenkins.tools.pluginmodernizer.core.github;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.utils.JenkinsPluginInfo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
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

    private final Config config;

    public GHService(Config config) {
        this.config = config;
        validate();
    }

    private void validate() {
        if (Settings.GITHUB_TOKEN == null) {
            throw new IllegalArgumentException("GitHub token is not set. Please set GH_TOKEN or GITHUB_TOKEN environment variable.");
        }
        if (config.getGithubOwner() == null) {
            throw new IllegalArgumentException("GitHub owner (username/organization) is not set. Please set GH_OWNER or GITHUB_OWNER environment variable.");
        }
    }

    // TODO: Change commit message and PR title based on applied recipes
    private static final String COMMIT_MESSAGE = "Applied transformations with specified recipes";
    private static final String PR_TITLE = "Automated PR";

    private String repoName;

    public void forkCloneAndCreateBranch(String pluginName, String branchName) throws IOException, GitAPIException, InterruptedException {
        Path pluginDirectory = Paths.get(Settings.TEST_PLUGINS_DIRECTORY, pluginName);

        JsonNode jsonNode = JenkinsPluginInfo.getCachedJsonNode(config.getCachePath());
        repoName = JenkinsPluginInfo.extractRepoName(pluginName, jsonNode);

        GitHub github = GitHub.connectUsingOAuth(Settings.GITHUB_TOKEN);
        GHRepository originalRepo = github.getRepository(Settings.ORGANIZATION + "/" + repoName);

        getOrCreateForkedRepo(github, originalRepo);

        cloneRepositoryIfNeeded(pluginDirectory, pluginName);

        createAndCheckoutBranch(pluginDirectory, branchName);
    }

    private void getOrCreateForkedRepo(GitHub github, GHRepository originalRepo) throws IOException, InterruptedException {
        GHOrganization organization = getOrganization(github, config.getGithubOwner());

        if (organization != null) {
            if (isRepositoryForked(organization, originalRepo.getName())) {
                // TODO: Fn to sync upstream
                LOG.info("Repository already forked to organization.");
            } else {
                forkRepositoryToOrganization(originalRepo, organization);
            }
        } else {
            if (isRepositoryForked(github, originalRepo.getName())) {
                // TODO: Fn to sync upstream
                LOG.info("Repository already forked to personal account.");
            } else {
                forkRepositoryToPersonalAccount(originalRepo);
            }
        }
    }

    private GHOrganization getOrganization(GitHub github, String owner) {
        try {
            return github.getOrganization(owner);
        } catch (IOException e) {
            LOG.debug("Owner is not an organization: {}", owner, e);
            return null;
        }
    }

    private boolean isRepositoryForked(GHOrganization organization, String repoName) throws IOException {
        return organization.getRepository(repoName) != null;
    }

    private boolean isRepositoryForked(GitHub github, String repoName) throws IOException {
        return github.getMyself().getRepository(repoName) != null;
    }

    private void forkRepositoryToOrganization(GHRepository originalRepo, GHOrganization organization) throws IOException, InterruptedException {
        LOG.info("Forking the repository to organization...");
        originalRepo.forkTo(organization);
        Thread.sleep(5000); // Ensure the completion of Fork
        LOG.info("Repository forked to organization successfully.");
    }

    private void forkRepositoryToPersonalAccount(GHRepository originalRepo) throws IOException, InterruptedException {
        LOG.info("Forking the repository to personal account...");
        originalRepo.fork();
        Thread.sleep(5000); // Ensure the completion of Fork
        LOG.info("Repository forked to personal account successfully.");
    }

    private void cloneRepositoryIfNeeded(Path pluginDirectory, String pluginName) throws GitAPIException {
        if (!Files.exists(pluginDirectory) || !Files.isDirectory(pluginDirectory)) {
            LOG.info("Cloning {}", pluginName);
            Git.cloneRepository()
                    .setURI("https://github.com/" + config.getGithubOwner() + "/" + repoName + ".git")
                    .setDirectory(pluginDirectory.toFile())
                    .call();
            LOG.info("Cloned successfully.");
        }
    }

    private void createAndCheckoutBranch(Path pluginDirectory, String branchName) throws IOException, GitAPIException {
        try (Git git = Git.open(pluginDirectory.toFile())) {
            try {
                git.checkout().setCreateBranch(true).setName(branchName).call();
            } catch (RefAlreadyExistsException e) {
                LOG.info("Branch already exists. Checking out the branch.");
                git.checkout().setName(branchName).call();
            }
        }
    }

    public void commitAndCreatePR(String pluginName, String branchName) throws IOException, GitAPIException {
        if (config.isDryRun()) {
            LOG.info("[Dry Run] Skipping commit and pull request creation for {}", pluginName);
            return;
        }

        LOG.info("Creating pull request for plugin: {}", pluginName);

        Path pluginDirectory = Paths.get(Settings.TEST_PLUGINS_DIRECTORY, pluginName);

        commitChanges(pluginDirectory);

        pushBranch(pluginDirectory, branchName);

        createPullRequest(branchName);
    }

    private void commitChanges(Path pluginDirectory) throws IOException, GitAPIException {
        try (Git git = Git.open(pluginDirectory.toFile())) {
            git.add().addFilepattern(".").call();

            git.commit()
                    .setMessage(COMMIT_MESSAGE)
                    .setSign(false)  // Maybe a new option to sign commit?
                    .call();

            LOG.info("Changes committed");
        }
    }

    private void pushBranch(Path pluginDirectory, String branchName) throws IOException, GitAPIException {
        try (Git git = Git.open(pluginDirectory.toFile())) {
            git.push()
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(Settings.GITHUB_TOKEN, ""))
                    .setRemote("origin")
                    .setRefSpecs(new RefSpec(branchName + ":" + branchName))
                    .call();

            LOG.info("Pushed changes to forked repository.");
        }
    }

    private void createPullRequest(String branchName) throws IOException {
        GitHub github = GitHub.connectUsingOAuth(Settings.GITHUB_TOKEN);
        GHRepository originalRepo = github.getRepository(Settings.ORGANIZATION + "/" + repoName);

        Optional<GHPullRequest> existingPR = checkIfPullRequestExists(originalRepo, branchName);

        if (existingPR.isPresent()) {
            LOG.info("Pull request already exists: {}", existingPR.get().getHtmlUrl());
        } else {
            String prBody = String.format("Applied the following recipes: %s", String.join(", ", config.getRecipes()));
            GHPullRequest pr = originalRepo.createPullRequest(
                    PR_TITLE,
                    config.getGithubOwner() + ":" + branchName,
                    originalRepo.getDefaultBranch(),
                    prBody
            );

            LOG.info("Pull request created: {}", pr.getHtmlUrl());
        }
    }

    private Optional<GHPullRequest> checkIfPullRequestExists(GHRepository originalRepo, String branchName) throws IOException {
        List<GHPullRequest> pullRequests = originalRepo.getPullRequests(GHIssueState.OPEN);
        return pullRequests.stream()
                .filter(pr -> pr.getHead().getRef().equals(branchName) && pr.getTitle().equals(PR_TITLE))
                .findFirst();
    }
}
