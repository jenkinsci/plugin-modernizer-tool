package io.jenkins.tools.pluginmodernizer.cli.command;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import java.util.concurrent.Callable;

/**
 * Interface for all subcommands
 */
public interface ICommand extends Callable<Integer> {

    /**
     * Enrich the configuration with the command specific options
     * @param builder the configuration builder
     * @return the enriched configuration
     */
    default Config setup(Config.Builder builder) {
        return builder.build();
    }
}
