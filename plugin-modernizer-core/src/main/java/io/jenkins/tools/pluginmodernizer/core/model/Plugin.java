package io.jenkins.tools.pluginmodernizer.core.model;

import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.github.GHService;
import io.jenkins.tools.pluginmodernizer.core.impl.MavenInvoker;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.kohsuke.github.GHRepository;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Mutable class representing a Jenkins plugin to modernize and refactor
 */
public class Plugin {

    /**
     * The plugin name
     */
    private String name;

    /**
     * Repository name under the jenkinsci organization
     */
    private String repositoryName;

    /**
     * Flag to indicate if the plugin has any commits to be pushed
     */
    private boolean hasCommits;

    /**
     * Return if the plugin has any error
     */
    private final List<Exception> errors = new LinkedList<>();

    private Plugin() {}

    /**
     * Build a minimal plugin object with name
     * @param name Name of the plugin
     * @return Plugin object
     */
    public static Plugin build(String name) {
        return new Plugin().withName(name);
    }

    /**
     * Set the name of the plugin
     * @param name Name of the plugin
     * @return Plugin object
     */
    public Plugin withName(String name) {
        this.name = name;
        return this;
    }

    /***
     * Set the repository name of the plugin
     * @param repositoryName Repository name of the plugin
     * @return Plugin object
     */
    public Plugin withRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
        return this;
    }

    /**
     * Indicate that the plugin has commits to be pushed
     * @return Plugin object
     */
    public Plugin withCommits() {
        this.hasCommits = true;
        return this;
    }

    /**
     * Indicate that the plugin has no commits to be pushed
     * @return Plugin object
     */
    public Plugin withoutCommits() {
        this.hasCommits = false;
        return this;
    }

    /**
     * Return if the plugin has any commits
     * @return True if the plugin has commits
     */
    public boolean hasCommits() {
        return hasCommits;
    }

    /**
     * Return if the plugin has any errors
     * @return True if the plugin has errors
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Add an error to the plugin
     * @param e The exception
     */
    public void addError(Exception e) {
        errors.add(e);
    }

    /**
     * Get the name of the plugin
     * @return Name of the plugin
     */
    public String getName() {
        return name;
    }

    /**
     * Get the repository name of the plugin
     * @return Repository name of the plugin
     */
    public String getRepositoryName() {
        return repositoryName;
    }

    /**
     * Get the local repository path
     * @return Local repository path
     */
    public Path getLocalRepository() {
        return Path.of(Settings.TEST_PLUGINS_DIRECTORY, getName());
    }

    /**
     * Get the URI of the repository on the given organization
     * @param organization Organization name (e.g. jenkinsci)
     * @return URI of the repository
     */
    public URI getGitRepositoryURI(String organization) {
        return URI.create("https://github.com/" + organization + "/" + repositoryName + ".git");
    }

    /**
     * Get the login marker for the plugin
     * @return Marker object
     */
    public Marker getMarker() {
        return MarkerFactory.getMarker(name);
    }

    /**
     * Execute maven clean on this plugin
     * @param maven The maven invoker instance
     */
    public void clean(MavenInvoker maven) {
        maven.invokeGoal(this, "clean");
    }

    /**
     * Run the openrewrite plugin on this plugin
     * @param maven The maven invoker instance
     */
    public void runOpenRewrite(MavenInvoker maven) {
        maven.invokeRewrite(this);
    }

    /**
     * Fork this plugin
     * @param service The GitHub service
     */
    public void fork(GHService service) {
        service.fork(this);
    }

    /**
     * Return if this plugin is forked
     * @param service The GitHub service
     */
    public boolean isForked(GHService service) {
        return service.isForked(this);
    }

    /**
     * Delete the plugin fork
     * @param service  The GitHub service
     */
    public void deleteFork(GHService service) {
        service.deleteFork(this);
    }

    /**
     * Checkout the plugin branch
     * @param service The GitHub service
     */
    public void checkoutBranch(GHService service) {
        service.checkoutBranch(this);
    }

    /**
     * Commit the changes to the plugin repository
     * @param service The GitHub service
     */
    public void commit(GHService service) {
        service.commitChanges(this);
    }

    /**
     * Push the changes to the plugin repository
     * @param service The GitHub service
     */
    public void push(GHService service) {
        service.pushChanges(this);
    }

    /**
     * Open a pull request for the plugin
     * @param service The GitHub service
     */
    public void openPullRequest(GHService service) {
        service.openPullRequest(this);
    }

    /**
     * Fetch the plugin code into local directory
     * @param service The GitHub service
     */
    public void fetch(GHService service) {
        service.fetch(this);
    }

    /**
     * Remove the plugin local data
     */
    public void removeLocalData() {
        Path path = getLocalRepository();
        File directory = path.toFile();
        if (directory.isDirectory() && directory.exists()) {
            try {
                FileUtils.deleteDirectory(directory);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to delete directory: " + directory, e);
            }
        }
    }

    /**
     * Get the associated repository for this plugin
     * @param service The GitHub service
     * @return The repository object
     */
    public GHRepository getRemoteRepository(GHService service) {
        return service.getRepository(this);
    }

    /**
     * Get the associated fork repository for this plugin
     * @param service The GitHub service
     * @return The repository object
     */
    public GHRepository getRemoteForkRepository(GHService service) {
        return service.getRepositoryFork(this);
    }

    @Override
    public String toString() {
        return name;
    }
}
