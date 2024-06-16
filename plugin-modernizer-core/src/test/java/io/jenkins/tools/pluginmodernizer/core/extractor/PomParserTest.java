package io.jenkins.tools.pluginmodernizer.core.extractor;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

public class PomParserTest implements RewriteTest {
    @Override
    public void defaults (RecipeSpec spec) {
        spec.recipe(new PomParser());
    }

    @Test
    void testPlugin() {
        // language=xml
        rewriteRun(pomXml(
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
                        
                          <licenses>
                            <license>
                              <name>GPL v2.0 License</name>
                              <url>http://www.gnu.org/licenses/old-licenses/gpl-2.0-standalone.html</url>
                            </license>
                          </licenses>
                        
                          <developers>
                            <developer>
                              <id>markyjackson-taulia</id>
                              <name>Marky Jackson</name>
                              <email>marky.r.jackson@gmail.com</email>
                            </developer>
                          </developers>
                        
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
                              <groupId>io.jenkins.plugins</groupId>
                              <artifactId>javax-activation-api</artifactId>
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
                                <!-- Provided by javax-activation-api plugin -->
                                <exclusion>
                                  <groupId>com.sun.activation</groupId>
                                  <artifactId>jakarta.activation</artifactId>
                                </exclusion>
                              </exclusions>
                            </dependency>
                            <dependency>
                              <groupId>org.jenkins-ci.plugins</groupId>
                              <artifactId>apache-httpcomponents-client-4-api</artifactId>
                            </dependency>
                            <dependency>
                              <groupId>org.jenkins-ci.plugins</groupId>
                              <artifactId>credentials</artifactId>
                            </dependency>
                            <dependency>
                              <groupId>net.karneim</groupId>
                              <artifactId>pojobuilder</artifactId>
                              <version>4.3.0</version>
                              <!-- 'provided' scope because this is only needed during compilation -->
                              <scope>provided</scope>
                            </dependency>
                            <dependency>
                              <groupId>io.jenkins</groupId>
                              <artifactId>configuration-as-code</artifactId>
                              <scope>test</scope>
                            </dependency>
                            <dependency>
                              <groupId>org.postgresql</groupId>
                              <artifactId>postgresql</artifactId>
                              <version>42.7.3</version>
                              <scope>test</scope>
                            </dependency>
                          </dependencies>
                        
                          <!-- get every artifact through repo.jenkins-ci.org, which proxies all the artifacts that we need -->
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
                        
                          <build>
                            <plugins>
                              <plugin>
                                <artifactId>maven-enforcer-plugin</artifactId>
                                <executions>
                                  <execution>
                                    <id>display-info</id>
                                    <configuration>
                                      <rules>
                                        <requireUpperBoundDeps>
                                          <excludes combine.children="append">
                                            <exclude>com.sun.activation:jakarta.activation</exclude>
                                            <exclude>jakarta.xml.bind:jakarta.xml.bind-api</exclude>
                                            <exclude>javax.servlet:javax.servlet-api</exclude>
                                          </excludes>
                                        </requireUpperBoundDeps>
                                      </rules>
                                    </configuration>
                                  </execution>
                                </executions>
                              </plugin>
                            </plugins>
                          </build>
                        
                          <profiles>
                            <profile>
                              <id>integration-test</id>
                              <properties>
                                <gitlab.version>8.17.4</gitlab.version>
                                <postgres.version>9.5-1</postgres.version>
                              </properties>
                              <build>
                                <plugins>
                                  <plugin>
                                    <groupId>org.apache.maven.plugins</groupId>
                                    <artifactId>maven-surefire-plugin</artifactId>
                                    <configuration>
                                      <skip>true</skip>
                                    </configuration>
                                  </plugin>
                                  <plugin>
                                    <groupId>org.apache.maven.plugins</groupId>
                                    <artifactId>maven-failsafe-plugin</artifactId>
                                    <version>3.2.5</version>
                                    <executions>
                                      <execution>
                                        <goals>
                                          <goal>integration-test</goal>
                                          <goal>verify</goal>
                                        </goals>
                                        <configuration>
                                          <systemProperties>
                                            <property>
                                              <name>gitlab.http.port</name>
                                              <value>${gitlab.http.port}</value>
                                            </property>
                                            <property>
                                              <name>postgres.port</name>
                                              <value>${postgres.port}</value>
                                            </property>
                                          </systemProperties>
                                        </configuration>
                                      </execution>
                                    </executions>
                                  </plugin>
                                </plugins>
                              </build>
                            </profile>
                          </profiles>
                        </project>
                        
                        """
        ));
    }
}
