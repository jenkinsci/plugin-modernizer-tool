package io.jenkins.tools.pluginmodernizer.core.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.model.UpdateCenterData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JenkinsPluginInfo {
    private static final Logger LOG = LoggerFactory.getLogger(JenkinsPluginInfo.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String extractRepoName(String pluginName, Path cacheDir, String updateCenterUrl) {
        try {
            UpdateCenterData updateCenterData = getCachedUpdateCenterData(cacheDir, updateCenterUrl);
            String scmUrl = updateCenterData.getScmUrl(pluginName);

            int lastSlashIndex = scmUrl.lastIndexOf('/');
            if (lastSlashIndex != -1 && lastSlashIndex < scmUrl.length() - 1) {
                return scmUrl.substring(lastSlashIndex + 1);
            } else {
                throw new IllegalArgumentException("Invalid SCM URL format: " + scmUrl);
            }
        } catch (IOException e) {
            LOG.error("Error extracting repository name: ", e);
            throw new UncheckedIOException(e);
        }
    }

    private static UpdateCenterData getCachedUpdateCenterData(Path cacheDir, String updateCenterUrl) throws IOException {
        Path cacheFile = cacheDir.resolve("update-center.json");

        if (Files.exists(cacheFile)) {
            String jsonStr = Files.readString(cacheFile);
            return parseJson(jsonStr);
        } else {
            String jsonStr = fetchUpdateCenterData(updateCenterUrl);
            Files.writeString(cacheFile, jsonStr);
            return parseJson(jsonStr);
        }
    }

    private static UpdateCenterData parseJson(String jsonStr) throws IOException {
        JsonNode jsonNode = objectMapper.readTree(jsonStr);
        return new UpdateCenterData(jsonNode);
    }

    @SuppressFBWarnings(value = "URLCONNECTION_SSRF_FD", justification = "safe url")
    private static String fetchUpdateCenterData(String updateCenterUrl) throws IOException {
        URL apiUrl = new URL(updateCenterUrl);

        StringBuilder response = new StringBuilder();

        try (BufferedReader in = new BufferedReader(new InputStreamReader(apiUrl.openStream(), StandardCharsets.UTF_8))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        } catch (IOException e) {
            throw new IOException("Error fetching update center data", e);
        }

        return response.toString();
    }
}
