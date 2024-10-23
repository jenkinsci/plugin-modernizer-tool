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

public class AddJellyXmlDeclarationTest implements RewriteTest {

    public Recipe getRecipe() {
        return new AddJellyXmlDeclaration();
    }

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
