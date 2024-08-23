package io.jenkins.tools.pluginmodernizer.core.model;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.extractor.PluginMetadata;
import io.jenkins.tools.pluginmodernizer.core.github.GHService;
import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import io.jenkins.tools.pluginmodernizer.core.impl.MavenInvoker;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.io.FileUtils;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.w3c.dom.Document;

/**
 * Mutable class representing a Jenkins plugin to modernize and refactor
 */
public class Plugin {

    private static final Logger LOG = LoggerFactory.getLogger(Plugin.class);

    /**
     * The configuration to use
     */
    private Config config;

    /**
     * The plugin name
     */
    private String name;

    /**
     * Repository name under the jenkinsci organization
     */
    private String repositoryName;

    /**
     * The JDK to use
     */
    private JDK jdk;

    /**
     * The metadata of the plugin
     */
    private PluginMetadata metadata;

    /**
     * Flag to indicate if the plugin has any commits to be pushed
     */
    private boolean hasCommits;

    /**
     * Flag to indicate if the plugin has any changes pushed and ready to be merged
     */
    private boolean hasChangesPushed;

    /**
     * Flag to indicate if the plugin has any pull request open
     */
    private boolean hasPullRequest;

    /**
     * Return if the plugin has any error
     */
    private final List<PluginProcessingException> errors = new LinkedList<>();

