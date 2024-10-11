package io.jenkins.tools.pluginmodernizer.core.model;

import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import io.jenkins.tools.pluginmodernizer.core.utils.JsonUtils;
import java.io.Serializable;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CacheEntry<T extends CacheEntry<T>> implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(CacheEntry.class);

    /**
     * Key of the cache
     */
    private String key;

    /**
     * Relative path of the object (must be serializable)
     */
    private String path;

    /**
     * Cache manager
     */
    private transient CacheManager cacheManager;

    /**
     * Concrete class of the object
     */
    private final transient Class<T> clazz;

    /**
     * Create a new cache entry
     * @param cacheManager The cache manager
     * @param key The key of the cache
     * @param path Relative path of the object
     */
    public CacheEntry(CacheManager cacheManager, Class<T> clazz, String key, Path path) {
        this.key = key;
        this.path = path.toString();
        this.cacheManager = cacheManager;
        this.clazz = clazz;
    }

    /**
     * Delete the object from cache
     */
    public final void delete() {
        cacheManager.remove(Path.of(path), key);
    }

    /**
     * Return the key of the cache
     * @return The key
     */
    public final String getKey() {
        return key;
    }

    /**
     * Return the path of the object
     * @return The path
     */
    public final Path getPath() {
        if (path == null) {
            return null;
        }
        return Path.of(path);
    }

    /**
     * Return the cache manager
     * @return The cache manager
     */
    public final CacheManager getCacheManager() {
        return cacheManager;
    }

    /**
     * Return a copy of this object refreshed from the cache
     * @return The refreshed object
     */
    public final T refresh() {
        LOG.trace(
                "Refreshing object from {}",
                cacheManager.getLocation().resolve(path).resolve(key));
        return cacheManager.get(Path.of(path), key, clazz);
    }

    /**
     * Return a copy of this object moved to another cache manager
     * @param newCacheManager The new cache manager
     * @return The refreshed object copied
     */
    public final T move(CacheManager newCacheManager, Path newPath, String newKey) {
        return move(newCacheManager, newPath, newKey, false);
    }

    /**
     * Return a copy of this object copied to another cache manager
     * @param newCacheManager The new cache manager
     * @return The refreshed object copied
     */
    public final T copy(CacheManager newCacheManager, Path newPath, String newKey) {
        return move(newCacheManager, newPath, newKey, true);
    }

    private T move(CacheManager newCacheManager, Path newPath, String newKey, boolean copy) {
        LOG.debug(
                "Moving object from {} to {}",
                cacheManager.getLocation().resolve(path).resolve(key),
                newCacheManager.getLocation().resolve(newPath).resolve(newKey));
        // Copy transient fields
        T refreshedObject = refresh();
        refreshedObject.setPath(newPath);
        refreshedObject.setKey(newKey);
        refreshedObject.setCacheManager(newCacheManager);
        newCacheManager.put(refreshedObject);
        LOG.debug(refreshedObject.getCacheManager().getLocation().toString());
        if (!copy) {
            this.delete();
        }
        return newCacheManager.get(newPath, newKey, clazz);
    }

    /**
     * Save this object to the cache
     */
    public final void save() {
        LOG.debug(
                "Saving object to {}",
                cacheManager.getLocation().resolve(path).resolve(key).toAbsolutePath());
        cacheManager.put(this);
    }

    /**
     * Return the relative path of the object
     * @return The relative path
     */
    public final Path getRelativePath() {
        return Path.of(path);
    }

    /**
     * Set the cache manager for the entry
     * @param cacheManager The cache manager
     */
    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Set the key of the cache
     * @param key The key
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Set the path of the object
     * @param path The path
     */
    public void setPath(Path path) {
        this.path = path.toString();
    }

    /**
     * Return the absolute path of the object
     * @return The absolute path
     */
    public final Path getLocation() {
        return cacheManager.getLocation().resolve(path).resolve(key);
    }

    /**
     * Return this object to JSON
     * @return The JSON string
     */
    public final String toJson() {
        return JsonUtils.toJson(this);
    }
}
