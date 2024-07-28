package io.jenkins.tools.pluginmodernizer.core.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginListParser {

    private static final Logger LOG = LoggerFactory.getLogger(PluginListParser.class);

    public static List<String> loadPluginsFromFile(Path pluginFile) {
        try (Stream<String> lines = Files.lines(pluginFile)) {
            return lines.filter(line -> !line.trim().isEmpty())
                    .map(line -> line.split(":")[0])
                    .collect(Collectors.toList());
        } catch (NoSuchFileException e) {
            LOG.error("File not found: {}", pluginFile);
            return null;
        } catch (IOException e) {
            LOG.error("Error reading plugins from file: {}", e.getMessage());
            return null;
        }
    }
}
