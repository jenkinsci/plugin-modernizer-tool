package io.jenkins.tools.pluginmodernizer.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

@ExtendWith(SystemStubsExtension.class)
public class SettingsUserDirTest {

    @SystemStub
    private SystemProperties properties = new SystemProperties("user.dir", "/opt/app").remove("user.home");

    @Test
    public void test() throws Exception {
        assertEquals(Paths.get("/opt/app/.cache/jenkins-plugin-modernizer-cli"), Settings.DEFAULT_CACHE_PATH);
    }
}
