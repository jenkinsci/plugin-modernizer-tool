package io.jenkins.tools.pluginmodernizer.core.github;

import java.io.File;
import java.io.IOException;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

    private static final String GITHUB_TOKEN = System.getenv("GITHUB_TOKEN");
    private static final String FORKED_REPO_OWNER = System.getenv("GITHUB_USERNAME");
    private static final String ORIGINAL_REPO_OWNER = "sridamul";
    private static final String COMMIT_MESSAGE = "Applied transformations with specified recipes";
    private static final String PR_TITLE = "Automated PR";

    public void forkCloneAndCreateBranch(String pluginName, String branchName) throws IOException, GitAPIException, InterruptedException {
        String root = System.getProperty("user.dir");
        File pluginDirectory = new File(root + "/test-plugins/" + pluginName);

        GitHub github = GitHub.connectUsingOAuth(GITHUB_TOKEN);
        GHRepository originalRepo = github.getRepository(ORIGINAL_REPO_OWNER + "/" + pluginName);

        // Fork the original repo
        GHRepository forkedRepo = github.getMyself().getRepository(pluginName);
        if (forkedRepo == null) {
            LOG.info("Forking the repository...");
            originalRepo.fork();
            Thread.sleep(5000); // Ensure the completion of Fork
            LOG.info("Repository forked successfully.");
        } else {
            LOG.info("Repository already forked.");
        }

        if (!pluginDirectory.exists() || !pluginDirectory.isDirectory()) {
            LOG.info("Cloning {}", pluginName);
            Git.cloneRepository()
                    .setURI("https://github.com/" + FORKED_REPO_OWNER + "/" + pluginName + ".git")
                    .setDirectory(pluginDirectory)
                    .call();
            LOG.info("Cloned successfully.");
        }

        try (Git git = Git.open(pluginDirectory)) {
            // Create and switch to new branch
            try {
                git.checkout().setCreateBranch(true).setName(branchName).call();
            } catch (RefAlreadyExistsException e) {
                LOG.info("Branch already exists. Checking out the branch.");
                git.checkout().setName(branchName).call();
            }
        }
    }

    public void commitAndCreatePR(String pluginName, String branchName, List<String> recipes) throws IOException, GitAPIException {
        String root = System.getProperty("user.dir");
        File pluginDirectory = new File(root + "/test-plugins/" + pluginName);

        try (Git git = Git.open(pluginDirectory)) {
            // Stage all
            git.add().addFilepattern(".").call();

            // Commit
            git.commit()
                    .setMessage(COMMIT_MESSAGE)
                    .setSign(false)  // Maybe a new option to sign commit?
                    .call();

            LOG.info("Changes committed");

            // Push the new branch to your fork
            git.push()
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(GITHUB_TOKEN, ""))
                    .setRemote("origin")
                    .setRefSpecs(new RefSpec(branchName + ":" + branchName))
                    .call();

            LOG.info("Pushed changes to forked repository.");
        }

        GitHub github = GitHub.connectUsingOAuth(GITHUB_TOKEN);
        GHRepository originalRepo = github.getRepository(ORIGINAL_REPO_OWNER + "/" + pluginName);

        // Check if a PR with the same branch already exists
        List<GHPullRequest> pullRequests = originalRepo.getPullRequests(GHIssueState.OPEN);
        boolean prExists = false;
        for (GHPullRequest pr : pullRequests) {
            if (pr.getHead().getRef().equals(branchName) && pr.getTitle().equals(PR_TITLE)) {
                LOG.info("Pull request already exists: {}", pr.getHtmlUrl());
                prExists = true;
                break;
            }
        }

        // Create a PR
        if (!prExists) {
            String prBody = String.format("Applied the following recipes: %s", String.join(", ", recipes));
            GHPullRequest pr = originalRepo.createPullRequest(
                    PR_TITLE,
                    FORKED_REPO_OWNER + ":" + branchName,
                    originalRepo.getDefaultBranch(),
                    prBody
            );

            LOG.info("Pull request created: {}", pr.getHtmlUrl());
        }
    }
}
