package io.jenkins.tools.pluginmodernizer.core.recipe;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

/**
 * Recipe to add an XML declaration to Jelly files.
 */
public class AddJellyXmlDeclaration extends Recipe {

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
                    String content = text.getText();
                    if (!content.startsWith("<?jelly escape-by-default='true'?>")) {
                        content = "<?jelly escape-by-default='true'?>\n" + content;
                        return text.withText(content);
                    }
                }
                return text;
            }
        };
    }
}
