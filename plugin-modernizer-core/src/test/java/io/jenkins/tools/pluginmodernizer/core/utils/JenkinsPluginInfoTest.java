package io.jenkins.tools.pluginmodernizer.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

class JenkinsPluginInfoTest {

    @TempDir
    private Path tempDir1;

    @TempDir
    private Path tempDir2;

    private Path cacheFile;

    private CacheManager cacheManager1;

    private CacheManager cacheManager2;

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
    void teardown() throws IOException {
        Files.deleteIfExists(cacheFile);
        Files.deleteIfExists(tempDir1);
    }

    @Test
    public void testExtractRepoNamePluginPresentWithoutUrl() {
        String result = JenkinsPluginInfo.extractRepoName(
                Plugin.build("login-theme").withConfig(Mockito.mock(Config.class)), cacheManager1, null);
        assertEquals("login-theme-plugin", result);
    }

    @Test
    public void testExtractRepoNamePluginAbsentWithoutUrl() {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            JenkinsPluginInfo.extractRepoName(
                    Plugin.build("not-present").withConfig(Mockito.mock(Config.class)), cacheManager1, null);
        });

        assertEquals("Plugin not found in update center", exception.getMessage());
    }

    @Test
    public void testExtractRepoNamePluginPresentWithUrl() throws MalformedURLException {
        URL updateCenterUrl = new URL("https://updates.jenkins.io/current/update-center.actual.json");

        String resultLoginTheme = JenkinsPluginInfo.extractRepoName(
                Plugin.build("login-theme").withConfig(Mockito.mock(Config.class)), cacheManager2, updateCenterUrl);
        assertEquals("login-theme-plugin", resultLoginTheme);

        String resultJobCacher = JenkinsPluginInfo.extractRepoName(
                Plugin.build("jobcacher").withConfig(Mockito.mock(Config.class)), cacheManager2, updateCenterUrl);
        assertEquals("jobcacher-plugin", resultJobCacher);
    }

    @Test
    public void testExtractRepoNamePluginAbsentWithUrl() throws MalformedURLException {
        URL updateCenterUrl = new URL("https://updates.jenkins.io/current/update-center.actual.json");

        Exception exception = assertThrows(RuntimeException.class, () -> {
            JenkinsPluginInfo.extractRepoName(
                    Plugin.build("not-present").withConfig(Mockito.mock(Config.class)), cacheManager2, updateCenterUrl);
        });

        assertEquals("Plugin not found in update center", exception.getMessage());
    }

    @Test
    public void testExtractRepoNameInvalidScmUrlFormat() {
        Exception exception = assertThrows(ModernizerException.class, () -> {
            JenkinsPluginInfo.extractRepoName(
                    Plugin.build("invalid-plugin").withConfig(Mockito.mock(Config.class)), cacheManager1, null);
        });
        assertEquals("Invalid SCM URL format", exception.getMessage());
    }

    @Test
    public void testExtractRepoNameInvalidScmUrlFormat2() {
        Exception exception = assertThrows(ModernizerException.class, () -> {
            JenkinsPluginInfo.extractRepoName(
                    Plugin.build("invalid-plugin-2").withConfig(Mockito.mock(Config.class)), cacheManager1, null);
        });
        assertEquals("Invalid SCM URL format", exception.getMessage());
    }

    @Test
    public void testInvalidJsonFormatWithProvidedUrl() throws MalformedURLException {
        URL updateCenterUrl = new URL("https://www.example.com");

        // Should fetch from default UC url as a fallback
        String resultLoginTheme = JenkinsPluginInfo.extractRepoName(
                Plugin.build("login-theme").withConfig(Mockito.mock(Config.class)), cacheManager2, updateCenterUrl);
        assertEquals("login-theme-plugin", resultLoginTheme);
    }
}
