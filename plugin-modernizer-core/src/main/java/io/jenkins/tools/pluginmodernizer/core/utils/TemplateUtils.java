package io.jenkins.tools.pluginmodernizer.core.utils;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import java.util.List;
import java.util.Map;
import org.openrewrite.Recipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to render JTE templates
 */
public class TemplateUtils {

    private static final Logger LOG = LoggerFactory.getLogger(TemplateUtils.class);

    /**
     * Hidden constructor
     */
    private TemplateUtils() {}

    /**
     * Render the pull request body
     * @param plugin Plugin to modernize
     * @param recipes List of recipes to apply
     * @return The rendered pull request body
     */
    public static String renderPullRequestBody(Plugin plugin, List<Recipe> recipes) {
        return renderTemplate("pr-body.jte", Map.of("plugin", plugin, "recipes", recipes));
    }

    /**
     * Render the commit message
     * @param plugin Plugin to modernize
     * @param recipes List of recipes to apply
     * @return The rendered commit message
     */
    public static String renderCommitMessage(Plugin plugin, List<Recipe> recipes) {
        return renderTemplate("commit.jte", Map.of("plugin", plugin, "recipes", recipes));
    }

    /**
     * Render the pull request title
     * @param plugin Plugin to modernize
     * @param recipes List of recipes to apply
     * @return The rendered pull request title
     */
    public static String renderPullRequestTitle(Plugin plugin, List<Recipe> recipes) {
        return renderTemplate("pr-title.jte", Map.of("plugin", plugin, "recipes", recipes));
    }

    /**
     * Render a generic template
     * @param templateName Name of the template
     * @param params Parameters to pass to the template
     * @return The rendered template
     */
    private static String renderTemplate(String templateName, Map<String, Object> params) {
        try {
            TemplateEngine templateEngine = TemplateEngine.createPrecompiled(ContentType.Html);
            TemplateOutput output = new StringOutput();
            templateEngine.render(templateName, params, output);
            return output.toString().trim();
        } catch (Exception e) {
            LOG.error("Error rendering template {}", templateName, e);
            throw new ModernizerException("Error rendering template " + templateName, e);
        }
    }
}
