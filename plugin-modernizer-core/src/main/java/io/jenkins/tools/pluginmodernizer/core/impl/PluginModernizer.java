package io.jenkins.tools.pluginmodernizer.core.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.github.GHService;
import io.jenkins.tools.pluginmodernizer.core.utils.JenkinsPluginInfo;
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
        LOG.info("Update Center Url: {}", config.getJenkinsUpdateCenter());
        LOG.debug("Cache Path: {}", config.getCachePath());
        LOG.debug("Dry Run: {}", config.isDryRun());
        LOG.debug("Maven rewrite plugin version: {}", Settings.MAVEN_REWRITE_PLUGIN_VERSION);

        cacheManager.createCache();

        // TODO: Replace with some PluginInfo model that will containes for example
        // plugin name, repo name, scmUrl, location on disk etc....
        for (String plugin : config.getPlugins()) {
            String pluginPath = Settings.TEST_PLUGINS_DIRECTORY + plugin;
            String branchName = "apply-transformation-" + plugin;

            try {

                String repoName = JenkinsPluginInfo.extractRepoName(
                        plugin, config.getCachePath(), config.getJenkinsUpdateCenter());

                LOG.info("Forking and cloning plugin {} locally from repo {}", plugin, repoName);
                ghService.forkCloneAndCreateBranch(repoName, plugin, branchName);

                LOG.info("Invoking clean phase for plugin: {}", plugin);
                mavenInvoker.invokeGoal(plugin, pluginPath, "clean");

                LOG.info("Invoking rewrite plugin for plugin: {}", plugin);
                mavenInvoker.invokeRewrite(plugin, pluginPath);

                ghService.commitAndCreatePR(repoName, plugin, branchName);
            } catch (Exception e) {
                LOG.error("Failed to process plugin: {}", plugin, e);
            }
        }
    }
}
