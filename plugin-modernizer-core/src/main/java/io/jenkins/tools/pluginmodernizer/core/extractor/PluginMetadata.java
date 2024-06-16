package io.jenkins.tools.pluginmodernizer.core.extractor;

import java.util.List;
import java.util.Map;

import org.openrewrite.maven.tree.*;

public class PluginMetadata {
    private String pluginName;
    private String pluginVersion;
    private boolean isLicense;
    private boolean isDevelopers;
    private List<Dependency> dependencies;
    private String jenkinsVersion;
    private Parent pluginParent;
    private Map<String, String> properties;
}
