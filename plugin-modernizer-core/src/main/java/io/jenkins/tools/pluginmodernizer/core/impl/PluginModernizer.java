package io.jenkins.tools.pluginmodernizer.core.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.extractor.PluginMetadata;
import io.jenkins.tools.pluginmodernizer.core.github.GHService;
import io.jenkins.tools.pluginmodernizer.core.model.JDK;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.PluginProcessingException;
import io.jenkins.tools.pluginmodernizer.core.utils.JdkFetcher;
import io.jenkins.tools.pluginmodernizer.core.utils.UpdateCenterUtils;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
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
        this.jdkFetcher = new JdkFetcher(config.getCachePath());
        this.mavenInvoker = new MavenInvoker(config, jdkFetcher);
        this.ghService = new GHService(config);
        this.cacheManager = new CacheManager(config.getCachePath());
    }

    /**
     * Entry point to start the plugin modernization process
     */
    public void start() {

        // Setup
        this.ghService.connect();
        cacheManager.init();

        // Debug config
        LOG.debug("Plugins: {}", config.getPluginNames());
        LOG.debug("Recipes: {}", config.getRecipes());
        LOG.debug("GitHub owner: {}", config.getGithubOwner());
        LOG.debug("Update Center Url: {}", config.getJenkinsUpdateCenter());
        LOG.debug("Cache Path: {}", config.getCachePath());
        LOG.debug("Dry Run: {}", config.isDryRun());
        LOG.debug("Skip Push: {}", config.isSkipPush());
        LOG.debug("Skip Pull Request: {}", config.isSkipPullRequest());
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
            plugin.withRepositoryName(UpdateCenterUtils.extractRepoName(plugin, cacheManager));

            if (config.isRemoveForks()) {
                plugin.deleteFork(ghService);
            }
            plugin.fork(ghService);
            plugin.sync(ghService);
            if (config.isRemoveLocalData()) {
                if (config.isDebug()) {
                    LOG.debug("Removing local data for plugin: {} at {}", plugin, plugin.getLocalRepository());
                } else {
                    LOG.info("Removing local data for plugin: {}", plugin);
                }
                plugin.removeLocalData();
            }

            plugin.fetch(ghService);

            if (plugin.hasErrors()) {
                LOG.info("Plugin {} has errors. Will not process this plugin.", plugin.getName());
            }

            // Compile the plugin with the first JDK that compile it
            JDK jdkCompile = compilePlugin(plugin);
            if (jdkCompile == null) {
                plugin.addError("Plugin failed to compile with all JDK.");
                LOG.info("Plugin {} fail to compile all JDK. Aborting this plugin.", plugin.getName());
                return;
            }
            LOG.info("Plugin {} compiled successfully with JDK {}", plugin.getName(), jdkCompile.getMajor());

            plugin.checkoutBranch(ghService);

            // Minimum JDK to run openrewrite
            plugin.withJDK(JDK.JAVA_17);

            // Collect metadata
            plugin.collectMetadata(mavenInvoker);

            // Move metadata from the target directory of the plugin to the common cache
            CacheManager pluginCacheManager = new CacheManager(Path.of(Settings.TEST_PLUGINS_DIRECTORY)
                    .resolve(plugin.getLocalRepository().resolve("target")));
            plugin.setMetadata(new PluginMetadata(pluginCacheManager)
                    .move(cacheManager, Path.of(plugin.getName()), CacheManager.PLUGIN_METADATA_CACHE_KEY));

            // Run OpenRewrite
            plugin.runOpenRewrite(mavenInvoker);
            if (plugin.hasErrors()) {
                LOG.warn(
                        "Skipping plugin {} due to openrewrite recipes errors. Check logs for more details.",
                        plugin.getName());
                return;
            }

            // Verify the plugin with the first JDK that verifies it
            JDK jdkVerify = verifyPlugin(plugin);
            if (jdkVerify == null) {
                plugin.addError("Plugin failed to verify with all JDKs.");
                LOG.info("Plugin {} failed to verify with all JDKs. Aborting this plugin.", plugin.getName());
                return;
            }
            LOG.info("Plugin {} verified successfully with JDK {}", plugin.getName(), jdkVerify.getMajor());

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
     * Compile a plugin and return the first JDK that compile it
     * @param plugin The plugin to compile
     * @return The JDK that compile the plugin
     */
    private JDK compilePlugin(Plugin plugin) {

        PluginMetadata metadata = plugin.getMetadata();
        JDK jdk;

        // TODO: For now it's always null because we don't persist nor cache metadata
        if (metadata == null) {
            LOG.info("Metadata is not yet computed for plugin {}. Using minimum JDK available", plugin.getName());
            jdk = JDK.min();
        } else {
            jdk = plugin.getJDK();
        }
        return Stream.iterate(jdk, JDK::hasNext, JDK::next)
                .sorted(JDK::compareMajor)
                .filter(j -> {
                    plugin.withJDK(j);
                    plugin.clean(mavenInvoker);
                    // TODO: Next PR, update metadata to include list of repositories using still HTTP
                    // plugin.ensureMinimalBuild(mavenInvoker);
                    plugin.compile(mavenInvoker);
                    if (plugin.hasErrors()) {
                        LOG.info(
                                "Plugin {} failed to compile with JDK {}. Trying next one",
                                plugin.getName(),
                                j.getMajor());
                        plugin.withoutErrors();
                        return false;
                    }
                    plugin.withoutErrors();
                    return true;
                })
                .findFirst()
                .orElse(null);
    }

    /**
     * Verify a plugin and return the first JDK that successfully verifies it, starting from the target JDK and moving backward
     * @param plugin The plugin to verify
     * @return The JDK that verifies the plugin
     */
    private JDK verifyPlugin(Plugin plugin) {
        PluginMetadata metadata = plugin.getMetadata();
        // Should not happen but let's not fail any null pointer in case
        if (metadata == null) {
            plugin.addError("Metadata is not yet computed for plugin " + plugin.getName());
            plugin.raiseLastError();
            return null;
        }
        String coreVersion = metadata.getJenkinsVersion();
        return Stream.iterate(JDK.max(), JDK::hasPrevious, JDK::previous)
                .filter(j -> j.supported(coreVersion))
                .filter(j -> {
                    plugin.withJDK(j);
                    plugin.clean(mavenInvoker);
                    plugin.verify(mavenInvoker);
                    if (plugin.hasErrors()) {
                        LOG.info(
                                "Plugin {} failed to verify with JDK {}. Trying previous one",
                                plugin.getName(),
                                j.getMajor());
                        plugin.withoutErrors();
                        return false;
                    }
                    plugin.withoutErrors();
                    return true;
                })
                .findFirst()
                .orElse(null);
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
}
