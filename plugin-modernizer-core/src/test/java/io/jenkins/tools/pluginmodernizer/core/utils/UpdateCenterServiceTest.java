package io.jenkins.tools.pluginmodernizer.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.google.inject.Guice;
import io.jenkins.tools.pluginmodernizer.core.GuiceModule;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.UpdateCenterData;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class})
@WireMockTest
class UpdateCenterServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Config config;

    @Mock
    private Path cacheRoot;

    // Update center test data
    private UpdateCenterData updateCenterData;

    @BeforeEach
    public void setup() throws Exception {

        updateCenterData = new UpdateCenterData(cacheManager);

        // Add plugins
        Map<String, UpdateCenterData.UpdateCenterPlugin> plugins = new HashMap<>();
        plugins.put(
                "valid-plugin",
                new UpdateCenterData.UpdateCenterPlugin(
                        "valid-plugin", "1.0", "https://github.com/jenkinsci/valid-url", "main", "gav", null));
        plugins.put(
                "invalid-plugin",
                new UpdateCenterData.UpdateCenterPlugin(
                        "invalid-plugin", "1.0", "invalid-scm-url", "main", "gav", null));
        plugins.put(
                "invalid-plugin-2",
                new UpdateCenterData.UpdateCenterPlugin("invalid-plugin-2", "1.0", "/", "main", "gav", null));

        // Set plugins
        Field field = ReflectionUtils.findFields(
                        UpdateCenterData.class,
                        f -> f.getName().equals("plugins"),
                        ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .get(0);
        field.setAccessible(true);
        field.set(updateCenterData, plugins);

        // Return this update center from the cache
        doReturn(updateCenterData)
                .when(cacheManager)
                .get(cacheRoot, CacheManager.UPDATE_CENTER_CACHE_KEY, UpdateCenterData.class);
        doReturn(cacheRoot).when(cacheManager).root();

        doReturn(cacheRoot).when(config).getCachePath();
    }

    @Test
    public void shouldExtractRepoName() throws Exception {
        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppId();
        UpdateCenterService service = getUpdateCenterService();
        String result = service.extractRepoName(Plugin.build("valid-plugin").withConfig(config));
        assertEquals("valid-url", result);
    }

    @Test
    public void shouldDownload(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {

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
        UpdateCenterService service = getUpdateCenterService();
        UpdateCenterData result = service.download();
        assertEquals(result.getPlugins().size(), updateCenterData.getPlugins().size());
    }

    @Test
    public void shouldThrowExceptionIfNotFound() throws Exception {

        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppId();

        UpdateCenterService service = getUpdateCenterService();
        Exception exception = assertThrows(ModernizerException.class, () -> {
            service.extractRepoName(Plugin.build("not-present").withConfig(config));
        });
        assertEquals("Plugin not found in update center", exception.getMessage());
    }

    @Test
    public void shouldFailIfSCMFormatIsInvalid() throws Exception {

        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppId();

        UpdateCenterService service = getUpdateCenterService();
        Exception exception = assertThrows(ModernizerException.class, () -> {
            service.extractRepoName(Plugin.build("invalid-plugin").withConfig(config));
        });
        assertEquals("Invalid SCM URL format", exception.getMessage());
    }

    /**
     * Get the update center service to test
     * @return Update center service
     * @throws Exception If an error occurs
     */
    private UpdateCenterService getUpdateCenterService() throws Exception {
        UpdateCenterService service =
                Guice.createInjector(new GuiceModule(config)).getInstance(UpdateCenterService.class);
        Field field = ReflectionUtils.findFields(
                        UpdateCenterService.class,
                        f -> f.getName().equals("cacheManager"),
                        ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .get(0);
        field.setAccessible(true);
        field.set(service, cacheManager);
        return service;
    }
}
