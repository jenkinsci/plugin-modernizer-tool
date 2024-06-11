package io.jenkins.tools.pluginmodernizer.core.extractor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PomParserTest {

    private PomParser parser;

    @BeforeEach
    void setUp() throws Exception {
        parser = new PomParser("src/test/resources/test-pom.xml");
    }


    @Test
    void testGetArtifactId() throws Exception {
        String artifactId = parser.getArtifactId();
        assertEquals("schedule-build", artifactId);
    }

    @Test
    void testGetVersion() throws Exception {
        String version = parser.getVersion();
        assertEquals("999999-SNAPSHOT", version);
    }

    @Test
    void testGetParentDetails() throws Exception {
        Parent parent = parser.getParentDetails();
        assertEquals("org.jenkins-ci.plugins", parent.groupId());
        assertEquals("plugin", parent.artifactId());
        assertEquals("4.83", parent.version());
    }

    @Test
    void testGetDevelopers() throws Exception {
        List<Developer> developers = parser.getDevelopers();
        assertEquals("markewaite", developers.get(0).id());
        assertEquals("Mark Waite", developers.get(0).name());
        assertEquals("mark.earl.waite@gmail.com", developers.get(0).email());
        assertEquals("developer", developers.get(0).role());
    }

    @Test
    void testGetLicenses() throws Exception {
        List<String> licenses = parser.getLicenses();
        assertEquals(1, licenses.size());
        assertEquals("MIT License", licenses.get(0));
    }

    @Test
    void testGetScmDetails() throws Exception {
        Scm scm = parser.getScmDetails();
        assertEquals("scm:git:https://github.com/jenkinsci/schedule-build-plugin", scm.connection());
        assertEquals("scm:git:git@github.com:jenkinsci/schedule-build-plugin.git", scm.developerConnection());
        assertEquals("https://github.com/jenkinsci/schedule-build-plugin", scm.url());
        assertEquals("HEAD", scm.tag());
    }

    @Test
    void testGetDependencies() throws Exception {
        List<Dependency> dependencies = parser.getDependencies();
        assertEquals(1, dependencies.size());
        assertEquals("io.jenkins.plugins", dependencies.get(0).groupId());
        assertEquals("test-dependency", dependencies.get(0).artifactId());
        assertEquals("1.0.0", dependencies.get(0).version());
        assertEquals("compile", dependencies.get(0).scope());
        assertEquals("jar", dependencies.get(0).type());
    }

    @Test
    void testGetJenkinsVersion() {
        String jenkinsVersion = parser.getJenkinsVersion();
        assertEquals("2.440.3", jenkinsVersion);
    }

    @Test
    void testGetUrl() throws Exception {
        String url = parser.getUrl();
        assertEquals("https://github.com/jenkinsci/schedule-build-plugin", url);
    }

    @Test
    void testGetPackagingType() throws Exception {
        String packagingType = parser.getPackagingType();
        assertEquals("hpi", packagingType);
    }

    @Test
    void testGetBomDetails() throws Exception {
        JenkinsBom jenkinsBom = parser.getBomDetails();
        assertEquals("3105.v672692894683", jenkinsBom.version());
        assertEquals("io.jenkins.tools.bom", jenkinsBom.groupId());
        assertEquals("bom-2.440.x", jenkinsBom.artifactId());
        assertEquals("pom", jenkinsBom.type());
        assertEquals("import", jenkinsBom.scope());
    }
}
