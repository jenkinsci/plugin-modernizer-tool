package io.jenkins.tools.pluginmodernizer.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
public class SettingsEnvTest {

    @SystemStub
    private EnvironmentVariables envVars = new EnvironmentVariables()
            .set("CACHE_DIR", ".my-cache")
            .remove("GH_TOKEN")
            .set("GITHUB_TOKEN", "fake-token")
            .set("GH_OWNER", "fake-org")
            .set("JENKINS_UC", "https://example-com")
            .remove("MAVEN_HOME")
            .set("M2_HOME", "/opt/maven-test");

    @Test
    public void testCustomCache() throws Exception {
        assertEquals(Paths.get(".my-cache"), Settings.DEFAULT_CACHE_PATH);
    }

    @Test
    public void testCustomM2Home() throws Exception {
        assertEquals(Paths.get("/opt/maven-test"), Settings.DEFAULT_MAVEN_HOME);
    }

    @Test
    public void testGithubToken() throws Exception {
        assertEquals("fake-token", Settings.GITHUB_TOKEN);
    }

    @Test
    public void testUpdateCenter() throws Exception {
        assertEquals("https://example-com", Settings.DEFAULT_UPDATE_CENTER_URL.toString());
    }

    @Test
    public void testGithubOwner() throws Exception {
        assertEquals("fake-org", Settings.GITHUB_OWNER);
    }
}
