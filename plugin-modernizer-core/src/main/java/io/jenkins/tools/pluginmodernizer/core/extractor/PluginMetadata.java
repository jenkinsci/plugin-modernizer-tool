package io.jenkins.tools.pluginmodernizer.core.extractor;

import java.io.Serializable;
import java.util.Map;

/**
 * Metadata of a plugin extracted from its POM file or code
 */
public class PluginMetadata implements Serializable {

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

    public PluginMetadata() {}

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
