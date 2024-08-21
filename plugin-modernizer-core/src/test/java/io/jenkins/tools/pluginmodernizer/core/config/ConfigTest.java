package io.jenkins.tools.pluginmodernizer.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openrewrite.Recipe;

public class ConfigTest {

    @Test
    public void testConfigBuilderWithAllFields() throws MalformedURLException {
        String version = "1.0";
        String githubOwner = "test-owner";
        List<Plugin> plugins =
                Stream.of("plugin1", "plugin2").map(Plugin::build).toList();
        List<Recipe> recipes = Arrays.asList(Mockito.mock(Recipe.class), Mockito.mock(Recipe.class));
        Mockito.doReturn("recipe1").when(recipes.get(0)).getName();
        Mockito.doReturn("recipe2").when(recipes.get(1)).getName();
        URL jenkinsUpdateCenter = new URL("https://updates.jenkins.io/current/update-center.actual.json");
        Path cachePath = Paths.get("/path/to/cache");
        Path mavenHome = Paths.get("/path/to/maven");
        boolean dryRun = true;

        Config config = Config.builder()
                .withVersion(version)
                .withGitHubOwner(githubOwner)
                .withPlugins(plugins)
                .withRecipes(recipes)
                .withJenkinsUpdateCenter(jenkinsUpdateCenter)
                .withCachePath(cachePath)
                .withMavenHome(mavenHome)
                .withDryRun(dryRun)
                .withSkipPullRequest(true)
                .withSkipPush(true)
                .withExportDatatables(true)
                .withRemoveForks(true)
                .withRemoveLocalData(true)
                .build();

        assertEquals(version, config.getVersion());
        assertEquals(githubOwner, config.getGithubOwner());
        assertEquals(plugins, config.getPlugins());
        assertEquals(recipes, config.getRecipes());
        assertEquals(jenkinsUpdateCenter, config.getJenkinsUpdateCenter());
        assertEquals(cachePath, config.getCachePath());
        assertEquals(mavenHome, config.getMavenHome());
        assertTrue(config.isRemoveForks());
        assertTrue(config.isSkipPush());
        assertTrue(config.isSkipPullRequest());
        assertTrue(config.isRemoveForks());
        assertTrue(config.isRemoveLocalData());
        assertTrue(config.isExportDatatables());
        assertTrue(config.isDryRun());
    }

    @Test
    public void testConfigBuilderWithDefaultValues() {
        Config config = Config.builder().build();

        assertNull(config.getVersion());
        assertNull(config.getPlugins());
        assertNull(config.getRecipes());
        assertEquals(Settings.DEFAULT_UPDATE_CENTER_URL, config.getJenkinsUpdateCenter());
        assertEquals(Settings.DEFAULT_CACHE_PATH, config.getCachePath());
        assertEquals(Settings.DEFAULT_MAVEN_HOME, config.getMavenHome());
        assertFalse(config.isRemoveForks());
        assertFalse(config.isSkipPush());
        assertFalse(config.isSkipPullRequest());
        assertFalse(config.isRemoveForks());
        assertFalse(config.isRemoveLocalData());
        assertFalse(config.isExportDatatables());
        assertFalse(config.isDryRun());
    }

    @Test
    public void testConfigBuilderWithPartialValues() {
        String version = "2.0";
        List<Plugin> plugins =
                Stream.of("plugin1", "plugin2").map(Plugin::build).toList();

        Config config =
                Config.builder().withVersion(version).withPlugins(plugins).build();

        assertEquals(version, config.getVersion());
        assertEquals(plugins, config.getPlugins());
        assertNull(config.getRecipes());
        assertEquals(Settings.DEFAULT_UPDATE_CENTER_URL, config.getJenkinsUpdateCenter());
        assertEquals(Settings.DEFAULT_CACHE_PATH, config.getCachePath());
        assertEquals(Settings.DEFAULT_MAVEN_HOME, config.getMavenHome());
        assertFalse(config.isDryRun());
    }

    @Test
    public void testConfigBuilderWithNullValues() {
        Config config = Config.builder()
                .withJenkinsUpdateCenter(null)
                .withCachePath(null)
                .withMavenHome(null)
                .build();

        assertNull(config.getVersion());
        assertNull(config.getPlugins());
        assertNull(config.getRecipes());
        assertEquals(Settings.DEFAULT_UPDATE_CENTER_URL, config.getJenkinsUpdateCenter());
        assertEquals(Settings.DEFAULT_CACHE_PATH, config.getCachePath());
        assertEquals(Settings.DEFAULT_MAVEN_HOME, config.getMavenHome());
        assertFalse(config.isDryRun());
    }

    @Test
    public void testConfigBuilderDryRun() {
        Config config = Config.builder().withDryRun(true).build();

        assertTrue(config.isDryRun());
    }
}
