package io.jenkins.tools.pluginmodernizer.core.utils;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.UpdateCenterData;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for Jenkins plugin center
 */
public class UpdateCenterService {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateCenterService.class);

    @Inject
    private Config config;

    @Inject
    private CacheManager cacheManager;

    /**
     * Extract the repository name for a plugin
     * @param plugin Plugin
     * @return Repository name
     */
    public String extractRepoName(Plugin plugin) {
        UpdateCenterData updateCenterData = get();
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
     * @return True if deprecated
     */
    public boolean isDeprecated(Plugin plugin) {
        // Some old plugin are under a deprecations list
        UpdateCenterData updateCenterData = get();
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

    /**
     * Check if a plugin is an API plugin
     * @param plugin Plugin
     * @return True if API plugin
     */
    public boolean isApiPlugin(Plugin plugin) {
        UpdateCenterData updateCenterData = get();
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
     * @return Version
     */
    public String extractVersion(Plugin plugin) {
        UpdateCenterData updateCenterData = get();
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
     * @return Update center data
     */
    public UpdateCenterData get() {
        UpdateCenterData updateCenterData =
                cacheManager.get(cacheManager.root(), CacheManager.UPDATE_CENTER_CACHE_KEY, UpdateCenterData.class);
        // Download and update cache
        if (updateCenterData == null) {
            updateCenterData = download();
            updateCenterData.setKey(CacheManager.UPDATE_CENTER_CACHE_KEY);
            updateCenterData.setPath(cacheManager.root());
            cacheManager.put(updateCenterData);
        }
        return updateCenterData;
    }

    /**
     * Download refreshed update center data from the remote service
     * @return Update center data
     */
    public UpdateCenterData download() {
        return JsonUtils.fromUrl(config.getJenkinsUpdateCenter(), UpdateCenterData.class);
    }
}
