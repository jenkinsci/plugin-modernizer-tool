package io.jenkins.tools.pluginmodernizer.core.model;

import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Map;

public class PluginInstallationStatsData extends CacheEntry<PluginInstallationStatsData> implements Serializable {

    /**
     * Plugins in the installation stats mapped by their name
     */
    private Map<String, Integer> plugins;

    public PluginInstallationStatsData(CacheManager cacheManager) {
        super(cacheManager, PluginInstallationStatsData.class, CacheManager.INSTALLATION_STATS_KEY, Path.of("."));
    }

    public Map<String, Integer> getPlugins() {
        return plugins;
    }

    public void setPlugins(Map<String, Integer> plugins) {
        this.plugins = plugins;
    }
}
