package io.jenkins.tools.pluginmodernizer.core.config;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Settings {

    private static final Logger LOG = LoggerFactory.getLogger(Settings.class);

    public static final URL DEFAULT_UPDATE_CENTER_URL;

    public static final Path DEFAULT_CACHE_PATH;

    public static final Path DEFAULT_MAVEN_HOME;

    public static final String MAVEN_REWRITE_PLUGIN_VERSION;

    public static final String GITHUB_TOKEN;

    public static final String GITHUB_OWNER;

    public static final String TEST_PLUGINS_DIRECTORY;

    public static final String ORGANIZATION = "jenkinsci";

    public static final String RECIPE_DATA_YAML_PATH = "recipe_data.yaml";

    public static final ComparableVersion MAVEN_MINIMAL_VERSION = new ComparableVersion("3.9.7");

    static {
        String cacheBaseDir = System.getProperty("user.home");
        if (cacheBaseDir == null) {
            cacheBaseDir = System.getProperty("user.dir");
        }

        String cacheDirFromEnv = System.getenv("CACHE_DIR");
        if (cacheDirFromEnv == null) {
            DEFAULT_CACHE_PATH = Paths.get(cacheBaseDir, ".cache", "jenkins-plugin-modernizer-cli");
        } else {
            DEFAULT_CACHE_PATH = Paths.get(cacheDirFromEnv);
        }
        DEFAULT_MAVEN_HOME = getDefaultMavenHome();
        MAVEN_REWRITE_PLUGIN_VERSION = getRewritePluginVersion();
        GITHUB_TOKEN = getGithubToken();
        GITHUB_OWNER = getGithubOwner();
        TEST_PLUGINS_DIRECTORY = getTestPluginsDirectory();
        try {
            DEFAULT_UPDATE_CENTER_URL = getUpdateCenterUrl();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL format", e);
        }
    }

    private static Path getDefaultMavenHome() {
        String mavenHome = System.getenv("MAVEN_HOME");
        if (mavenHome == null) {
            mavenHome = System.getenv("M2_HOME");
        }
        if (mavenHome == null) {
            return null;
        }
        return Path.of(mavenHome);
    }

    private static @Nullable String getRewritePluginVersion() {
        return readProperty("openrewrite.maven.plugin.version", "versions.properties");
    }

    private static @Nullable URL getUpdateCenterUrl() throws MalformedURLException {
        String url = System.getenv("JENKINS_UC");

        if (url != null) {
            if (isValidURL(url)) {
                return new URL(url);
            } else {
                LOG.warn("Invalid URL in JENKINS_UC environment variable: {}", url);
                LOG.info("Using Default Update Center URL");
            }
        }

        url = readProperty("update.center.url", "update_center.properties");
        return new URL(url);
    }

    private static boolean isValidURL(String url) {
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private static String getGithubToken() {
        String token = System.getenv("GH_TOKEN");
        if (token == null) {
            token = System.getenv("GITHUB_TOKEN");
        }
        return token;
    }

    private static String getGithubOwner() {
        String username = System.getenv("GH_OWNER");
        if (username == null) {
            username = System.getenv("GITHUB_OWNER");
        }
        return username;
    }

    private static String getTestPluginsDirectory() {
        return System.getProperty("user.dir") + "/test-plugins/";
    }

    /**
     * Read a property from a resource file.
     * @param key The key to read
     * @param resource The resource file to read from
     * @return The value of the property or null if it could not be read
     */
    private static @Nullable String readProperty(@NonNull final String key, @NonNull final String resource) {
        Properties properties = new Properties();
        try (InputStream input = Settings.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                LOG.error("Error reading {} from settings", resource);
                throw new IOException(String.format("Unable to load `%s`", resource));
            }
            properties.load(input);
        } catch (IOException e) {
            LOG.error("Error reading key {} from {}", key, resource, e);
            return null;
        }

        String value = properties.getProperty(key);
        if (value == null || value.isEmpty()) {
            LOG.error(String.format("Unable to read `%s` from `%s`", key, resource));
            return null;
        }

        return value.trim();
    }
}
