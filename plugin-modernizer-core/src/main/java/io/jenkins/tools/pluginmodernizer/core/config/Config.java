package io.jenkins.tools.pluginmodernizer.core.config;

import java.nio.file.Path;
import java.util.List;

public class Config {

    public static boolean DEBUG = false;
    private final String version;
    private final List<String> plugins;
    private final List<String> recipes;
    private final Path cachePath;

    private Config(String version, List<String> plugins, List<String> recipes, Path cachePath) {
        this.version = version;
        this.plugins = plugins;
        this.recipes = recipes;
        this.cachePath = cachePath;
    }

    public String getVersion() {
        return version;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String version;
        private List<String> plugins;
        private List<String> recipes;
        private Path cachePath = Settings.DEFAULT_CACHE_PATH;

        public Builder withVersion(String version) {
            this.version = version;
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
            this.cachePath = cachePath;
            return this;
        }

        public Config build() {
            return new Config(version, plugins, recipes, cachePath);
        }
    }

}
