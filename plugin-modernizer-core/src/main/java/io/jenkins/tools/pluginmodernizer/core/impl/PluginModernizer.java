package io.jenkins.tools.pluginmodernizer.core.impl;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginModernizer {

    private static final Logger LOG = LoggerFactory.getLogger(PluginModernizer.class);

    private final Config config;

    public PluginModernizer(Config config) {
        this.config = config;
    }

    public void start() {
        LOG.info("Plugins: {}", config.getPlugins());
        LOG.info("Recipes: {}", config.getRecipes());
        LOG.debug("Cache Path: {}", config.getCachePath());
    }

}
