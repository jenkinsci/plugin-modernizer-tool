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

        request.setGoals(Arrays.asList("exec:exec"));
        request.setProperties(System.getProperties());
        request.getProperties().put("exec.executable", "java");
        request.getProperties()
                .put(
                        "exec.args",
                        "-jar target/plugin-modernizer-cli/target/jenkins-plugin-modernizer-999999-SNAPSHOT.jar");

        request.setPomFile(new File("pom.xml")); 

        invoker.setMavenHome(new File(System.getenv("MAVEN_HOME")));

        // Execute the request
        InvocationResult result = invoker.execute(request);
        if (result.getExitCode() != 0) {
            throw new IllegalStateException("Build failed.");
        }

        assertTrue(result.getExitCode() == 0, "Maven build failed");
    }
}
