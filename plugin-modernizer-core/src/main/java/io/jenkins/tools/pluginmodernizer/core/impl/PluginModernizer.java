package io.jenkins.tools.pluginmodernizer.core.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.github.GHService;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
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

        LOG.info("Plugins: {}", config.getPluginNames());
        LOG.info("Recipes: {}", config.getRecipes());
        LOG.info("GitHub owner: {}", config.getGithubOwner());
        LOG.info("Update Center Url: {}", config.getJenkinsUpdateCenter());
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
        collectErrors(plugins);
    }

    /**
     * Process a plugin
     * @param plugin The plugin to process
     */
    private void process(Plugin plugin) {
        try {
            // Determine repo name
            plugin.withRepositoryName(
                    JenkinsPluginInfo.extractRepoName(plugin.getName(), cacheManager, config.getJenkinsUpdateCenter()));

            if (config.isRemoveForks()) {
                plugin.deleteFork(ghService);
            }
            plugin.fork(ghService);
            if (config.isRemoveLocalData()) {
                LOG.info("Removing local data for plugin: {} at {}", plugin, plugin.getLocalRepository());
                plugin.removeLocalData();
            }

            Path jdkSourcePath = getEffectiveJDKPath(config, jdkFetcher, config.getSourceJavaMajorVersion());
            Path jdkTargetPath = getEffectiveJDKPath(config, jdkFetcher, config.getTargetJavaMajorVersion());

            LOG.info("Using JDK build path: {}", jdkSourcePath);
            LOG.info("Using JDK target path: {}", jdkTargetPath);

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

        } catch (Exception e) {
            LOG.error("Failed to process plugin: {}", plugin, e);
            plugin.addError(e);
        }
    }

    /**
     * Collect errors from the plugins and diplays a sumarry
     * @param plugins
     */
    private void collectErrors(List<Plugin> plugins) {
        // TODO: https://github.com/jenkinsci/plugin-modernizer-tool/issues/143
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
