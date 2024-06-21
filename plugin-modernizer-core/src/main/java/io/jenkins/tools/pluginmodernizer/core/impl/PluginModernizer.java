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
public class PluginModernizer {

    private static final Logger LOG = LoggerFactory.getLogger(PluginModernizer.class);

    private final Config config;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public PluginModernizer(Config config) {
        this.config = config;
    }

    public void start() {
        String projectRoot = System.getProperty("user.dir");
        // TODO: Fn to populate the directory with the plugins
        File pluginsDir = new File(projectRoot, "test-plugins");

        File[] subdirectories = pluginsDir.listFiles(File::isDirectory);
        if (subdirectories != null) {
            for (File pluginDir : subdirectories) {
                try {
                    List<String> goals = createGoalsList();
                    if (goals == null) {
                        LOG.info("No active recipes.");
                        return;
                    }
                    invokeMavenGoals(pluginDir, goals);
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        } else {
            LOG.error("No subdirectories found in {}", pluginsDir.getAbsolutePath());
        }
        LOG.info("Plugins: {}", config.getPlugins());
        LOG.info("Recipes: {}", config.getRecipes());
        LOG.debug("Cache Path: {}", config.getCachePath());
    }

    private static void invokeMavenGoals(File pluginDir, List<String> goals) {
        Invoker invoker = new DefaultInvoker();
        String mavenHome = getMavenHomePath();
        if (mavenHome == null) {
            LOG.error("Neither MAVEN_HOME nor M2_HOME environment variables are set.");
            return;
        }
        try {
            Path mavenHomePath = Paths.get(mavenHome).toRealPath();
            if (!Files.isDirectory(mavenHomePath) || !Files.isExecutable(mavenHomePath.resolve("bin/mvn"))) {
                LOG.error("Invalid Maven home directory. Aborting build.");
                return;
            }
            invoker.setMavenHome(new File(mavenHome));
            InvocationRequest request = new DefaultInvocationRequest();
            request.setPomFile(new File(pluginDir, "pom.xml"));
            request.setGoals(goals);
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                LOG.error("Build failed.");
                if (result.getExecutionException() != null) {
                    LOG.error("Execution exception occurred: ", result.getExecutionException());
                }
            } else {
                LOG.info("Build succeeded.");
            }
        } catch (MavenInvocationException e) {
            LOG.error("Maven invocation failed: ", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getMavenHomePath() {
        String mavenHome = System.getenv("MAVEN_HOME");
        if (mavenHome == null) {
            mavenHome = System.getenv("M2_HOME");
        }
        return mavenHome;
    }

    List<String> createGoalsList() throws IOException {
        List<String> goals = new ArrayList<>();
        goals.add("org.openrewrite.maven:rewrite-maven-plugin:run");

        try (InputStream inputStream = PluginModernizer.class.getResourceAsStream("/recipe_data.json")) {
            JsonNode recipesNode = objectMapper.readTree(inputStream).get("recipes");

            List<String> recipes = config.getRecipes();
            List<String> activeRecipes = new ArrayList<>();
            List<String> recipeArtifactCoordinates = new ArrayList<>();

            for (String recipe : recipes) {
                JsonNode recipeNode = recipesNode.get(recipe);
                if (recipeNode != null) {
                    String fqcn = recipeNode.get("fqcn").asText();
                    activeRecipes.add(fqcn);

                    String artifactCoordinates = recipeNode.get("artifactCoordinates").asText();
                    recipeArtifactCoordinates.add(artifactCoordinates);
                } else {
                    System.err.println("Recipe '" + recipe + "' not found.");
                }
            }

            if (!activeRecipes.isEmpty()) {
                goals.add("-Drewrite.activeRecipes=" + String.join(",", activeRecipes));
            } else {
                return null;
            }
            if (!recipeArtifactCoordinates.isEmpty()) {
                goals.add("-Drewrite.recipeArtifactCoordinates=" + String.join(",", recipeArtifactCoordinates));
            }
        }

        return goals;
    }
}
