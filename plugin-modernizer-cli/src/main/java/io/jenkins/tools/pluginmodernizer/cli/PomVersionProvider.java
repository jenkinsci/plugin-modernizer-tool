package io.jenkins.tools.pluginmodernizer.cli;

import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import picocli.CommandLine;

public class PomVersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() throws Exception {
        return new String[] {getVersionFromProperties()};
    }

    private String getVersionFromProperties() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("pom.properties")) {
            if (input == null) {
                throw new ModernizerException("Unable to find pom.properties");
            }
            properties.load(input);
        }

        String version = properties.getProperty("project.version");
        if (version == null || version.isEmpty()) {
            throw new ModernizerException("Version not found in pom.properties");
        }

        return version;
    }
}
