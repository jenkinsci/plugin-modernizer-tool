package io.jenkins.tools.pluginmodernizer.core.impl;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.openrewrite.Recipe;
import org.openrewrite.config.YamlResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

@SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "false positive")
public class MavenInvoker {

    private static final Logger LOG = LoggerFactory.getLogger(MavenInvoker.class);

    private final Config config;

    public MavenInvoker(Config config) {
        this.config = config;
        validateMavenHome();
        validateMavenVersion();
    }

    public @Nullable ComparableVersion getMavenVersion() {
        AtomicReference<String> version = new AtomicReference<>();
        try {
            Invoker invoker = new DefaultInvoker();
            invoker.setMavenHome(config.getMavenHome().toFile());
            InvocationRequest request = new DefaultInvocationRequest();
            request.setBatchMode(true);
            request.addArg("-q");
            request.addArg("--version");
            request.setOutputHandler(version::set);
            invoker.execute(request);
            return new ComparableVersion(version.get());
        } catch (MavenInvocationException e) {
            LOG.error("Failed to check for maven version", e);
            return null;
        }
    }

    public void invokeGoal(String plugin, String pluginPath, String goal) {
        invokeGoals(plugin, pluginPath, List.of(goal));
    }

    public void invokeRewrite(String plugin, String pluginPath) {
        try {
            List<String> goals = createGoalsList();
            if (goals == null) {
                LOG.info(MarkerFactory.getMarker(plugin), "No active recipes.");
                return;
            }
            invokeGoals(plugin, pluginPath, goals);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private List<String> createGoalsList() throws IOException {
        List<String> goals = new ArrayList<>();
        String mode = config.isDryRun() ? "dryRun" : "run";
        goals.add("org.openrewrite.maven:rewrite-maven-plugin:" + Settings.MAVEN_REWRITE_PLUGIN_VERSION + ":" + mode);
        goals.add("-Drewrite.exportDatatables=" + config.isExportDatatables());

        try (InputStream inputStream = getClass().getResourceAsStream("/" + Settings.RECIPE_DATA_YAML_PATH)) {
            YamlResourceLoader yamlResourceLoader =
                    new YamlResourceLoader(inputStream, URI.create(Settings.RECIPE_DATA_YAML_PATH), new Properties());
            List<Recipe> recipeDescriptors =
                    yamlResourceLoader.listRecipes().stream().toList();

            List<String> recipes = config.getRecipes();
            List<String> activeRecipes = getActiveRecipes(recipes, recipeDescriptors);
            LOG.debug("Active recipes: {}", activeRecipes);
            if (activeRecipes.isEmpty()) {
                return null;
            }
            goals.add("-Drewrite.activeRecipes=" + String.join(",", activeRecipes));
            goals.add("-Drewrite.recipeArtifactCoordinates=io.jenkins.plugin-modernizer:plugin-modernizer-core:"
                    + config.getVersion());
        }
        return goals;
    }

    private List<String> getActiveRecipes(List<String> recipes, List<Recipe> recipeDescriptors) {
        return recipes.stream()
                .flatMap(recipe ->
                        recipeDescriptors.stream().map(Recipe::getName).filter(name -> name.endsWith(recipe)))
                .collect(Collectors.toList());
    }

    private void invokeGoals(String plugin, String pluginPath, List<String> goals) {
        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(config.getMavenHome().toFile());
        try {
            InvocationRequest request = createInvocationRequest(pluginPath, goals);
            request.setBatchMode(true);
            request.setNoTransferProgress(false);
            request.setErrorHandler((message) -> {
                LOG.error(
                        MarkerFactory.getMarker(plugin),
                        String.format("Something went wrong when running maven: %s", message));
            });
            request.setOutputHandler((message) -> {
                LOG.info(MarkerFactory.getMarker(plugin), message);
            });
            InvocationResult result = invoker.execute(request);
            handleInvocationResult(plugin, result);
        } catch (MavenInvocationException e) {
            LOG.error(MarkerFactory.getMarker(plugin), "Maven invocation failed: ", e);
        }
    }

    /**
     * Validate the Maven home directory.
     * @throws IllegalArgumentException if the Maven home directory is not set or invalid.
     */
    private void validateMavenHome() {
        Path mavenHome = config.getMavenHome();
        if (mavenHome == null) {
            LOG.error("Neither MAVEN_HOME nor M2_HOME environment variables are set.");
            throw new IllegalArgumentException("Maven home directory not set.");
        }

        if (!Files.isDirectory(mavenHome) || !Files.isExecutable(mavenHome.resolve("bin/mvn"))) {
            LOG.error("Invalid Maven home directory. Aborting build.");
            throw new IllegalArgumentException("Invalid Maven home directory.");
        }
    }

    /**
     * Validate the Maven version.
     * @throws IllegalArgumentException if the Maven version is too old or cannot be determined.
     */
    private void validateMavenVersion() {
        ComparableVersion mavenVersion = getMavenVersion();
        LOG.debug("Maven version detected: {}", mavenVersion);
        if (mavenVersion == null) {
            LOG.error("Failed to check Maven version. Aborting build.");
            throw new IllegalArgumentException("Failed to check Maven version.");
        }
        if (mavenVersion.compareTo(Settings.MAVEN_MINIMAL_VERSION) < 0) {
            LOG.error(
                    "Maven version detected {}, is too old. Please use at least version {}",
                    mavenVersion,
                    Settings.MAVEN_MINIMAL_VERSION);
            throw new IllegalArgumentException("Maven version is too old.");
        }
    }

    private InvocationRequest createInvocationRequest(String pluginPath, List<String> goals) {
        InvocationRequest request = new DefaultInvocationRequest();
        Path pomPath = Paths.get(pluginPath, "pom.xml");
        request.setPomFile(pomPath.toFile());
        request.setGoals(goals);
        return request;
    }

    private void handleInvocationResult(String plugin, InvocationResult result) {
        if (result.getExitCode() != 0) {
            LOG.error(MarkerFactory.getMarker(plugin), "Build fail with code: {}", result.getExitCode());
            if (result.getExecutionException() != null) {
                LOG.error(
                        MarkerFactory.getMarker(plugin),
                        "Execution exception occurred: ",
                        result.getExecutionException());
            }
        } else {
            LOG.info(MarkerFactory.getMarker(plugin), "Build success!");
        }
    }
}
