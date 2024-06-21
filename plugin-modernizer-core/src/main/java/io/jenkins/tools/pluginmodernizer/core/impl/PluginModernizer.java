package io.jenkins.tools.pluginmodernizer.core.impl;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginModernizer {

    private static final Logger LOG = LoggerFactory.getLogger(PluginModernizer.class);

    private final Config config;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final MavenGoalInvoker mavenGoalInvoker;

    public PluginModernizer(Config config) {
        this.config = config;
        this.mavenGoalInvoker = new MavenGoalInvoker(config);
    }

    public void start() {
        String projectRoot = System.getProperty("user.dir");
        // TODO: Fn to populate the directory with the plugins
        File pluginsDir = new File(projectRoot, "test-plugins");

        File[] subdirectories = pluginsDir.listFiles(File::isDirectory);
        if (subdirectories != null) {
            for (File pluginDir : subdirectories) {
                try {
                    List<String> goals = mavenGoalInvoker.createGoalsList();
                    if (goals == null) {
                        LOG.info("No active recipes.");
                        return;
                    }
                    mavenGoalInvoker.invokeMavenGoals(pluginDir, goals);
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        } else {
            LOG.error("No subdirectories found in {}", pluginsDir.getAbsolutePath());
        }
        LOG.info("Plugins: {}", config.getPlugins());
        LOG.info("Recipes: {}", config.getRecipes());
        LOG.debug("Cache Path: {}", config.getCachePath());
    }

}
