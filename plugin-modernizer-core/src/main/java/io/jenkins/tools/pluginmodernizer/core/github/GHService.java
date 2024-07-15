package io.jenkins.tools.pluginmodernizer.core.github;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.kohsuke.github.GHIssueState;
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
        if (config.getGithubUsername() == null) {
            throw new IllegalArgumentException("GitHub username/organization is not set for forked repos. Please set GH_USERNAME or GITHUB_USERNAME environment variable.");
        }
    }

    // TODO: Change commit message and PR title based on applied recipes
    private static final String COMMIT_MESSAGE = "Applied transformations with specified recipes";
    private static final String PR_TITLE = "Automated PR";

    public void forkCloneAndCreateBranch(String pluginName, String branchName) throws IOException, GitAPIException, InterruptedException {
        Path pluginDirectory = Paths.get(Settings.TEST_PLUGINS_DIRECTORY, pluginName);

        GitHub github = GitHub.connectUsingOAuth(Settings.GITHUB_TOKEN);
        GHRepository originalRepo = github.getRepository(Settings.ORGANIZATION + "/" + pluginName);

        getOrCreateForkedRepo(github, originalRepo);

        cloneRepositoryIfNeeded(pluginDirectory, pluginName);

        createAndCheckoutBranch(pluginDirectory, branchName);
    }

    private void getOrCreateForkedRepo(GitHub github, GHRepository originalRepo) throws IOException, InterruptedException {
        GHRepository forkedRepo = github.getMyself().getRepository(originalRepo.getName());
        if (forkedRepo == null) {
            LOG.info("Forking the repository...");
            originalRepo.fork();
            Thread.sleep(5000); // Ensure the completion of Fork
            LOG.info("Repository forked successfully.");
        } else {
            LOG.info("Repository already forked.");
        }
    }

    private void cloneRepositoryIfNeeded(Path pluginDirectory, String pluginName) throws GitAPIException {
        if (!Files.exists(pluginDirectory) || !Files.isDirectory(pluginDirectory)) {
            LOG.info("Cloning {}", pluginName);
            Git.cloneRepository()
                    .setURI("https://github.com/" + config.getGithubUsername() + "/" + pluginName + ".git")
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

        createPullRequest(pluginName, branchName);
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

    private void createPullRequest(String pluginName, String branchName) throws IOException {
        GitHub github = GitHub.connectUsingOAuth(Settings.GITHUB_TOKEN);
        GHRepository originalRepo = github.getRepository(Settings.ORGANIZATION + "/" + pluginName);

        Optional<GHPullRequest> existingPR = checkIfPullRequestExists(originalRepo, branchName);

        if (existingPR.isPresent()) {
            LOG.info("Pull request already exists: {}", existingPR.get().getHtmlUrl());
        } else {
            String prBody = String.format("Applied the following recipes: %s", String.join(", ", config.getRecipes()));
            GHPullRequest pr = originalRepo.createPullRequest(
                    PR_TITLE,
                    config.getGithubUsername() + ":" + branchName,
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
