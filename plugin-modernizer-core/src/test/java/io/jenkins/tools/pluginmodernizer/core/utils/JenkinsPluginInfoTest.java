package io.jenkins.tools.pluginmodernizer.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JenkinsPluginInfoTest {

    @TempDir
    Path tempDir;

    @TempDir
    Path tempDir2;

    @BeforeEach
    public void setup() throws IOException {
        String jsonContent = "{\"plugins\": {\"login-theme\": {\"scm\": \"https://github.com/jenkinsci/login-theme-plugin\"}}}";
        Path cacheFile = tempDir.resolve("update-center.json");
        Files.write(cacheFile, jsonContent.getBytes());
    }

    @Test
    public void testExtractRepoNamePluginPresentWithoutUrl() {
        String result = JenkinsPluginInfo.extractRepoName("login-theme", tempDir, null);
        assertEquals("login-theme-plugin", result);
    }

    @Test
    public void testExtractRepoNamePluginAbsentWithoutUrl() {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            JenkinsPluginInfo.extractRepoName("not-present", tempDir, null);
        });

        assertEquals("Plugin not found in update center: not-present", exception.getMessage());
    }

    @Test
    public void testExtractRepoNamePluginPresentWithUrl() {
        String updateCenterUrl = "https://updates.jenkins.io/current/update-center.actual.json";

        String resultLoginTheme = JenkinsPluginInfo.extractRepoName("login-theme", tempDir2, updateCenterUrl);
        assertEquals("login-theme-plugin", resultLoginTheme);

        String resultJobCacher = JenkinsPluginInfo.extractRepoName("jobcacher", tempDir2, updateCenterUrl);
        assertEquals("jobcacher-plugin", resultJobCacher);
    }

    @Test
    public void testExtractRepoNamePluginAbsentWithUrl() {
        String updateCenterUrl = "https://updates.jenkins.io/current/update-center.actual.json";

        Exception exception = assertThrows(RuntimeException.class, () -> {
            JenkinsPluginInfo.extractRepoName("not-present", tempDir2, updateCenterUrl);
        });

        assertEquals("Plugin not found in update center: not-present", exception.getMessage());
    }
}
