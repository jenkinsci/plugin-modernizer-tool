package io.jenkins.tools.pluginmodernizer.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.Recipe;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Settings {

    private static final Logger LOG = LoggerFactory.getLogger(Settings.class);

    public static final URL DEFAULT_UPDATE_CENTER_URL;

    public static final URL DEFAULT_PLUGIN_VERSIONS;

    public static final URL DEFAULT_PLUGINS_STATS_INSTALLATIONS_URL;

    public static final URL DEFAULT_HEALTH_SCORE_URL;

    public static final URL GITHUB_API_URL;

    public static final Path DEFAULT_CACHE_PATH;
    public static final String CACHE_SUBDIR = "jenkins-plugin-modernizer-cli";

    public static final Path DEFAULT_MAVEN_HOME;

    public static final Path DEFAULT_MAVEN_LOCAL_REPO;

    public static final String MAVEN_REWRITE_PLUGIN_VERSION;

    public static final String GITHUB_TOKEN;

    public static final Path SSH_PRIVATE_KEY;

    public static final String GITHUB_OWNER;
    public static final Path GITHUB_APP_PRIVATE_KEY_FILE;

    public static final String ORGANIZATION = getTargetOrganisation();

    public static final String RECIPE_DATA_YAML_PATH = "META-INF/rewrite/recipes.yml";

    public static final String RECIPE_FQDN_PREFIX = "io.jenkins.tools.pluginmodernizer";

    public static final Double PLUGIN_LOW_SCORE_THRESHOLD = 80.0;

    public static final String ADOPTIUM_GITHUB_API_URL = "https://api.github.com/repos/adoptium";

    public static final ComparableVersion MAVEN_MINIMAL_VERSION = new ComparableVersion("3.9.7");

    public static final String REMEDIATION_JENKINS_MINIMUM_VERSION;

    public static final String REMEDIATION_PLUGIN_PARENT_VERSION;

    public static final String REMEDIATION_BOM_BASE;

    public static final String REMEDIATION_BOM_VERSION;

    public static final List<Recipe> AVAILABLE_RECIPES;

    public static final Recipe FETCH_METADATA_RECIPE;

    private Settings() {}

    static {
        String userBaseDir = System.getProperty("user.home");
        if (userBaseDir == null) {
            userBaseDir = System.getProperty("user.dir");
        }

        String cacheDirFromEnv = System.getenv("CACHE_DIR");
        if (cacheDirFromEnv == null) {
            DEFAULT_CACHE_PATH = Paths.get(userBaseDir, ".cache", CACHE_SUBDIR);
        } else {
            DEFAULT_CACHE_PATH = Paths.get(cacheDirFromEnv, CACHE_SUBDIR);
        }
        DEFAULT_MAVEN_HOME = getDefaultMavenHome();
        DEFAULT_MAVEN_LOCAL_REPO = getDefaultMavenLocalRepo();
        MAVEN_REWRITE_PLUGIN_VERSION = getRewritePluginVersion();
        String sshPrivateKey = System.getenv("SSH_PRIVATE_KEY");
        if (sshPrivateKey != null) {
            SSH_PRIVATE_KEY = Paths.get(sshPrivateKey);
        } else {
            SSH_PRIVATE_KEY = Paths.get(userBaseDir, ".ssh", "id_rsa");
        }

        GITHUB_TOKEN = getGithubToken();
        GITHUB_OWNER = getGithubOwner();
        GITHUB_APP_PRIVATE_KEY_FILE = getGithubAppPrivateKeyFile();
        try {
            DEFAULT_UPDATE_CENTER_URL = getUpdateCenterUrl();
        } catch (MalformedURLException e) {
            throw new ModernizerException("Invalid URL format", e);
        }
        try {
            DEFAULT_PLUGIN_VERSIONS = getPluginVersions();
        } catch (MalformedURLException e) {
            throw new ModernizerException("Invalid URL format", e);
        }
        try {
            DEFAULT_HEALTH_SCORE_URL = getHealthScoreUrl();
        } catch (MalformedURLException e) {
            throw new ModernizerException("Invalid URL format", e);
        }
        try {
            DEFAULT_PLUGINS_STATS_INSTALLATIONS_URL = getPluginsStatsInstallationsUrl();
        } catch (MalformedURLException e) {
            throw new ModernizerException("Invalid URL format", e);
        }
        try {
            GITHUB_API_URL = getGithubApiUrl();
        } catch (MalformedURLException e) {
            throw new ModernizerException("Invalid URL format", e);
        }

        REMEDIATION_JENKINS_MINIMUM_VERSION = getRemediationJenkinsMinimumVersion();
        REMEDIATION_BOM_BASE = getRemediationBomBase();
        REMEDIATION_BOM_VERSION = getRemediationBomVersion();
        REMEDIATION_PLUGIN_PARENT_VERSION = getRemediationPluginParentVersion();

        // Get recipes module
        try (InputStream inputStream = Settings.class.getResourceAsStream("/" + Settings.RECIPE_DATA_YAML_PATH)) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            YAMLFactory yamlFactory = new YAMLFactory();
            YAMLParser yamlParser = yamlFactory.createParser(inputStream);
            List<Recipe> recipes = new ArrayList<>();
            while (yamlParser.nextToken() != null) {
                Recipe recipe = mapper.readValue(yamlParser, Recipe.class);
                if (recipe.getTags().contains("condition") || recipe.getName().contains(".conditions.")) {
                    continue;
                }
                recipes.add(recipe);
            }
            AVAILABLE_RECIPES = recipes;

        } catch (IOException e) {
            LOG.error("Error reading recipes", e);
            throw new ModernizerException("Error reading recipes", e);
        }

        FETCH_METADATA_RECIPE = AVAILABLE_RECIPES.stream()
                .filter(recipe -> recipe.getName().equals("io.jenkins.tools.pluginmodernizer.FetchMetadata"))
                .findFirst()
                .orElseThrow(() ->
                        new ModernizerException("io.jenkins.tools.pluginmodernizer.FetchMetadata recipe not found"));
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

    private static Path getDefaultMavenLocalRepo() {
        String mavenLocalRepo = System.getenv("MAVEN_LOCAL_REPO");
        if (mavenLocalRepo == null) {
            String userBaseDir = System.getProperty("user.home");
            if (userBaseDir == null) {
                userBaseDir = System.getProperty("user.dir");
            }
            return Path.of(userBaseDir, ".m2", "repository").toAbsolutePath();
        }
        return Path.of(mavenLocalRepo);
    }

    private static @Nullable String getRewritePluginVersion() {
        return readProperty("openrewrite.maven.plugin.version", "versions.properties");
    }

    private static @Nullable URL getUpdateCenterUrl() throws MalformedURLException {
        String url = System.getenv("JENKINS_UC");
        if (url != null) {
            return new URL(url);
        }
        return new URL(readProperty("update.center.url", "urls.properties"));
    }

    private static @Nullable URL getPluginVersions() throws MalformedURLException {
        String url = System.getenv("JENKINS_PLUGIN_INFO");
        if (url != null) {
            return new URL(url);
        }
        return new URL(readProperty("plugin.versions.url", "urls.properties"));
    }

    private static URL getGithubApiUrl() throws MalformedURLException {
        String host = System.getenv("GH_HOST");
        if (host == null) {
            host = System.getenv("GITHUB_HOST");
        }
        if (host == null) {
            host = "api.github.com";
        }
        return new URL("https://%s".formatted(host));
    }

    private static @NotNull String getRemediationJenkinsMinimumVersion() {
        return readProperty("remediation.jenkins.minimum.version", "versions.properties");
    }

    private static @NotNull String getRemediationPluginParentVersion() {
        return readProperty("remediation.jenkins.plugin.parent.version", "versions.properties");
    }

    private static @NotNull String getRemediationBomBase() {
        return readProperty("remediation.bom.base", "versions.properties");
    }

    private static @NotNull String getRemediationBomVersion() {
        return readProperty("remediation.bom.version", "versions.properties");
    }

    private static @Nullable URL getHealthScoreUrl() throws MalformedURLException {
        String url = System.getenv("JENKINS_PHS");
        if (url != null) {
            return new URL(url);
        }
        return new URL(readProperty("plugin.health.score.url", "urls.properties"));
    }

    private static @Nullable URL getPluginsStatsInstallationsUrl() throws MalformedURLException {
        String url = System.getenv("JENKINS_PLUGINS_STATS_INSTALLATIONS_URL");
        if (url != null) {
            return new URL(url);
        }
        return new URL(readProperty("plugin.stats.installations.plugin.url", "urls.properties"));
    }

    private static String getGithubToken() {
        String token = System.getenv("GH_TOKEN");
        if (token == null) {
            token = System.getenv("GITHUB_TOKEN");
        }
        return token;
    }

    private static Path getGithubAppPrivateKeyFile() {
        String privateKeyFile = System.getenv("GH_APP_PRIVATE_KEY_FILE");
        if (privateKeyFile != null) {
            return Path.of(privateKeyFile);
        }
        return null;
    }

    private static String getGithubOwner() {
        String username = System.getenv("GH_OWNER");
        if (username == null) {
            username = System.getenv("GITHUB_OWNER");
        }
        return username;
    }

    private static String getTargetOrganisation() {
        String targetOrganisation = System.getenv("GH_TARGET_ORGANISATION");
        if (targetOrganisation == null) {
            targetOrganisation = "jenkinsci";
        }
        return targetOrganisation;
    }

    public static Path getDefaultSdkManJava(final String key) {
        String homeDir = System.getProperty("user.home") != null ? System.getProperty("user.home") : "";
        String propertyValue = readProperty(key, "sdkman.properties").replace("$HOME", homeDir);
        return Path.of(propertyValue);
    }

    public static Path getPluginsDirectory(Plugin plugin) {
        return plugin.getConfig().getCachePath().resolve(plugin.getName());
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
