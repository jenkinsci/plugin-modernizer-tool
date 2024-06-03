package io.jenkins.tools.pluginmodernizer.cli;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "Plugin Modernizer", mixinStandardHelpOptions = true, versionProvider = PomVersionProvider.class, description = "Applies recipes to the plugins.", requiredOptionMarker = '*')
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

    public List<String> getPlugins() {
        return plugins;
    }

    public List<String> getRecipes() {
        return recipes;
    }


    @Override
    public void run() {
        // TODO: Modify the logic after building core
        LOG.info("Triggers Plugin Modernizer");
    }
}
