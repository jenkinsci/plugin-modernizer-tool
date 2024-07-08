package io.jenkins.tools.pluginmodernizer.core.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS", justification = "safe because versions from pom.xml")
public class PluginModernizer {

    private static final Logger LOG = LoggerFactory.getLogger(PluginModernizer.class);

    private final Config config;

    private final MavenInvoker mavenInvoker;

    public PluginModernizer(Config config) {
        this.config = config;
        this.mavenInvoker = new MavenInvoker(config);
    }

    public void start() {
        String projectRoot = System.getProperty("user.dir");
        LOG.info("Plugins: {}", config.getPlugins());
        LOG.info("Recipes: {}", config.getRecipes());
        LOG.debug("Cache Path: {}", config.getCachePath());
        LOG.debug("Dry Run: {}", config.isDryRun());
        LOG.debug("Maven rewrite plugin version: {}", Settings.MAVEN_REWRITE_PLUGIN_VERSION);
        for (String plugin : config.getPlugins()) {
            String pluginPath = projectRoot + "/test-plugins/" + plugin;
            LOG.info("Invoking clean phase for plugin: {}", plugin);
            mavenInvoker.invokeGoal(plugin, pluginPath, "clean");
            LOG.info("Invoking rewrite plugin for plugin: {}", plugin);
            mavenInvoker.invokeRewrite(plugin, pluginPath);
        }
    }

}
