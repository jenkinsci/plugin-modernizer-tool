package io.jenkins.tools.pluginmodernizer.cli;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration test for the command line interface
 */
@WireMockTest
public class CommandLineITCase {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(CommandLineITCase.class);

    @TempDir
    private Path outputPath;

    @Test
    public void testVersion() throws Exception {
        LOG.info("Running testVersion");
        Invoker invoker = buildInvoker();
        InvocationRequest request = buildRequest("--version");
        InvocationResult result = invoker.execute(request);
        assertAll(
                () -> assertEquals(0, result.getExitCode()),
                () -> assertTrue(Files.readAllLines(outputPath.resolve("stdout.txt")).stream()
                        .anyMatch(line -> line.matches("plugin modernizer (.*) (.*)"))));
    }

    @Test
    public void testHelp() throws Exception {
        LOG.info("Running testHelp");
        Invoker invoker = buildInvoker();
        InvocationRequest request = buildRequest("--help");
        InvocationResult result = invoker.execute(request);
        assertAll(
                () -> assertEquals(0, result.getExitCode()),
                () -> assertTrue(Files.readAllLines(outputPath.resolve("stdout.txt")).stream()
                        .anyMatch(line -> line.matches("(.*)Usage: plugin-modernizer (.*) COMMAND(.*)"))));
    }

    @Test
    public void testValidate(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        LOG.info("Running testValidate");

        // Setup
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        wireMock.register(WireMock.get(WireMock.urlEqualTo("/api/user"))
                .willReturn(WireMock.jsonResponse(USER_API_RESPONSE, 200)));

        Invoker invoker = buildInvoker();
        InvocationRequest request =
                buildRequest("validate --debug --github-api-url " + wmRuntimeInfo.getHttpBaseUrl() + "/api");
        InvocationResult result = invoker.execute(request);
        assertAll(
                () -> assertEquals(0, result.getExitCode()),
                () -> assertTrue(Files.readAllLines(outputPath.resolve("stdout.txt")).stream()
                        .anyMatch(line -> line.matches("(.*)GitHub owner: fake-owner(.*)"))),
                () -> assertTrue(Files.readAllLines(outputPath.resolve("stdout.txt")).stream()
                        .anyMatch(line -> line.matches("(.*)Validation successful(.*)"))));
    }

    @Test
    public void testListRecipes() throws Exception {
        LOG.info("Running testListRecipes");
        Invoker invoker = buildInvoker();
        InvocationRequest request = buildRequest("recipes");
        InvocationResult result = invoker.execute(request);
        assertAll(
                () -> assertEquals(0, result.getExitCode()),
                () -> assertTrue(Files.readAllLines(outputPath.resolve("stdout.txt")).stream()
                        .anyMatch(
                                line -> line.matches(".*FetchMetadata - Extracts metadata from a Jenkins plugin.*"))));
    }

    /**
     * Build the invoker
     * @return the invoker
     */
    private Invoker buildInvoker() {
        String mavenHomeEnv = System.getenv("MAVEN_HOME");
        assertNotNull(mavenHomeEnv, "MAVEN_HOME is not set");
        Path mavenHome = Path.of(mavenHomeEnv);
        assertTrue(Files.exists(mavenHome), "MAVEN_HOME does not exist");
        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(mavenHome.toFile());
        return invoker;
    }

    /**
     * Build the request
     * @return the request
     */
    private InvocationRequest buildRequest(String args) {
        String javaHomeEnv = System.getenv("JAVA_HOME");
        assertNotNull(javaHomeEnv, "JAVA_HOME is not set");
        Path javaHome = Path.of(javaHomeEnv);
        assertTrue(Files.exists(javaHome), "JAVA_HOME does not exist");

        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File("pom-it.xml"));
        request.addArg("verify");

        // Add properties
        Properties properties = new Properties();
        String changeList = System.getProperty("set.changelist");
        if (changeList != null) {
            properties.put("set.changelist", "true");
        }
        properties.put("exec.executable", javaHome.resolve("bin/java").toString());
        properties.put("test.cliArgs", args);
        request.setProperties(properties);

        // Other options
        request.setBatchMode(true);
        request.setNoTransferProgress(true);
        request.setJavaHome(javaHome.toFile());
        request.setOutputHandler(line -> {
            try {
                Files.write(
                        outputPath.resolve("stdout.txt"),
                        (line + System.lineSeparator()).getBytes(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
            } catch (Exception e) {
                LOG.error("Error writing to stdout", e);
                throw new RuntimeException(e);
            }
            LOG.info(line);
        });
        request.setErrorHandler(line -> {
            try {
                Files.write(
                        outputPath.resolve("stderr.txt"),
                        (line + System.lineSeparator()).getBytes(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
            } catch (Exception e) {
                LOG.error("Error writing to stderr", e);
                throw new RuntimeException(e);
            }
            LOG.error(line);
        });
        return request;
    }

    /**
     * Login API response
     */
    private record UserApiResponse(String login, String type) {}

    private static final UserApiResponse USER_API_RESPONSE = new UserApiResponse("fake-user", "User");
}
