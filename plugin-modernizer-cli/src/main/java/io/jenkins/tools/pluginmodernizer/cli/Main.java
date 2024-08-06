package io.jenkins.tools.pluginmodernizer.cli;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.impl.PluginModernizer;
import io.jenkins.tools.pluginmodernizer.core.utils.PluginListParser;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "Plugin Modernizer",
        separator = " ",
        helpCommand = true,
        mixinStandardHelpOptions = true,
        versionProvider = PomVersionProvider.class,
        description = "Applies recipes to the plugins.",
        requiredOptionMarker = '*')
public class Main implements Runnable {

    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    private static Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(final String[] args) {
        new CommandLine(new Main()).setOptionsCaseInsensitive(true).execute(args);
    }

    @Option(
            names = {"-p", "--plugins"},
            description = "List of Plugins to Modernize.",
            split = ",",
            parameterConsumer = CommaSeparatedParameterConsumer.class)
    private List<String> plugins;

    @Option(
            names = {"-r", "--recipes"},
            required = true,
            description = "List of Recipes to be applied.",
            split = ",",
            parameterConsumer = CommaSeparatedParameterConsumer.class)
    private List<String> recipes;

    @Option(
            names = {"-f", "--plugin-file"},
            description = "Path to the file that contains a list of plugins.")
    private Path pluginFile;

    @Option(
            names = {"-g", "--github-owner"},
            description = "GitHub owner for forked repositories.")
    private String githubOwner = Settings.GITHUB_OWNER;

    @Option(
            names = {"-n", "--dry-run"},
            description = "Perform a dry run without making any changes.")
    public boolean dryRun;

    @Option(
            names = {"--skip-push"},
            description = "Skip pushing changes to the forked repositories. Always true if --dry-run is set.")
    public boolean skipPush;

    @Option(
            names = {"--skip-pull-request"},
            description = "Skip creating pull requests but pull changes to the fork. Always true if --dry-run is set.")
    public boolean skipPullRequest;

    @Option(
            names = {"--clean-local-data"},
            description = "Remove local plugin data before and after the modernization process.")
    public boolean removeLocalData;

    @Option(
            names = {"--clean-forks"},
            description =
                    "Remove forked repositories before and after the modernization process. Might cause data loss if you have other changes pushed on those forks. Forks with open pull request targeting original repo are not removed to prevent closing unmerged pull requests.")
    public boolean removeForks;

    @Option(
            names = {"-e", "--export-datatables"},
            description = "Creates a report or summary of the changes made through OpenRewrite.")
    public boolean exportDatatables;

    @Option(
            names = {"-d", "--debug"},
            description = "Enable debug logging.")
    public boolean debug;

    @Option(
            names = "--jenkins-update-center",
            description =
                    "Sets main update center; will override JENKINS_UC environment variable. If not set via CLI option or environment variable, will use default update center url.")
    public URL jenkinsUpdateCenter = Settings.DEFAULT_UPDATE_CENTER_URL;

    @Option(
            names = {"-c", "--cache-path"},
            description = "Path to the cache directory.")
    public Path cachePath = Settings.DEFAULT_CACHE_PATH;

    @Option(
            names = {"-m", "--maven-home"},
            description = "Path to the Maven Home directory.")
    public Path mavenHome = Settings.DEFAULT_MAVEN_HOME;

    @Option(
            names = {"--source-java-major-version"},
            description =
                    "Source Java major version to compile the plugin before attempting to modernize it for recommended Java version")
    public int sourceJavaMajorVersion = Settings.SOURCE_JAVA_MAJOR_VERSION;

    @Option(
            names = {"--target-java-major-version"},
            description = "Target Java major version to modernize the plugin and run recipes")
    public int minimalJavaMajorVersion = Settings.TARGET_JAVA_MAJOR_VERSION;

    @Option(
            names = {"-l", "--list-recipes"},
            help = true,
            description = "List available recipes.")
    public boolean listRecipes;

    public Config setup() {
        Config.DEBUG = debug;
        return Config.builder()
                .withVersion(getVersion())
                .withGitHubOwner(githubOwner)
                .withPlugins(plugins)
                .withRecipes(recipes)
                .withDryRun(dryRun)
                .withSkipPush(skipPush)
                .withSkipPullRequest(skipPullRequest)
                .withRemoveLocalData(removeLocalData)
                .withRemoveForks(removeForks)
                .withExportDatatables(exportDatatables)
                .withJenkinsUpdateCenter(jenkinsUpdateCenter)
                .withCachePath(cachePath)
                .withMavenHome(mavenHome)
                .withSourceJavaMajorVersion(sourceJavaMajorVersion)
                .withTargetJavaMajorVersion(minimalJavaMajorVersion)
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
        LOG.info("Available recipes:");
        // Strip the FQDN prefix from the recipe name
        Settings.AVAILABLE_RECIPES.forEach(recipe -> LOG.info(
                "{} - {}",
                recipe.getName().replaceAll(Settings.RECIPE_FQDN_PREFIX + ".", ""),
                recipe.getDescription()));
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

        return loadedPlugins.stream().distinct().collect(Collectors.toCollection(ArrayList::new));
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
