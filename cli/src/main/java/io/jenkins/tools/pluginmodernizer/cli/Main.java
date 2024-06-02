package io.jenkins.tools.pluginmodernizer.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;

@Command(name = "Plugin Modernizer", mixinStandardHelpOptions = true, versionProvider = PomVersionProvider.class, description = "Applies recipes to the plugins.", requiredOptionMarker = '*')
public class Main implements Runnable{
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
        System.out.println("Triggers Plugin Modernizer");
    }
}