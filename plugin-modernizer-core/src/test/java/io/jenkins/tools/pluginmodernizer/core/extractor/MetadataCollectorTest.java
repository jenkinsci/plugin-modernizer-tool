package io.jenkins.tools.pluginmodernizer.core.extractor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.maven.Assertions.pomXml;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

public class MetadataCollectorTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MetadataCollector());
    }

    @Test
    void testPlugin() throws Exception {

        rewriteRun(
                // language=groovy
                groovy(
                        """
                          buildPlugin()
                          """,
                        spec -> spec.path("Jenkinsfile")),
                // language=xml
                pomXml(
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
                        """));
        PluginMetadata pluginMetadata = new PluginMetadata().refresh();
        String pluginName = pluginMetadata.getPluginName();
        assertEquals("GitLab Plugin", pluginName);
        assertEquals("4.80", pluginMetadata.getParentVersion());
        String jenkinsVersion = pluginMetadata.getJenkinsVersion();
        assertEquals("2.426.3", jenkinsVersion);
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
    }
}
