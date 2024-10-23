package io.jenkins.tools.pluginmodernizer.core.recipe;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.RewriteTestRunner;
import org.openrewrite.test.SourceSpec;

import java.nio.file.Paths;

import static org.openrewrite.test.SourceSpecs.text;

public class AddJellyXmlDeclarationTest implements RewriteTest {

    @Override
    public Recipe getRecipe() {
        return new AddJellyXmlDeclaration();
    }

    @Test
    void addXmlDeclarationToJellyFile() {
        rewriteRun(
                spec -> spec.recipe(new AddJellyXmlDeclaration())
                        .parser(new PlainTextParser())
                        .expectedCyclesThatMakeChanges(1)
                        .cycles(1),
                text(
                        """
                        <j:jelly xmlns:j="jelly:core" xmlns:st="jelly:stapler" xmlns:d="jelly:define">
                            <st:contentType value="text/html"/>
                            <h1>Hello, World!</h1>
                        </j:jelly>
                        """,
                        """
                        <?jelly escape-by-default='true'?>
                        <j:jelly xmlns:j="jelly:core" xmlns:st="jelly:stapler" xmlns:d="jelly:define">
                            <st:contentType value="text/html"/>
                            <h1>Hello, World!</h1>
                        </j:jelly>
                        """
                )
        );
    }

    @Test
    void doNotAddXmlDeclarationIfAlreadyPresent() {
        rewriteRun(
                spec -> spec.recipe(new AddJellyXmlDeclaration())
                        .parser(new PlainTextParser())
                        .expectedCyclesThatMakeChanges(1)
                        .cycles(1),
                text(
                        """
                        <?jelly escape-by-default='true'?>
                        <j:jelly xmlns:j="jelly:core" xmlns:st="jelly:stapler" xmlns:d="jelly:define">
                            <st:contentType value="text/html"/>
                            <h1>Hello, World!</h1>
                        </j:jelly>
                        """,
                        """
                        <?jelly escape-by-default='true'?>
                        <j:jelly xmlns:j="jelly:core" xmlns:st="jelly:stapler" xmlns:d="jelly:define">
                            <st:contentType value="text/html"/>
                            <h1>Hello, World!</h1>
                        </j:jelly>
                        """
                )
        );
    }
}
