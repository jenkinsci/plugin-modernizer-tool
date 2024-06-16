package io.jenkins.tools.pluginmodernizer.cli;

import java.util.List;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.impl.PluginModernizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "Plugin Modernizer", separator = " ", helpCommand = true, mixinStandardHelpOptions = true, versionProvider = PomVersionProvider.class, description = "Applies recipes to the plugins.", requiredOptionMarker = '*')
public class Main implements Runnable {

    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    private static Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(final String[] args) {
        new CommandLine(new Main()).execute(args);
    }

    @Option(names = {"-p", "--plugins"}, required = true, description = "List of Plugins to Modernize.", split = ",", parameterConsumer = CommaSeparatedParameterConsumer.class)
    private List<String> plugins;

    @Option(names = {"-r", "--recipes"}, required = true, description = "List of Recipes to be applied.", split = ",", parameterConsumer = CommaSeparatedParameterConsumer.class)
    private List<String> recipes;

    @Option(names = {"-d", "--debug"}, description = "Enable debug logging.")
    public boolean debug;

    public Config setup() {
        Config.DEBUG = debug;
        return Config.builder()
                .withVersion(getVersion())
                .withPlugins(plugins)
                .withRecipes(recipes)
                .build();
    }

    public String getVersion() {
        try {
            return new PomVersionProvider().getVersion()[0];
        } catch (Exception e) {
            LOG.error("Error getting version from pom.properties", e);
            return "unknown";
        }
    }

    @Override
    public void run() {
        LOG.info("Starting Plugin Modernizer");
        PluginModernizer modernizer = new PluginModernizer(setup());
        modernizer.start();
    }
}
