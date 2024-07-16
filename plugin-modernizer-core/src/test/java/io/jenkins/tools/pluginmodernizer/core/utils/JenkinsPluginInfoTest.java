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

    @BeforeEach
    public void setup() throws IOException {
        String jsonContent = "{\"plugins\": {\"login-theme\": {\"scm\": \"https://github.com/jenkinsci/login-theme-plugin\"}}}";
        Path cacheFile = tempDir.resolve("update-center.json");
        Files.write(cacheFile, jsonContent.getBytes());
    }

    @Test
    public void testExtractRepoNamePresent() {
        String result = JenkinsPluginInfo.extractRepoName("login-theme", tempDir);
        assertEquals("login-theme-plugin", result);
    }

    @Test
    public void testExtractRepoNameAbsent() {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            JenkinsPluginInfo.extractRepoName("not-present", tempDir);
        });

        assertEquals("Plugin not found in update center: not-present", exception.getMessage());
    }
}
