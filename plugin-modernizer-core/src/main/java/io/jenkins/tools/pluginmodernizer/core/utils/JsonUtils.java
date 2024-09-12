package io.jenkins.tools.pluginmodernizer.core.utils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonUtils {

    private static final Logger LOG = LoggerFactory.getLogger(JsonUtils.class);

    private static final Gson gson;

    private JsonUtils() {
        // Hide constructor
    }

    static {
        gson = new Gson();
    }

    /**
     * Convert an object to a JSON string
     * @param object The object to convert
     * @return The JSON string
     */
    public static String toJson(Object object) {
        return gson.toJson(object);
    }

    /**
     * Convert an object to a JSON file
     * @param object The object to convert
     * @param path The path to the JSON file
     */
    public static void toJsonFile(Object object, Path path) {
        try {
            LOG.trace("Writing JSON file to {}", path);
            FileUtils.writeStringToFile(path.toFile(), gson.toJson(object), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ModernizerException("Unable to write JSON file due to IO error", e);
        }
    }

    /**
     * Convert a JSON string to an object
     * @param json The JSON string
     * @param clazz The class of the object
     * @param <T> The type of the object
     * @return The object
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    /**
     * Convert a JSON string to an object
     * @param path The path to the JSON file
     * @param clazz The class of the object
     * @param <T> The type of the object
     * @return The object
     */
    public static <T> T fromJson(Path path, Class<T> clazz) {
        try {
            return gson.fromJson(FileUtils.readFileToString(path.toFile(), StandardCharsets.UTF_8), clazz);
        } catch (IOException e) {
            throw new ModernizerException("Unable to read JSON file due to IO error", e);
        }
    }

    /**
     * Download JSON data from a URL and convert it to an object
     * @param url The URL to download from
     * @param clazz The class of the object
     * @return The object
     * @param <T> The type of the object
     */
    public static <T> T fromUrl(URL url, Class<T> clazz) {
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
                        "Failed to get JSON data. Received response code: " + response.statusCode());
            }
            LOG.debug("Fetched data from: {}", url);
            return JsonUtils.fromJson(response.body(), clazz);
        } catch (IOException | JsonSyntaxException | URISyntaxException | InterruptedException e) {
            throw new ModernizerException("Unable to fetch data from " + url, e);
        }
    }
}
