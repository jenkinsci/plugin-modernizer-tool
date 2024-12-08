package io.jenkins.tools.pluginmodernizer.cli.command;

import io.jenkins.tools.pluginmodernizer.cli.options.GlobalOptions;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * A command to list available recipes
 */
@CommandLine.Command(name = "recipes", description = "List recipes")
public class ListRecipesCommand implements ICommand {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ValidateCommand.class);

    @CommandLine.Mixin
    private GlobalOptions options;

    @Override
    public Integer call() throws Exception {
        Settings.AVAILABLE_RECIPES.forEach(recipe -> LOG.info(
                "{} - {}",
                recipe.getName().replaceAll(Settings.RECIPE_FQDN_PREFIX + ".", ""),
                recipe.getDescription()));
        return 0;
    }
}
