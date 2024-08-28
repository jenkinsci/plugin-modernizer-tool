package io.jenkins.tools.pluginmodernizer.core.utils;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import io.jenkins.tools.pluginmodernizer.core.model.PluginVersionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for Jenkins plugin center
 */
public class PluginVersionUtils {

    private static final Logger LOG = LoggerFactory.getLogger(PluginVersionUtils.class);

    /**
     * Retrieve update center data from the given URL of from cache if it exists
     * @param cacheManager Cache manager
     * @return Update center data
     */
    public static PluginVersionData get(Config config, CacheManager cacheManager) {
        PluginVersionData updateCenterData =
                cacheManager.get(cacheManager.root(), CacheManager.PLUGIN_VERSIONS_CACHE_KEY, PluginVersionData.class);
        // Download and update cache
        if (updateCenterData == null) {
            updateCenterData = download(config);
            updateCenterData.setKey(CacheManager.PLUGIN_VERSIONS_CACHE_KEY);
            updateCenterData.setPath(cacheManager.root());
            cacheManager.put(updateCenterData);
        }
        return updateCenterData;
    }

    /**
     * Download refreshed update center data from the remote service
     * @param config Configuration
     * @return Update center data
     */
    public static PluginVersionData download(Config config) {
        return JsonUtils.fromUrl(config.getJenkinsPluginVersions(), PluginVersionData.class);
    }
}
