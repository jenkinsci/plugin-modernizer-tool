package io.jenkins.tools.pluginmodernizer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

public class MainTest {

    private CommandLine commandLine;
    private ByteArrayOutputStream outputStream;
    private Main main;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        main = new Main();
        commandLine = new CommandLine(main);
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
    }

    @Test
    public void testGetPlugins() {
        String[] args = {"-p", "plugin1,plugin2", "-r", "recipe1,recipe2"};
        commandLine.execute(args);

        List<String> plugins = main.setup().getPlugins();
        assertNotNull(plugins);
        assertEquals(2, plugins.size());
        assertEquals("plugin1", plugins.get(0));
        assertEquals("plugin2", plugins.get(1));
    }

    @Test
    public void testGetRecipes() {
        String[] args = {"-p", "plugin1,plugin2", "-r", "recipe1,recipe2"};
        commandLine.execute(args);

        List<String> recipes = main.setup().getRecipes();
        assertNotNull(recipes);
        assertEquals(2, recipes.size());
        assertEquals("recipe1", recipes.get(0));
        assertEquals("recipe2", recipes.get(1));
    }

    @Test
    public void testMissingRecipesArgument() {
        String[] args = {"-p", "plugin1,plugin2"};
        int exitCode = commandLine.execute(args);
        assertEquals(CommandLine.ExitCode.USAGE, exitCode);
    }

    @Test
    public void testPluginFile() throws IOException {
        Path pluginFile = tempDir.resolve("plugins.txt");
        Files.write(pluginFile, List.of("plugin1", "", "plugin2", "   ", "plugin3"));
        String[] args = {"-f", pluginFile.toString(), "-r", "recipe1,recipe2"};
        commandLine.execute(args);
        List<String> plugins = main.setup().getPlugins();
        assertNotNull(plugins);
        assertEquals(3, plugins.size());
        assertTrue(plugins.contains("plugin1"));
        assertTrue(plugins.contains("plugin2"));
        assertTrue(plugins.contains("plugin3"));
    }

    @Test
    public void testPluginFileAlongWithPluginOptionWithoutCommonPlugins() throws IOException {
        Path pluginFile = tempDir.resolve("plugins.txt");
        Files.write(pluginFile, List.of("plugin1", "", "plugin2", "   ", "plugin3"));
        String[] args = {"-f", pluginFile.toString(), "-r", "recipe1,recipe2", "-p", "plugin4,plugin5"};
        commandLine.execute(args);
        List<String> plugins = main.setup().getPlugins();
        assertNotNull(plugins);
        assertEquals(5, plugins.size());
        assertTrue(plugins.contains("plugin1"));
        assertTrue(plugins.contains("plugin2"));
        assertTrue(plugins.contains("plugin3"));
        assertTrue(plugins.contains("plugin4"));
        assertTrue(plugins.contains("plugin5"));
    }

    @Test
    public void testPluginFileAlongWithPluginOptionWithCommonPlugins() throws IOException {
        Path pluginFile = tempDir.resolve("plugins.txt");
        Files.write(pluginFile, List.of("plugin1", "", "plugin2", "   ", "plugin3"));
        String[] args = {"-f", pluginFile.toString(), "-r", "recipe1,recipe2", "-p", "plugin2,plugin3"};
        commandLine.execute(args);
        List<String> plugins = main.setup().getPlugins();
        assertNotNull(plugins);
        assertEquals(3, plugins.size());
        assertTrue(plugins.contains("plugin1"));
        assertTrue(plugins.contains("plugin2"));
        assertTrue(plugins.contains("plugin3"));
    }

    @Test
    public void testMavenHome() throws IOException {
        String[] args = {"--maven-home", Files.createTempDirectory("unused").toString()};
        int exitCode = commandLine.execute(args);
        assertEquals(CommandLine.ExitCode.USAGE, exitCode);
    }

    @Test
    public void testDryRunOption() {
        String[] args = {"-p", "plugin1,plugin2", "-r", "recipe1,recipe2", "-n"};
        commandLine.execute(args);
        assertTrue(main.setup().isDryRun());
    }

    @Test
    public void testDebugOption() {
        String[] args = {"-p", "plugin1,plugin2", "-r", "recipe1,recipe2", "-d"};
        commandLine.execute(args);
        main.setup();
        assertTrue(Config.DEBUG);
    }

    @Test
    public void testCachePathOption() {
        String[] args = {"-p", "plugin1,plugin2", "-r", "recipe1,recipe2", "-c", "/tmp/cache"};
        commandLine.execute(args);
        assertEquals(Paths.get("/tmp/cache"), main.setup().getCachePath());
    }

    @Test
    public void testJenkinsUpdateCenterOptionWithoutValidOption() throws MalformedURLException {
        URL defaultUrl = new URL("https://updates.jenkins.io/current/update-center.actual.json");
        String[] args = {"--jenkins-update-center", "test-url"};
        commandLine.execute(args);
        assertEquals(defaultUrl, main.setup().getJenkinsUpdateCenter());
    }

    @Test
    public void testJenkinsUpdateCenterOptionWithValidOption() throws MalformedURLException {
        URL cliUrl = new URL("https://www.jenkins.io/");
        String[] args = {"--jenkins-update-center", "https://www.jenkins.io/"};
        commandLine.execute(args);
        assertEquals(cliUrl, main.setup().getJenkinsUpdateCenter());
    }

    @Test
    public void testListRecipesOption() {
        String[] args = {"-l"};
        commandLine.execute(args);
        assertTrue(main.listRecipes);
        main.run();
        assertTrue(outputStream.toString().contains("Available recipes:"));
    }

    @Test
    public void testCaseInsensitiveOption() {
        int exitCode = commandLine.setOptionsCaseInsensitive(true).execute("-H");
        assertEquals(CommandLine.ExitCode.OK, exitCode);
    }

    @Test
    public void testExecuteWithInvalidArgs() {
        String[] args = {"--invalidOption", "value"};
        int exitCode = commandLine.execute(args);
        assertNotEquals(CommandLine.ExitCode.OK, exitCode);
    }

    @Test
    public void testExecuteWithNoArgs() {
        String[] args = {};
        int exitCode = commandLine.execute(args);
        assertNotEquals(CommandLine.ExitCode.OK, exitCode);
    }

}