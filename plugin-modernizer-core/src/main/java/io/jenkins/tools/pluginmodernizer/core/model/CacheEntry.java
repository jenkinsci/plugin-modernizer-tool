package io.jenkins.tools.pluginmodernizer.core.model;

import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import io.jenkins.tools.pluginmodernizer.core.utils.JsonUtils;
import java.io.Serializable;
import java.nio.file.Path;

public abstract class CacheEntry<T extends CacheEntry<T>> implements Serializable {

    /**
     * Key of the cache
     */
    public final String key;

    /**
     * Relative path of the object
     */
    public final transient Path path;

    /**
     * Cache manager
     */
    private final transient CacheManager cacheManager;

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
        this.path = path;
        this.cacheManager = cacheManager;
        this.clazz = clazz;
    }

    /**
     * Delete the object from cache
     */
    public final void delete() {
        cacheManager.remove(path, key);
    }

    /**
     * Return the key of the cache
     * @return The key
     */
    public final String getKey() {
        return key;
    }

    /**
     * Return a copy of this object refreshed from the cache
     * @return The refreshed object
     */
    public final T refresh() {
        return JsonUtils.fromJson(cacheManager.get(path, key), clazz);
    }

    /**
     * Return a copy of this object moved to another cache manager
     * @param newCacheManager The new cache manager
     * @return The refreshed object copied
     */
    public final T move(CacheManager newCacheManager) {
        T refreshed = refresh();
        newCacheManager.put(path, key, refresh().toJson());
        this.delete();
        return refreshed;
    }

    /**
     * Save this object to the cache
     */
    public final void save() {
        cacheManager.put(path, key, toJson());
    }

    /**
     * Return the relative path of the object
     * @return The relative path
     */
    public final Path getRelativePath() {
        return path;
    }

    /**
     * Return the absolute path of the object
     * @return The absolute path
     */
    public final Path getAbsolutePath() {
        return cacheManager.getLocation().resolve(path);
    }

    /**
     * Return this object to JSON
     * @return The JSON string
     */
    public final String toJson() {
        return JsonUtils.toJson(this);
    }
}
