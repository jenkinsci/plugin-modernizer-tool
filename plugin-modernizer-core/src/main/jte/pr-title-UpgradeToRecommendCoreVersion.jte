@import io.jenkins.tools.pluginmodernizer.core.model.Plugin
@import org.openrewrite.Recipe
@param Plugin plugin
@param Recipe recipe
Require ${plugin.getMetadata().getJenkinsVersion()}