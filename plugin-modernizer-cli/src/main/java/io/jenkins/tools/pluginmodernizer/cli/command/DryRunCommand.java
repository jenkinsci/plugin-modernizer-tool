package io.jenkins.tools.pluginmodernizer.cli.command;

import io.jenkins.tools.pluginmodernizer.cli.converter.RecipeConverter;
import io.jenkins.tools.pluginmodernizer.cli.options.GlobalOptions;
import io.jenkins.tools.pluginmodernizer.cli.options.PluginOptions;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.impl.PluginModernizer;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import io.jenkins.tools.pluginmodernizer.core.model.Recipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * Dry Run command
 */
@CommandLine.Command(name = "dry-run", description = "Dry Run")
public class DryRunCommand implements ICommand {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(DryRunCommand.class);

    /**
     * Plugins options
     */
    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
    private PluginOptions pluginOptions;

    /**
     * Recipe to be applied
     */
    @CommandLine.Option(
            names = {"-r", "--recipe"},
            required = true,
            description = "Recipe to be applied.",
            converter = RecipeConverter.class)
    private Recipe recipe;

    /**
     * Global options for all commands
     */
    @CommandLine.Mixin
    private GlobalOptions options;

    @Override
    public Config setup(Config.Builder builder) {
        options.config(builder);
        pluginOptions.config(builder);
        return builder.withDryRun(true).withRecipe(recipe).build();
    }

    @Override
    public Integer call() throws Exception {
        LOG.info("Run Plugin Modernizer in dry-run mode");
        PluginModernizer modernizer = getModernizer();
        try {
            modernizer.validate();
        } catch (ModernizerException e) {
            LOG.error("Validation error");
            LOG.error(e.getMessage());
            return 1;
        }
        modernizer.start();
        return 0;
    }
}
