package io.jenkins.tools.pluginmodernizer.core.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import org.openrewrite.Recipe;

public class Config {

    @SuppressFBWarnings(value = "MS_SHOULD_BE_FINAL", justification = "Because usage on ConsoleLogFilter")
    public static boolean DEBUG = false;

    private final String version;
    private final List<Plugin> plugins;
    private final List<Recipe> recipes;
    private final URL jenkinsUpdateCenter;
    private final URL pluginHealthScore;
    private final Path cachePath;
    private final Path mavenHome;
    private final boolean dryRun;
    private final boolean skipPush;
    private final boolean skipPullRequest;
    private final boolean removeLocalData;
    private final boolean removeForks;
    private final boolean exportDatatables;
    private final String githubOwner;

    private Config(
            String version,
            String githubOwner,
            List<Plugin> plugins,
            List<Recipe> recipes,
            URL jenkinsUpdateCenter,
            URL pluginHealthScore,
            Path cachePath,
            Path mavenHome,
            boolean dryRun,
            boolean skipPush,
            boolean skipPullRequest,
            boolean removeLocalData,
            boolean removeForks,
            boolean exportDatatables) {
        this.version = version;
        this.githubOwner = githubOwner;
        this.plugins = plugins;
        this.recipes = recipes;
        this.jenkinsUpdateCenter = jenkinsUpdateCenter;
        this.pluginHealthScore = pluginHealthScore;
        this.cachePath = cachePath;
        this.mavenHome = mavenHome;
        this.dryRun = dryRun;
        this.skipPush = skipPush;
        this.skipPullRequest = skipPullRequest;
        this.removeLocalData = removeLocalData;
        this.removeForks = removeForks;
        this.exportDatatables = exportDatatables;
    }

    public String getVersion() {
        return version;
    }

    public String getGithubOwner() {
        return githubOwner;
    }

    public List<Plugin> getPlugins() {
        return plugins;
    }

    public List<Recipe> getRecipes() {
        return recipes;
    }

    /**
     * Return if the current configuration is only fetching metadata which will skip compile and verify steps
     * @return True if only fetching metadata
     */
    public boolean isFetchMetadataOnly() {
        return recipes.size() == 1 && recipes.get(0).getName().equals(Settings.FETCH_METADATA_RECIPE.getName());
    }

    public URL getJenkinsUpdateCenter() {
        return jenkinsUpdateCenter;
    }

    public URL getPluginHealthScore() {
        return pluginHealthScore;
    }

    public Path getCachePath() {
        return cachePath;
    }

    public Path getMavenHome() {
        return mavenHome;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public boolean isDebug() {
        return DEBUG;
    }

    public boolean isSkipPullRequest() {
        return skipPullRequest;
    }

    public boolean isSkipPush() {
        return skipPush;
    }

    public boolean isRemoveLocalData() {
        return removeLocalData;
    }

    public boolean isRemoveForks() {
        return removeForks;
    }

    public boolean isExportDatatables() {
        return exportDatatables;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String version;
        private String githubOwner = Settings.GITHUB_OWNER;
        private List<Plugin> plugins;
        private List<Recipe> recipes;
        private URL jenkinsUpdateCenter = Settings.DEFAULT_UPDATE_CENTER_URL;
        private URL pluginHealthScore = Settings.DEFAULT_HEALTH_SCORE_URL;
        private Path cachePath = Settings.DEFAULT_CACHE_PATH;
        private Path mavenHome = Settings.DEFAULT_MAVEN_HOME;
        private boolean dryRun = false;
        private boolean skipPush = false;
        private boolean skipPullRequest = false;
        private boolean exportDatatables = false;
        public boolean removeLocalData = false;
        public boolean removeForks = false;

        public Builder withVersion(String version) {
            this.version = version;
            return this;
        }

        public Builder withGitHubOwner(String githubOwner) {
            this.githubOwner = githubOwner;
            return this;
        }

        public Builder withPlugins(List<Plugin> plugins) {
            this.plugins = plugins;
            return this;
        }

        public Builder withRecipes(List<Recipe> recipes) {
            this.recipes = recipes;
            return this;
        }

        public Builder withJenkinsUpdateCenter(URL jenkinsUpdateCenter) {
            if (jenkinsUpdateCenter != null) {
                this.jenkinsUpdateCenter = jenkinsUpdateCenter;
            }
            return this;
        }

        public Builder withPluginHealthScore(URL pluginHealthScore) {
            if (pluginHealthScore != null) {
                this.pluginHealthScore = pluginHealthScore;
            }
            return this;
        }

        public Builder withCachePath(Path cachePath) {
            if (cachePath != null) {
                this.cachePath = cachePath;
            }
            return this;
        }

        public Builder withMavenHome(Path mavenHome) {
            if (mavenHome != null) {
                this.mavenHome = mavenHome;
            }
            return this;
        }

        public Builder withDryRun(boolean dryRun) {
            this.dryRun = dryRun;
            return this;
        }

        public Builder withSkipPush(boolean skipPush) {
            this.skipPush = skipPush;
            return this;
        }

        public Builder withSkipPullRequest(boolean skipPullRequest) {
            this.skipPullRequest = skipPullRequest;
            return this;
        }

        public Builder withRemoveLocalData(boolean removeLocalData) {
            this.removeLocalData = removeLocalData;
            return this;
        }

        public Builder withRemoveForks(boolean removeForks) {
            this.removeForks = removeForks;
            return this;
        }

        public Builder withExportDatatables(boolean exportDatatables) {
            this.exportDatatables = exportDatatables;
            return this;
        }

        public Config build() {
            return new Config(
                    version,
                    githubOwner,
                    plugins,
                    recipes,
                    jenkinsUpdateCenter,
                    pluginHealthScore,
                    cachePath,
                    mavenHome,
                    dryRun,
                    skipPush,
                    skipPullRequest,
                    removeLocalData,
                    removeForks,
                    exportDatatables);
        }
    }
}
