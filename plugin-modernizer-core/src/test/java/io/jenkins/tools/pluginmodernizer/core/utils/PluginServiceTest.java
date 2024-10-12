package io.jenkins.tools.pluginmodernizer.core.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.google.inject.Guice;
import io.jenkins.tools.pluginmodernizer.core.GuiceModule;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import io.jenkins.tools.pluginmodernizer.core.model.HealthScoreData;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.UpdateCenterData;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class})
@WireMockTest
class PluginServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Config config;

    @Mock
    private Path cacheRoot;

    @TempDir
    private Path tempDir;

    // Update center test data
    private UpdateCenterData updateCenterData;

    // Health score test data
    private HealthScoreData healthScoreData;

    @BeforeEach
    public void setup() throws Exception {

        updateCenterData = new UpdateCenterData(cacheManager);
        healthScoreData = new HealthScoreData(cacheManager);

        Map<String, UpdateCenterData.UpdateCenterPlugin> updateCenterPlugins = new HashMap<>();
        updateCenterPlugins.put(
                "valid-plugin",
                new UpdateCenterData.UpdateCenterPlugin(
                        "valid-plugin", "1.0", "https://github.com/jenkinsci/valid-url", "main", "gav", null));
        updateCenterPlugins.put(
                "invalid-plugin",
                new UpdateCenterData.UpdateCenterPlugin(
                        "invalid-plugin", "1.0", "invalid-scm-url", "main", "gav", null));
        updateCenterPlugins.put(
                "invalid-plugin-2",
                new UpdateCenterData.UpdateCenterPlugin("invalid-plugin-2", "1.0", "/", "main", "gav", null));

        // Add health plugin
        Map<String, HealthScoreData.HealthScorePlugin> healthPlugins = new HashMap<>();
        healthPlugins.put("valid-plugin", new HealthScoreData.HealthScorePlugin(100d));
        healthPlugins.put("valid-plugin2", new HealthScoreData.HealthScorePlugin(50d));

        // Set plugins
        Field updateCenterPluginField = ReflectionUtils.findFields(
                        UpdateCenterData.class,
                        f -> f.getName().equals("plugins"),
                        ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .get(0);
        updateCenterPluginField.setAccessible(true);
        updateCenterPluginField.set(updateCenterData, updateCenterPlugins);

        doReturn(cacheRoot).when(config).getCachePath();

        Field healthScorePluginField = ReflectionUtils.findFields(
                        HealthScoreData.class,
                        f -> f.getName().equals("plugins"),
                        ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .get(0);
        healthScorePluginField.setAccessible(true);
        healthScorePluginField.set(healthScoreData, healthPlugins);
    }

    @Test
    public void shouldExtractRepoName() throws Exception {
        setupUpdateCenterMocks();
        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppId();
        PluginService service = getService();
        String result = service.extractRepoName(Plugin.build("valid-plugin").withConfig(config));
        assertEquals("valid-url", result);
    }

    @Test
    public void shouldDownloadPluginVersionDataUpdateCenterData(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {

        setupUpdateCenterMocks();
        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppId();

        // Download through wiremock to avoid hitting the real Jenkins update center
        WireMock wireMock = wmRuntimeInfo.getWireMock();

        wireMock.register(WireMock.get(WireMock.urlEqualTo("/update-center.json"))
                .willReturn(WireMock.okJson(JsonUtils.toJson(updateCenterData))));

        // No found from cache
        doReturn(new URL(wmRuntimeInfo.getHttpBaseUrl() + "/update-center.json"))
                .when(config)
                .getJenkinsUpdateCenter();

        Mockito.reset(cacheManager);

        // Get result
        PluginService service = getService();
        UpdateCenterData result = service.downloadUpdateCenterData();
        assertEquals(result.getPlugins().size(), updateCenterData.getPlugins().size());
    }

    @Test
    public void shouldThrowExceptionIfNotFound() throws Exception {

        setupUpdateCenterMocks();
        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppId();

        PluginService service = getService();
        Exception exception = assertThrows(ModernizerException.class, () -> {
            service.extractRepoName(Plugin.build("not-present").withConfig(config));
        });
        assertEquals("Plugin not found in update center", exception.getMessage());
    }

    @Test
    public void shouldFailIfSCMFormatIsInvalid() throws Exception {

        setupUpdateCenterMocks();
        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppId();

        PluginService service = getService();
        Exception exception = assertThrows(ModernizerException.class, () -> {
            service.extractRepoName(Plugin.build("invalid-plugin").withConfig(config));
        });
        assertEquals("Invalid SCM URL format", exception.getMessage());
    }

    @Test
    public void shouldDownloadPluginVersionDataPluginHealthScore(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {

        setupHealthScoreMocks();
        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppId();

        // Download through wiremock to avoid hitting the real Jenkins update center
        WireMock wireMock = wmRuntimeInfo.getWireMock();

        wireMock.register(WireMock.get(WireMock.urlEqualTo("/api/scores"))
                .willReturn(WireMock.okJson(JsonUtils.toJson(healthScoreData))));

        // No found from cache
        doReturn(new URL(wmRuntimeInfo.getHttpBaseUrl() + "/api/scores"))
                .when(config)
                .getPluginHealthScore();

        Mockito.reset(cacheManager);

        // Get result
        PluginService service = getService();
        HealthScoreData result = service.downloadHealthScoreData();
        assertEquals(result.getPlugins().size(), healthScoreData.getPlugins().size());
    }

    @Test
    public void testLoadPluginsFromFileWithEmptyLines() throws Exception {

        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppId();

        Path pluginFile = tempDir.resolve("plugins.txt");
        Files.write(pluginFile, List.of("plugin1", "", "plugin2", "   ", "plugin3"));

        PluginService service = getService();
        List<Plugin> plugins = service.loadPluginsFromFile(pluginFile);

        assertNotNull(plugins);
        assertEquals(3, plugins.size());
        assertTrue(plugins.contains(Plugin.build("plugin1")));
        assertTrue(plugins.contains(Plugin.build("plugin2")));
        assertTrue(plugins.contains(Plugin.build("plugin3")));
    }

    @Test
    public void testLoadPluginsFromFileEmptyFile() throws Exception {

        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppId();

        Path pluginFile = tempDir.resolve("plugins.txt");
        Files.createFile(pluginFile);

        PluginService service = getService();
        List<Plugin> plugins = service.loadPluginsFromFile(pluginFile);
        assertNotNull(plugins);
        assertTrue(plugins.isEmpty());
    }

    @Test
    public void testLoadPluginsFromResourceFile() throws Exception {

        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppId();

        Path resourceFilePath = Path.of("src", "test", "resources", "plugins.txt");

        PluginService service = getService();
        List<Plugin> plugins = service.loadPluginsFromFile(resourceFilePath);

        assertNotNull(plugins);
        assertEquals(4, plugins.size());
        assertTrue(plugins.contains(Plugin.build("jobcacher")));
        assertTrue(plugins.contains(Plugin.build("login-theme")));
        assertTrue(plugins.contains(Plugin.build("next-executions")));
        assertTrue(plugins.contains(Plugin.build("cloudbees-bitbucket-branch-source")));
    }

    @Test
    public void testLoadPluginsFromResourceFileWithEmptyLines() throws Exception {

        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppId();

        Path resourceFilePath = Path.of("src", "test", "resources", "empty-plugins.txt");

        PluginService service = getService();
        List<Plugin> plugins = service.loadPluginsFromFile(resourceFilePath);
        assertEquals(0, plugins.size());
    }

    @Test
    public void testLoadPluginsFromFileFileNotFound() throws Exception {

        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppId();

        Path resourceFilePath = Path.of("src", "test", "resources", "invalid-plugins.txt");
        PluginService service = getService();
        List<Plugin> plugins = service.loadPluginsFromFile(resourceFilePath);
        assertNull(plugins);
    }

    @Test
    public void testIOExceptionWhenLoadingPluginsFromFile() {

        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppId();

        Path mockPath = mock(Path.class);

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.lines(mockPath)).thenThrow(new IOException("Mocked IOException"));
            PluginService service = getService();
            List<Plugin> result = service.loadPluginsFromFile(mockPath);
            assertNull(result, "Result should be null when IOException is thrown");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the update center service to test
     * @return Update center service
     * @throws Exception If an error occurs
     */
    private PluginService getService() throws Exception {
        PluginService service = Guice.createInjector(new GuiceModule(config)).getInstance(PluginService.class);
        Field field = ReflectionUtils.findFields(
                        PluginService.class,
                        f -> f.getName().equals("cacheManager"),
                        ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .get(0);
        field.setAccessible(true);
        field.set(service, cacheManager);
        return service;
    }

    private void setupUpdateCenterMocks() {
        doReturn(updateCenterData)
                .when(cacheManager)
                .get(cacheRoot, CacheManager.UPDATE_CENTER_CACHE_KEY, UpdateCenterData.class);
        doReturn(cacheRoot).when(cacheManager).root();
    }

    private void setupHealthScoreMocks() {
        doReturn(healthScoreData)
                .when(cacheManager)
                .get(cacheRoot, CacheManager.HEALTH_SCORE_KEY, HealthScoreData.class);
        doReturn(cacheRoot).when(cacheManager).root();
    }
}
