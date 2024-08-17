package io.jenkins.tools.pluginmodernizer.core.utils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import io.jenkins.tools.pluginmodernizer.core.model.UpdateCenterData;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JenkinsPluginInfo {
    private static final Logger LOG = LoggerFactory.getLogger(JenkinsPluginInfo.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String extractRepoName(String pluginName, CacheManager cacheManager, URL updateCenterUrl) {
        try {
            UpdateCenterData updateCenterData = getCachedUpdateCenterData(cacheManager, updateCenterUrl);
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

    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS", justification = "false positive")
    private static UpdateCenterData getCachedUpdateCenterData(CacheManager cacheManager, URL updateCenterUrl)
            throws IOException {
        String cacheKey = Settings.UPDATE_CENTER_CACHE_KEY;
        String cachedData = cacheManager.retrieveFromCache(cacheKey);

        if (cachedData != null) {
            try {
                return parseJson(cachedData);
            } catch (JsonParseException e) {
                LOG.error("Failed to parse JSON, deleting cache entry for key: {}", cacheKey, e);
                cacheManager.removeFromCache(cacheKey);
            }
        }

        String jsonStr = fetchUpdateCenterData(updateCenterUrl);
        if (!isValidJson(jsonStr)) {
            LOG.warn(
                    "Invalid JSON format from URL: {}. Falling back to default URL {}.",
                    updateCenterUrl,
                    Settings.DEFAULT_UPDATE_CENTER_URL);
            jsonStr = fetchUpdateCenterData(Settings.DEFAULT_UPDATE_CENTER_URL);
        }

        UpdateCenterData updateCenterData = parseJson(jsonStr);
        cacheManager.addToCache(cacheKey, jsonStr);

        return updateCenterData;
    }

    private static boolean isValidJson(String jsonStr) {
        return jsonStr != null && jsonStr.startsWith("{") && jsonStr.startsWith("[");
    }

    private static UpdateCenterData parseJson(String jsonStr) throws IOException {
        JsonNode jsonNode = objectMapper.readTree(jsonStr);
        return new UpdateCenterData(jsonNode);
    }

    @SuppressFBWarnings(value = "URLCONNECTION_SSRF_FD", justification = "safe url")
    private static String fetchUpdateCenterData(URL updateCenterUrl) throws IOException {
        StringBuilder response = new StringBuilder();

        try (BufferedReader in =
                new BufferedReader(new InputStreamReader(updateCenterUrl.openStream(), StandardCharsets.UTF_8))) {
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
