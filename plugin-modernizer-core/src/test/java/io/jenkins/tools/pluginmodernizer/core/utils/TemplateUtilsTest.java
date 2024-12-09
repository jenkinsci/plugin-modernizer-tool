package io.jenkins.tools.pluginmodernizer.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.jenkins.tools.pluginmodernizer.core.extractor.PluginMetadata;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

public class TemplateUtilsTest {

    @Test
    public void testDefaultPrTitle() {

        // Mocks
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        Recipe recipe = mock(Recipe.class);

        doReturn(metadata).when(plugin).getMetadata();
        doReturn("io.jenkins.tools.pluginmodernizer.FakeRecipe").when(recipe).getName();

        // Test
        String result = TemplateUtils.renderPullRequestTitle(plugin, recipe);

        // Assert
        assertEquals("Applied recipe FakeRecipe", result);
    }

    @Test
    public void testFriendlyPrTitleUpgradeBomVersion() {

        // Mocks
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        Recipe recipe = mock(Recipe.class);

        doReturn(metadata).when(plugin).getMetadata();
        doReturn("3208.vb_21177d4b_cd9").when(metadata).getBomVersion();
        doReturn("io.jenkins.tools.pluginmodernizer.UpgradeBomVersion")
                .when(recipe)
                .getName();

        // Test
        String result = TemplateUtils.renderPullRequestTitle(plugin, recipe);

        // Assert
        assertEquals("Bump bom to 3208.vb_21177d4b_cd9", result);
    }

    @Test
    public void testFriendlyPrTitleUpgradeParentVersion() {

        // Mocks
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        Recipe recipe = mock(Recipe.class);

        doReturn(metadata).when(plugin).getMetadata();
        doReturn("4.88").when(metadata).getParentVersion();
        doReturn("io.jenkins.tools.pluginmodernizer.UpgradeParentVersion")
                .when(recipe)
                .getName();

        // Test
        String result = TemplateUtils.renderPullRequestTitle(plugin, recipe);

        // Assert
        assertEquals("Bump parent pom to 4.88", result);
    }

    @Test
    public void testFriendlyPrTitleUpgradeToRecommendCoreVersion() {

        // Mocks
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        Recipe recipe = mock(Recipe.class);

        doReturn(metadata).when(plugin).getMetadata();
        doReturn("2.452.4").when(metadata).getJenkinsVersion();
        doReturn("io.jenkins.tools.pluginmodernizer.UpgradeToRecommendCoreVersion")
                .when(recipe)
                .getName();

        // Test
        String result = TemplateUtils.renderPullRequestTitle(plugin, recipe);

        // Assert
        assertEquals("Require 2.452.4", result);
    }

    @Test
    public void testFriendlyPrTitleUpgradeToLatestJava11CoreVersion() {

        // Mocks
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        Recipe recipe = mock(Recipe.class);

        doReturn(metadata).when(plugin).getMetadata();
        doReturn("2.462.3").when(metadata).getJenkinsVersion();
        doReturn("io.jenkins.tools.pluginmodernizer.UpgradeToLatestJava11CoreVersion")
                .when(recipe)
                .getName();

        // Test
        String result = TemplateUtils.renderPullRequestTitle(plugin, recipe);

        // Assert
        assertEquals("Require 2.462.3", result);
    }
}
