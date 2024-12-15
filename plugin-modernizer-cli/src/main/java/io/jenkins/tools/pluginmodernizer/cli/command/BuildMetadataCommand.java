package io.jenkins.tools.pluginmodernizer.cli.command;

import io.jenkins.tools.pluginmodernizer.cli.options.EnvOptions;
import io.jenkins.tools.pluginmodernizer.cli.options.GlobalOptions;
import io.jenkins.tools.pluginmodernizer.cli.options.PluginOptions;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.impl.PluginModernizer;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * Run command
 */
@CommandLine.Command(name = "build-metadata", description = "Build local metadata")
public class BuildMetadataCommand implements ICommand {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(BuildMetadataCommand.class);

    /**
     * Plugins options
     */
    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
    private PluginOptions pluginOptions;

    /**
     * Path to the authentication key in case of private repo
     */
    @CommandLine.Option(
            names = {"--ssh-private-key"},
            description = "Path to the authentication key for GitHub. Default to ~/.ssh/id_rsa")
    private Path sshPrivateKey = Settings.SSH_PRIVATE_KEY;

    /**
     * Global options for all commands
     */
    @CommandLine.Mixin
    private GlobalOptions options;

    /**
     * Environment options
     */
    @CommandLine.Mixin
    private EnvOptions envOptions;

    @Override
    public Config setup(Config.Builder builder) {
        options.config(builder);
        envOptions.config(builder);
        pluginOptions.config(builder);
        return builder.withSshPrivateKey(sshPrivateKey)
                .withRecipe(Settings.FETCH_METADATA_RECIPE)
                .build();
    }

    @Override
    public Integer call() {
        PluginModernizer modernizer = getModernizer();
        try {
            modernizer.validate();
        } catch (ModernizerException e) {
            LOG.error("Validation error");
            LOG.error(e.getMessage());
            return 1;
        }
        modernizer.start();
        return 0;
    }
}
