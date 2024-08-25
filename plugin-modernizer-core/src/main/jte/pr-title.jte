@import io.jenkins.tools.pluginmodernizer.core.config.Settings
@import io.jenkins.tools.pluginmodernizer.core.model.Plugin
@import org.openrewrite.Recipe
@import java.util.List
@import static io.jenkins.tools.pluginmodernizer.core.config.Settings.RECIPE_FQDN_PREFIX
@param Plugin plugin
@param List<Recipe> recipes
Applied recipes ${recipes.stream().map(r -> r.getName().replaceAll(RECIPE_FQDN_PREFIX + ".", "")).collect(java.util.stream.Collectors.joining(", "))}
