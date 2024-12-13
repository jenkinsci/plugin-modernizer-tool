package io.jenkins.tools.pluginmodernizer.cli;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Arrays;
import org.apache.maven.shared.invoker.*;
import org.junit.jupiter.api.Test;

public class MavenInvokerIT {

    @Test
    public void runIntegrationTest() throws MavenInvocationException {
        Invoker invoker = new DefaultInvoker();
        InvocationRequest request = new DefaultInvocationRequest();

        // Set goals and properties
        request.setGoals(Arrays.asList("exec:exec"));
        request.setProperties(System.getProperties());
        request.getProperties().put("exec.executable", "java");
        request.getProperties()
                .put(
                        "exec.args",
                        "-jar target/plugin-modernizer-cli/target/jenkins-plugin-modernizer-999999-SNAPSHOT.jar");

        // Specify the POM file (ensure this points to your actual POM file)
        request.setPomFile(new File("pom.xml")); // Update this path if necessary

        invoker.setMavenHome(new File(System.getenv("MAVEN_HOME")));

        // Execute the request
        InvocationResult result = invoker.execute(request);
        if (result.getExitCode() != 0) {
            throw new IllegalStateException("Build failed.");
        }

        // Optionally, you can add a simple assertion to ensure the build was successful
        assertTrue(result.getExitCode() == 0, "Maven build failed");
    }
}
