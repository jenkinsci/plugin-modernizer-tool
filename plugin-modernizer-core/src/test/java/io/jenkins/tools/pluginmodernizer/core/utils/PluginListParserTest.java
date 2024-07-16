package io.jenkins.tools.pluginmodernizer.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class PluginListParserTest {

    @TempDir
    Path tempDir;

    @Test
    public void testLoadPluginsFromFileWithEmptyLines() throws IOException {
        Path pluginFile = tempDir.resolve("plugins.txt");
        Files.write(pluginFile, List.of("plugin1", "", "plugin2", "   ", "plugin3"));

        List<String> plugins = PluginListParser.loadPluginsFromFile(pluginFile);

        assertNotNull(plugins);
        assertEquals(3, plugins.size());
        assertTrue(plugins.contains("plugin1"));
        assertTrue(plugins.contains("plugin2"));
        assertTrue(plugins.contains("plugin3"));
    }

    @Test
    public void testLoadPluginsFromFileEmptyFile() throws IOException {
        Path pluginFile = tempDir.resolve("plugins.txt");
        Files.createFile(pluginFile);

        List<String> plugins = PluginListParser.loadPluginsFromFile(pluginFile);
        assertNotNull(plugins);
        assertTrue(plugins.isEmpty());
    }

    @Test
    public void testLoadPluginsFromFileFileNotFound() {
        Path pluginFile = tempDir.resolve("nonexistent.txt");

        List<String> plugins = PluginListParser.loadPluginsFromFile(pluginFile);

        assertNull(plugins);
    }

    @Test
    public void testLoadPluginsFromResourceFile() {
        Path resourceFilePath = Path.of("src", "test", "resources", "plugins.txt");

        List<String> plugins = PluginListParser.loadPluginsFromFile(resourceFilePath);

        assertNotNull(plugins);
        assertEquals(4, plugins.size());
        assertTrue(plugins.contains("jobcacher"));
        assertTrue(plugins.contains("login-theme"));
        assertTrue(plugins.contains("next-executions"));
        assertTrue(plugins.contains("cloudbees-bitbucket-branch-source"));
    }
}
