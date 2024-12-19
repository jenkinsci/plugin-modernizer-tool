package io.jenkins.tools.pluginmodernizer.cli.utils;

import java.util.Optional;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ModernizerTestWatcher implements TestWatcher {

    private static final Logger LOG = LoggerFactory.getLogger(ModernizerTestWatcher.class);

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        LOG.info("Test disabled: {}", context.getDisplayName());
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        LOG.info("Test successful: {}", context.getDisplayName());
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        LOG.info("Test aborted: {}", context.getDisplayName());
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        LOG.error("Test failed: {}", context.getDisplayName(), cause);
    }
}
