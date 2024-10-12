package io.jenkins.tools.pluginmodernizer.core.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.extractor.PluginMetadata;
import io.jenkins.tools.pluginmodernizer.core.github.GHService;
import io.jenkins.tools.pluginmodernizer.core.model.JDK;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.PluginProcessingException;
import io.jenkins.tools.pluginmodernizer.core.utils.PluginService;
import jakarta.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;
import org.openrewrite.Recipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS", justification = "safe because versions from pom.xml")
public class PluginModernizer {

    private static final Logger LOG = LoggerFactory.getLogger(PluginModernizer.class);

    @Inject
    private Config config;

    @Inject
    private MavenInvoker mavenInvoker;

    @Inject
    private GHService ghService;

    @Inject
    private PluginService pluginService;

    @Inject
    private CacheManager cacheManager;

    /**
     * Entry point to start the plugin modernization process
     */
    public void start() {

        // Validate maven
        mavenInvoker.validateMavenHome();
        mavenInvoker.validateMavenVersion();

        // Setup
        this.ghService.connect();
        if (config.isRemoveLocalData()) {
            cacheManager.wipe();
        }
        cacheManager.init();

        // Debug config
        LOG.debug("Plugins: {}", config.getPlugins());
        LOG.debug(
                "Recipes: {}", config.getRecipes().stream().map(Recipe::getName).collect(Collectors.joining(", ")));
        LOG.debug("GitHub owner: {}", config.getGithubOwner());
        LOG.debug("Update Center Url: {}", config.getJenkinsUpdateCenter());
        LOG.debug("Plugin versions Url: {}", config.getJenkinsPluginVersions());
        LOG.debug("Plugin Health Score Url: {}", config.getPluginHealthScore());
        LOG.debug("Installation Stats Url: {}", config.getPluginStatsInstallations());
        LOG.debug("Cache Path: {}", config.getCachePath());
        LOG.debug("Dry Run: {}", config.isDryRun());
        LOG.debug("Skip Push: {}", config.isSkipPush());
        LOG.debug("Skip Pull Request: {}", config.isSkipPullRequest());
        LOG.debug("Maven rewrite plugin version: {}", Settings.MAVEN_REWRITE_PLUGIN_VERSION);

        // Fetch plugin versions
        pluginService.getPluginVersionData();

        List<Plugin> plugins = config.getPlugins();
        plugins.forEach(this::process);
        printResults(plugins);
    }

