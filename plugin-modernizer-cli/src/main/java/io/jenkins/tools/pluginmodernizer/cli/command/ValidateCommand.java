package io.jenkins.tools.pluginmodernizer.cli.command;

import io.jenkins.tools.pluginmodernizer.cli.options.EnvOptions;
import io.jenkins.tools.pluginmodernizer.cli.options.GitHubOptions;
import io.jenkins.tools.pluginmodernizer.cli.options.GlobalOptions;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.impl.PluginModernizer;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import java.nio.file.Files;
import java.nio.file.Path;
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
     * Environment options
     */
    @CommandLine.Mixin
    private EnvOptions envOptions;

    /**
     * Global options for all commands
     */
    @CommandLine.Mixin
    private GlobalOptions options;

    /**
     * GitHub options
     */
    @CommandLine.Mixin
    private GitHubOptions githubOptions;

    @Override
    public Config setup(Config.Builder builder) {
        options.config(builder);
        envOptions.config(builder);
        githubOptions.config(builder);
        return builder.build();
    }

    @Override
    public Integer call() throws Exception {
        PluginModernizer modernizer = getModernizer();
        try {
            modernizer.validate();
            LOG.info("GitHub owner: {}", modernizer.getGithubOwner());
            if (Files.isRegularFile(Path.of(modernizer.getSshPrivateKeyPath()))) {
                LOG.info("SSH key path: {}", modernizer.getSshPrivateKeyPath());
            } else {
                LOG.info("SSH key not set. Will use GitHub token for Git operation");
            }
            LOG.info("Maven home: {}", modernizer.getMavenHome());
            LOG.info("Maven local repository: {}", modernizer.getMavenLocalRepo());
            LOG.info("Maven version: {}", modernizer.getMavenVersion());
            LOG.info("Java version: {}", modernizer.getJavaVersion());
            LOG.info("Cache path: {}", modernizer.getCachePath());
        } catch (ModernizerException e) {
            LOG.error("Validation error");
            LOG.error(e.getMessage());
            return 1;
        }
        LOG.info("Validation successful");
        return 0;
    }
}
