package io.jenkins.tools.pluginmodernizer.cli.command;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.cli.VersionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * Version command
 */
@CommandLine.Command(name = "version", description = "Display version information")
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS", justification = "safe because versions from pom.xml")
public class VersionCommand implements ICommand {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(VersionCommand.class);

    /**
     * Display the short version
     */
    @CommandLine.Option(
            names = {"--short"},
            description = "Display the short version")
    private boolean shortVersion;

    @Override
    public Integer call() throws Exception {
        VersionProvider versionProvider = new VersionProvider();
        if (shortVersion) {
            LOG.info(versionProvider.getMavenVersion());
        } else {
            LOG.info(versionProvider.getVersion()[0].trim());
        }
        return 0;
    }
}
