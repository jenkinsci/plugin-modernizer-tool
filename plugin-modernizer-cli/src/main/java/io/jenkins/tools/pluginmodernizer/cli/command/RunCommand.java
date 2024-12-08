package io.jenkins.tools.pluginmodernizer.cli.command;

import com.google.inject.Guice;
import io.jenkins.tools.pluginmodernizer.cli.converter.RecipeConverter;
import io.jenkins.tools.pluginmodernizer.cli.options.GlobalOptions;
import io.jenkins.tools.pluginmodernizer.cli.options.PluginOptions;
import io.jenkins.tools.pluginmodernizer.core.GuiceModule;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.impl.PluginModernizer;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import java.util.ArrayList;
import org.openrewrite.Recipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * Run command
 */
@CommandLine.Command(name = "run", description = "Run")
public class RunCommand implements ICommand {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RunCommand.class);

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
        return builder.withPlugins(pluginOptions != null ? pluginOptions.getEffectivePlugins() : new ArrayList<>())
                .withRecipe(recipe)
                .build();
    }

    @Override
    public Integer call() throws Exception {
        LOG.info("Run Plugin Modernizer");
        PluginModernizer modernizer = Guice.createInjector(new GuiceModule(setup(options.getBuilderForOptions())))
                .getInstance(PluginModernizer.class);
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
