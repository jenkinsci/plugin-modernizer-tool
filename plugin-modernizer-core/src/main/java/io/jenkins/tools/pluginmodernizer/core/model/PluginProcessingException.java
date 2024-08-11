package io.jenkins.tools.pluginmodernizer.core.model;

/**
 * A plugin processing exception
 */
public class PluginProcessingException extends ModernizerException {

    /**
     * The plugin that caused the exception
     */
    private final Plugin plugin;

    /**
     * Create a new PluginProcessingException
     * @param message The message
     */
    public PluginProcessingException(String message, Plugin plugin) {
        this(message, null, plugin);
    }

    /**
     * Create a new PluginProcessingException
     * @param message The message
     * @param cause The cause
     */
    public PluginProcessingException(String message, Throwable cause, Plugin plugin) {
        super(message, cause);
        this.plugin = plugin;
    }

    /**
     * Get the plugin that caused the exception
     * @return The plugin
     */
    public Plugin getPlugin() {
        return plugin;
    }
}
