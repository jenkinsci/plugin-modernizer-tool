package io.jenkins.tools.pluginmodernizer.core.model;

import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.utils.JdkFetcher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.apache.maven.artifact.versioning.ComparableVersion;

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
     * See <a href="https://www.jenkins.io/doc/book/platform-information/support-policy-java/">Java Support Policy</a> for details
     */
    JAVA_8(8, true, null, "2.346.1"),
    JAVA_11(11, true, "2.164.1", "2.463"), // No LTS drop Java 11 yet
    JAVA_17(17, true, "2.346.1", null),
    JAVA_21(21, true, "2.426.1", null);

    /**
     * The major version
     */
    private final int major;

    /**
     * If the major
     */
    private final boolean lts;

    /**
     * The compatibility since for this JDK
     */
    private final String compatibleSince;

    /**
     * The maximum required core version
     */
    private final String maximumCoreVersion;

    /**
     * Constructor
     * @param major The Java major version
     * @param lts If the version is LTS
     */
    JDK(int major, boolean lts, String compatibleSince, String maximumCoreVersion) {
        this.major = major;
        this.lts = lts;
        this.compatibleSince = compatibleSince;
        this.maximumCoreVersion = maximumCoreVersion;
    }

    /**
     * Get the major version
     * @return The major version
     */
    public int getMajor() {
        return major;
    }

    /**
     * Get the minimum required core version
     * @return The minimum required core version
     */
    public String getCompatibleSince() {
        return compatibleSince;
    }

    /**
     * Get the maximum required core version
     * @return The maximum required core version
     */
    public String getMaxCoreVersion() {
        return maximumCoreVersion;
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
     * Check if the JDK is supported for a given Jenkins version
     * @param jenkinsVersion The Jenkins version
     * @return True if supported
     */
    public boolean supported(String jenkinsVersion) {
        return get(jenkinsVersion).contains(this);
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
     * Get the oldest JDK available
     * @return The oldest JDK
     */
    public static JDK min() {
        JDK jdk = null;
        for (JDK j : JDK.values()) {
            if (jdk == null || j.getMajor() < jdk.getMajor()) {
                jdk = j;
            }
        }
        return jdk;
    }

    /**
     * Return the minimum JDK from a list of JDKs
     * @param jdks List of JDKS. Can be null or empty
     * @return The minimum JDK. If the list is empty, return the minimum JDK available
     */
    public static JDK min(List<JDK> jdks) {
        if (jdks == null || jdks.isEmpty()) {
            return JDK.min();
        }
        return jdks.stream().min(JDK::compareMajor).orElseThrow();
    }

    /**
     * Return a list of all JDK sorted by major version
     * @return The list of JDKs
     */
    public static List<JDK> all() {
        return Arrays.stream(values()).sorted(JDK::compareMajor).toList();
    }

    /**
     * Get list of buildable JDKs for a given Jenkins version
     * @param jenkinsVersion The Jenkins version
     * @return The list of buildable JDKs
     */
    public static List<JDK> get(String jenkinsVersion) {
        ComparableVersion jenkinsVersionComparable = new ComparableVersion(jenkinsVersion);
        return Arrays.stream(JDK.values())

                // Filter since
                .filter(jdk -> {
                    if (jdk.getCompatibleSince() == null) {
                        return true;
                    }
                    ComparableVersion compatibleSince = new ComparableVersion(jdk.getCompatibleSince());
                    return jenkinsVersionComparable.compareTo(compatibleSince) >= 0;
                })
                .filter(jdk -> {
                    if (jdk.getMaxCoreVersion() == null) {
                        return true;
                    }
                    ComparableVersion maxCoreVersion = new ComparableVersion(jdk.getMaxCoreVersion());
                    return jenkinsVersionComparable.compareTo(maxCoreVersion) <= 0;
                })
                .toList();
    }
}
