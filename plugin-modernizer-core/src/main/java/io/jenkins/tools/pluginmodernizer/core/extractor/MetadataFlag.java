package io.jenkins.tools.pluginmodernizer.core.extractor;

import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import java.util.Optional;
import java.util.function.Predicate;
import org.openrewrite.xml.tree.Xml;

/**
 * Flag for metadata
 */
public enum MetadataFlag {

    /**
     * If the SCM URL uses HTTPS
     */
    SCM_HTTPS(
            tag -> {
                if ("scm".equals(tag.getName())) {
                    Optional<String> connection = tag.getChildValue("connection");
                    return connection.isPresent() && connection.get().startsWith("scm:git:https");
                }
                return false;
            },
            null),

    /**
     * If the plugin uses HTTPS for all its repositories
     */
    MAVEN_REPOSITORIES_HTTPS(
            tag -> {
                if ("repositories".equals(tag.getName())) {
                    return tag.getChildren().stream()
                            .filter(c -> "repository".equals(c.getName()))
                            .map(Xml.Tag.class::cast)
                            .map(r -> r.getChildValue("url").orElseThrow())
                            .allMatch(url -> url.startsWith("https"));
                }
                return false;
            },
            null),

    /**
     * If the license block is set
     */
    LICENSE_SET(
            tag -> {
                if ("licenses".equals(tag.getName())) {
                    return tag.getChildren().stream()
                            .filter(c -> "license".equals(c.getName()))
                            .map(Xml.Tag.class::cast)
                            .map(r -> r.getChildValue("name").orElseThrow())
                            .findAny()
                            .isPresent();
                }
                return false;
            },
            null),

    /**
     * If the develop block is set
     */
    DEVELOPER_SET(
            tag -> {
                if ("developers".equals(tag.getName())) {
                    return tag.getChildren().stream()
                            .filter(c -> "developer".equals(c.getName()))
                            .map(Xml.Tag.class::cast)
                            .map(r -> r.getChildValue("id").orElseThrow())
                            .findAny()
                            .isPresent();
                }
                return false;
            },
            null),

    /**
     * If the plugin is an API plugin
     */
    IS_API_PLUGIN(null, plugin -> plugin.getMetadata().getProperties().containsKey("isApiPlugin"));

    /**
     * Function to check if the flag is applicable for the given XML tag
     */
    private final Predicate<Xml.Tag> isApplicableTag;

    /**
     * Function to check if the flag is applicable for the given plugin
     */
    private final Predicate<Plugin> isApplicablePlugin;

    /**
     * Constructor
     * @param isApplicableTag Predicate to check if the flag is applicable for the given XML tag
     */
    MetadataFlag(Predicate<Xml.Tag> isApplicableTag, Predicate<Plugin> isApplicablePlugin) {
        this.isApplicableTag = isApplicableTag;
        this.isApplicablePlugin = isApplicablePlugin;
    }

    /**
     * Check if the flag is applicable for the given XML tag
     * @param tag XML tag
     * @return true if the flag is applicable
     */
    public boolean isApplicable(Xml.Tag tag) {
        if (isApplicableTag == null) {
            return false;
        }
        return isApplicableTag.test(tag);
    }

    /**
     * Check if the flag is applicable for the given plugin
     * @param plugin Plugin
     * @return true if the flag is applicable
     */
    public boolean isApplicable(Plugin plugin) {
        if (plugin.getMetadata() == null) {
            return false;
        }
        if (plugin.getMetadata().hasFlag(this)) {
            return true;
        }
        if (isApplicablePlugin == null) {
            return false;
        }
        return isApplicablePlugin.test(plugin);
    }
}
