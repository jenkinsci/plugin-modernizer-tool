package io.jenkins.tools.pluginmodernizer.core.extractor;

public record Dependency(String groupId, String artifactId, String version, String scope, String type) {

    @Override
    public String toString() {
        return "Dependency{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                ", scope='" + scope + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
