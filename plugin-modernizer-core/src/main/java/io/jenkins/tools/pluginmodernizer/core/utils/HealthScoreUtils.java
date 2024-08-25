package io.jenkins.tools.pluginmodernizer.core.utils;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import io.jenkins.tools.pluginmodernizer.core.model.HealthScoreData;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for Health Score
 */
public class HealthScoreUtils {

    private static final Logger LOG = LoggerFactory.getLogger(HealthScoreUtils.class);

    /**
     * Extract the score for a plugin
     * @param plugin Plugin
     * @param cacheManager Cache manager
     * @return Score
     */
    public static Double extractScore(Plugin plugin, CacheManager cacheManager) {
        HealthScoreData healthScoreData = get(plugin.getConfig(), cacheManager);
        HealthScoreData.HealthScorePlugin healthScorePlugin =
                healthScoreData.getPlugins().get(plugin.getName());
        if (healthScorePlugin == null) {
            plugin.addError("Plugin not found in health score data: " + plugin.getName());
            plugin.raiseLastError();
            return null;
        }
        return healthScorePlugin.value();
    }

    /**
     * Retrieve health score data from the given URL of from cache if it exists
     * @param cacheManager Cache manager
     * @return Health score data
     */
    public static HealthScoreData get(Config config, CacheManager cacheManager) {
        HealthScoreData healthScoreData =
                cacheManager.get(cacheManager.root(), CacheManager.HEALTH_SCORE_KEY, HealthScoreData.class);
        // Download and update cache
        if (healthScoreData == null) {
            healthScoreData = download(config);
            healthScoreData.setKey(CacheManager.HEALTH_SCORE_KEY);
            healthScoreData.setPath(cacheManager.root());
            cacheManager.put(healthScoreData);
        }
        return healthScoreData;
    }

    /**
     * Download refreshed update center data from the remote service
     * @param config Configuration
     * @return Update center data
     */
    public static HealthScoreData download(Config config) {
        return JsonUtils.fromUrl(config.getPluginHealthScore(), HealthScoreData.class);
    }
}
