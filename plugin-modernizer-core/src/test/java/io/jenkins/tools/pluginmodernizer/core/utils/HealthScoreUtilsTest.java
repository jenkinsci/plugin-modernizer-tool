package io.jenkins.tools.pluginmodernizer.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import io.jenkins.tools.pluginmodernizer.core.model.HealthScoreData;
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
public class HealthScoreUtilsTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Config config;

    @Mock
    private Path cacheRoot;

    private HealthScoreData healthScoreData;

    @BeforeEach
    public void setup() throws Exception {

        healthScoreData = new HealthScoreData(cacheManager);

        // Add plugins
        Map<String, HealthScoreData.HealthScorePlugin> plugins = new HashMap<>();
        plugins.put("valid-plugin", new HealthScoreData.HealthScorePlugin(100d));
        plugins.put("valid-plugin2", new HealthScoreData.HealthScorePlugin(50d));

        // Set plugins
        Field field = ReflectionUtils.findFields(
                        HealthScoreData.class,
                        f -> f.getName().equals("plugins"),
                        ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .get(0);
        field.setAccessible(true);
        field.set(healthScoreData, plugins);

        // Return this update center from the cache
        doReturn(healthScoreData)
                .when(cacheManager)
                .get(cacheRoot, CacheManager.HEALTH_SCORE_KEY, HealthScoreData.class);
        doReturn(cacheRoot).when(cacheManager).root();
    }

    @Test
    public void shouldDownload(WireMockRuntimeInfo wmRuntimeInfo) throws MalformedURLException {

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
        HealthScoreData result = HealthScoreUtils.download(config);
        assertEquals(result.getPlugins().size(), healthScoreData.getPlugins().size());
    }
}
