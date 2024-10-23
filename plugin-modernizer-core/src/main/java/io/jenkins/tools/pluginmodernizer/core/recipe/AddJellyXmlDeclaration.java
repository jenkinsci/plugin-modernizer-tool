package io.jenkins.tools.pluginmodernizer.core.recipe;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

public class AddJellyXmlDeclaration extends Recipe {

    @Override
    public String getDisplayName() {
        return "Add XML declaration to Jelly files";
    }

    @Override
    public String getDescription() {
        return "Ensure the XML declaration `<?jelly escape-by-default='true'?>` is present in all `.jelly` files.";
    }

    @Override
    public PlainTextVisitor<ExecutionContext> getVisitor() {
        return new PlainTextVisitor<ExecutionContext>() {
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
