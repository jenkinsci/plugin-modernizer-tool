package io.jenkins.tools.pluginmodernizer.cli.command;

import io.jenkins.tools.pluginmodernizer.cli.options.GlobalOptions;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.impl.PluginModernizer;
import picocli.CommandLine;

/**
 * A command to list available recipes
 */
@CommandLine.Command(name = "recipes", description = "List recipes")
public class ListRecipesCommand implements ICommand {

    @CommandLine.Mixin
    private GlobalOptions options;

    @Override
    public Config setup(Config.Builder builder) {
        options.config(builder);
        return builder.build();
    }

    @Override
    public Integer call() throws Exception {
        PluginModernizer modernizer = getModernizer();
        modernizer.listRecipes();
        return 0;
    }
}
