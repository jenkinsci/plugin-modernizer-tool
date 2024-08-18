package io.jenkins.tools.pluginmodernizer.core.model;

import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.utils.JdkFetcher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

/**
 * JDK enum to compile and build a Jenkins plugin
 * Most of the time only LTS are available, but adding new version would
 * be useful to test compilation for new major LTS version and be ready every
 * 2 years when a new LTS is released.
 * See <a href="https://www.jenkins.io/blog/2023/11/06/introducing-2-2-2-java-support-plan/">2+2+2 Jenkins Java support plan</a> for details
 */
public enum JDK {

    /**
     * Available JDKs
     */
    JAVA_8(8, true),
    JAVA_11(11, true),
    JAVA_17(17, true),
    JAVA_21(21, true);

    /**
     * The major version
     */
    private final int major;

    /**
     * If the major
     */
    private final boolean lts;

    /**
     * Constructor
     * @param major The major version
     */
    JDK(int major, boolean lts) {
        this.major = major;
        this.lts = lts;
    }

    /**
     * Get the major version
     * @return The major version
     */
    public int getMajor() {
        return major;
    }

    /**
     * Check if the JDK is LTS
     * @return True if LTS
     */
    public boolean isLts() {
        return lts;
    }

    /**
     * Get the JDK home for this enum
     * @param jdkFetcher The JDK fetcher use to download the JDK
     * @return The JDK home
     * @throws IOException If an error occurs
     * @throws InterruptedException If an error occurs
     */
    public Path getHome(JdkFetcher jdkFetcher) throws IOException, InterruptedException {
        return Files.isDirectory(getDefaultSdkMan()) ? getDefaultSdkMan() : jdkFetcher.getJdkPath(major);
    }

    /**
     * Return the next JDK available
     * @return The next JDK
     */
    public JDK next() {
        int major = getMajor();
        return Arrays.stream(JDK.values())
                .sorted(Comparator.comparingInt(JDK::getMajor))
                .filter(jdk -> jdk.getMajor() > major)
                .findFirst()
                .orElse(null);
    }

    /**
     * Return the previous JDK available
     * @return The previous JDK
     */
    public JDK previous() {
        int major = getMajor();
        return Arrays.stream(JDK.values())
                .sorted(Comparator.comparingInt(JDK::getMajor).reversed())
                .filter(jdk -> jdk.getMajor() < major)
                .findFirst()
                .orElse(null);
    }

    /**
     * Has next predicate
     * @param jdk The JDK
     * @return True if there is a next JDK
     */
    public static boolean hasNext(JDK jdk) {
        return jdk.next() != null;
    }

    /**
     * Has before predicate
     * @param jdk The JDK
     * @return True if there is a previous JDK
     */
    public static boolean hasPrevious(JDK jdk) {
        return jdk.previous() != null;
    }

    public final int compareMajor(JDK jdk) {
        return Integer.compare(this.getMajor(), jdk.getMajor());
    }

    /**
     * Get the JDK home for SDK man
     * @return The JDK home
     */
    private Path getDefaultSdkMan() {
        return Settings.getDefaultSdkManJava(this.name() + "_HOME");
    }

    /**
     * Get the JDK for a major version
     * @param major The major version
     * @return The JDK or null if not found
     */
    public static JDK get(int major) {
        return Arrays.stream(JDK.values())
                .filter(j -> j.getMajor() == major)
                .findFirst()
                .orElse(null);
    }

    /**
     * Get the latest JDK available
     * @return The latest JDK
     */
    public static JDK max() {
        JDK jdk = null;
        for (JDK j : JDK.values()) {
            if (jdk == null || j.getMajor() > jdk.getMajor()) {
                jdk = j;
            }
        }
        return jdk;
    }

    /**
     * Get the default source JDK to compile before modernization
     * @return The default source JDK
     */
    public static JDK getDefaultSource() {
        return JDK.JAVA_8;
    }

    /**
     * Get the default target JDK to compile after modernization
     * @return The default target JDK
     */
    public static JDK getDefaultTarget() {
        return JDK.JAVA_17;
    }
}
