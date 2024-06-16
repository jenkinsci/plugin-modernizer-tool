package io.jenkins.tools.pluginmodernizer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

public class MainTest {
    @Test
    public void testGetPlugins() {
        String[] args = {"-p", "plugin1,plugin2", "-r", "recipe1,recipe2"};
        Main main = new Main();
        new CommandLine(main).execute(args);

        List<String> plugins = main.setup().getPlugins();
        assertNotNull(plugins);
        assertEquals(2, plugins.size());
        assertEquals("plugin1", plugins.get(0));
        assertEquals("plugin2", plugins.get(1));
    }

    @Test
    public void testGetRecipes() {
        String[] args = {"-p", "plugin1,plugin2", "-r", "recipe1,recipe2"};
        Main main = new Main();
        new CommandLine(main).execute(args);

        List<String> recipes = main.setup().getRecipes();
        assertNotNull(recipes);
        assertEquals(2, recipes.size());
        assertEquals("recipe1", recipes.get(0));
        assertEquals("recipe2", recipes.get(1));
    }

    @Test
    public void testMissingPluginsArgument() {
        String[] args = {"-r", "recipe1,recipe2"};
        Main main = new Main();
        int exitCode = new CommandLine(main).execute(args);
        assertEquals(CommandLine.ExitCode.USAGE, exitCode);
    }

    @Test
    public void testMissingRecipesArgument() {
        String[] args = {"-p", "plugin1,plugin2"};
        Main main = new Main();
        int exitCode = new CommandLine(main).execute(args);
        assertEquals(CommandLine.ExitCode.USAGE, exitCode);
    }
}