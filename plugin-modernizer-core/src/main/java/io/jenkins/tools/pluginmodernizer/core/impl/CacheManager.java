package io.jenkins.tools.pluginmodernizer.core.impl;

import io.jenkins.tools.pluginmodernizer.core.utils.JenkinsPluginInfo;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheManager {
    private static final Logger LOG = LoggerFactory.getLogger(JenkinsPluginInfo.class);
    private final Path cache;

    public CacheManager(Path cache) {
        this.cache = cache;
    }

    void createCache() {
        if (!Files.exists(cache)) {
            try {
                Path parent = cache.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectory(parent);
                }
                Files.createDirectory(cache);
                LOG.info("Creating cache at {}", cache);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
