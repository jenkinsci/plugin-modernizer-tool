package io.jenkins.tools.pluginmodernizer.core.recipe;

import static org.openrewrite.test.SourceSpecs.text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.Recipe;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainTextParser;

/**
 * Test class for the AddJellyXmlDeclaration recipe.
 */
public class AddJellyXmlDeclarationTest implements RewriteTest {

    /**
     * Returns the recipe to be tested.
     *
     * @return the AddJellyXmlDeclaration recipe
     */
    public Recipe getRecipe() {
        return new AddJellyXmlDeclaration();
    }

    /**
     * Test to verify that the XML declaration is added to a Jelly file.
     *
     * @param tempDir the temporary directory provided by JUnit
     * @throws IOException if an I/O error occurs
     */
    @Test
    void addXmlDeclarationToJellyFile(@TempDir Path tempDir) throws IOException {
        Path inputFile = tempDir.resolve("example.jelly");
        Files.writeString(
                inputFile,
                """
            <j:jelly xmlns:j="jelly:core" xmlns:st="jelly:stapler" xmlns:d="jelly:define">
                <st:contentType value="text/html"/>
                <h1>Hello, World!</h1>
            </j:jelly>
        """);

        Path expectedFile = tempDir.resolve("expected.jelly");
        Files.writeString(
                expectedFile,
                """
            <?jelly escape-by-default='true'?>
            <j:jelly xmlns:j="jelly:core" xmlns:st="jelly:stapler" xmlns:d="jelly:define">
                <st:contentType value="text/html"/>
                <h1>Hello, World!</h1>
            </j:jelly>
        """);

        rewriteRun(
                spec -> spec.recipe(new AddJellyXmlDeclaration())
                        .parser(PlainTextParser.builder())
                        .expectedCyclesThatMakeChanges(1)
                        .cycles(1),
                text(Files.readString(inputFile), Files.readString(expectedFile)));
    }

    /**
     * Test to verify that the XML declaration is not added if it is already present in the Jelly file.
     *
     * @param tempDir the temporary directory provided by JUnit
     * @throws IOException if an I/O error occurs
     */
    @Test
    void doNotAddXmlDeclarationIfAlreadyPresent(@TempDir Path tempDir) throws IOException {
        Path inputFile = tempDir.resolve("example.jelly");
        Files.writeString(
                inputFile,
                """
            <?jelly escape-by-default='true'?>
            <j:jelly xmlns:j="jelly:core" xmlns:st="jelly:stapler" xmlns:d="jelly:define">
                <st:contentType value="text/html"/>
                <h1>Hello, World!</h1>
            </j:jelly>
        """);

        rewriteRun(
                spec -> spec.recipe(new AddJellyXmlDeclaration())
                        .parser(PlainTextParser.builder())
                        .expectedCyclesThatMakeChanges(1)
                        .cycles(0),
                text(Files.readString(inputFile)));
    }
}
