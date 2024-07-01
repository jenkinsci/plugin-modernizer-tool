package io.jenkins.tools.pluginmodernizer.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class ConfigTest {

    @Test
    public void testConfigBuilderWithAllFields() {
        String version = "1.0";
        List<String> plugins = Arrays.asList("plugin1", "plugin2");
        List<String> recipes = Arrays.asList("recipe1", "recipe2");
        Path cachePath = Paths.get("/path/to/cache");
        Path mavenHome = Paths.get("/path/to/maven");
        boolean dryRun = true;

        Config config = Config.builder()
                .withVersion(version)
                .withPlugins(plugins)
                .withRecipes(recipes)
                .withCachePath(cachePath)
                .withMavenHome(mavenHome)
                .withDryRun(dryRun)
                .build();

        assertEquals(version, config.getVersion());
        assertEquals(plugins, config.getPlugins());
        assertEquals(recipes, config.getRecipes());
        assertEquals(cachePath, config.getCachePath());
        assertEquals(mavenHome, config.getMavenHome());
        assertTrue(config.isDryRun());
    }

    @Test
    public void testConfigBuilderWithDefaultValues() {
        Config config = Config.builder().build();

        assertNull(config.getVersion());
        assertNull(config.getPlugins());
        assertNull(config.getRecipes());
        assertEquals(Settings.DEFAULT_CACHE_PATH, config.getCachePath());
        assertEquals(Settings.DEFAULT_MAVEN_HOME, config.getMavenHome());
        assertFalse(config.isDryRun());
    }

    @Test
    public void testConfigBuilderWithPartialValues() {
        String version = "2.0";
        List<String> plugins = Arrays.asList("plugin1", "plugin2");

        Config config = Config.builder()
                .withVersion(version)
                .withPlugins(plugins)
                .build();

        assertEquals(version, config.getVersion());
        assertEquals(plugins, config.getPlugins());
        assertNull(config.getRecipes());
        assertEquals(Settings.DEFAULT_CACHE_PATH, config.getCachePath());
        assertEquals(Settings.DEFAULT_MAVEN_HOME, config.getMavenHome());
        assertFalse(config.isDryRun());
    }

    @Test
    public void testConfigBuilderWithNullValues() {
        Config config = Config.builder()
                .withCachePath(null)
                .withMavenHome(null)
                .build();

        assertNull(config.getVersion());
        assertNull(config.getPlugins());
        assertNull(config.getRecipes());
        assertEquals(Settings.DEFAULT_CACHE_PATH, config.getCachePath());
        assertEquals(Settings.DEFAULT_MAVEN_HOME, config.getMavenHome());
        assertFalse(config.isDryRun());
    }

    @Test
    public void testConfigBuilderDryRun() {
        Config config = Config.builder()
                .withDryRun(true)
                .build();

        assertTrue(config.isDryRun());
    }
}
