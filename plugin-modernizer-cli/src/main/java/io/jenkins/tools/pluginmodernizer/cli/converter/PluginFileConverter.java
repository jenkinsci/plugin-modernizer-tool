package io.jenkins.tools.pluginmodernizer.cli.converter;

import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import picocli.CommandLine;

/**
 * Custom converter to get a list of plugin from a file
 */
public final class PluginFileConverter implements CommandLine.ITypeConverter<List<Plugin>> {
    @Override
    public List<Plugin> convert(String value) {
        if (value.trim().isBlank()) {
            return List.of();
        }
        try (Stream<String> lines = Files.lines(Path.of(value))) {
            return lines.filter(line -> !line.trim().isEmpty())
                    .map(line -> line.split(":")[0])
                    .map(Plugin::build)
                    .collect(Collectors.toList());
        } catch (NoSuchFileException e) {
            throw new ModernizerException("File not found: " + value);
        } catch (IOException e) {
            throw new ModernizerException("Error reading plugins from file: " + e.getMessage());
        }
    }
}
