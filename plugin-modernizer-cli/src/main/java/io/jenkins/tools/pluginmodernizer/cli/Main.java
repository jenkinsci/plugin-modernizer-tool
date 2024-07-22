package io.jenkins.tools.pluginmodernizer.cli;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.impl.PluginModernizer;
import io.jenkins.tools.pluginmodernizer.core.model.RecipeDescriptor;
import io.jenkins.tools.pluginmodernizer.core.utils.PluginListParser;
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
        new CommandLine(new Main()).setOptionsCaseInsensitive(true).execute(args);
    }

    @Option(names = {"-p", "--plugins"}, description = "List of Plugins to Modernize.", split = ",", parameterConsumer = CommaSeparatedParameterConsumer.class)
    private List<String> plugins;

    @Option(names = {"-r", "--recipes"}, required = true, description = "List of Recipes to be applied.", split = ",", parameterConsumer = CommaSeparatedParameterConsumer.class)
    private List<String> recipes;

    @Option(names = {"-f", "--plugin-file"}, description = "Path to the file that contains a list of plugins")
    private Path pluginFile;

    @Option(names = {"-g", "--github-owner"}, description = "GitHub owner for forked repositories")
    private String githubOwner = Settings.GITHUB_OWNER;

    @Option(names = {"-n", "--dry-run"}, description = "Perform a dry run without making any changes.")
    public boolean dryRun;

    @Option(names = {"-e", "--export-datatables"}, description = "Creates a report or summary of the changes made through OpenRewrite.")
    public boolean exportDatatables;

    @Option(names = {"-d", "--debug"}, description = "Enable debug logging.")
    public boolean debug;

    @Option(names = "--jenkins-update-center", description = "Sets main update center; will override JENKINS_UC environment variable. If not set via CLI option or environment variable, will use default update center url")
    public URL jenkinsUpdateCenter = Settings.DEFAULT_UPDATE_CENTER_URL;

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
                .withGitHubOwner(githubOwner)
                .withPlugins(plugins)
                .withRecipes(recipes)
                .withDryRun(dryRun)
                .withExportDatatables(exportDatatables)
                .withJenkinsUpdateCenter(jenkinsUpdateCenter)
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
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(Settings.RECIPE_DATA_YAML_PATH);
        if (inputStream == null) {
            LOG.error("Failed to load the available recipes.");
            return;
        }
        try {
            List<RecipeDescriptor> recipeDescriptors = new YAMLMapper().readValue(inputStream, new TypeReference<List<RecipeDescriptor>>() {});

            if (recipeDescriptors == null || recipeDescriptors.isEmpty()) {
                LOG.error("No recipes found in the YAML file.");
                return;
            }

            LOG.info("Available recipes:");
            recipeDescriptors.forEach(recipe -> LOG.info("{} - {}", recipe.name(), recipe.description()));
        } catch (Exception e) {
            LOG.error("Error loading recipes from YAML: {}", e.getMessage());
        }
    }

    private List<String> loadPlugins() {
        List<String> loadedPlugins = new ArrayList<>();

        if (pluginFile != null) {
            List<String> pluginsFromFile = PluginListParser.loadPluginsFromFile(pluginFile);
            if (pluginsFromFile != null) {
                loadedPlugins.addAll(pluginsFromFile);
            }
        }

        if (plugins != null) {
            loadedPlugins.addAll(plugins);
        }

        return loadedPlugins.stream()
                            .distinct()
                            .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public void run() {
        plugins = loadPlugins();

        if (listRecipes) {
            listAvailableRecipes();
            return;
        }
        LOG.info("Starting Plugin Modernizer");
        PluginModernizer modernizer = new PluginModernizer(setup());
        modernizer.start();
    }
}
