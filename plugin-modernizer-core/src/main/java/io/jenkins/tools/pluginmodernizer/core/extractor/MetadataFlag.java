package io.jenkins.tools.pluginmodernizer.core.extractor;

import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.utils.PluginService;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    IS_API_PLUGIN(null, (plugin, pluginService) -> pluginService.isApiPlugin(plugin)),

    /**
     * If the plugin is deprecated
     */
    IS_DEPRECATED(null, (plugin, pluginService) -> pluginService.isDeprecated(plugin)),

    /**
     * If the plugin is for adoption
     */
    IS_FOR_ADOPTION(null, (plugin, pluginService) -> pluginService.isForAdoption(plugin)),

    /**
     * If the plugin has a max score (100 %)
     */
    HAS_MAX_SCORE(null, (plugin, pluginService) -> pluginService.hasMaxScore(plugin)),

    /**
     * If the plugin has a low score
     */
    HAS_LOW_SCORE(null, (plugin, pluginService) -> pluginService.hasLowScore(plugin)),

    /**
     * If the plugin has no known installation
     */
    NO_KNOWN_INSTALLATION(null, (plugin, pluginService) -> pluginService.hasNoKnownInstallations(plugin));

    /**
     * Function to check if the flag is applicable for the given XML tag
     */
    private final Predicate<MetadataXmlTag> isApplicableTag;

    /**
     * Function to check if the flag is applicable for the given plugin
     */
    private final BiPredicate<Plugin, PluginService> isApplicablePlugin;

    /**
     * Constructor
     * @param isApplicableTag Predicate to check if the flag is applicable for the given XML tag
     */
    MetadataFlag(Predicate<MetadataXmlTag> isApplicableTag, BiPredicate<Plugin, PluginService> isApplicablePlugin) {
        this.isApplicableTag = isApplicableTag;
        this.isApplicablePlugin = isApplicablePlugin;
    }

    /**
     * Check if the flag is applicable for the given XML tag
     * @param tag XML tag
     * @return true if the flag is applicable
     */
    public boolean isApplicable(MetadataXmlTag tag) {
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
    public boolean isApplicable(Plugin plugin, PluginService pluginService) {
        if (plugin.getMetadata() == null) {
            LOG.debug("Metadata not found for plugin {}", plugin.getName());
            return false;
        }
        if (plugin.getMetadata().hasFlag(this)) {
            LOG.trace("Flag {} already set for plugin {}", this, plugin.getName());
            return true;
        }
        if (isApplicablePlugin == null) {
            LOG.trace("No applicable plugin check for flag {}", this);
            return false;
        }
        boolean result = isApplicablePlugin.test(plugin, pluginService);
        LOG.debug("Flag {} applicable for plugin {}: {}", this, plugin.getName(), result);
        return result;
    }

    private static final Logger LOG = LoggerFactory.getLogger(MetadataFlag.class);
}
