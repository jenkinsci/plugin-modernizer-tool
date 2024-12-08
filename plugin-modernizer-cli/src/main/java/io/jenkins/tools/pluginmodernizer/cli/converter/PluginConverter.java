package io.jenkins.tools.pluginmodernizer.cli.converter;

import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import picocli.CommandLine;

/**
 * Custom converter for Plugin class.
 */
public final class PluginConverter implements CommandLine.ITypeConverter<Plugin> {
    @Override
    public Plugin convert(String value) {
        if (value.trim().isBlank()) {
            return null;
        }
        return Plugin.build(value);
    }
}
