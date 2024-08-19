package io.jenkins.tools.pluginmodernizer.core.utils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.UpdateCenterData;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JenkinsPluginInfo {
    private static final Logger LOG = LoggerFactory.getLogger(JenkinsPluginInfo.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String extractRepoName(Plugin plugin, CacheManager cacheManager, URL updateCenterUrl) {
        try {
            UpdateCenterData updateCenterData = getCachedUpdateCenterData(cacheManager, updateCenterUrl);
            String scmUrl = updateCenterData.getScmUrl(plugin);

            int lastSlashIndex = scmUrl.lastIndexOf('/');
            if (lastSlashIndex != -1 && lastSlashIndex < scmUrl.length() - 1) {
                return scmUrl.substring(lastSlashIndex + 1);
            } else {
                plugin.addError("Invalid SCM URL format");
                plugin.raiseLastError();
            }
        } catch (IOException e) {
            plugin.addError("Error extracting repository name", e);
            plugin.raiseLastError();
        }
        return null;
    }

    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS", justification = "false positive")
    private static UpdateCenterData getCachedUpdateCenterData(CacheManager cacheManager, URL updateCenterUrl)
            throws IOException {
        String cacheKey = CacheManager.UPDATE_CENTER_CACHE_KEY;

        try {
            String cachedData = cacheManager.get(cacheManager.root(), cacheKey);
            return parseJson(cacheManager, cachedData);
        } catch (JsonParseException e) {
            LOG.error("Failed to parse JSON, deleting cache entry for key: {}", cacheKey, e);
            cacheManager.remove(Path.of("."), cacheKey);
        } catch (ModernizerException e) {
            LOG.debug("Update center data not found in cache", e);
        }

        String jsonStr = fetchUpdateCenterData(updateCenterUrl);
        if (!isValidJson(jsonStr)) {
            LOG.warn(
                    "Invalid JSON format from URL: {}. Falling back to default URL {}.",
                    updateCenterUrl,
                    Settings.DEFAULT_UPDATE_CENTER_URL);
            jsonStr = fetchUpdateCenterData(Settings.DEFAULT_UPDATE_CENTER_URL);
        }

        UpdateCenterData updateCenterData = parseJson(cacheManager, jsonStr);
        cacheManager.put(cacheManager.root(), cacheKey, jsonStr);

        return updateCenterData;
    }

    private static boolean isValidJson(String jsonStr) {
        return jsonStr != null && jsonStr.startsWith("{");
    }

    private static UpdateCenterData parseJson(CacheManager cacheManager, String jsonStr) throws IOException {
        JsonNode jsonNode = objectMapper.readTree(jsonStr);
        return new UpdateCenterData(cacheManager, jsonNode);
    }

    @SuppressFBWarnings(value = "URLCONNECTION_SSRF_FD", justification = "safe url")
    private static String fetchUpdateCenterData(URL updateCenterUrl) {
        StringBuilder response = new StringBuilder();

        try (BufferedReader in =
                new BufferedReader(new InputStreamReader(updateCenterUrl.openStream(), StandardCharsets.UTF_8))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        } catch (IOException e) {
            throw new ModernizerException("Error fetching update center data", e);
        }

        return response.toString();
    }
}
