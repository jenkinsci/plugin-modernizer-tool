package io.jenkins.tools.pluginmodernizer.core.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "false positive")
public class MavenInvoker {

    private static final Logger LOG = LoggerFactory.getLogger(MavenInvoker.class);
    private static final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    private final Config config;

    public MavenInvoker(Config config) {
        this.config = config;
    }

    public void invokeGoal(String pluginPath, String goal) {
        List<String> goals = new ArrayList<>();
        goals.add(goal);
        invokeGoals(pluginPath, goals);
    }

    public void invokeRewrite(String pluginPath) {
        try {
            List<String> goals = createGoalsList();
            if (goals == null) {
                LOG.info("No active recipes.");
                return;
            }
            invokeGoals(pluginPath, goals);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private List<String> createGoalsList() throws IOException {
        List<String> goals = new ArrayList<>();
        String mavenPluginVersion = config.getMavenPluginVersion();
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

    private void invokeGoals(String pluginPath, List<String> goals) {
        if (!validateMavenHome()) {
            return;
        }

        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(new File(config.getMavenHome()));

        try {
            InvocationRequest request = createInvocationRequest(pluginPath, goals);
            InvocationResult result = invoker.execute(request);
            handleInvocationResult(result);
        } catch (MavenInvocationException e) {
            LOG.error("Maven invocation failed: ", e);
        }
    }

    private boolean validateMavenHome() {
        String mavenHome = config.getMavenHome();
        if (mavenHome == null) {
            LOG.error("Neither MAVEN_HOME nor M2_HOME environment variables are set.");
            return false;
        }

        try {
            Path mavenHomePath = Paths.get(mavenHome).toRealPath();
            if (!Files.isDirectory(mavenHomePath) || !Files.isExecutable(mavenHomePath.resolve("bin/mvn"))) {
                LOG.error("Invalid Maven home directory. Aborting build.");
                return false;
            }
        } catch (IOException e) {
            LOG.error("Error validating Maven home directory: ", e);
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

    private void handleInvocationResult(InvocationResult result) {
        if (result.getExitCode() != 0) {
            LOG.error("Build failed.");
            if (result.getExecutionException() != null) {
                LOG.error("Execution exception occurred: ", result.getExecutionException());
            }
        } else {
            LOG.info("Build succeeded.");
        }
    }
}
