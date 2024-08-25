package io.jenkins.tools.pluginmodernizer.core.model;

import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Map;

/**
 * Data from the update center
 * We are storing only the data we are interested in (like plugins).
 * Further implementation can consider ignoring plugin with deprecation
 */
public class HealthScoreData extends CacheEntry<HealthScoreData> implements Serializable {

    /**
     * Plugins in the health score mapped by their name
     */
    private Map<String, HealthScorePlugin> plugins;

    public HealthScoreData(CacheManager cacheManager) {
        super(cacheManager, HealthScoreData.class, CacheManager.HEALTH_SCORE_KEY, Path.of("."));
    }

    /**
     * Get the plugins
     * @return Plugins
     */
    public Map<String, HealthScorePlugin> getPlugins() {
        return plugins;
    }

    /**
     * A health score plugin record with what we need
     */
    public record HealthScorePlugin(Double value) implements Serializable {}
}
