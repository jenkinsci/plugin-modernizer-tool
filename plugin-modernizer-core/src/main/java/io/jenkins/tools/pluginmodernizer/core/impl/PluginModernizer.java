package io.jenkins.tools.pluginmodernizer.core.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.github.GHService;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.PluginProcessingException;
import io.jenkins.tools.pluginmodernizer.core.utils.JdkFetcher;
import io.jenkins.tools.pluginmodernizer.core.utils.JenkinsPluginInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS", justification = "safe because versions from pom.xml")
public class PluginModernizer {

    private static final Logger LOG = LoggerFactory.getLogger(PluginModernizer.class);

    private final Config config;

    private final MavenInvoker mavenInvoker;

    private final GHService ghService;

    private final CacheManager cacheManager;

    private final JdkFetcher jdkFetcher;

    /**
     * Create a new PluginModernizer
     * @param config The configuration to use
     */
    public PluginModernizer(Config config) {
        this.config = config;
        this.mavenInvoker = new MavenInvoker(config);
        this.ghService = new GHService(config);
        this.cacheManager = new CacheManager(config.getCachePath());
        this.jdkFetcher = new JdkFetcher(config.getCachePath());
    }

    /**
     * Entry point to start the plugin modernization process
     */
    public void start() {

        // Setup
        this.ghService.connect();
        cacheManager.createCache();

        // Debug config
        LOG.debug("Plugins: {}", config.getPluginNames());
        LOG.debug("Recipes: {}", config.getRecipes());
        LOG.debug("GitHub owner: {}", config.getGithubOwner());
        LOG.debug("Update Center Url: {}", config.getJenkinsUpdateCenter());
        LOG.debug("Cache Path: {}", config.getCachePath());
        LOG.debug("Dry Run: {}", config.isDryRun());
        LOG.debug("Skip Push: {}", config.isSkipPush());
        LOG.debug("Skip Pull Request: {}", config.isSkipPullRequest());
        LOG.debug("Source Java Major Version: {}", config.getSourceJavaMajorVersion());
        LOG.debug("Target Java Major Version: {}", config.getTargetJavaMajorVersion());
        LOG.debug("Maven rewrite plugin version: {}", Settings.MAVEN_REWRITE_PLUGIN_VERSION);

        List<Plugin> plugins =
                config.getPluginNames().stream().map(Plugin::build).toList();
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
            plugin.withRepositoryName(
                    JenkinsPluginInfo.extractRepoName(plugin, config.getCachePath(), config.getJenkinsUpdateCenter()));

            if (config.isRemoveForks()) {
                plugin.deleteFork(ghService);
            }
            plugin.fork(ghService);
            if (config.isRemoveLocalData()) {
                if (config.isDebug()) {
                    LOG.debug("Removing local data for plugin: {} at {}", plugin, plugin.getLocalRepository());
                } else {
                    LOG.info("Removing local data for plugin: {}", plugin);
                }
                plugin.removeLocalData();
            }

            Path jdkSourcePath = getEffectiveJDKPath(config, jdkFetcher, config.getSourceJavaMajorVersion());
            Path jdkTargetPath = getEffectiveJDKPath(config, jdkFetcher, config.getTargetJavaMajorVersion());

            LOG.debug("Using JDK build path: {}", jdkSourcePath);
            LOG.debug("Using JDK target path: {}", jdkTargetPath);

            plugin.fetch(ghService);

            // Use source JDK path
            plugin.withJdkPath(jdkSourcePath);

            // Compile
            plugin.compile(mavenInvoker);
            if (plugin.hasErrors()) {
                LOG.warn(
                        "Skipping plugin {} due to compilation errors. Check logs for more details.", plugin.getName());
                return;
            }

            plugin.checkoutBranch(ghService);

            // Switch to the target JDK path
            plugin.withJdkPath(jdkTargetPath);

            // Run OpenRewrite
            plugin.runOpenRewrite(mavenInvoker);
            if (plugin.hasErrors()) {
                LOG.warn(
                        "Skipping plugin {} due to openrewrite recipes errors. Check logs for more details.",
                        plugin.getName());
                return;
            }

            // Verify
            plugin.verify(mavenInvoker);
            if (plugin.hasErrors()) {
                LOG.warn(
                        "Skipping plugin {} due to verification errors after modernization. Check logs for more details.",
                        plugin.getName());
                return;
            }

            plugin.commit(ghService);
            plugin.push(ghService);
            plugin.openPullRequest(ghService);
            if (config.isRemoveForks()) {
                plugin.deleteFork(ghService);
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
     * Collect results from the plugins and diplay a summarry
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
                if (config.isDryRun()) {
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

    /**
     * Get the JDK source path for compiling/building the plugin before modernization process
     * @param config The configuration
     * @param jdkFetcher The JDK fetcher
     * @return The JDK source path
     * @return majorVersion The major version of the JDK
     * @throws IOException IOException
     * @throws InterruptedException InterruptedException
     */
    private Path getEffectiveJDKPath(Config config, JdkFetcher jdkFetcher, int majorVersion)
            throws IOException, InterruptedException {
        return Files.isDirectory(Settings.getJavaVersionPath(majorVersion))
                ? Settings.getJavaVersionPath(majorVersion)
                : jdkFetcher.getJdkPath(majorVersion);
    }
}
