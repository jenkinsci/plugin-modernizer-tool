package io.jenkins.tools.pluginmodernizer.core.utils;

import com.google.gson.JsonSyntaxException;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSVUtils {

    private static final Logger LOG = LoggerFactory.getLogger(CSVUtils.class);

    private CSVUtils() {
        // Hide constructor
    }

    /**
     * Download CSV data from a URL and convert it to an object
     * @param url The URL to download from
     * @return The object
     */
    public static String fromUrl(URL url) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request =
                    HttpRequest.newBuilder().GET().uri(url.toURI()).build();
            LOG.debug("Fetching data from: {}", url);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ModernizerException(
                        "Failed to get CSV data. Received response code: " + response.statusCode());
            }
            LOG.debug("Fetched data from: {}", url);
            return response.body();
        } catch (IOException | JsonSyntaxException | URISyntaxException | InterruptedException e) {
            throw new ModernizerException("Unable to fetch data from " + url, e);
        }
    }

    /**
     * Parse a 2 column CSV stats
     * @param data The CSV data
     * @return The parsed stats
     */
    public static Map<String, Integer> parseStats(String data) {
        Map<String, Integer> stats = new HashMap<>();
        String[] lines = data.split("\n");
        for (String line : lines) {
            String[] columns = line.split(",");
            if (columns.length == 2) {
                String pluginName = columns[0].trim().replace("\"", "");
                Integer installations = Integer.parseInt(columns[1].trim().replace("\"", ""));
                stats.put(pluginName, installations);
            }
        }
        return stats;
    }
}