    /**
     * Tags to apply on pull request for the applied changes
     */
    private final Set<String> tags = new HashSet<>();

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
     * Set the config of the plugin
     * @param config The config
     * @return Plugin object
     */
    public Plugin withConfig(Config config) {
        this.config = config;
        return this;
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

    /***
     * Set the current JDK
     * @param jdk The JDK
     * @return Plugin object
     */
    public Plugin withJDK(JDK jdk) {
        this.jdk = jdk;
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
     * Indicate that the plugin has changes pushed and ready to be merged
     * @return Plugin object
     */
    public Plugin withChangesPushed() {
        this.hasChangesPushed = true;
        return this;
    }

    /**
     * Indicate that the plugin has no changes pushed and ready to be merged
     * @return Plugin object
     */
    public Plugin withoutChangesPushed() {
        this.hasChangesPushed = false;
        return this;
    }

    /**
     * Indicate that the plugin has a pull request open
     * @return Plugin object
     */
    public Plugin withPullRequest() {
        this.hasPullRequest = true;
        return this;
    }

    /**
     * Indicate that the plugin has no pull request open
     * @return Plugin object
     */
    public Plugin withoutPullRequest() {
        this.hasPullRequest = false;
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
     * Return if the plugin has any changes pushed and ready to be merged
     * @return True if the plugin has changes pushed
     */
    public boolean hasChangesPushed() {
        return hasChangesPushed;
    }

    /**
     * Return if the plugin has any changes pushed and ready to be merged
     * @return True if the plugin has changes pushed
     */
    public boolean hasPullRequest() {
        return hasPullRequest;
    }

    /**
     * Return if the plugin has any errors
     * @return True if the plugin has errors
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Return if the plugin has any precondition errors
     * @return True if the plugin has precondition errors
     */
    public boolean hasPreconditionErrors() {
        return metadata != null
                && metadata.getErrors() != null
                && !metadata.getErrors().isEmpty();
    }

    /**
     * Add precondition errors to the plugin errors
     */
    public void addPreconditionErrors(PluginMetadata metadata) {
        if (metadata == null) {
            return;
        }
        metadata.getErrors().forEach(error -> addError(error.getError()));
    }

    /**
     * Get the errors of the plugin
     * @return List of errors
     */
    public List<PluginProcessingException> getErrors() {
        return errors;
    }

    /**
     * Add an error to the plugin
     * @param message The message
     * @param e The exception
     */
    public void addError(String message, Exception e) {
        LOG.error(getMarker(), message, e);
        if (config.isDebug()) {
            LOG.error(message, e);
        } else {
            LOG.error(message);
        }
        errors.add(new PluginProcessingException(message, e, this));
    }

    /**
     * Add an error to the plugin
     * @param message The message
     */
    public void addError(String message) {
        LOG.error(message);
        errors.add(new PluginProcessingException(message, this));
    }

    /**
     * Raise the last error as exception of the plugin
     * Do nothing if no errors
     */
    public void raiseLastError() throws PluginProcessingException {
        if (!hasErrors()) {
            return;
        }
        throw errors.get(errors.size() - 1);
    }

    /**
     * Add a tag to the plugin
     * @param tag Tag to add
     * @return Plugin object
     */
    public Plugin addTag(String tag) {
        tags.add(tag);
        return this;
    }

    /**
     * Add tags to the plugin
     * @param tags Tags to add
     * @return Plugin object
     */
    public Plugin addTags(Set<String> tags) {
        this.tags.addAll(tags);
        return this;
    }

    /**
     * Remove tags from the plugin
     * @return Plugin object
     */
    public Plugin withoutTags() {
        tags.clear();
        return this;
    }

    /**
     * Remove errors from the plugin
     * @return Plugin object
     */
    public Plugin withoutErrors() {
        errors.clear();
        return this;
    }

    /**
     * Get the configuration of the plugin
     * @return Configuration
     */
    public Config getConfig() {
        return config;
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
     * Get the path of the JDK directory
     * @return Path of the JDK directory
     */
    public JDK getJDK() {
        return jdk;
    }

    /**
     * Get the path of the log file for the plugin
     * @return Path of the log file
     */
    public Path getLogFile() {
        return Path.of("logs", getName() + ".log");
    }

    /**
     * Get the login marker for the plugin
     * @return Marker object
     */
    public Marker getMarker() {
        return MarkerFactory.getMarker(name);
    }

    /**
     * Get the list of tags for the plugin
     * @return List of tags
     */
    public Set<String> getTags() {
        return tags;
    }

    /**
     * Execute maven clean on this plugin
     * @param maven The maven invoker instance
     */
    public void clean(MavenInvoker maven) {
        maven.invokeGoal(this, "clean");
    }

    /**
     * Execute maven compile on this plugin
     * @param maven The maven invoker instance
     */
    public void compile(MavenInvoker maven) {
        if (config.isFetchMetadataOnly()) {
            LOG.info("Skipping compilation for plugin {} as only metadata is required", name);
            return;
        }
        LOG.info(
                "Compiling plugin {} with JDK {} ... Please be patient",
                name,
                this.getJDK().getMajor());
        maven.invokeGoal(this, "compile");
        if (!hasErrors()) {
            LOG.info("Done");
        }
    }

    /**
     * Execute maven verify on this plugin
     * @param maven The maven invoker instance
     */
    public void verify(MavenInvoker maven) {
        if (config.isFetchMetadataOnly()) {
            LOG.info("Skipping verification for plugin {} as only metadata is required", name);
            return;
        }
        LOG.info(
                "Verifying plugin {} with JDK {}... Please be patient",
                name,
                this.getJDK().getMajor());
        maven.invokeGoal(this, "verify");
        LOG.info("Done");
    }

    /**
     * Collect plugin metadata
     * @param maven The maven invoker instance
     */
    public void collectMetadata(MavenInvoker maven) {

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();

        // Static parse of the pom file and check for pattern preventing minimal build
        Path pom = getLocalRepository().resolve("pom.xml");
        if (!getLocalRepository().resolve("target").toFile().mkdir()) {
            LOG.debug("Failed to create target directory for plugin {}", name);
        }
        Document document = staticPomParse(pom);

        // Collect precondition errors
        PluginMetadata pluginMetadata = new PluginMetadata();
        pluginMetadata.setCacheManager(buildPluginTargetDirectoryCacheManager());
        pluginMetadata.setErrors(Arrays.stream(PreconditionError.values())
                .filter(error -> error.isApplicable(document, xpath))
                .collect(Collectors.toSet()));

        if (!pluginMetadata.getErrors().isEmpty()) {
            LOG.debug("Precondition errors found for plugin {}", name);
            pluginMetadata.save();
            return;
        }

        // Collect using OpenRewrite
        maven.collectMetadata(this);
    }

    /**
     * Run the openrewrite plugin on this plugin
     * @param maven The maven invoker instance
     */
    public void runOpenRewrite(MavenInvoker maven) {
        if (config.isFetchMetadataOnly()) {
            LOG.info("Skipping OpenRewrite recipe application for plugin {} as only metadata is required", name);
            return;
        }
        maven.invokeRewrite(this);
    }

    /**
     * Fork this plugin
     * @param service The GitHub service
     */
    public void fork(GHService service) {
        if (config.isFetchMetadataOnly()) {
            LOG.info("Skipping fork for plugin {} as only metadata is required", name);
            return;
        }
        service.fork(this);
    }

    /**
     * Fork sync this plugin
     * @param service The GitHub service
     */
    public void sync(GHService service) {
        if (config.isFetchMetadataOnly()) {
            LOG.info("Skipping sync for plugin {} as only metadata is required", name);
            return;
        }
        service.sync(this);
    }

    /**
     * Return if this plugin is forked
     * @param service The GitHub service
     */
    public boolean isForked(GHService service) {
        return service.isForked(this);
    }

    /**
     * Return if this plugin is archived
     * @param service The GitHub service
     * @return True if the plugin is archived
     */
    public boolean isArchived(GHService service) {
        return service.isArchived(this);
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

    /**
     * Get the metadata of the plugin
     * @return Plugin metadata
     */
    public PluginMetadata getMetadata() {
        return metadata;
    }

    /**
     * Set the metadata of the plugin
     * @param metadata Plugin metadata
     */
    public void setMetadata(PluginMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Build cache manager for this plugin
     * @return Cache manager
     */
    public CacheManager buildPluginTargetDirectoryCacheManager() {
        return new CacheManager(Path.of(Settings.TEST_PLUGINS_DIRECTORY)
                .resolve(getLocalRepository().resolve("target")));
    }

    /**
     * Static parse of the pom file to a XML document
     * @param pom The path to the pom file
     * @return The XML document
     */
    private Document staticPomParse(Path pom) {
        if (pom == null || !pom.toFile().exists()) {
            addError("No pom file found");
            raiseLastError();
            return null;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(pom.toFile());
        } catch (Exception e) {
            addError("Failed to parse pom file: " + pom, e);
            raiseLastError();
            return null;
        }
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Plugin plugin = (Plugin) o;
        return Objects.equals(name, plugin.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
