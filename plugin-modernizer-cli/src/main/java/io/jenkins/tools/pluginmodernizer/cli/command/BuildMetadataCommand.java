package io.jenkins.tools.pluginmodernizer.cli.command;

import com.google.inject.Guice;
import io.jenkins.tools.pluginmodernizer.cli.options.GlobalOptions;
import io.jenkins.tools.pluginmodernizer.cli.options.PluginOptions;
import io.jenkins.tools.pluginmodernizer.core.GuiceModule;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.impl.PluginModernizer;
import java.util.ArrayList;
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

    @Override
    public Config setup(Config.Builder builder) {
        return builder.withPlugins(pluginOptions != null ? pluginOptions.getEffectivePlugins() : new ArrayList<>())
                .withRecipe(Settings.FETCH_METADATA_RECIPE)
                .build();
    }

    @Override
    public Integer call() throws Exception {
        PluginModernizer modernizer = Guice.createInjector(new GuiceModule(setup(options.getBuilderForOptions())))
                .getInstance(PluginModernizer.class);
        modernizer.start();
        return 0;
    }
}
