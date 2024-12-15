package io.jenkins.tools.pluginmodernizer.cli.command;

import io.jenkins.tools.pluginmodernizer.cli.options.GlobalOptions;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.impl.PluginModernizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * Cleanup command
 */
@CommandLine.Command(name = "cleanup", description = "Cleanup local cache data")
public class CleanupCommand implements ICommand {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(CleanupCommand.class);

    /**
     * Global options for all commands
     */
    @CommandLine.Mixin
    private GlobalOptions options;

    @CommandLine.Option(
            names = {"--dry-run"},
            description = "Dry run. Do not remove anything.")
    private boolean dryRun;

    @Override
    public Config setup(Config.Builder builder) {
        options.config(builder);
        builder.withDryRun(dryRun);
        return builder.build();
    }

    @Override
    public Integer call() throws Exception {
        PluginModernizer modernizer = getModernizer();
        if (modernizer.isDryRun()) {
            LOG.info("Would remove path: {}", modernizer.getCachePath());
        } else {
            modernizer.cleanCache();
            LOG.info("Removed path: {}", modernizer.getCachePath());
        }
        return 0;
    }
}
