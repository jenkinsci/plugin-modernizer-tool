package io.jenkins.tools.pluginmodernizer.core.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.github.GHService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS", justification = "safe because versions from pom.xml")
public class PluginModernizer {

    private static final Logger LOG = LoggerFactory.getLogger(PluginModernizer.class);

    private final Config config;

    private final MavenInvoker mavenInvoker;

    private final GHService ghService;

    private final CacheManager cacheManager;

    public PluginModernizer(Config config) {
        this.config = config;
        this.mavenInvoker = new MavenInvoker(config);
        this.ghService = new GHService(config);
        this.cacheManager = new CacheManager(config.getCachePath());
    }

    public void start() {
        LOG.info("Plugins: {}", config.getPlugins());
        LOG.info("Recipes: {}", config.getRecipes());
        LOG.info("GitHub owner: {}", config.getGithubOwner());
        LOG.debug("Cache Path: {}", config.getCachePath());
        LOG.debug("Dry Run: {}", config.isDryRun());
        LOG.debug("Maven rewrite plugin version: {}", Settings.MAVEN_REWRITE_PLUGIN_VERSION);

        cacheManager.createCache();

        for (String plugin : config.getPlugins()) {
            String pluginPath = Settings.TEST_PLUGINS_DIRECTORY + plugin;
            String branchName = "apply-transformation-" + plugin;

            try {
                LOG.info("Forking and cloning {} locally", plugin);
                ghService.forkCloneAndCreateBranch(plugin, branchName);

                LOG.info("Invoking clean phase for plugin: {}", plugin);
                mavenInvoker.invokeGoal(plugin, pluginPath, "clean");

                LOG.info("Invoking rewrite plugin for plugin: {}", plugin);
                mavenInvoker.invokeRewrite(plugin, pluginPath);

                ghService.commitAndCreatePR(plugin, branchName);
            } catch (Exception e) {
                LOG.error("Failed to process plugin: {}", plugin, e);
            }
        }
    }

}
