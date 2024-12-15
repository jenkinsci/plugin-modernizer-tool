package io.jenkins.tools.pluginmodernizer.cli.command;

import io.jenkins.tools.pluginmodernizer.cli.options.EnvOptions;
import io.jenkins.tools.pluginmodernizer.cli.options.GlobalOptions;
import io.jenkins.tools.pluginmodernizer.cli.options.PluginOptions;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.impl.PluginModernizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * Run command
 */
@CommandLine.Command(name = "build-metadata", description = "Build local metadata")
public class BuildMetadataCommand implements ICommand {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(BuildMetadataCommand.class);

    /**
     * Plugins options
     */
    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
    private PluginOptions pluginOptions;

    /**
     * Global options for all commands
     */
    @CommandLine.Mixin
    private GlobalOptions options;

    /**
     * Environment options
     */
    @CommandLine.Mixin
    private EnvOptions envOptions;

    @Override
    public Config setup(Config.Builder builder) {
        options.config(builder);
        envOptions.config(builder);
        pluginOptions.config(builder);
        return builder.withRecipe(Settings.FETCH_METADATA_RECIPE).build();
    }

    @Override
    public Integer call() throws Exception {
        PluginModernizer modernizer = getModernizer();
        modernizer.start();
        return 0;
    }
}
