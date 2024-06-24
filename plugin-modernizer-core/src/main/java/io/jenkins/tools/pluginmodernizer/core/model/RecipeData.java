package io.jenkins.tools.pluginmodernizer.core.model;

import java.util.List;

public class RecipeData {
    private List<RecipeDescriptor> recipes;

    public RecipeData() {
    }

    public List<RecipeDescriptor> getRecipes() {
        return recipes;
    }

    public void setRecipes(List<RecipeDescriptor> recipes) {
        this.recipes = recipes;
    }
}
