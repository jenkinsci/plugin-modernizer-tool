package io.jenkins.tools.pluginmodernizer.core.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginModernizer {

    private static final Logger LOG = LoggerFactory.getLogger(PluginModernizer.class);

    private final Config config;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final MavenInvoker mavenInvoker;

    public PluginModernizer(Config config) {
        this.config = config;
        this.mavenInvoker = new MavenInvoker(config);
    }

    public void start() {
        String projectRoot = System.getProperty("user.dir");
        for (String plugin : config.getPlugins()) {
            String pluginPath = projectRoot + "/test-plugins/" + plugin;
            mavenInvoker.invokeGoal(pluginPath, "clean");
            mavenInvoker.invokeRewrite(pluginPath);
        }
    }

}
