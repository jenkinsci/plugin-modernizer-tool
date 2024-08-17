package io.jenkins.tools.pluginmodernizer.core.config;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jenkins.tools.pluginmodernizer.core.model.JDK;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.openrewrite.Recipe;
import org.openrewrite.config.YamlResourceLoader;
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

    public static final String RECIPE_DATA_YAML_PATH = "META-INF/rewrite/recipes.yml";

    public static final String RECIPE_FQDN_PREFIX = "io.jenkins.tools.pluginmodernizer";

    public static final String ADOPTIUM_GITHUB_API_URL = "https://api.github.com/repos/adoptium";

    public static final String UPDATE_CENTER_CACHE_KEY = "update-center";

    public static final ComparableVersion MAVEN_MINIMAL_VERSION = new ComparableVersion("3.9.7");

    public static final List<Recipe> AVAILABLE_RECIPES;

    // Default JDK to use when compiling plugin
    public static final int SOURCE_JAVA_MAJOR_VERSION = JDK.getDefaultSource().getMajor();
    public static final int TARGET_JAVA_MAJOR_VERSION = JDK.getDefaultTarget().getMajor();

    private Settings() {}

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
            throw new ModernizerException("Invalid URL format", e);
        }

        // Get recipes module
        try (InputStream inputStream = Settings.class.getResourceAsStream("/" + Settings.RECIPE_DATA_YAML_PATH)) {
            YamlResourceLoader yamlResourceLoader =
                    new YamlResourceLoader(inputStream, URI.create(Settings.RECIPE_DATA_YAML_PATH), new Properties());
            AVAILABLE_RECIPES = yamlResourceLoader.listRecipes().stream().toList();
        } catch (IOException e) {
            LOG.error("Error reading recipes", e);
            throw new ModernizerException("Error reading recipes", e);
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
            return new URL(url);
        }
        return new URL(readProperty("update.center.url", "update_center.properties"));
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

    public static Path getDefaultSdkManJava(final String key) {
        String homeDir = System.getProperty("user.home") != null ? System.getProperty("user.home") : "";
        String propertyValue = readProperty(key, "sdkman.properties").replace("$HOME", homeDir);
        return Path.of(propertyValue);
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
    private static String readProperty(@NonNull final String key, @NonNull final String resource) {
        Properties properties = new Properties();
        try (InputStream input = Settings.class.getClassLoader().getResourceAsStream(resource)) {
            properties.load(input);
        } catch (IOException e) {
            LOG.error("Error reading key {} from {}", key, resource, e);
            throw new ModernizerException("Error reading key " + key + " from " + resource, e);
        }

        return properties.getProperty(key).trim();
    }
}
