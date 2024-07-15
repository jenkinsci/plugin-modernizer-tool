package io.jenkins.tools.pluginmodernizer.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JenkinsPluginInfoTest {

    @TempDir
    Path tempDir;

    @Test
    public void testGetCachedJsonNode() throws IOException {
        String jsonContent = "{\"plugins\": {\"login-theme\": {\"scm\": \"https://github.com/jenkinsci/login-theme-plugin\"}}}";
        Path cacheFile = tempDir.resolve("update-center.json");
        Files.write(cacheFile, jsonContent.getBytes());

        JsonNode jsonNode = JenkinsPluginInfo.getCachedJsonNode(tempDir);

        assertTrue(jsonNode.has("plugins"));
        assertEquals("https://github.com/jenkinsci/login-theme-plugin",
                jsonNode.get("plugins").get("login-theme").get("scm").asText());
    }

    @Test
    public void testExtractRepoName() throws IOException {
        String jsonContent = "{\"plugins\": {\"login-theme\": {\"scm\": \"https://github.com/jenkinsci/login-theme-plugin\"}}}";
        Path cacheFile = tempDir.resolve("update-center.json");
        Files.write(cacheFile, jsonContent.getBytes());

        JsonNode jsonNode = JenkinsPluginInfo.getCachedJsonNode(tempDir);
        String result = JenkinsPluginInfo.extractRepoName("login-theme", jsonNode);
        assertEquals("login-theme-plugin", result);
    }

}