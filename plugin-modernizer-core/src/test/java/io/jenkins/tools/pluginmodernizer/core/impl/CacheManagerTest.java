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
    void setUp() throws IOException {
        cachePath = tempDir.resolve("cache");
        cacheManager = new CacheManager(cachePath);
        cacheManager.createCache();
    }

    @Test
    void testAddToCache() throws IOException {
        String cacheKey = "testKey";
        String value = "{\"key\": \"value\"}";
        cacheManager.addToCache(cacheKey, value);

        Path fileToCache = cachePath.resolve(cacheKey + ".json");
        assertTrue(Files.exists(fileToCache));
        assertEquals(value, Files.readString(fileToCache));
    }

    @Test
    public void cacheReturnsNullWhenJsonWasPutIntoCacheMoreThanAnHourAgo() {
        CacheManager managerWithExpiredEntries = cacheManagerWithExpiredEntries();

        managerWithExpiredEntries.addToCache("the-cache-key", "{Dummy-json}");

        String jsonStr = managerWithExpiredEntries.retrieveFromCache("the-cache-key");

        assertNull(jsonStr);
    }

    @Test
    public void cacheReturnsJsonStringWhenJsonWasPutIntoCacheLessThanAnHourAgo() {
        CacheManager managerWithoutExpiredEntries = cacheManagerWithoutExpiredEntries();

        managerWithoutExpiredEntries.addToCache("the-cache-key", "{Dummy-json}");

        String jsonStr = managerWithoutExpiredEntries.retrieveFromCache("the-cache-key");

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
        manager.createCache();
        return manager;
    }

    @Test
    void testRetrieveFromCacheWhenNotExpired() throws IOException {
        Path cachePath = tempDir.resolve("cache");
        CacheManager cacheManager = new CacheManager(cachePath, Clock.systemDefaultZone(), true);
        cacheManager.createCache();

        String cacheKey = "testKey";
        String value = "{\"key\": \"value\"}";
        cacheManager.addToCache(cacheKey, value);

        // Simulate not expired
        assertEquals(value, cacheManager.retrieveFromCache(cacheKey));
    }

    @Test
    void testRetrieveFromCacheWithExpirationDisabled() throws IOException {
        Path cachePath = tempDir.resolve("cache");
        CacheManager cacheManager = new CacheManager(
                cachePath,
                Clock.fixed(
                        Instant.now().minus(Duration.ofHours(2)),
                        Clock.systemDefaultZone().getZone()),
                false);
        cacheManager.createCache();

        String cacheKey = "testKey";
        String value = "{\"key\": \"value\"}";
        cacheManager.addToCache(cacheKey, value);

        assertEquals(value, cacheManager.retrieveFromCache(cacheKey));
    }

    @Test
    void testRemoveFromCache() throws IOException {
        Path cachePath = tempDir.resolve("cache");
        CacheManager cacheManager = new CacheManager(cachePath);
        cacheManager.createCache();

        String cacheKey = "testKey";
        String value = "{\"key\": \"value\"}";
        cacheManager.addToCache(cacheKey, value);

        Path fileToCache = cachePath.resolve(cacheKey + ".json");
        assertTrue(Files.exists(fileToCache));

        cacheManager.removeFromCache(cacheKey);
        assertFalse(Files.exists(fileToCache));
    }

    @Test
    void testRemoveFromCacheWhenFileDoesNotExist() throws IOException {
        Path cachePath = tempDir.resolve("cache");
        CacheManager cacheManager = new CacheManager(cachePath);
        cacheManager.createCache();

        String cacheKey = "testKey";

        Path fileToRemove = cachePath.resolve(cacheKey + ".json");
        assertFalse(Files.exists(fileToRemove));

        cacheManager.removeFromCache(cacheKey);
        assertFalse(Files.exists(fileToRemove));
    }
}
