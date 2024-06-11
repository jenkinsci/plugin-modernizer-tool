package io.jenkins.tools.pluginmodernizer.core.extractor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("NM_METHOD_NAMING_CONVENTION")
public record Scm(String connection, String developerConnection, String url, String tag) {
}
