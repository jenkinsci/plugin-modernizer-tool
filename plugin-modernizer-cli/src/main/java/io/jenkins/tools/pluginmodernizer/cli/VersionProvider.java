package io.jenkins.tools.pluginmodernizer.cli;

import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import picocli.CommandLine;

/**
 * Version provider for the CLI
 */
public class VersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() throws Exception {
        return new String[] {
            "plugin modernizer %s (%s)".formatted(getValue("project.version"), getValue("build.timestamp")),
        };
    }

    /**
     * Get the maven version
     * @return the maven version
     * @throws Exception if the version is not found
     */
    public String getMavenVersion() throws Exception {
        return getValue("project.version");
    }

    /**
     * Get a value from the pom.properties file
     * @param property the property to get
     * @return the value of the property
     * @throws IOException if the file is not found
     */
    private String getValue(String property) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("pom.properties")) {
            if (input == null) {
                throw new ModernizerException("Unable to find pom.properties");
            }
            properties.load(input);
        }

        String value = properties.getProperty(property);
        if (value == null || value.isEmpty()) {
            throw new ModernizerException("%s not found in pom.properties".formatted(property));
        }

        return value;
    }
}
