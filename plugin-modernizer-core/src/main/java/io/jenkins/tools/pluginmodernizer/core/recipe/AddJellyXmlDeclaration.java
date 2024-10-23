package io.jenkins.tools.pluginmodernizer.core.recipe;

import io.jenkins.tools.pluginmodernizer.core.utils.PomModifier;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recipe to add an XML declaration to Jelly files.
 */
public class AddJellyXmlDeclaration extends Recipe {

    private static final Logger LOG = LoggerFactory.getLogger(AddJellyXmlDeclaration.class);

    /**
     * Returns the display name of the recipe.
     *
     * @return the display name of the recipe
     */
    @Override
    public String getDisplayName() {
        return "Add XML declaration to Jelly files";
    }

    /**
     * Returns the description of the recipe.
     *
     * @return the description of the recipe
     */
    @Override
    public String getDescription() {
        return "Ensure the XML declaration `<?jelly escape-by-default='true'?>` is present in all `.jelly` files.";
    }

    /**
     * Returns the visitor that will be used to apply the recipe.
     *
     * @return the visitor that will be used to apply the recipe
     */
    @Override
    public PlainTextVisitor<ExecutionContext> getVisitor() {
        return new PlainTextVisitor<ExecutionContext>() {
            public static final String JELLY_DECLARATION = "<?jelly escape-by-default='true'?>";

            /**
             * Visits the text and adds the XML declaration if it is a Jelly file and the declaration is not already present.
             *
             * @param text the text to visit
             * @param executionContext the execution context
             * @return the modified text if the XML declaration was added, otherwise the original text
             */
            @Override
            public PlainText visitText(PlainText text, ExecutionContext executionContext) {
                if (text.getSourcePath().toString().endsWith(".jelly")) {
                    LOG.debug("Processing Jelly file: {}", text.getSourcePath());
                    String content = text.getText();
                    // Handle empty files
                    if (content.trim().isEmpty()) {
                        LOG.debug("Adding declaration to empty file");
                        return text.withText(JELLY_DECLARATION);
                    }
                    // Detect line ending style
                    String lineEnding = content.contains("\r\n") ? "\r\n" : "\n";

                    // Check for and handle malformed declarations
                    if (content.trim().toLowerCase().startsWith("<?jelly") && !content.startsWith(JELLY_DECLARATION)) {
                        LOG.debug("Adding missing declaration");
                        // Remove existing malformed declaration up to first line ending
                        content = content.substring(content.indexOf(lineEnding) + lineEnding.length());
                    } // Add declaration if missing
                    if (!content.startsWith(JELLY_DECLARATION)) {
                        LOG.debug("Declaration already present");
                        content = JELLY_DECLARATION + lineEnding + content;
                        return text.withText(content);
                    }
                }
                return text;
            }
        };
    }
}
