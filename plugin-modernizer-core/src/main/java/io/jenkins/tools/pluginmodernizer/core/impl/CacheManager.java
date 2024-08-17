package io.jenkins.tools.pluginmodernizer.core.impl;

import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheManager {
    private static final Logger LOG = LoggerFactory.getLogger(CacheManager.class);
    private final Path cache;
    private final Clock clock;
    private final boolean expires;

    public CacheManager(Path cache) {
        this(cache, Clock.systemDefaultZone(), true);
    }

    CacheManager(Path cache, Clock clock, boolean expires) {
        this.cache = cache;
        this.clock = clock;
        this.expires = expires;
    }

    void createCache() {
        if (!Files.exists(cache)) {
            try {
                Path parent = cache.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectory(parent);
                }
                Files.createDirectory(cache);
                LOG.debug("Creating cache at {}", cache);
            } catch (IOException e) {
                throw new ModernizerException("Unable to create cache", e);
            }
        }
    }

    public void addToCache(String cacheKey, String value) {
        Path fileToCache = cache.resolve(cacheKey + ".json");
        try (Writer writer = Files.newBufferedWriter(fileToCache, StandardCharsets.UTF_8)) {
            writer.write(value);
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
     * @param cacheKey key to lookup, i.e. update-center
     * @return the cached json object as a string or null
     */
    public String retrieveFromCache(String cacheKey) {
        String filename = cacheKey + ".json";
        Path cachedPath = cache.resolve(filename);
        try {
            FileTime lastModifiedTime = Files.getLastModifiedTime(cachedPath);
            Duration between = Duration.between(lastModifiedTime.toInstant(), clock.instant());
            long betweenHours = between.toHours();

            if (betweenHours > 0L) {
                LOG.info(
                        "Cache entry expired: {}{}",
                        cacheKey,
                        expires ? ". Will skip it" : ". Will accept it, because expiration is disabled");
                if (expires) {
                    return null;
                }
            }

            // Read the file contents as a String
            try (InputStream is = Files.newInputStream(cachedPath);
                    Scanner scanner = new Scanner(is, StandardCharsets.UTF_8)) {
                return scanner.useDelimiter("\\A").next();
            }
        } catch (IOException e) {
            return null;
        }
    }

    public void removeFromCache(String cacheKey) {
        Path fileToRemove = cache.resolve(cacheKey + ".json");
        try {
            if (Files.exists(fileToRemove)) {
                Files.delete(fileToRemove);
                LOG.info("Cache entry removed for key: {}", cacheKey);
            }
        } catch (IOException e) {
            throw new ModernizerException("Failed to remove cache entry for key: " + cacheKey, e);
        }
    }
}
