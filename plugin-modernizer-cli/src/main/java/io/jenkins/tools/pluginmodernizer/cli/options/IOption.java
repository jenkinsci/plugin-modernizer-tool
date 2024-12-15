package io.jenkins.tools.pluginmodernizer.cli.options;

import io.jenkins.tools.pluginmodernizer.core.config.Config;

/**
 * Interface for all options
 */
public interface IOption {

    /**
     * Enrich the configuration build with specific options
     * @param builder the configuration builder
     */
    default void config(Config.Builder builder) {}
}
