package io.jenkins.tools.pluginmodernizer.core.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

public class UpdateCenterData {
    private final JsonNode jsonNode;

    public UpdateCenterData(JsonNode jsonNode) {
        this.jsonNode = Objects.requireNonNull(jsonNode, "jsonNode must not be null");
    }

    public JsonNode getPlugin(String pluginName) {
        JsonNode plugins = jsonNode.get("plugins");
        if (plugins == null || !plugins.has(pluginName)) {
            throw new IllegalArgumentException("Plugin not found in update center: " + pluginName);
        }
        return plugins.get(pluginName);
    }

    public String getScmUrl(String pluginName) {
        JsonNode pluginInfo = getPlugin(pluginName);
        JsonNode scmNode = pluginInfo.get("scm");

        if (scmNode == null) {
            throw new NoSuchElementException("SCM information is missing for plugin: " + pluginName);
        }

        if (scmNode.isObject()) {
            return Optional.ofNullable(scmNode.get("url"))
                    .map(JsonNode::asText)
                    .orElseThrow(() -> new NoSuchElementException("SCM URL is missing for plugin: " + pluginName));
        } else if (scmNode.isTextual()) {
            return scmNode.asText();
        } else {
            throw new IllegalStateException("Unexpected type for SCM URL: " + scmNode.getNodeType());
        }
    }
}
