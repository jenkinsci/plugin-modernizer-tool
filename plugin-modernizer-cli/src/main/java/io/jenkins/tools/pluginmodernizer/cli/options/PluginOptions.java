package io.jenkins.tools.pluginmodernizer.cli.options;

import io.jenkins.tools.pluginmodernizer.cli.converter.PluginConverter;
import io.jenkins.tools.pluginmodernizer.cli.converter.PluginFileConverter;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import picocli.CommandLine;

/**
 * Plugin option that are mutually exclusive.
 */
public final class PluginOptions implements IOption {

    /**
     * List of plugins from CLI
     */
    @CommandLine.Option(
            names = {"-p", "--plugins"},
            description = "List of Plugins to Modernize.",
            split = ",",
            converter = PluginConverter.class)
    private List<Plugin> plugins;

    /**
     * List of plugins from file
     */
    @CommandLine.Option(
            names = {"-f", "--plugin-file"},
            description = "Path to the file that contains a list of plugins.",
            converter = PluginFileConverter.class)
    private List<Plugin> pluginsFromFile;

    @Override
    public void config(Config.Builder builder) {
        builder.withPlugins(getEffectivePlugins());
    }

    /**
     * Get effective plugins
     * @return List of plugins from CLI and/or file
     */
    private List<Plugin> getEffectivePlugins() {
        if (plugins == null) {
            plugins = List.of();
        }
        if (pluginsFromFile == null) {
            pluginsFromFile = List.of();
        }
        return Stream.concat(plugins.stream(), pluginsFromFile.stream()).collect(Collectors.toList());
    }
}
