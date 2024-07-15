package io.jenkins.tools.pluginmodernizer.core.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JenkinsPluginInfo {
    private static final Logger LOG = LoggerFactory.getLogger(JenkinsPluginInfo.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressFBWarnings(value = "URLCONNECTION_SSRF_FD", justification = "safe url")
    public static JsonNode getCachedJsonNode(Path cacheDir) throws IOException {
        Path cacheFile = cacheDir.resolve("update-center.json");

        if (!Files.exists(cacheDir)) {
            Files.createDirectories(cacheDir);
        }

        if (Files.exists(cacheFile)) {
            String jsonStr = Files.readString(cacheFile);
            return objectMapper.readTree(jsonStr);
        } else {
            URL apiUrl = new URL(Settings.UPDATE_CENTER_URL);
            HttpURLConnection con = null;
            BufferedReader in = null;

            try {
                con = (HttpURLConnection) apiUrl.openConnection();
                con.setRequestMethod("GET");

                in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }

                Files.writeString(cacheFile, response.toString());
                return objectMapper.readTree(response.toString());
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        LOG.error("Error closing BufferedReader: ", e);
                    }
                }
                if (con != null) {
                    con.disconnect();
                }
            }
        }
    }

    public static String extractRepoName(String pluginName, JsonNode jsonNode) {
        JsonNode plugins = jsonNode.get("plugins");
        if (plugins == null || !plugins.has(pluginName)) {
            throw new RuntimeException("Plugin not found in update center: " + pluginName);
        }

        JsonNode pluginInfo = plugins.get(pluginName);

        JsonNode scmNode = pluginInfo.get("scm");
        String scmUrl;
        if (scmNode.isObject()) {
            scmUrl = scmNode.get("url").asText();
        } else if (scmNode.isTextual()) {
            scmUrl = scmNode.asText();
        } else {
            throw new RuntimeException("Unexpected type for SCM URL: " + scmNode.getNodeType());
        }

        int lastSlashIndex = scmUrl.lastIndexOf('/');
        if (lastSlashIndex != -1 && lastSlashIndex < scmUrl.length() - 1) {
            return scmUrl.substring(lastSlashIndex + 1);
        } else {
            throw new RuntimeException("Invalid SCM URL format: " + scmUrl);
        }
    }
}
