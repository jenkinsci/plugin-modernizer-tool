package io.jenkins.tools.pluginmodernizer.core.impl;

import static java.time.Clock.systemUTC;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CacheManagerTest {

    @TempDir
    Path tempDir;

    private CacheManager cacheManager;

    private Path cachePath;

    @BeforeEach
    void setUp() {
        cachePath = tempDir.resolve("cache");
        cacheManager = new CacheManager(cachePath);
        cacheManager.init();
    }

    @Test
    void testPut() throws IOException {
        String cacheKey = "testKey";
        String value = "{\"key\": \"value\"}";
        cacheManager.put(cacheManager.root(), cacheKey, value);

        Path fileToCache = cachePath.resolve(cacheKey + ".json");
        assertTrue(Files.exists(fileToCache));
        assertEquals(value, Files.readString(fileToCache));
    }

    @Test
    public void cacheReturnsNullWhenJsonWasPutIntoCacheMoreThanAnHourAgo() {
        CacheManager managerWithExpiredEntries = cacheManagerWithExpiredEntries();

        managerWithExpiredEntries.put(cacheManager.root(), "the-cache-key", "{Dummy-json}");

        String jsonStr = managerWithExpiredEntries.get(cacheManager.root(), "the-cache-key");

        assertNull(jsonStr);
    }

    @Test
    public void cacheReturnsJsonStringWhenJsonWasPutIntoCacheLessThanAnHourAgo() {
        CacheManager managerWithoutExpiredEntries = cacheManagerWithoutExpiredEntries();

        managerWithoutExpiredEntries.put(cacheManager.root(), "the-cache-key", "{Dummy-json}");

        String jsonStr = managerWithoutExpiredEntries.get(cacheManager.root(), "the-cache-key");

        assertNotNull(jsonStr);
    }

    private CacheManager cacheManagerWithoutExpiredEntries() {
        Clock fiftyNineMinutesInTheFuture =
                Clock.fixed(systemUTC().instant().plus(59, MINUTES), ZoneId.systemDefault());
        return cacheManager(fiftyNineMinutesInTheFuture);
    }

    private CacheManager cacheManagerWithExpiredEntries() {
        Clock oneHourAndOneMinuteInTheFuture =
                Clock.fixed(systemUTC().instant().plus(61, MINUTES), ZoneId.systemDefault());
        return cacheManager(oneHourAndOneMinuteInTheFuture);
    }

    private CacheManager cacheManager(Clock clock) {
        CacheManager manager = new CacheManager(cachePath, clock, true);
        manager.init();
        return manager;
    }

    @Test
    void testGetWhenNotExpired() {
        Path cachePath = tempDir.resolve("cache");
        CacheManager cacheManager = new CacheManager(cachePath, Clock.systemDefaultZone(), true);
        cacheManager.init();

        String cacheKey = "testKey";
        String value = "{\"key\": \"value\"}";
        cacheManager.put(cacheManager.root(), cacheKey, value);

        // Simulate not expired
        assertEquals(value, cacheManager.get(cacheManager.root(), cacheKey));
    }

    @Test
    void testGetWithExpirationDisabled() {
        Path cachePath = tempDir.resolve("cache");
        CacheManager cacheManager = new CacheManager(
                cachePath,
                Clock.fixed(
                        Instant.now().minus(Duration.ofHours(2)),
                        Clock.systemDefaultZone().getZone()),
                false);
        cacheManager.init();

        String cacheKey = "testKey";
        String value = "{\"key\": \"value\"}";
        cacheManager.put(cacheManager.root(), cacheKey, value);

        assertEquals(value, cacheManager.get(cacheManager.root(), cacheKey));
    }

    @Test
    void testRemove() {
        Path cachePath = tempDir.resolve("cache");
        CacheManager cacheManager = new CacheManager(cachePath);
        cacheManager.init();

        String cacheKey = "testKey";
        String value = "{\"key\": \"value\"}";
        cacheManager.put(cacheManager.root(), cacheKey, value);

        Path fileToCache = cachePath.resolve(cacheKey + ".json");
        assertTrue(Files.exists(fileToCache));

        cacheManager.remove(cacheManager.root(), cacheKey);
        assertFalse(Files.exists(fileToCache));
    }

    @Test
    void testRemoveWhenFileDoesNotExist() {
        Path cachePath = tempDir.resolve("cache");
        CacheManager cacheManager = new CacheManager(cachePath);
        cacheManager.init();

        String cacheKey = "testKey";

        Path fileToRemove = cachePath.resolve(cacheKey + ".json");
        assertFalse(Files.exists(fileToRemove));

        cacheManager.remove(cacheManager.root(), cacheKey);
        assertFalse(Files.exists(fileToRemove));
    }
}
