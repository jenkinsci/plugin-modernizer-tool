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
public class MavenGoalInvoker {

    private static final Logger LOG = LoggerFactory.getLogger(MavenGoalInvoker.class);


    private final Config config;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public MavenGoalInvoker(Config config) {
        this.config = config;

    }

    public List<String> createGoalsList() throws IOException {
        List<String> goals = new ArrayList<>();
        String mavenPluginVersion = config.getMavenPluginVersion();
        goals.add("org.openrewrite.maven:rewrite-maven-plugin:" + mavenPluginVersion + ":run");

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

    public void invokeMavenGoals(File pluginDir, List<String> goals) {
        Invoker invoker = new DefaultInvoker();
        String mavenHome = config.getMavenHome();
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
}
