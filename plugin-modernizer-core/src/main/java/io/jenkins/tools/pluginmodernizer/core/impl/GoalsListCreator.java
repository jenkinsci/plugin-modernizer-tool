package io.jenkins.tools.pluginmodernizer.core.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jenkins.tools.pluginmodernizer.core.config.Config;

public class GoalsListCreator {

    private final Config config;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public GoalsListCreator(Config config) {
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
}
