package io.jenkins.tools.pluginmodernizer.core.impl;

import io.jenkins.tools.pluginmodernizer.core.model.CacheEntry;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import io.jenkins.tools.pluginmodernizer.core.utils.JsonUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheManager {

    // Cache keys
    public static final String UPDATE_CENTER_CACHE_KEY = "update-center";
    public static final String PLUGIN_VERSIONS_CACHE_KEY = "plugin-versions";
    public static final String HEALTH_SCORE_KEY = "health-score";
    public static final String INSTALLATION_STATS_KEY = "plugin-installation-stats";
    public static final String PLUGIN_METADATA_CACHE_KEY = "plugin-metadata";

    private static final Logger LOG = LoggerFactory.getLogger(CacheManager.class);

    private final Path location;
    private final Clock clock;
    private final boolean expires;

    /**
     * Creates a new cache manager
     * @param cache The location of the cache
     */
    public CacheManager(Path cache) {
        this(cache, Clock.systemDefaultZone(), true);
    }

    /**
     * Creates a new cache manager with a custom clock and expiration
     * @param cache The location of the cache
     * @param clock The clock to use
     * @param expires Whether the cache expires
     */
    CacheManager(Path cache, Clock clock, boolean expires) {
        this.location = cache;
        this.clock = clock;
        this.expires = expires;
    }

    /**
     * Initializes the cache directory
     */
    public void init() {
        if (!Files.exists(location)) {
            try {
                Path parent = location.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectory(parent);
                }
                Files.createDirectory(location);
                LOG.debug("Creating cache at {}", location);
            } catch (IOException e) {
                throw new ModernizerException("Unable to create cache", e);
            }
        }
    }

    /**
     * Wipe the cache including all cloned plugins
     */
    public void wipe() {
        LOG.debug("Removing local data at {}", location.toAbsolutePath());
        try {
            FileUtils.deleteDirectory(location.toFile());
        } catch (Exception e) {
            throw new ModernizerException("Error removing local data", e);
        }
    }

    /**
     * Put an object to the cache
     * @param entry The object to store
     */
    public void put(CacheEntry<? extends CacheEntry<?>> entry) {
        if (entry.getKey() == null) {
            throw new ModernizerException("Cache entry key is null");
        }
        if (entry.getPath() == null) {
            throw new ModernizerException("Cache entry path is null");
        }
        Path fileToCache = location.resolve(entry.getPath()).resolve(entry.getKey());
        JsonUtils.toJsonFile(entry, fileToCache);
    }

    /**
     * Retrieves a json object from the cache.
     * <p>
     * Will return null if the key can't be found or if it hasn't been
     * modified for 1 hour
     *
     * @param path     subdirectory of the object
     * @param cacheKey key to lookup, i.e. update-center
     * @return the cached json object as a string or null
     */
    public <T extends CacheEntry<T>> T get(Path path, String cacheKey, Class<T> clazz) {
        Path cachedPath = location.resolve(path).resolve(cacheKey);
        try {
            FileTime lastModifiedTime = Files.getLastModifiedTime(cachedPath);
            Duration between = Duration.between(lastModifiedTime.toInstant(), clock.instant());
            long betweenHours = between.toHours();

            if (betweenHours > 0L) {
                LOG.debug(
                        "Cache entry expired: {}{}",
                        cacheKey,
                        expires ? ". Will skip it" : ". Will accept it, because expiration is disabled");
                if (expires) {
                    return null;
                }
            }
            LOG.debug("Cache entry found for cache {} at path {} and key {}", location, path, cacheKey);
            T entry = JsonUtils.fromJson(cachedPath, clazz);
            entry.setCacheManager(this);
            return entry;
        } catch (NoSuchFileException e) {
            LOG.debug("Cache entry not found for cache {} at path {} and key {}", location, path, cacheKey);
            return null;
        } catch (IOException e) {
            throw new ModernizerException("Failed to read cache entry for key: " + cacheKey, e);
        }
    }

    /**
     * Removes a cache entry
     * @param cacheKey The key to remove
     */
    public void remove(Path path, String cacheKey) {
        Path fileToRemove = location.resolve(path).resolve(cacheKey);
        try {
            if (Files.exists(fileToRemove)) {
                Files.delete(fileToRemove);
                LOG.debug("Cache entry removed for key: {} at location {}", cacheKey, location);
            }
        } catch (IOException e) {
            throw new ModernizerException("Failed to remove cache entry for key: " + cacheKey, e);
        }
    }

    /**
     * Move a cache entry to the new cache manager
     * @param cacheManager The cache manager
     * @param newPath The new path
     * @param newKey The new key
     * @param entry The cache entry to move
     */
    public <T extends CacheEntry<T>> T move(
            CacheManager cacheManager, Path newPath, String newKey, CacheEntry<T> entry) {
        return entry.move(cacheManager, newPath, newKey);
    }

    /**
     * Copy a cache entry to the new cache manager
     * @param cacheManager The cache manager
     * @param newPath The new path
     * @param newKey The new key
     * @param entry The cache entry to move
     */
    public <T extends CacheEntry<T>> T copy(
            CacheManager cacheManager, Path newPath, String newKey, CacheEntry<T> entry) {
        return entry.copy(cacheManager, newPath, newKey);
    }

    /**
     * Get the location of the cache
     * @return The location
     */
    public Path getLocation() {
        return location;
    }

    /**
     * Get the root of the cache
     * @return The root
     */
    public Path root() {
        return Path.of(".");
    }
}
