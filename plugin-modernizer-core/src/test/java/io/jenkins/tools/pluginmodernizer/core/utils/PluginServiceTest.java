package io.jenkins.tools.pluginmodernizer.core.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.google.inject.Guice;
import io.jenkins.tools.pluginmodernizer.core.GuiceModule;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import io.jenkins.tools.pluginmodernizer.core.model.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.Mock;
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

    // Plugin installation stats test data
    private PluginInstallationStatsData pluginInstallationStatsData;

    @BeforeEach
    public void setup() throws Exception {

        updateCenterData = new UpdateCenterData(cacheManager);
        healthScoreData = new HealthScoreData(cacheManager);
        pluginInstallationStatsData = new PluginInstallationStatsData(cacheManager);

        Map<String, UpdateCenterData.UpdateCenterPlugin> updateCenterPlugins = new HashMap<>();
        updateCenterPlugins.put(
                "valid-plugin",
                new UpdateCenterData.UpdateCenterPlugin(
                        "valid-plugin", "1.0", "https://github.com/jenkinsci/valid-url", "main", "gav", null));
        updateCenterPlugins.put(
                "valid-plugin-2",
                new UpdateCenterData.UpdateCenterPlugin(
                        "valid-plugin", "1.0", "git@github.com/jenkinsci/valid-git-repo.git", "main", "gav", null));
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

        // Add installations
        Map<String, Integer> installations = new HashMap<>();
        installations.put("valid-plugin", 1000);
        installations.put("valid-plugin2", 500);

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

        Field pluginInstallationDataField = ReflectionUtils.findFields(
                        PluginInstallationStatsData.class,
                        f -> f.getName().equals("plugins"),
                        ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .get(0);
        pluginInstallationDataField.setAccessible(true);
        pluginInstallationDataField.set(pluginInstallationStatsData, healthPlugins);
    }

    @Test
    public void shouldExtractRepoName() throws Exception {
        setupUpdateCenterMocks();
        PluginService service = getService();
        String result = service.extractRepoName(Plugin.build("valid-plugin").withConfig(config));
        assertEquals("valid-url", result);
    }

    @Test
    public void shouldExtractRepoNameWithGitSuffix() throws Exception {
        setupUpdateCenterMocks();
        PluginService service = getService();
        String result = service.extractRepoName(Plugin.build("valid-plugin-2").withConfig(config));
        assertEquals("valid-git-repo", result);
    }

    @Test
    public void shouldDownloadPluginVersionDataUpdateCenterData(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {

        setupUpdateCenterMocks();

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

        PluginService service = getService();
        Exception exception = assertThrows(ModernizerException.class, () -> {
            service.extractRepoName(Plugin.build("not-present").withConfig(config));
        });
        assertEquals("Plugin not found in update center", exception.getMessage());
    }

    @Test
    public void shouldFailIfSCMFormatIsInvalid() throws Exception {

        setupUpdateCenterMocks();

        PluginService service = getService();
        Exception exception = assertThrows(ModernizerException.class, () -> {
            service.extractRepoName(Plugin.build("invalid-plugin").withConfig(config));
        });
        assertEquals("Invalid SCM URL format", exception.getMessage());
    }

    @Test
    public void shouldDownloadPluginVersionDataPluginHealthScore(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {

        setupHealthScoreMocks();

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
    public void shouldDownloadPluginInstallationsData(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {

        setupHealthScoreMocks();

        // Download through wiremock to avoid hitting the real stats
        WireMock wireMock = wmRuntimeInfo.getWireMock();

        wireMock.register(WireMock.get(WireMock.urlEqualTo("/jenkins-stats/svg/202406-plugins.csv"))
                .willReturn(WireMock.ok("\"valid-plugin\",\"1\"\n" + "\"valid-plugin2\",\"1\"")));

        // No found from cache
        doReturn(new URL(wmRuntimeInfo.getHttpBaseUrl() + "/jenkins-stats/svg/202406-plugins.csv"))
                .when(config)
                .getPluginStatsInstallations();

        Mockito.reset(cacheManager);

        // Get result
        PluginService service = getService();
        PluginInstallationStatsData result = service.downloadInstallationStatsData();
        assertEquals(
                result.getPlugins().size(),
                pluginInstallationStatsData.getPlugins().size());
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
