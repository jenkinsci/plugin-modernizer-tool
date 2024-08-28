package io.jenkins.tools.pluginmodernizer.core.model;

import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Map;

/**
 * Data from the plugin version
 * We are storing only the data we are interested in (like plugins).
 * Further implementation can consider ignoring plugin with deprecation
 */
public class PluginVersionData extends CacheEntry<PluginVersionData> implements Serializable {

    /**
     * Plugins in the update center mapped by their name
     */
    private Map<String, Map<String, PluginVersionPlugin>> plugins;

    public PluginVersionData(CacheManager cacheManager) {
        super(cacheManager, PluginVersionData.class, CacheManager.UPDATE_CENTER_CACHE_KEY, Path.of("."));
    }

    /**
     * Get the plugins
     * @return Plugins
     */
    public Map<String, Map<String, PluginVersionPlugin>> getPlugins() {
        return plugins;
    }

    /**
     * An update center plugin record with what we need
     * @param name Plugin name
     */
    public record PluginVersionPlugin(String name, String version) implements Serializable {}
}