    /**
     * Process a plugin
     * @param plugin The plugin to process
     */
    private void process(Plugin plugin) {
        try {

            // Set config
            plugin.withConfig(config);

            // Determine repo name
            plugin.withRepositoryName(pluginService.extractRepoName(plugin));

            LOG.debug("Plugin {} latest version: {}", plugin.getName(), pluginService.extractVersion(plugin));
            LOG.debug("Plugin {} health score: {}", plugin.getName(), pluginService.extractScore(plugin));
            LOG.debug("Plugin {} installations: {}", plugin.getName(), pluginService.extractInstallationStats(plugin));
            LOG.debug("Is API plugin {} : {}", plugin.getName(), plugin.isApiPlugin(pluginService));
            if (plugin.isDeprecated(pluginService)) {
                LOG.info("Plugin {} is deprecated. Skipping.", plugin.getName());
                plugin.addError("Plugin is deprecated");
                return;
            }
            if (plugin.isArchived(ghService)) {
                LOG.info("Plugin {} is archived. Skipping.", plugin.getName());
                plugin.addError("Plugin is archived");
                return;
            }

            if (config.isRemoveForks()) {
                plugin.deleteFork(ghService);
            }
            plugin.fork(ghService);
            plugin.sync(ghService);
            plugin.fetch(ghService);

            if (plugin.hasErrors()) {
                LOG.info("Plugin {} has errors. Will not process this plugin.", plugin.getName());
            }

            // Set the metadata from cache if available
            plugin.loadMetadata(cacheManager);

            // Compile only if we are able to find metadata
            // For the moment it's local cache only but later will fetch on remote storage
            if (!config.isFetchMetadataOnly()) {
                if (plugin.getMetadata() != null && !plugin.hasPreconditionErrors()) {
                    JDK jdk = compilePlugin(plugin);
                    LOG.debug("Plugin {} compiled successfully with JDK {}", plugin.getName(), jdk.getMajor());
                } else {
                    LOG.debug(
                            "No metadata or precondition errors found for plugin {}. Skipping initial compilation.",
                            plugin.getName());
                }
            }

            mavenInvoker.ensureMinimalBuild(plugin);
            plugin.checkoutBranch(ghService);

            // Minimum JDK to run openrewrite
            plugin.withJDK(JDK.JAVA_17);

            // Collect metadata and move metadata from the target directory of the plugin to the common cache
            if (!plugin.hasMetadata() || config.isFetchMetadataOnly()) {
                plugin.collectMetadata(mavenInvoker);
                plugin.moveMetadata(cacheManager);
                plugin.loadMetadata(cacheManager);
                plugin.enrichMetadata(pluginService);

            } else {
                LOG.debug("Metadata already computed for plugin {}. Using cached metadata.", plugin.getName());
            }

            // Try to remediate precondition errors
            if (plugin.hasPreconditionErrors()) {
                plugin.getPreconditionErrors().forEach(preconditionError -> {
                    if (preconditionError.remediate(plugin)) {
                        plugin.removePreconditionError(preconditionError);
                        LOG.info(
                                "Precondition error {} was remediated for plugin {}",
                                preconditionError,
                                plugin.getName());
                    } else {
                        LOG.info(
                                "Precondition error {} was not remediated for plugin {}",
                                preconditionError,
                                plugin.getName());
                    }
                });

                // Retry to collect metadata after remediation to get up-to-date results
                if (!config.isFetchMetadataOnly()) {
                    plugin.collectMetadata(mavenInvoker);
                    plugin.moveMetadata(cacheManager);
                    plugin.loadMetadata(cacheManager);
                    plugin.enrichMetadata(pluginService);
                }
            }

            // Check if we still have errors and abort if not remediation is possible
            if (plugin.hasErrors() || plugin.hasPreconditionErrors()) {
                plugin.addPreconditionErrors(plugin.getMetadata());
                LOG.info(
                        "Skipping plugin {} due to metadata/precondition errors. Check logs for more details.",
                        plugin.getName());
                return;
            }

            // Run OpenRewrite
            plugin.runOpenRewrite(mavenInvoker);
            if (plugin.hasErrors()) {
                LOG.warn(
                        "Skipping plugin {} due to openrewrite recipes errors. Check logs for more details.",
                        plugin.getName());
                return;
            }

            // Verify plugin
            if (!config.isFetchMetadataOnly()) {
                JDK jdk = verifyPlugin(plugin);
                LOG.info("Plugin {} verified successfully with JDK {}", plugin.getName(), jdk.getMajor());
            }

            if (plugin.hasErrors()) {
                LOG.warn(
                        "Skipping plugin {} due to verification errors after modernization. Check logs for more details.",
                        plugin.getName());
                return;
            }

            // Recollect metadata after modernization
            if (!config.isFetchMetadataOnly()) {
                plugin.collectMetadata(mavenInvoker);
                plugin.moveMetadata(cacheManager);
                plugin.loadMetadata(cacheManager);
                plugin.enrichMetadata(pluginService);
                LOG.debug(
                        "Plugin {} metadata after modernization: {}",
                        plugin.getName(),
                        plugin.getMetadata().toJson());

                plugin.commit(ghService);
                plugin.push(ghService);
                plugin.openPullRequest(ghService);
                if (config.isRemoveForks()) {
                    plugin.deleteFork(ghService);
                }
            }

        }
        // Uncatched plugin processing errors
        catch (PluginProcessingException e) {
            if (!plugin.hasErrors()) {
                plugin.addError("Plugin processing error. Check the logs at " + plugin.getLogFile(), e);
            }
        }
        // Catch any unexpected exception here
        catch (Exception e) {
            if (!plugin.hasErrors()) {
                plugin.addError("Unexpected processing error. Check the logs at " + plugin.getLogFile(), e);
            }
        }
    }

