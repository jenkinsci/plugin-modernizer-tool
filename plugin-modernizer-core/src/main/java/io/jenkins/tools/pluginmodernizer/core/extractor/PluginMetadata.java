package io.jenkins.tools.pluginmodernizer.core.extractor;

import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import io.jenkins.tools.pluginmodernizer.core.model.CacheEntry;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Map;

/**
 * Metadata of a plugin extracted from its POM file or code
 */
public class PluginMetadata extends CacheEntry<PluginMetadata> implements Serializable {

    /**
     * Name of the plugin
     */
    private String pluginName;

    /**
     * Whether the plugin has a Java level defined in its POM file
     */
    private boolean hasJavaLevel;

    /**
     * Whether the plugin uses HTTPS for its SCM URL
     */
    private boolean usesScmHttps;

    /**
     * Whether the plugin uses HTTPS for all its repositories
     */
    private boolean usesRepositoriesHttps;

    /**
     * If the plugin has a Jenkinsfile
     */
    private boolean hasJenkinsfile;

    /**
     * Jenkins version required by the plugin
     */
    private String jenkinsVersion;

    /**
     * Parent version
     */
    private String parentVersion;

    /**
     * Properties defined in the POM file of the plugin
     */
    private Map<String, String> properties;

    /**
     * Create a new plugin metadata
     * Store the metadata in in the relative target directory of current folder
     */
    public PluginMetadata() {
        super(
                new CacheManager(Path.of("target")),
                PluginMetadata.class,
                CacheManager.PLUGIN_METADATA_CACHE_KEY,
                Path.of("."));
    }

    /**
     * Create a new plugin metadata. Store the metadata at the root of the given cache manager
     * @param cacheManager The cache manager
     */
    public PluginMetadata(CacheManager cacheManager) {
        super(cacheManager, PluginMetadata.class, CacheManager.PLUGIN_METADATA_CACHE_KEY, cacheManager.root());
    }

    /**
     * Create a new plugin metadata. Store the metadata to the plugin subdirectory of the given cache manager
     * @param cacheManager The cache manager
     * @param plugin The plugin
     */
    public PluginMetadata(CacheManager cacheManager, Plugin plugin) {
        super(cacheManager, PluginMetadata.class, CacheManager.PLUGIN_METADATA_CACHE_KEY, Path.of(plugin.getName()));
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public boolean hasJavaLevel() {
        return hasJavaLevel;
    }

    public void setHasJavaLevel(boolean hasJavaLevel) {
        this.hasJavaLevel = hasJavaLevel;
    }

    public boolean isUsesScmHttps() {
        return usesScmHttps;
    }

    public void setUsesScmHttps(boolean usesScmHttps) {
        this.usesScmHttps = usesScmHttps;
    }

    public boolean isUsesRepositoriesHttps() {
        return usesRepositoriesHttps;
    }

    public void setUsesRepositoriesHttps(boolean usesRepositoriesHttps) {
        this.usesRepositoriesHttps = usesRepositoriesHttps;
    }

    public boolean hasJenkinsfile() {
        return hasJenkinsfile;
    }

    public void setHasJenkinsfile(boolean hasJenkinsfile) {
        this.hasJenkinsfile = hasJenkinsfile;
    }

    public String getJenkinsVersion() {
        return jenkinsVersion;
    }

    public void setJenkinsVersion(String jenkinsVersion) {
        this.jenkinsVersion = jenkinsVersion;
    }

    public String getParentVersion() {
        return parentVersion;
    }

    public void setParentVersion(String parentVersion) {
        this.parentVersion = parentVersion;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
