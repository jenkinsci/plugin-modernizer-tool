package io.jenkins.tools.pluginmodernizer.core.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.Recipe;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

public class Config {

    @SuppressFBWarnings(value = "MS_SHOULD_BE_FINAL", justification = "Because usage on ConsoleLogFilter")
    public static boolean DEBUG = false;

    private final String version;
    private final List<Plugin> plugins;
    private final Recipe recipe;
    private final URL jenkinsUpdateCenter;
    private final URL jenkinsPluginVersions;
    private final URL pluginHealthScore;
    private final URL pluginStatsInstallations;
    private final Path cachePath;
    private final Path mavenHome;
    private final boolean dryRun;
    private final boolean skipPush;
    private final boolean skipBuild;
    private final boolean draft;
    private final boolean skipPullRequest;
    private final boolean removeLocalData;
    private final boolean removeForks;
    private final boolean exportDatatables;
    private final String githubOwner;
    private final Long githubAppId;
    private final Long githubAppSourceInstallationId;
    private final Long githubAppTargetInstallationId;

    private Config(
            String version,
            String githubOwner,
            Long githubAppId,
            Long githubAppSourceInstallationId,
            Long githubAppTargetInstallationId,
            List<Plugin> plugins,
            Recipe recipe,
            URL jenkinsUpdateCenter,
            URL jenkinsPluginVersions,
            URL pluginHealthScore,
            URL pluginStatsInstallations,
            Path cachePath,
            Path mavenHome,
            boolean dryRun,
            boolean skipPush,
            boolean skipBuild,
            boolean draft,
            boolean skipPullRequest,
            boolean removeLocalData,
            boolean removeForks,
            boolean exportDatatables) {
        this.version = version;
        this.githubOwner = githubOwner;
        this.githubAppId = githubAppId;
        this.githubAppSourceInstallationId = githubAppSourceInstallationId;
        this.githubAppTargetInstallationId = githubAppTargetInstallationId;
        this.plugins = plugins;
        this.recipe = recipe;
        this.jenkinsUpdateCenter = jenkinsUpdateCenter;
        this.jenkinsPluginVersions = jenkinsPluginVersions;
        this.pluginHealthScore = pluginHealthScore;
        this.pluginStatsInstallations = pluginStatsInstallations;
        this.cachePath = cachePath;
        this.mavenHome = mavenHome;
        this.dryRun = dryRun;
        this.skipPush = skipPush;
        this.skipBuild = skipBuild;
        this.draft = draft;
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

    public Long getGithubAppId() {
        return githubAppId;
    }

    public Long getGithubAppSourceInstallationId() {
        return githubAppSourceInstallationId;
    }

    public Long getGithubAppTargetInstallationId() {
        return githubAppTargetInstallationId;
    }

    public List<Plugin> getPlugins() {
        return plugins;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    /**
     * Return if the current configuration is only fetching metadata which will skip compile and verify steps
     * @return True if only fetching metadata
     */
    public boolean isFetchMetadataOnly() {
        return recipe != null && recipe.getName().equals(Settings.FETCH_METADATA_RECIPE.getName());
    }

    public URL getJenkinsUpdateCenter() {
        return jenkinsUpdateCenter;
    }

    public URL getJenkinsPluginVersions() {
        return jenkinsPluginVersions;
    }

    public URL getPluginHealthScore() {
        return pluginHealthScore;
    }

    public URL getPluginStatsInstallations() {
        return pluginStatsInstallations;
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

    public boolean isDraft() {
        return draft;
    }

    public boolean isSkipPush() {
        return skipPush;
    }

    public boolean isSkipBuild() {
        return skipBuild;
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
        private Long githubAppId;
        private Long githubAppSourceInstallationId;
        private Long githubAppTargetInstallationId;
        private List<Plugin> plugins;
        private Recipe recipe;
        private URL jenkinsUpdateCenter = Settings.DEFAULT_UPDATE_CENTER_URL;
        private URL jenkinsPluginVersions = Settings.DEFAULT_PLUGIN_VERSIONS;
        private URL pluginStatsInstallations = Settings.DEFAULT_PLUGINS_STATS_INSTALLATIONS_URL;
        private URL pluginHealthScore = Settings.DEFAULT_HEALTH_SCORE_URL;
        private Path cachePath = Settings.DEFAULT_CACHE_PATH;
        private Path mavenHome = Settings.DEFAULT_MAVEN_HOME;
        private boolean dryRun = false;
        private boolean draft = false;
        private boolean skipPush = false;
        private boolean skipBuild = false;
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

        public Builder withGitHubAppId(Long githubAppId) {
            this.githubAppId = githubAppId;
            return this;
        }

        public Builder withGitHubAppSourceInstallationId(Long githubAppInstallationId) {
            this.githubAppSourceInstallationId = githubAppInstallationId;
            return this;
        }

        public Builder withGitHubAppTargetInstallationId(Long githubAppInstallationId) {
            this.githubAppTargetInstallationId = githubAppInstallationId;
            return this;
        }

        public Builder withPlugins(List<Plugin> plugins) {
            this.plugins = plugins;
            return this;
        }

        public Builder withRecipe(Recipe recipe) {
            this.recipe = recipe;
            return this;
        }

        public Builder withJenkinsUpdateCenter(URL jenkinsUpdateCenter) {
            if (jenkinsUpdateCenter != null) {
                this.jenkinsUpdateCenter = jenkinsUpdateCenter;
            }
            return this;
        }

        public Builder withJenkinsPluginVersions(URL jenkinsPluginVersions) {
            if (jenkinsPluginVersions != null) {
                this.jenkinsPluginVersions = jenkinsPluginVersions;
            }
            return this;
        }

        public Builder withPluginHealthScore(URL pluginHealthScore) {
            if (pluginHealthScore != null) {
                this.pluginHealthScore = pluginHealthScore;
            }
            return this;
        }

        public Builder withPluginStatsInstallations(URL pluginStatsInstallations) {
            if (pluginStatsInstallations != null) {
                this.pluginStatsInstallations = pluginStatsInstallations;
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

        public Builder withDraft(boolean draft) {
            this.draft = draft;
            return this;
        }

        public Builder withSkipPush(boolean skipPush) {
            this.skipPush = skipPush;
            return this;
        }

        public Builder withSkipBuild(boolean skipBuild) {
            this.skipBuild = skipBuild;
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
                    githubAppId,
                    githubAppSourceInstallationId,
                    githubAppTargetInstallationId,
                    plugins,
                    recipe,
                    jenkinsUpdateCenter,
                    jenkinsPluginVersions,
                    pluginHealthScore,
                    pluginStatsInstallations,
                    cachePath,
                    mavenHome,
                    dryRun,
                    skipPush,
                    skipBuild,
                    draft,
                    skipPullRequest,
                    removeLocalData,
                    removeForks,
                    exportDatatables);
        }
    }
}