    /**
     * Compile a plugin
     * @param plugin The plugin to compile
     */
    private JDK compilePlugin(Plugin plugin) {
        PluginMetadata metadata = plugin.getMetadata();
        JDK jdk = JDK.min(metadata.getJdks());
        plugin.withJDK(jdk);
        plugin.clean(mavenInvoker);
        plugin.compile(mavenInvoker);
        return jdk;
    }

    /**
     * Verify a plugin and return the first JDK that successfully verifies it, starting from the target JDK and moving backward
     * @param plugin The plugin to verify
     * @return The JDK that verifies the plugin
     */
    private JDK verifyPlugin(Plugin plugin) {
        PluginMetadata metadata = plugin.getMetadata();

        // Determine the JDK
        JDK jdk;
        if (metadata.getJdks() == null || metadata.getJdks().isEmpty()) {
            jdk = JDK.JAVA_17;
            LOG.info(
                    "No JDKs found in metadata for plugin {}. Using same JDK as rewrite for verification",
                    plugin.getName());
        } else {
            jdk = JDK.min(metadata.getJdks());
            LOG.info("Using minimum JDK {} from metadata for plugin {}", jdk.getMajor(), plugin.getName());
        }
        // If the plugin was modernized we should find next JDK compatible
        // For example a Java 8 plugin was modernized to Java 11
        while (JDK.hasNext(jdk) && !jdk.supported(metadata.getJenkinsVersion())) {
            jdk = jdk.next();
        }

        // Build it
        plugin.withJDK(jdk);
        plugin.clean(mavenInvoker);
        plugin.format(mavenInvoker);
        plugin.verify(mavenInvoker);
        if (plugin.hasErrors()) {
            LOG.info("Plugin {} failed to verify with JDK {}", plugin.getName(), jdk.getMajor());
            plugin.withoutErrors();
        }
        plugin.withoutErrors();

        return jdk;
    }

    /**
     * Collect results from the plugins and display a summary
     * @param plugins The plugins
     */
    private void printResults(List<Plugin> plugins) {
        for (Plugin plugin : plugins) {
            LOG.info("*************");
            LOG.info("Plugin: {}", plugin.getName());

            // Display error
            if (plugin.hasErrors()) {
                for (PluginProcessingException error : plugin.getErrors()) {
                    LOG.error("Error: {}", error.getMessage());
                    if (config.isDebug()) {
                        LOG.error("Stacktrace: ", error);
                    }
                }
            }
            // Display what's done
            else {
                if (config.isFetchMetadataOnly()) {
                    LOG.info(
                            "Metadata was fetched for plugin {} and is available at {}",
                            plugin.getName(),
                            plugin.getMetadata().getLocation().toAbsolutePath());
                } else if (config.isDryRun()) {
                    LOG.info("Dry run mode. Changes were commited on on " + plugin.getLocalRepository()
                            + " but not pushed");
                } else {
                    if (config.isSkipPush()) {
                        LOG.info("Skip push mode. Changes were commited on on " + plugin.getLocalRepository()
                                + " but not pushed");
                    } else if (config.isSkipPullRequest()) {
                        LOG.info("Skip pull request mode. Changes were pushed on "
                                + plugin.getRemoteForkRepository(this.ghService).getHtmlUrl()
                                + " but no pull request was open on "
                                + plugin.getRemoteRepository(this.ghService).getHtmlUrl());
                    }
                    // Change were made
                    else {
                        LOG.info("Pull request was open on "
                                + plugin.getRemoteRepository(this.ghService).getHtmlUrl());
                    }
                }
            }
            LOG.info("*************");
        }
    }
}
