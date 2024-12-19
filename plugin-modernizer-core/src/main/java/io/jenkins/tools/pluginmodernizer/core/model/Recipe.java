package io.jenkins.tools.pluginmodernizer.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Set;

public class Recipe {

    private String name;
    private String displayName;
    private String description;
    private Set<String> tags;

    @JsonIgnore
    private Object type;

    @JsonIgnore
    private Object recipeList; // Use Object to avoid mapping complex nested structures.

    @JsonIgnore
    private Object preconditions; // Use Object to avoid mapping complex nested structures.

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public Object getType() {
        return type;
    }

    public void setType(Object type) {
        this.type = type;
    }

    public Object getRecipeList() {
        return recipeList;
    }

    public void setRecipeList(Object recipeList) {
        this.recipeList = recipeList;
    }

    public Object getPreconditions() {
        return preconditions;
    }
}
