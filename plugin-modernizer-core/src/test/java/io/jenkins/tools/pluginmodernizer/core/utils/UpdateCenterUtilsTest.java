package io.jenkins.tools.pluginmodernizer.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class})
class UpdateCenterUtilsTest {

    @TempDir
    private Path tempDir1;

    @TempDir
    private Path tempDir2;

    private Path cacheFile;

    @Mock
    private CacheManager cacheManager1;

    @Mock
    private CacheManager cacheManager2;

    @Mock
    private Config config;

    @BeforeEach
    public void setup() throws IOException {
        cacheManager1 = new CacheManager(tempDir1);
        cacheManager2 = new CacheManager(tempDir2);
        String jsonContent =
                "{\"plugins\": {\"login-theme\": {\"scm\": \"https://github.com/jenkinsci/login-theme-plugin\"}, \"invalid-plugin\": {\"scm\": \"invalid-scm-url\"}, \"invalid-plugin-2\": {\"scm\": \"/\"}}}";
        cacheFile = tempDir1.resolve("update-center.json");
        Files.write(cacheFile, jsonContent.getBytes());
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(cacheFile);
        FileUtils.deleteDirectory(tempDir1.toFile());
    }

    @Test
    public void testExtractRepoNamePluginPresentWithoutUrl() {
        doReturn(Settings.DEFAULT_UPDATE_CENTER_URL).when(config).getJenkinsUpdateCenter();
        String result =
                UpdateCenterUtils.extractRepoName(Plugin.build("login-theme").withConfig(config), cacheManager1);
        assertEquals("login-theme-plugin", result);
    }

    @Test
    public void testExtractRepoNamePluginAbsentWithoutUrl() {
        doReturn(Settings.DEFAULT_UPDATE_CENTER_URL).when(config).getJenkinsUpdateCenter();
        Exception exception = assertThrows(RuntimeException.class, () -> {
            UpdateCenterUtils.extractRepoName(Plugin.build("not-present").withConfig(config), cacheManager1);
        });

        assertEquals("Plugin not found in update center: not-present", exception.getMessage());
    }

    @Test
    public void testExtractRepoNamePluginPresentWithUrl() throws MalformedURLException {
        URL updateCenterUrl = new URL("https://updates.jenkins.io/current/update-center.actual.json");
        doReturn(updateCenterUrl).when(config).getJenkinsUpdateCenter();

        String resultLoginTheme =
                UpdateCenterUtils.extractRepoName(Plugin.build("login-theme").withConfig(config), cacheManager2);
        assertEquals("login-theme-plugin", resultLoginTheme);

        String resultJobCacher =
                UpdateCenterUtils.extractRepoName(Plugin.build("jobcacher").withConfig(config), cacheManager2);
        assertEquals("jobcacher-plugin", resultJobCacher);
    }

    @Test
    public void testExtractRepoNamePluginAbsentWithUrl() throws MalformedURLException {
        URL updateCenterUrl = new URL("https://updates.jenkins.io/current/update-center.actual.json");
        doReturn(updateCenterUrl).when(config).getJenkinsUpdateCenter();

        Exception exception = assertThrows(RuntimeException.class, () -> {
            UpdateCenterUtils.extractRepoName(Plugin.build("not-present").withConfig(config), cacheManager2);
        });

        assertEquals("Plugin not found in update center: not-present", exception.getMessage());
    }

    @Test
    public void testInvalidJsonFormatWithProvidedUrl() throws MalformedURLException {
        URL updateCenterUrl = new URL("https://www.example.com");
        doReturn(updateCenterUrl).when(config).getJenkinsUpdateCenter();
        Exception exception = assertThrows(ModernizerException.class, () -> {
            UpdateCenterUtils.extractRepoName(Plugin.build("login-theme").withConfig(config), cacheManager2);
        });
        assertEquals("Unable to fetch update center data", exception.getMessage());
    }
}
