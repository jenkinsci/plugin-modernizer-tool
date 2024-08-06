package io.jenkins.tools.pluginmodernizer.core.config;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;

public class Config {

    public static boolean DEBUG = false;
    private final String version;
    private final List<String> pluginNames;
    private final List<String> recipes;
    private final URL jenkinsUpdateCenter;
    private final Path cachePath;
    private final Path mavenHome;
    private final int sourceJavaMajorVersion;
    private final int targetJavaMajorVersion;
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
            List<String> pluginNames,
            List<String> recipes,
            URL jenkinsUpdateCenter,
            Path cachePath,
            Path mavenHome,
            int sourceJavaMajorVersion,
            int targetJavaMajorVersion,
            boolean dryRun,
            boolean skipPush,
            boolean skipPullRequest,
            boolean removeLocalData,
            boolean removeForks,
            boolean exportDatatables) {
        this.version = version;
        this.githubOwner = githubOwner;
        this.pluginNames = pluginNames;
        this.recipes = recipes;
        this.jenkinsUpdateCenter = jenkinsUpdateCenter;
        this.cachePath = cachePath;
        this.mavenHome = mavenHome;
        this.sourceJavaMajorVersion = sourceJavaMajorVersion;
        this.targetJavaMajorVersion = targetJavaMajorVersion;
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

    public List<String> getPluginNames() {
        return pluginNames;
    }

    public List<String> getRecipes() {
        return recipes;
    }

    public URL getJenkinsUpdateCenter() {
        return jenkinsUpdateCenter;
    }

    public Path getCachePath() {
        return cachePath;
    }

    public Path getMavenHome() {
        return mavenHome;
    }

    public int getSourceJavaMajorVersion() {
        return sourceJavaMajorVersion;
    }

    public int getTargetJavaMajorVersion() {
        return targetJavaMajorVersion;
    }

    public boolean isDryRun() {
        return dryRun;
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
        private List<String> plugins;
        private List<String> recipes;
        private URL jenkinsUpdateCenter = Settings.DEFAULT_UPDATE_CENTER_URL;
        private Path cachePath = Settings.DEFAULT_CACHE_PATH;
        private Path mavenHome = Settings.DEFAULT_MAVEN_HOME;
        private int sourceJavaMajorVersion = Settings.SOURCE_JAVA_MAJOR_VERSION;
        private int targetJavaMajorVersion = Settings.TARGET_JAVA_MAJOR_VERSION;
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

        public Builder withPlugins(List<String> plugins) {
            this.plugins = plugins;
            return this;
        }

        public Builder withRecipes(List<String> recipes) {
            this.recipes = recipes;
            return this;
        }

        public Builder withJenkinsUpdateCenter(URL jenkinsUpdateCenter) {
            if (jenkinsUpdateCenter != null) {
                this.jenkinsUpdateCenter = jenkinsUpdateCenter;
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

        public Builder withSourceJavaMajorVersion(int sourceJavaMajorVersion) {
            this.sourceJavaMajorVersion = sourceJavaMajorVersion;
            return this;
        }

        public Builder withTargetJavaMajorVersion(int targetJavaMajorVersion) {
            this.targetJavaMajorVersion = targetJavaMajorVersion;
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
                    cachePath,
                    mavenHome,
                    sourceJavaMajorVersion,
                    targetJavaMajorVersion,
                    dryRun,
                    skipPush,
                    skipPullRequest,
                    removeLocalData,
                    removeForks,
                    exportDatatables);
        }
    }
}
