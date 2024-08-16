package io.jenkins.tools.pluginmodernizer.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
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
    private Path tempDir;

    @TempDir
    private Path tempDir2;

    private Path cacheFile;

    @BeforeEach
    public void setup() throws IOException {
        String jsonContent =
                "{\"plugins\": {\"login-theme\": {\"scm\": \"https://github.com/jenkinsci/login-theme-plugin\"}, \"invalid-plugin\": {\"scm\": \"invalid-scm-url\"}, \"invalid-plugin-2\": {\"scm\": \"/\"}}}";
        cacheFile = tempDir.resolve("update-center.json");
        Files.write(cacheFile, jsonContent.getBytes());
    }

    @AfterEach
    void teardown() throws IOException {
        Files.deleteIfExists(cacheFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    public void testExtractRepoNamePluginPresentWithoutUrl() {
        String result = JenkinsPluginInfo.extractRepoName(
                Plugin.build("login-theme").withConfig(Mockito.mock(Config.class)), tempDir, null);
        assertEquals("login-theme-plugin", result);
    }

    @Test
    public void testExtractRepoNamePluginAbsentWithoutUrl() {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            JenkinsPluginInfo.extractRepoName(
                    Plugin.build("not-present").withConfig(Mockito.mock(Config.class)), tempDir, null);
        });

        assertEquals("Plugin not found in update center", exception.getMessage());
    }

    @Test
    public void testExtractRepoNamePluginPresentWithUrl() throws MalformedURLException {
        URL updateCenterUrl = new URL("https://updates.jenkins.io/current/update-center.actual.json");

        String resultLoginTheme = JenkinsPluginInfo.extractRepoName(
                Plugin.build("login-theme").withConfig(Mockito.mock(Config.class)), tempDir2, updateCenterUrl);
        assertEquals("login-theme-plugin", resultLoginTheme);

        String resultJobCacher = JenkinsPluginInfo.extractRepoName(
                Plugin.build("jobcacher").withConfig(Mockito.mock(Config.class)), tempDir2, updateCenterUrl);
        assertEquals("jobcacher-plugin", resultJobCacher);
    }

    @Test
    public void testExtractRepoNamePluginAbsentWithUrl() throws MalformedURLException {
        URL updateCenterUrl = new URL("https://updates.jenkins.io/current/update-center.actual.json");

        Exception exception = assertThrows(RuntimeException.class, () -> {
            JenkinsPluginInfo.extractRepoName(
                    Plugin.build("not-present").withConfig(Mockito.mock(Config.class)), tempDir2, updateCenterUrl);
        });

        assertEquals("Plugin not found in update center", exception.getMessage());
    }

    @Test
    public void testExtractRepoNameInvalidScmUrlFormat() {
        Exception exception = assertThrows(ModernizerException.class, () -> {
            JenkinsPluginInfo.extractRepoName(
                    Plugin.build("invalid-plugin").withConfig(Mockito.mock(Config.class)), tempDir, null);
        });
        assertEquals("Invalid SCM URL format", exception.getMessage());
    }

    @Test
    public void testExtractRepoNameInvalidScmUrlFormat2() {
        Exception exception = assertThrows(ModernizerException.class, () -> {
            JenkinsPluginInfo.extractRepoName(
                    Plugin.build("invalid-plugin-2").withConfig(Mockito.mock(Config.class)), tempDir, null);
        });
        assertEquals("Invalid SCM URL format", exception.getMessage());
    }
}
