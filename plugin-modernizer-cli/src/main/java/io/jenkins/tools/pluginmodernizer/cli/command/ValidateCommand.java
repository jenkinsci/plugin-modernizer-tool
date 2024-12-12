package io.jenkins.tools.pluginmodernizer.cli.command;

import com.google.inject.Guice;
import io.jenkins.tools.pluginmodernizer.cli.options.GlobalOptions;
import io.jenkins.tools.pluginmodernizer.core.GuiceModule;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.impl.PluginModernizer;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * Validate command
 */
@CommandLine.Command(name = "validate", description = "Validate")
public class ValidateCommand implements ICommand {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ValidateCommand.class);

    /**
     * Global options for all commands
     */
    @CommandLine.Mixin
    private GlobalOptions options;

    @Override
    public Integer call() throws Exception {
        Config config = options.getBuilderForOptions().build();
        PluginModernizer modernizer =
                Guice.createInjector(new GuiceModule(config)).getInstance(PluginModernizer.class);
        try {
            modernizer.validate();
            LOG.info("GitHub owner: {}", modernizer.getGithubOwner());
            LOG.info("Maven home: {}", config.getMavenHome());
            LOG.info("Cache path: {}", config.getCachePath());
        } catch (ModernizerException e) {
            LOG.error("Validation error");
            LOG.error(e.getMessage());
            return 1;
        }
        LOG.info("Validation successful");
        return 0;
    }
}
