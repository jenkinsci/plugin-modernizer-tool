package io.jenkins.tools.pluginmodernizer.core.config;

import java.nio.file.Path;
import java.util.List;

public class Config {

    public static boolean DEBUG = false;
    private final String version;
    private final List<String> plugins;
    private final List<String> recipes;
    private final Path cachePath;
    private final Path mavenHome;
    private final boolean dryRun;
    private final String githubUsername;

    private Config(String version, String githubUsername, List<String> plugins, List<String> recipes, Path cachePath, Path mavenHome, boolean dryRun) {
        this.version = version;
        this.githubUsername = githubUsername;
        this.plugins = plugins;
        this.recipes = recipes;
        this.cachePath = cachePath;
        this.mavenHome = mavenHome;
        this.dryRun = dryRun;
    }

    public String getVersion() {
        return version;
    }

    public String getGithubUsername() {
        return githubUsername;
    }

    public List<String> getPlugins() {
        return plugins;
    }

    public List<String> getRecipes() {
        return recipes;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String version;
        private String githubUsername = Settings.GITHUB_USERNAME;
        private List<String> plugins;
        private List<String> recipes;
        private Path cachePath = Settings.DEFAULT_CACHE_PATH;
        private Path mavenHome = Settings.DEFAULT_MAVEN_HOME;
        private boolean dryRun = false;

        public Builder withVersion(String version) {
            this.version = version;
            return this;
        }

        public Builder withGitHubUsername(String githubUsername) {
            this.githubUsername = githubUsername;
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

        public Config build() {
            return new Config(version, githubUsername, plugins, recipes, cachePath, mavenHome, dryRun);
        }
    }

}
