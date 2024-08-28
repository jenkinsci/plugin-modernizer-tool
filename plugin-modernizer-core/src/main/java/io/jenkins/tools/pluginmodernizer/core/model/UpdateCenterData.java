package io.jenkins.tools.pluginmodernizer.core.model;

import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Data from the update center
 * We are storing only the data we are interested in (like plugins).
 * Further implementation can consider ignoring plugin with deprecation
 */
public class UpdateCenterData extends CacheEntry<UpdateCenterData> implements Serializable {

    /**
     * Plugins in the update center mapped by their name
     */
    private Map<String, UpdateCenterPlugin> plugins;

    /**
     * List of deprecation
     */
    private Map<String, DeprecatedPlugin> deprecations;

    public UpdateCenterData(CacheManager cacheManager) {
        super(cacheManager, UpdateCenterData.class, CacheManager.UPDATE_CENTER_CACHE_KEY, Path.of("."));
    }

    /**
     * Get the plugins
     * @return Plugins
     */
    public Map<String, UpdateCenterPlugin> getPlugins() {
        return plugins;
    }

    /**
     * Get the deprecations
     * @return Deprecations
     */
    public Map<String, DeprecatedPlugin> getDeprecations() {
        return deprecations;
    }

    /**
     * An update center plugin record with what we need
     * @param name Plugin name
     * @param scm SCM URL
     * @param defaultBranch Default branch
     * @param gav GAV
     * @param labels Labels
     */
    public record UpdateCenterPlugin(
            String name, String version, String scm, String defaultBranch, String gav, List<String> labels)
            implements Serializable {}

    /**
     * Hold a deprecated plugin in the update center
     * @param url URL
     */
    public record DeprecatedPlugin(String url) implements Serializable {}
}
