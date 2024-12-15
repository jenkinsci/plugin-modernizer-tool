package io.jenkins.tools.pluginmodernizer.cli;

import io.jenkins.tools.pluginmodernizer.cli.command.*;
import org.slf4j.bridge.SLF4JBridgeHandler;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "plugin-modernizer",
        description = "Plugin Modernizer. A tool to modernize Jenkins plugins",
        synopsisSubcommandLabel = "COMMAND",
        subcommands = {
            ValidateCommand.class,
            ListRecipesCommand.class,
            BuildMetadataCommand.class,
            DryRunCommand.class,
            RunCommand.class,
            CleanupCommand.class
        },
        mixinStandardHelpOptions = true,
        versionProvider = VersionProvider.class)
public class Main {

    static {
        System.setProperty("slf4j.internal.verbosity", "WARN");
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    /**
     * Main method
     * @param args Command line arguments
     */
    public static void main(final String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}
