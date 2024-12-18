package io.jenkins.tools.pluginmodernizer.cli;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.jenkins.tools.pluginmodernizer.cli.utils.GitHubServerContainer;
import io.jenkins.tools.pluginmodernizer.core.extractor.PluginMetadata;
import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.utils.JsonUtils;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Security;
import java.util.Properties;
import java.util.stream.Stream;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Integration test for the command line interface
 */
@WireMockTest
@Testcontainers(disabledWithoutDocker = true)
public class CommandLineITCase {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(CommandLineITCase.class);

    /**
     * Tests plugins
     * @return the plugins
     */
    private static Stream<Arguments> testsPlugins() {
        return Stream.of(Arguments.of(new PluginMetadata() {
            {
                setPluginName("empty");
                setJenkinsVersion("2.440.3");
            }

            {
                setPluginName("replace-by-api-plugins");
                setJenkinsVersion("2.452.4");
            }
        }));
    }

    @TempDir
    private Path outputPath;

    @TempDir
    private Path cachePath;

    @TempDir
    private Path keysPath;

    @Test
    public void testVersion() throws Exception {
        LOG.info("Running testVersion");
        Invoker invoker = buildInvoker();
        InvocationResult result1 = invoker.execute(buildRequest("--version"));
        assertAll(
                () -> assertEquals(0, result1.getExitCode()),
                () -> assertTrue(Files.readAllLines(outputPath.resolve("stdout.txt")).stream()
                        .anyMatch(line -> line.matches("plugin modernizer ([a-zA-Z0-9.-_]+) (.*)"))));

        Files.delete(outputPath.resolve("stdout.txt"));

        InvocationResult result2 = invoker.execute(buildRequest("version"));
        assertAll(
                () -> assertEquals(0, result2.getExitCode()),
                () -> assertTrue(Files.readAllLines(outputPath.resolve("stdout.txt")).stream()
                        .anyMatch(line -> line.matches("plugin modernizer ([a-zA-Z0-9.-_]+) (.*)"))));

        Files.delete(outputPath.resolve("stdout.txt"));

        InvocationResult result3 = invoker.execute(buildRequest("version --short"));
        assertAll(
                () -> assertEquals(0, result3.getExitCode()),
                () -> assertTrue(Files.readAllLines(outputPath.resolve("stdout.txt")).stream()
                        .anyMatch(line -> line.matches("(.*)\\s*[a-zA-Z0-9.-_]+\\s*"))));
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
    public void testCleanupWithDryRun() throws Exception {
        LOG.info("Running testCleanupWithDryRun");
        Invoker invoker = buildInvoker();
        InvocationRequest request = buildRequest("cleanup --cache-path %s --dry-run".formatted(cachePath));
        InvocationResult result = invoker.execute(request);
        assertAll(
                () -> assertEquals(0, result.getExitCode()),
                () -> assertTrue(Files.readAllLines(outputPath.resolve("stdout.txt")).stream()
                        .anyMatch(line -> line.matches("(.*)Would remove path: (.*)"))));
    }

    @Test
    public void testCleanup() throws Exception {
        LOG.info("Running testCleanup");
        Invoker invoker = buildInvoker();
        InvocationRequest request = buildRequest("cleanup --cache-path %s".formatted(cachePath));
        InvocationResult result = invoker.execute(request);
        assertAll(
                () -> assertEquals(0, result.getExitCode()),
                () -> assertTrue(Files.readAllLines(outputPath.resolve("stdout.txt")).stream()
                        .anyMatch(line -> line.matches("(.*)Removed path: (.*)"))));
    }

    @Test
    public void testValidateWithSshKey(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        LOG.info("Running testValidateWithSshKey");

        // Setup
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        wireMock.register(WireMock.get(WireMock.urlEqualTo("/api/user"))
                .willReturn(
                        WireMock.jsonResponse(new GitHubServerContainer.UserApiResponse("fake-owner", "User"), 200)));

        Invoker invoker = buildInvoker();
        InvocationRequest request =
                buildRequest("validate --maven-home %s --ssh-private-key %s --debug --github-api-url %s/api"
                        .formatted(
                                getModernizerMavenHome(),
                                generatePrivateKey("testValidateWithSshKey"),
                                wmRuntimeInfo.getHttpBaseUrl()));
        InvocationResult result = invoker.execute(request);
        assertAll(
                () -> assertEquals(0, result.getExitCode()),
                () -> assertTrue(Files.readAllLines(outputPath.resolve("stdout.txt")).stream()
                        .anyMatch(line -> line.matches("(.*)GitHub owner: fake-owner(.*)"))),
                () -> assertTrue(Files.readAllLines(outputPath.resolve("stdout.txt")).stream()
                        .anyMatch(line -> line.matches("(.*)Validation successful(.*)"))));
    }

    @Test
    public void testValidate(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        LOG.info("Running testValidate");

        // Setup
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        wireMock.register(WireMock.get(WireMock.urlEqualTo("/api/user"))
                .willReturn(
                        WireMock.jsonResponse(new GitHubServerContainer.UserApiResponse("fake-owner", "User"), 200)));

        Invoker invoker = buildInvoker();
        InvocationRequest request =
                buildRequest("validate --debug --github-api-url %s/api".formatted(wmRuntimeInfo.getHttpBaseUrl()));
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

    @ParameterizedTest
    @MethodSource("testsPlugins")
    public void testBuildMetadata(PluginMetadata expectedMetadata, WireMockRuntimeInfo wmRuntimeInfo) throws Exception {

        String plugin = expectedMetadata.getPluginName();

        // Junit attachment with logs file for the plugin build
        System.out.printf(
                "[[ATTACHMENT|%s]]%n", Plugin.build(plugin).getLogFile().toAbsolutePath());

        try (GitHubServerContainer gitRemote = new GitHubServerContainer(wmRuntimeInfo, keysPath, plugin, "main")) {

            gitRemote.start();

            Invoker invoker = buildInvoker();
            InvocationRequest request = buildRequest("build-metadata %s".formatted(getRunArgs(wmRuntimeInfo, plugin)));
            InvocationResult result = invoker.execute(request);

            // Assert output
            assertAll(
                    () -> assertEquals(0, result.getExitCode()),
                    () -> assertTrue(Files.readAllLines(outputPath.resolve("stdout.txt")).stream()
                            .anyMatch(line -> line.matches("(.*)GitHub owner: fake-owner(.*)"))),
                    () -> assertTrue(Files.readAllLines(outputPath.resolve("stdout.txt")).stream()
                            .anyMatch(line ->
                                    line.matches(".*Metadata was fetched for plugin (.*) and is available at.*"))));

            // Assert some metadata
            PluginMetadata metadata = JsonUtils.fromJson(
                    cachePath
                            .resolve("jenkins-plugin-modernizer-cli")
                            .resolve(plugin)
                            .resolve(CacheManager.PLUGIN_METADATA_CACHE_KEY),
                    PluginMetadata.class);

            assertEquals(expectedMetadata.getJenkinsVersion(), metadata.getJenkinsVersion());
        }
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
     * Generate a Ed25519 private key and save it
     * @param name The name of the key
     * @throws Exception If an error occurs
     */
    private Path generatePrivateKey(String name) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("Ed25519", "BC");
        KeyPair pair = keyPairGenerator.generateKeyPair();
        PrivateKey privateKey = pair.getPrivate();
        File privateKeyFile = keysPath.resolve(name).toFile();
        try (FileWriter fileWriter = new FileWriter(privateKeyFile);
                JcaPEMWriter pemWriter = new JcaPEMWriter(fileWriter)) {
            pemWriter.writeObject(privateKey);
        }
        return privateKeyFile.toPath();
    }

    /**
     * Get the modernizer maven home
     * @return Use version from the target directory
     */
    private Path getModernizerMavenHome() {
        return Path.of("target/apache-maven-3.9.9").toAbsolutePath();
    }

    /**
     * Get the URL arguments
     * @param wmRuntimeInfo The WireMock runtime info
     * @param plugin The plugin
     * @return the URL arguments
     */
    private String getRunArgs(WireMockRuntimeInfo wmRuntimeInfo, String plugin) {
        return """
        --plugins %s
        --debug
        --maven-home %s
        --ssh-private-key %s
        --cache-path %s
        --github-api-url %s
        --jenkins-update-center %s
        --jenkins-plugin-info %s
        --plugin-health-score %s
        --jenkins-plugins-stats-installations-url %s
        """
                .formatted(
                        plugin,
                        getModernizerMavenHome(),
                        keysPath.resolve(plugin),
                        cachePath,
                        wmRuntimeInfo.getHttpBaseUrl() + "/api",
                        wmRuntimeInfo.getHttpBaseUrl() + "/update-center.json",
                        wmRuntimeInfo.getHttpBaseUrl() + "/plugin-versions.json",
                        wmRuntimeInfo.getHttpBaseUrl() + "/scores",
                        wmRuntimeInfo.getHttpBaseUrl() + "/jenkins-stats/svg/202406-plugins.csv")
                .replaceAll("\\s+", " ");
    }
}
