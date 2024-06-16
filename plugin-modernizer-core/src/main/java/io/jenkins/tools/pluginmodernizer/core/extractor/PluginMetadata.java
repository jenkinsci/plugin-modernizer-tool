package io.jenkins.tools.pluginmodernizer.core.extractor;

import java.util.List;
import java.util.Map;

import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.Parent;

public class PluginMetadata {
    private String pluginName;
    private boolean isLicensed;
    private boolean hasDevelopersTag;
    private boolean hasJavaLevel;
    private boolean usesHttps;
    private List<Dependency> dependencies;
    private String jenkinsVersion;
    private Parent pluginParent;
    private Map<String, String> properties;

    private static volatile PluginMetadata instance;

    private PluginMetadata() {}

    public static PluginMetadata getInstance() {
        if (instance == null) {
            synchronized (PluginMetadata.class) {
                if (instance == null) {
                    instance = new PluginMetadata();
                }
            }
        }
        return instance;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public boolean isLicensed() {
        return isLicensed;
    }

    public void setLicensed(boolean isLicensed) {
        this.isLicensed = isLicensed;
    }

    public boolean hasJavaLevel() {
        return hasJavaLevel;
    }

    public void setHasJavaLevel(boolean hasJavaLevel) {
        this.hasJavaLevel = hasJavaLevel;
    }

    public boolean hasDevelopersTag() {
        return hasDevelopersTag;
    }

    public void setHasDevelopersTag(boolean hasDevelopersTag) {
        this.hasDevelopersTag = hasDevelopersTag;
    }

    public boolean isUsesHttps() {
        return usesHttps;
    }

    public void setUsesHttps(boolean usesHttps) {
        this.usesHttps = usesHttps;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }

    public String getJenkinsVersion() {
        return jenkinsVersion;
    }

    public void setJenkinsVersion(String jenkinsVersion) {
        this.jenkinsVersion = jenkinsVersion;
    }

    public Parent getPluginParent() {
        return pluginParent;
    }

    public void setPluginParent(Parent pluginParent) {
        this.pluginParent = pluginParent;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
