package io.jenkins.tools.pluginmodernizer.core.utils;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.UpdateCenterData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for Jenkins plugin center
 */
public class UpdateCenterUtils {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateCenterUtils.class);

    /**
     * Extract the repository name for a plugin
     * @param plugin Plugin
     * @param cacheManager Cache manager
     * @return Repository name
     */
    public static String extractRepoName(Plugin plugin, CacheManager cacheManager) {
        UpdateCenterData updateCenterData = get(plugin.getConfig(), cacheManager);
        UpdateCenterData.UpdateCenterPlugin updateCenterPlugin =
                updateCenterData.getPlugins().get(plugin.getName());
        if (updateCenterPlugin == null) {
            plugin.addError("Plugin not found in update center");
            plugin.raiseLastError();
            return null;
        }
        String scmUrl = updateCenterPlugin.scm();
        int lastSlashIndex = scmUrl.lastIndexOf('/');
        if (lastSlashIndex != -1 && lastSlashIndex < scmUrl.length() - 1) {
            return scmUrl.substring(lastSlashIndex + 1);
        } else {
            plugin.addError("Invalid SCM URL format");
            plugin.raiseLastError();
        }
        throw new ModernizerException("Invalid SCM URL format: " + scmUrl);
    }

    /**
     * Check if a plugin is deprecated
     * @param plugin Plugin
     * @param cacheManager Cache manager
     * @return True if deprecated
     */
    public static boolean isDeprecated(Plugin plugin, CacheManager cacheManager) {
        // Some old plugin are under a deprecations list
        UpdateCenterData updateCenterData = get(plugin.getConfig(), cacheManager);
        if (updateCenterData.getDeprecations().containsKey(plugin.getName())) {
            return true;
        }
        // More recent deprecated plugins are marked with a label
        UpdateCenterData.UpdateCenterPlugin updateCenterPlugin =
                updateCenterData.getPlugins().get(plugin.getName());
        return updateCenterPlugin != null
                && updateCenterPlugin.labels() != null
                && updateCenterPlugin.labels().contains("deprecated");
    }

    public static boolean isApiPlugin(Plugin plugin, CacheManager cacheManager) {
        UpdateCenterData updateCenterData = get(plugin.getConfig(), cacheManager);
        UpdateCenterData.UpdateCenterPlugin updateCenterPlugin =
                updateCenterData.getPlugins().get(plugin.getName());

        // Let's consider only recent convention that API plugins have a labels and end with -api
        return updateCenterPlugin != null
                && updateCenterPlugin.labels() != null
                && updateCenterPlugin.labels().contains("api-plugin")
                && plugin.getName().endsWith("-api");
    }

    /**
     * Extract the version for a plugin in the update center
     * @param plugin Plugin
     * @param cacheManager Cache manager
     * @return Version
     */
    public static String extractVersion(Plugin plugin, CacheManager cacheManager) {
        UpdateCenterData updateCenterData = get(plugin.getConfig(), cacheManager);
        UpdateCenterData.UpdateCenterPlugin updateCenterPlugin =
                updateCenterData.getPlugins().get(plugin.getName());
        if (updateCenterPlugin == null) {
            plugin.addError("Plugin not found in update center");
            plugin.raiseLastError();
            return null;
        }
        return updateCenterPlugin.version();
    }

    /**
     * Retrieve update center data from the given URL of from cache if it exists
     * @param cacheManager Cache manager
     * @return Update center data
     */
    public static UpdateCenterData get(Config config, CacheManager cacheManager) {
        UpdateCenterData updateCenterData =
                cacheManager.get(cacheManager.root(), CacheManager.UPDATE_CENTER_CACHE_KEY, UpdateCenterData.class);
        // Download and update cache
        if (updateCenterData == null) {
            updateCenterData = download(config);
            updateCenterData.setKey(CacheManager.UPDATE_CENTER_CACHE_KEY);
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
    public static UpdateCenterData download(Config config) {
        return JsonUtils.fromUrl(config.getJenkinsUpdateCenter(), UpdateCenterData.class);
    }
}
