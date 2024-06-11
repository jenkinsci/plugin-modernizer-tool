package io.jenkins.tools.pluginmodernizer.core.extractor;

public record JenkinsBom(String groupId, String artifactId, String version, String type, String scope) {
}
