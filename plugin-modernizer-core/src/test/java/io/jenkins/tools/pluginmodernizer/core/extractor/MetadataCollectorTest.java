package io.jenkins.tools.pluginmodernizer.core.extractor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.maven.Assertions.pomXml;

import io.jenkins.tools.pluginmodernizer.core.model.JDK;
import io.jenkins.tools.pluginmodernizer.core.utils.JsonUtils;
import java.util.*;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

public class MetadataCollectorTest implements RewriteTest {

    private static final PluginMetadata EXPECTED_POM_METADATA;

    static {
        EXPECTED_POM_METADATA = new PluginMetadata();
        EXPECTED_POM_METADATA.setPluginName("GitLab Plugin");
        EXPECTED_POM_METADATA.setParentVersion("4.80");
        EXPECTED_POM_METADATA.setJenkinsVersion("2.426.3");
        EXPECTED_POM_METADATA.setBomVersion("2950.va_633b_f42f759");
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("revision", "1.8.1");
        properties.put("java.level", "8");
        properties.put("changelist", "-SNAPSHOT");
        properties.put("jenkins.version", "2.426.3");
        properties.put("spotbugs.effort", "Max");
        properties.put("spotbugs.threshold", "Low");
        properties.put("gitHubRepo", "jenkinsci/${project.artifactId}");
        properties.put("hpi.compatibleSinceVersion", "1.4.0");
        properties.put("mockserver.version", "5.15.0");
        properties.put("spotless.check.skip", "false");
        EXPECTED_POM_METADATA.setProperties(properties);
        List<ArchetypeCommonFile> commonFiles = new LinkedList<>();
        commonFiles.add(ArchetypeCommonFile.JENKINSFILE);
        commonFiles.add(ArchetypeCommonFile.POM);
        EXPECTED_POM_METADATA.setCommonFiles(commonFiles);
        Set<MetadataFlag> flags = new LinkedHashSet<>();
        flags.add(MetadataFlag.DEVELOPER_SET);
        flags.add(MetadataFlag.LICENSE_SET);
        flags.add(MetadataFlag.SCM_HTTPS);
        flags.add(MetadataFlag.MAVEN_REPOSITORIES_HTTPS);
        EXPECTED_POM_METADATA.setFlags(flags);
    }

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

                          <artifactId>gitlab-plugin</artifactId>
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

    @Test
    void testPluginWithJenkinsfileWithoutJdkInfo() throws Exception {
        EXPECTED_POM_METADATA.setJdks(Set.of());
        EXPECTED_POM_METADATA.setJdks(Collections.emptySet());
        rewriteRun(
                recipeSpec -> recipeSpec.recipe(new MetadataCollector(true)),
                // language=groovy
                groovy(
                        """
                          buildPlugin()
                          """,
                        spec -> spec.path("Jenkinsfile")),
                pomXml(POM_XML, "<!--~~(" + JsonUtils.toJson(EXPECTED_POM_METADATA) + ")~~>-->" + POM_XML));
    }

    @Test
    void testPluginWithJenkinsfileWithJdkInfo() {
        Set<JDK> jdks = new LinkedHashSet<>();
        jdks.add(JDK.JAVA_21);
        jdks.add(JDK.JAVA_17);
        EXPECTED_POM_METADATA.setJdks(jdks);
        rewriteRun(
                recipeSpec -> recipeSpec.recipe(new MetadataCollector(true)),
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
                pomXml(POM_XML, "<!--~~(" + JsonUtils.toJson(EXPECTED_POM_METADATA) + ")~~>-->" + POM_XML));
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
        Set<JDK> jdks = new LinkedHashSet<>();
        jdks.add(JDK.JAVA_11);
        jdks.add(JDK.JAVA_17);
        EXPECTED_POM_METADATA.setJdks(jdks);
        rewriteRun(
                recipeSpec -> recipeSpec.recipe(new MetadataCollector(true)),
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
                pomXml(POM_XML, "<!--~~(" + JsonUtils.toJson(EXPECTED_POM_METADATA) + ")~~>-->" + POM_XML));
        PluginMetadata pluginMetadata = new PluginMetadata().refresh();
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.JENKINSFILE));
        Set<JDK> jdkVersion = pluginMetadata.getJdks();
        assertEquals(2, jdkVersion.size());
    }

    @Test
    void testJenkinsfileWithInlineConfigurations() {
        Set<JDK> jdks = new LinkedHashSet<>();
        jdks.add(JDK.JAVA_11);
        jdks.add(JDK.JAVA_17);
        EXPECTED_POM_METADATA.setJdks(jdks);
        rewriteRun(
                recipeSpec -> recipeSpec.recipe(new MetadataCollector(true)),
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
                pomXml(POM_XML, "<!--~~(" + JsonUtils.toJson(EXPECTED_POM_METADATA) + ")~~>-->" + POM_XML));
        PluginMetadata pluginMetadata = new PluginMetadata().refresh();
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.JENKINSFILE));
        Set<JDK> jdkVersion = pluginMetadata.getJdks();
        assertEquals(2, jdkVersion.size());
    }
}
