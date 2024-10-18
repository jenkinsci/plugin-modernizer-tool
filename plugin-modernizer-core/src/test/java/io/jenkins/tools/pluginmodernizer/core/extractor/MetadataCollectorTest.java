package io.jenkins.tools.pluginmodernizer.core.extractor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.maven.Assertions.pomXml;

import io.jenkins.tools.pluginmodernizer.core.model.JDK;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.PreconditionError;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

public class MetadataCollectorTest implements RewriteTest {

    @Language("xml")
    private static final String POM_XML =
            """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>4.80</version>
                            <relativePath />
                          </parent>

                          <artifactId>gitx  lab-plugin</artifactId>
                          <version>${revision}${changelist}</version>
                          <packaging>hpi</packaging>
                          <name>GitLab Plugin</name>
                          <url>https://github.com/jenkinsci/${project.artifactId}</url>
                          <developers>
                            <developer>
                              <id>john.doe</id>
                              <name>John Doe</name>
                              <email>john.doe@example.com</email>
                            </developer>
                          </developers>
                          <licenses>
                            <license>
                              <name>GPL v2.0 License</name>
                              <url>http://www.gnu.org/licenses/old-licenses/gpl-2.0-standalone.html</url>
                            </license>
                          </licenses>
                          <scm>
                            <connection>scm:git:https://github.com/${gitHubRepo}.git</connection>
                            <developerConnection>scm:git:git@github.com:${gitHubRepo}.git</developerConnection>
                            <tag>${scmTag}</tag>
                            <url>https://github.com/${gitHubRepo}</url>
                          </scm>
                          <distributionManagement>
                            <repository>
                              <id>maven.jenkins-ci.org</id>
                              <name>jenkinsci-releases</name>
                              <url>https://repo.jenkins-ci.org/releases</url>
                            </repository>
                            <snapshotRepository>
                              <id>maven.jenkins-ci.org</id>
                              <name>jenkinsci-snapshots</name>
                              <url>https://repo.jenkins-ci.org/snapshots</url>
                            </snapshotRepository>
                          </distributionManagement>

                          <properties>
                            <revision>1.8.1</revision>
                            <java.level>8</java.level>
                            <changelist>-SNAPSHOT</changelist>
                            <jenkins.version>2.426.3</jenkins.version>
                            <spotbugs.effort>Max</spotbugs.effort>
                            <spotbugs.threshold>Low</spotbugs.threshold>
                            <gitHubRepo>jenkinsci/${project.artifactId}</gitHubRepo>
                            <hpi.compatibleSinceVersion>1.4.0</hpi.compatibleSinceVersion>
                            <mockserver.version>5.15.0</mockserver.version>
                            <spotless.check.skip>false</spotless.check.skip>
                          </properties>

                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <!-- Pick up common dependencies for the selected LTS line: https://github.com/jenkinsci/bom#usage -->
                                <groupId>io.jenkins.tools.bom</groupId>
                                <artifactId>bom-2.414.x</artifactId>
                                <version>2950.va_633b_f42f759</version>
                                <type>pom</type>
                                <scope>import</scope>
                              </dependency>
                            </dependencies>
                          </dependencyManagement>

                          <dependencies>
                            <dependency>
                              <groupId>io.jenkins.plugins</groupId>
                              <artifactId>caffeine-api</artifactId>
                            </dependency>
                            <dependency>
                              <groupId>org.jboss.resteasy</groupId>
                              <artifactId>resteasy-client</artifactId>
                              <version>3.15.6.Final</version>
                              <exclusions>
                                <!-- Provided by Jenkins core -->
                                <exclusion>
                                  <groupId>com.github.stephenc.jcip</groupId>
                                  <artifactId>jcip-annotations</artifactId>
                                </exclusion>
                              </exclusions>
                            </dependency>
                            <dependency>
                              <groupId>org.jenkins-ci.plugins</groupId>
                              <artifactId>credentials</artifactId>
                            </dependency>
                          </dependencies>

                          <repositories>
                            <repository>
                              <id>repo.jenkins-ci.org</id>
                              <url>https://repo.jenkins-ci.org/public/</url>
                            </repository>
                          </repositories>

                          <pluginRepositories>
                            <pluginRepository>
                              <id>repo.jenkins-ci.org</id>
                              <url>https://repo.jenkins-ci.org/public/</url>
                            </pluginRepository>
                          </pluginRepositories>
                        </project>
                        """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MetadataCollector());
    }

    @Test
    void testPluginWithJenkinsfileWithoutJdkInfo() throws Exception {
        rewriteRun(
                // language=groovy
                groovy(
                        """
                          buildPlugin()
                          """,
                        spec -> spec.path("Jenkinsfile")),
                pomXml(POM_XML));
        PluginMetadata pluginMetadata = new PluginMetadata().refresh();
        String pluginName = pluginMetadata.getPluginName();
        assertEquals("GitLab Plugin", pluginName);
        assertEquals("4.80", pluginMetadata.getParentVersion());
        String jenkinsVersion = pluginMetadata.getJenkinsVersion();
        assertEquals("2.426.3", jenkinsVersion);
        assertEquals("2950.va_633b_f42f759", pluginMetadata.getBomVersion());
        assertNotNull(pluginMetadata.getProperties().get("java.level"));
        assertTrue(pluginMetadata.hasFlag(MetadataFlag.SCM_HTTPS));
        assertTrue(pluginMetadata.hasFlag(MetadataFlag.MAVEN_REPOSITORIES_HTTPS));
        assertTrue(pluginMetadata.hasFlag(MetadataFlag.LICENSE_SET));
        assertTrue(pluginMetadata.hasFlag(MetadataFlag.DEVELOPER_SET));
        Map<String, String> properties = pluginMetadata.getProperties();
        assertNotNull(properties);
        assertEquals(10, properties.size());

        // Files are present
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.JENKINSFILE));
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.POM));

        // Absent
        assertFalse(pluginMetadata.hasFile(ArchetypeCommonFile.WORKFLOW_CD));

        Set<JDK> jdkVersion = pluginMetadata.getJdks();
        assertEquals(0, jdkVersion.size());
    }

    @Test
    void testPluginWithJenkinsfileWithJdkInfo() {
        rewriteRun(
                // language=groovy
                groovy(
                        """
                         buildPlugin(
                         useContainerAgent: true,
                         configurations: [
                                [platform: 'linux', jdk: 21],
                                [platform: 'windows', jdk: 17],
                         ])
                         """,
                        spec -> spec.path("Jenkinsfile")),
                pomXml(POM_XML));
        PluginMetadata pluginMetadata = new PluginMetadata().refresh();
        // Files are present
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.JENKINSFILE));
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.POM));

        Set<JDK> jdkVersion = pluginMetadata.getJdks();

        assertEquals(2, jdkVersion.size());
        assertTrue(jdkVersion.contains(JDK.JAVA_21));
        assertTrue(jdkVersion.contains(JDK.JAVA_17));
    }

    @Test
    void testJenkinsfileWithConfigurationsAsParameter() {
        rewriteRun(
                // language=groovy
                groovy(
                        """
                            def configurations = [
                              [ platform: "linux", jdk: "11" ],
                              [ platform: "windows", jdk: "17" ]
                            ]

                            def params = [
                                failFast: false,
                                configurations: configurations,
                                checkstyle: [qualityGates: [[threshold: 1, type: 'NEW', unstable: true]]],
                                pmd: [qualityGates: [[threshold: 1, type: 'NEW', unstable: true]]],
                                jacoco: [sourceCodeRetention: 'MODIFIED']
                                ]

                            buildPlugin(params)
                            """,
                        spec -> spec.path("Jenkinsfile")),
                pomXml(POM_XML));
        PluginMetadata pluginMetadata = new PluginMetadata().refresh();
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.JENKINSFILE));
        Set<JDK> jdkVersion = pluginMetadata.getJdks();
        assertEquals(2, jdkVersion.size());
    }

    @Test
    void testJenkinsfileWithInlineConfigurations() {
        rewriteRun(
                // language=groovy
                groovy(
                        """
                            def configurations = [
                              [ platform: "linux", jdk: "11" ],
                              [ platform: "windows", jdk: "17" ]
                            ]

                            buildPlugin(
                                failFast: false,
                                configurations: configurations,
                                checkstyle: [qualityGates: [[threshold: 1, type: 'NEW', unstable: true]]],
                                pmd: [qualityGates: [[threshold: 1, type: 'NEW', unstable: true]]],
                                jacoco: [sourceCodeRetention: 'MODIFIED'])
                            """,
                        spec -> spec.path("Jenkinsfile")),
                pomXml(POM_XML));
        PluginMetadata pluginMetadata = new PluginMetadata().refresh();
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.JENKINSFILE));
        Set<JDK> jdkVersion = pluginMetadata.getJdks();
        assertEquals(2, jdkVersion.size());
    }

    @Test
    void testRemediationFunctionForMavenRepositoriesHttp() throws Exception {
        Path tempPom = Files.createTempFile("test-pom", ".xml");
        Files.writeString(tempPom, POM_XML.replace("https://repo.jenkins-ci.org/public/", "http://repo.jenkins-ci.org/public/"));

        Plugin plugin = Plugin.build("test-plugin").withLocalRepository(tempPom.getParent());
        PreconditionError.MAVEN_REPOSITORIES_HTTP.remediate(plugin);

        String modifiedPom = Files.readString(tempPom);
        assertTrue(modifiedPom.contains("https://repo.jenkins-ci.org/public/"));
        assertFalse(modifiedPom.contains("http://repo.jenkins-ci.org/public/"));
    }
}
