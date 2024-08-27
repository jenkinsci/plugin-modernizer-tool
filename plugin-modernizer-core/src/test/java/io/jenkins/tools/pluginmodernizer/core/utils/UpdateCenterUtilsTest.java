package io.jenkins.tools.pluginmodernizer.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.UpdateCenterData;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
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
class UpdateCenterUtilsTest {

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
    }

    @Test
    public void shouldExtractRepoName() {
        String result =
                UpdateCenterUtils.extractRepoName(Plugin.build("valid-plugin").withConfig(config), cacheManager);
        assertEquals("valid-url", result);
    }

    @Test
    public void shouldDownload(WireMockRuntimeInfo wmRuntimeInfo) throws MalformedURLException {

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
        UpdateCenterData result = UpdateCenterUtils.download(config);
        assertEquals(result.getPlugins().size(), updateCenterData.getPlugins().size());
    }

    @Test
    public void shouldThrowExceptionIfNotFound() {
        Exception exception = assertThrows(ModernizerException.class, () -> {
            UpdateCenterUtils.extractRepoName(Plugin.build("not-present").withConfig(config), cacheManager);
        });
        assertEquals("Plugin not found in update center", exception.getMessage());
    }

    @Test
    public void shouldFailIfSCMFormatIsInvalid() {
        Exception exception = assertThrows(ModernizerException.class, () -> {
            UpdateCenterUtils.extractRepoName(Plugin.build("invalid-plugin").withConfig(config), cacheManager);
        });
        assertEquals("Invalid SCM URL format", exception.getMessage());
    }
}
