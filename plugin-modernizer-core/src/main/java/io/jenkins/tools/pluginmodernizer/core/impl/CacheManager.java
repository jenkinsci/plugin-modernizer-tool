package io.jenkins.tools.pluginmodernizer.core.impl;

import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

    // TODO: Need to be removed. This is only for testing purposes
    // Same can be achieved by using ReflectionUtils.setField
    @Deprecated
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
     * Put an object to the cache
     * @param path The relative path to the object
     * @param cacheKey The key to store the object
     * @param value The object to store
     */
    public void put(Path path, String cacheKey, String value) {
        Path fileToCache = location.resolve(path).resolve(cacheKey + ".json");
        try {
            Files.writeString(fileToCache, value, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ModernizerException("Unable to add cache", e);
        }
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
    public String get(Path path, String cacheKey) {
        String filename = cacheKey + ".json";
        Path cachedPath = location.resolve(path).resolve(filename);
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

            return FileUtils.readFileToString(cachedPath.toFile(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ModernizerException("Unable to retrieve cache for key " + cacheKey, e);
        }
    }

    /**
     * Removes a cache entry
     * @param cacheKey The key to remove
     */
    public void remove(Path path, String cacheKey) {
        Path fileToRemove = location.resolve(path).resolve(cacheKey + ".json");
        try {
            if (Files.exists(fileToRemove)) {
                Files.delete(fileToRemove);
                LOG.debug("Cache entry removed for key: {}", cacheKey);
            }
        } catch (IOException e) {
            throw new ModernizerException("Failed to remove cache entry for key: " + cacheKey, e);
        }
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
