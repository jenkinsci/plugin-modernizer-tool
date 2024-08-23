package io.jenkins.tools.pluginmodernizer.core.extractor;

import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import io.jenkins.tools.pluginmodernizer.core.model.CacheEntry;
import io.jenkins.tools.pluginmodernizer.core.model.JDK;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.PreconditionError;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Metadata of a plugin extracted from its POM file or code
 */
public class PluginMetadata extends CacheEntry<PluginMetadata> implements Serializable {

    /**
     * Name of the plugin
     */
    private String pluginName;

    /**
     * List of flags present in the plugin
     */
    private Set<MetadataFlag> flags;

    /**
     * List of errors present in the plugin
     */
    private Set<PreconditionError> errors;

    /**
     * List of well known files present in the plugin
     */
    private List<ArchetypeCommonFile> commonFiles;

    /**
     * List of other files present in the plugin
     */
    private List<String> otherFiles;

    /**
     * JDK versions supported by the plugin
     */
    private Set<JDK> jdkVersions;

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
     * Store the metadata in the relative target directory of current folder
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

    public Set<MetadataFlag> getFlags() {
        return flags;
    }

    public void setFlags(Set<MetadataFlag> flags) {
        this.flags = flags;
    }

    public boolean hasFlag(MetadataFlag flag) {
        return flags.contains(flag);
    }

    public Set<PreconditionError> getErrors() {
        return errors;
    }

    public void setErrors(Set<PreconditionError> errors) {
        this.errors = errors;
    }

    public List<ArchetypeCommonFile> getCommonFiles() {
        return commonFiles;
    }

    public void setCommonFiles(List<ArchetypeCommonFile> commonFiles) {
        this.commonFiles = commonFiles;
    }

    public List<String> getOtherFiles() {
        return otherFiles;
    }

    public void setOtherFiles(List<String> otherFiles) {
        this.otherFiles = otherFiles;
    }

    public Set<JDK> getJdks() {
        return jdkVersions;
    }

    public void setJdks(Set<JDK> jdkVersions) {
        this.jdkVersions = jdkVersions;
    }

    /**
     * The file with the given path or null if not found
     * @param path The path
     * @return The file or null
     */
    public ArchetypeCommonFile getFile(String path) {
        return commonFiles.stream()
                .filter(f -> f.getPath().equals(path))
                .findFirst()
                .orElse(null);
    }

    /**
     * Check if the plugin has a file with the given path
     * @param path The path
     * @return True if the file is present
     */
    public boolean hasFile(String path) {
        return commonFiles.stream().anyMatch(f -> f.getPath().equals(path));
    }

    /**
     * Check if the plugin has the given file
     * @param file The file
     * @return True if the file is present
     */
    public boolean hasFile(ArchetypeCommonFile file) {
        return commonFiles.contains(file);
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
