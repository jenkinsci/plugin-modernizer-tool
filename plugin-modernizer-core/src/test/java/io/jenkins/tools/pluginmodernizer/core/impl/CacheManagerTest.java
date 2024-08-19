package io.jenkins.tools.pluginmodernizer.core.impl;

import static java.time.Clock.systemUTC;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jenkins.tools.pluginmodernizer.core.model.CacheEntry;
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

    private static class TestCacheEntry extends CacheEntry<TestCacheEntry> {
        public TestCacheEntry(CacheManager cacheManager, Class<TestCacheEntry> clazz, String key, Path path) {
            super(cacheManager, clazz, key, path);
        }
    }

    @BeforeEach
    void setUp() {
        cachePath = tempDir.resolve("cache");
        cacheManager = new CacheManager(cachePath);
        cacheManager.init();
    }

    @Test
    void testPut() throws IOException {
        String cacheKey = "testKey";
        TestCacheEntry value = new TestCacheEntry(cacheManager, TestCacheEntry.class, cacheKey, cachePath);
        cacheManager.put(value);
        Path fileToCache = cachePath.resolve(cacheKey);
        assertTrue(Files.exists(fileToCache));
    }

    @Test
    public void cacheReturnsNullWhenJsonWasPutIntoCacheMoreThanAnHourAgo() {
        String cacheKey = "testKey";
        CacheManager managerWithExpiredEntries = cacheManagerWithExpiredEntries();
        TestCacheEntry value = new TestCacheEntry(cacheManager, TestCacheEntry.class, cacheKey, cachePath);
        managerWithExpiredEntries.put(value);
        TestCacheEntry jsonStr =
                managerWithExpiredEntries.get(cacheManager.root(), "the-cache-key", TestCacheEntry.class);
        assertNull(jsonStr);
    }

    @Test
    public void cacheReturnsJsonStringWhenJsonWasPutIntoCacheLessThanAnHourAgo() {
        String cacheKey = "testKey";
        CacheManager managerWithoutExpiredEntries = cacheManagerWithoutExpiredEntries();
        TestCacheEntry value = new TestCacheEntry(cacheManager, TestCacheEntry.class, cacheKey, cachePath);
        managerWithoutExpiredEntries.put(value);

        TestCacheEntry entry = managerWithoutExpiredEntries.get(cacheManager.root(), cacheKey, TestCacheEntry.class);
        assertNotNull(entry);
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
        TestCacheEntry value = new TestCacheEntry(cacheManager, TestCacheEntry.class, cacheKey, cachePath);
        cacheManager.put(value);

        // Simulate not expired
        assertNotNull(cacheManager.get(cacheManager.root(), cacheKey, TestCacheEntry.class));
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
        TestCacheEntry value = new TestCacheEntry(cacheManager, TestCacheEntry.class, cacheKey, cachePath);
        cacheManager.put(value);

        assertNotNull(cacheManager.get(cacheManager.root(), cacheKey, TestCacheEntry.class));
    }

    @Test
    void testRemove() {
        Path cachePath = tempDir.resolve("cache");
        CacheManager cacheManager = new CacheManager(cachePath);
        cacheManager.init();

        String cacheKey = "testKey";
        TestCacheEntry value = new TestCacheEntry(cacheManager, TestCacheEntry.class, cacheKey, cachePath);
        cacheManager.put(value);

        Path fileToCache = cachePath.resolve(cacheKey);
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

        Path fileToRemove = cachePath.resolve(cacheKey);
        assertFalse(Files.exists(fileToRemove));

        cacheManager.remove(cacheManager.root(), cacheKey);
        assertFalse(Files.exists(fileToRemove));
    }
}
