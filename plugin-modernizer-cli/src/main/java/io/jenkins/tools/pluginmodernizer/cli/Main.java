package io.jenkins.tools.pluginmodernizer.cli;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.impl.PluginModernizer;
import io.jenkins.tools.pluginmodernizer.core.model.RecipeData;
import io.jenkins.tools.pluginmodernizer.core.model.RecipeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.yaml.snakeyaml.Yaml;
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
        new CommandLine(new Main()).setOptionsCaseInsensitive(true).execute(args);
    }

    @Option(names = {"-p", "--plugins"}, required = true, description = "List of Plugins to Modernize.", split = ",", parameterConsumer = CommaSeparatedParameterConsumer.class)
    private List<String> plugins;

    @Option(names = {"-r", "--recipes"}, required = true, description = "List of Recipes to be applied.", split = ",", parameterConsumer = CommaSeparatedParameterConsumer.class)
    private List<String> recipes;

    @Option(names = {"-n", "--dry-run"}, description = "Perform a dry run without making any changes.")
    public boolean dryRun;

    @Option(names = {"-d", "--debug"}, description = "Enable debug logging.")
    public boolean debug;

    @Option(names = {"-c", "--cache-path"}, description = "Path to the cache directory.")
    public Path cachePath = Settings.DEFAULT_CACHE_PATH;

    @Option(names = {"-m", "--maven-home"}, description = "Path to the Maven Home directory.")
    public Path mavenHome = Settings.DEFAULT_MAVEN_HOME;

    @Option(names = {"-l", "--list-recipes"}, help = true, description = "List available recipes.")
    public boolean listRecipes;

    public Config setup() {
        Config.DEBUG = debug;
        return Config.builder()
                .withVersion(getVersion())
                .withPlugins(plugins)
                .withRecipes(recipes)
                .withDryRun(dryRun)
                .withCachePath(cachePath)
                .withMavenHome(mavenHome)
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

    public void listAvailableRecipes() {
        Yaml yaml = new Yaml();
        InputStream inputStream = Settings.RECIPE_DATA_YAML;
        if (inputStream == null) {
            LOG.error("Failed to load the available recipes.");
            return;
        }
        try {
            RecipeData data = yaml.loadAs(inputStream, RecipeData.class);
            if (data == null || data.getRecipes() == null) {
                LOG.error("No recipes found in the YAML file.");
                return;
            }

            LOG.info("Available recipes:");
            List<RecipeDescriptor> recipes = data.getRecipes();
            recipes.forEach(recipe -> LOG.info("{} - {}", recipe.getName(), recipe.getDescription()));
        } catch (Exception e) {
            LOG.error("Error loading recipes from YAML: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        if (listRecipes) {
            listAvailableRecipes();
            return;
        }
        LOG.info("Starting Plugin Modernizer");
        PluginModernizer modernizer = new PluginModernizer(setup());
        modernizer.start();
    }
}
