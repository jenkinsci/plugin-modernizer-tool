package io.jenkins.tools.pluginmodernizer.core.config;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Settings {

    public static final Path DEFAULT_CACHE_PATH;

    public static final String MAVEN_HOME_PATH;

    public static final String MAVEN_REWRITE_PLUGIN_VERSION = "5.34.1";

    static {
        String cacheBaseDir = System.getProperty("user.home");
        if (cacheBaseDir == null) {
            cacheBaseDir = System.getProperty("user.dir");
        }

        String cacheDirFromEnv = System.getenv("CACHE_DIR");
        if (cacheDirFromEnv == null) {
            DEFAULT_CACHE_PATH = Paths.get(cacheBaseDir, ".cache", "jenkins-plugin-modernizer-cli");
        } else {
            DEFAULT_CACHE_PATH = Paths.get(cacheDirFromEnv);
        }
        MAVEN_HOME_PATH = getMavenHomePath();
    }

    private static String getMavenHomePath() {
        String mavenHome = System.getenv("MAVEN_HOME");
        if (mavenHome == null) {
            mavenHome = System.getenv("M2_HOME");
        }
        return mavenHome;
    }
}
