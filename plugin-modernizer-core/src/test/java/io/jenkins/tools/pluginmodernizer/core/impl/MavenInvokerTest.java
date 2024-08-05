package io.jenkins.tools.pluginmodernizer.core.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MavenInvokerTest {

    private Config config;
    private MavenInvoker mavenInvoker;
    private Plugin plugin;

    private static final Logger LOG = LoggerFactory.getLogger(MavenInvoker.class);

    @BeforeEach
    void setUp() {
        config = mock(Config.class);
        Path mavenHome = getMavenHome();
        when(config.getMavenHome()).thenReturn(mavenHome);
        when(config.getRecipes()).thenReturn(List.of("FetchMetadata"));

        mavenInvoker = spy(new MavenInvoker(config));

        plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn("test-plugin");
        when(plugin.getLocalRepository()).thenReturn(Path.of("src/test/resources/test-plugin"));
    }

    private Path getMavenHome() {
        String mavenHomePath = System.getenv("MAVEN_HOME");
        if (mavenHomePath == null) {
            mavenHomePath = System.getProperty("maven.home");
        }
        if (mavenHomePath == null) {
            throw new IllegalStateException(
                    "Maven home directory is not set. Please set the MAVEN_HOME environment variable or maven.home system property.");
        }
        return Path.of(mavenHomePath);
    }

    @Test
    void testGetMavenVersion() {
        Invoker invoker = mock(Invoker.class);
        InvocationResult result = mock(InvocationResult.class);

        try {
            when(invoker.execute(any(InvocationRequest.class))).thenReturn(result);
            when(result.getExitCode()).thenReturn(0);

            ComparableVersion version = mavenInvoker.getMavenVersion();
            assertNotNull(version, "Maven version should not be null");
        } catch (MavenInvocationException e) {
            fail("Exception should not be thrown");
        }
    }

    @Test
    void testInvokeGoal() throws Exception {
        Path pluginDir = plugin.getLocalRepository();

        Path targetDir = pluginDir.resolve("target");
        Files.createDirectory(targetDir);

        Path testFile = targetDir.resolve("test-file.txt");
        Files.createFile(testFile);

        try {
            assertTrue(Files.exists(testFile), "Test file should exist before clean");

            mavenInvoker.invokeGoal(plugin, "clean");

            assertFalse(Files.exists(targetDir), "The target directory should be removed after clean");
        } finally {
            if (Files.exists(targetDir)) {
                Files.walk(targetDir).sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (Exception e) {
                        LOG.error("Error deleting file {}", path, e);
                    }
                });
            }
        }
    }

    @Test
    void testInvokeRewrite() throws Exception {
        Path pluginDir = plugin.getLocalRepository();
        Path targetDir = pluginDir.resolve("target");

        try {
            assertFalse(Files.exists(targetDir), "target should not exist before rewrite");
            mavenInvoker.invokeRewrite(plugin);
            assertTrue(Files.exists(targetDir), "target should exist after rewrite");
        } finally {
            if (Files.exists(targetDir)) {
                Files.walk(targetDir).sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (Exception e) {
                        LOG.error("Error deleting file {}", path, e);
                    }
                });
            }
        }
    }

    @Test
    void testValidateMavenHome() {
        // Valid maven home
        when(config.getMavenHome()).thenReturn(getMavenHome());
        assertDoesNotThrow(() -> new MavenInvoker(config));

        // Invalid maven home
        when(config.getMavenHome()).thenReturn(Path.of("/invalid/path/to/maven"));
        assertThrows(IllegalArgumentException.class, () -> new MavenInvoker(config));
    }

    @Test
    void testValidateMavenVersion() {
        // Valid maven version
        when(config.getMavenHome()).thenReturn(getMavenHome());
        assertDoesNotThrow(() -> new MavenInvoker(config));

        // Invalid maven version
        when(config.getMavenHome()).thenReturn(Path.of("/path/to/old/maven"));
        assertThrows(IllegalArgumentException.class, () -> new MavenInvoker(config));
    }

    @Test
    void testValidateSelectedRecipes() {
        // Valid recipes
        when(config.getRecipes()).thenReturn(List.of("FetchMetadata"));
        assertDoesNotThrow(() -> new MavenInvoker(config));

        // No valid recipes
        when(config.getRecipes()).thenReturn(List.of());
        assertThrows(IllegalArgumentException.class, () -> new MavenInvoker(config));
    }
}
