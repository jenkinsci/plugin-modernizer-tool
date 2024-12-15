package io.jenkins.tools.pluginmodernizer.cli.command;

import com.google.inject.Guice;
import io.jenkins.tools.pluginmodernizer.core.GuiceModule;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.impl.PluginModernizer;
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

    /**
     * Get the modernizer instance
     * @return the modernizer instance
     */
    default PluginModernizer getModernizer() {
        return Guice.createInjector(new GuiceModule(setup(Config.builder()))).getInstance(PluginModernizer.class);
    }
}
