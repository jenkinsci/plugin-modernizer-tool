package io.jenkins.tools.pluginmodernizer.core.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

@SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "false positive")
public class MavenInvoker {

    private static final Logger LOG = LoggerFactory.getLogger(MavenInvoker.class);
    private static final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    private final Config config;

    public MavenInvoker(Config config) {
        this.config = config;
    }

    public void invokeGoal(String plugin, String pluginPath, String goal) {
        List<String> goals = new ArrayList<>();
        goals.add(goal);
        invokeGoals(plugin, pluginPath, goals);
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
        String mavenPluginVersion = Settings.MAVEN_REWRITE_PLUGIN_VERSION;
        String mode = config.isDryRun() ? "dryRun" : "run";
        goals.add("org.openrewrite.maven:rewrite-maven-plugin:" + mavenPluginVersion + ":" + mode);

        try (InputStream inputStream = getClass().getResourceAsStream("/recipe_data.yaml")) {
            JsonNode recipesNode = objectMapper.readTree(inputStream).get("recipes");

            List<String> recipes = config.getRecipes();
            List<String> activeRecipes = getActiveRecipes(recipes, recipesNode);
            List<String> recipeArtifactCoordinates = getRecipeArtifactCoordinates(recipes, recipesNode);

            if (activeRecipes.isEmpty()) {
                return null;
            }
            goals.add("-Drewrite.activeRecipes=" + String.join(",", activeRecipes));
            if (!recipeArtifactCoordinates.isEmpty()) {
                goals.add("-Drewrite.recipeArtifactCoordinates=" + String.join(",", recipeArtifactCoordinates));
            }
        }
        return goals;
    }

    private List<String> getActiveRecipes(List<String> recipes, JsonNode recipesNode) {
        List<String> activeRecipes = new ArrayList<>();
        for (String recipe : recipes) {
            JsonNode recipeNode = recipesNode.get(recipe);
            if (recipeNode != null) {
                activeRecipes.add(recipeNode.get("fqcn").asText());
            } else {
                LOG.error("Recipe {} not found", recipe);
            }
        }
        return activeRecipes;
    }

    private List<String> getRecipeArtifactCoordinates(List<String> recipes, JsonNode recipesNode) {
        List<String> recipeArtifactCoordinates = new ArrayList<>();
        for (String recipe : recipes) {
            JsonNode recipeNode = recipesNode.get(recipe);
            if (recipeNode != null) {
                recipeArtifactCoordinates.add(recipeNode.get("artifactCoordinates").asText());
            }
        }
        return recipeArtifactCoordinates;
    }

    private void invokeGoals(String plugin, String pluginPath, List<String> goals) {
        if (!validateMavenHome()) {
            return;
        }

        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(config.getMavenHome().toFile());
        try {
            InvocationRequest request = createInvocationRequest(pluginPath, goals);
            request.setBatchMode(true);
            request.setNoTransferProgress(false);
            request.setErrorHandler((message) -> {
                LOG.error(MarkerFactory.getMarker(plugin), String.format("Something went wrong when running maven: %s", message));
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

    private boolean validateMavenHome() {
        Path mavenHome = config.getMavenHome();
        if (mavenHome == null) {
            LOG.error("Neither MAVEN_HOME nor M2_HOME environment variables are set.");
            return false;
        }

        if (!Files.isDirectory(mavenHome) || !Files.isExecutable(mavenHome.resolve("bin/mvn"))) {
            LOG.error("Invalid Maven home directory. Aborting build.");
            return false;
        }

        return true;
    }

    private InvocationRequest createInvocationRequest(String pluginPath, List<String> goals) {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File(pluginPath, "pom.xml"));
        request.setGoals(goals);
        return request;
    }

    private void handleInvocationResult(String plugin, InvocationResult result) {
        if (result.getExitCode() != 0) {
            LOG.error(MarkerFactory.getMarker(plugin), "Build fail with code: {}", result.getExitCode());
            if (result.getExecutionException() != null) {
                LOG.error(MarkerFactory.getMarker(plugin), "Execution exception occurred: ", result.getExecutionException());
            }
        } else {
            LOG.info(MarkerFactory.getMarker(plugin), "Build success!");
        }
    }
}
