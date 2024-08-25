@import io.jenkins.tools.pluginmodernizer.core.model.Plugin
@import org.openrewrite.Recipe
@import java.util.List
@param Plugin plugin
@param List<Recipe> recipes
Hello `${plugin.getName()}` developers!

This is an automated pull request created by the [Jenkins Plugin Modernizer](https://github.com/jenkinsci/plugin-modernizer-tool) tool. The tool has applied the following recipes to modernize the plugin:
@for(var recipe : recipes)
<details>
    <summary>${recipe.getDisplayName()}</summary>
    <p><em>${recipe.getName()}</em></p>
    <blockquote>${recipe.getDescription()}</blockquote>
</details>
@endfor
