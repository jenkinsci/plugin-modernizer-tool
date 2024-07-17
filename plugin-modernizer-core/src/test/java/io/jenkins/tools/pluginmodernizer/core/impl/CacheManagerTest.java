package io.jenkins.tools.pluginmodernizer.core.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CacheManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void testCreateCacheWhenNotExists() throws IOException {
        Path cachePath = tempDir.resolve("cache");
        assertFalse(Files.exists(cachePath));
        CacheManager cacheManager = new CacheManager(cachePath);
        cacheManager.createCache();
        assertTrue(Files.exists(cachePath));
    }

    @Test
    void testCreateCacheWhenExists() throws IOException {
        Path cachePath = tempDir.resolve("cache");
        Files.createDirectories(cachePath);
        CacheManager cacheManager = new CacheManager(cachePath);
        cacheManager.createCache();
        assertTrue(Files.exists(cachePath));
    }
}
