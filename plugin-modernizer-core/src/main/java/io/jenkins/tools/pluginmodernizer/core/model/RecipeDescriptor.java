package io.jenkins.tools.pluginmodernizer.core.model;

public class RecipeDescriptor {
    private String name;
    private String description;
    private String fqcn;
    private String artifactCoordinates;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFqcn() {
        return fqcn;
    }

    public void setFqcn(String fqcn) {
        this.fqcn = fqcn;
    }

    public String getArtifactCoordinates() {
        return artifactCoordinates;
    }

    public void setArtifactCoordinates(String artifactCoordinates) {
        this.artifactCoordinates = artifactCoordinates;
    }
}