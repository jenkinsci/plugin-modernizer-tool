package io.jenkins.tools.pluginmodernizer.core.utils;

import static java.nio.file.Files.createTempFile;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Test class for PomModifier.
 */
public class PomModifierTest {
    private static final Logger logger = Logger.getLogger(PomModifierTest.class.getName());

    private static final String TEST_POM_PATH = "src/test/resources/test-pom.xml";
    private static final String OUTPUT_POM_PATH;

    static {
        try {
            OUTPUT_POM_PATH = Files.move(
                            createTempFile(null, null),
                            Paths.get(System.getProperty("java.io.tmpdir"), "output-pom.xml"),
                            StandardCopyOption.REPLACE_EXISTING)
                    .toAbsolutePath()
                    .toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets up the test environment by copying the test POM file to a temporary file.
     *
     * @throws Exception if an error occurs during setup
     */
    @BeforeEach
    public void setUp() throws Exception {
        Path tempFile = new File(OUTPUT_POM_PATH).toPath();
        Files.copy(Path.of(TEST_POM_PATH), tempFile, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Setup completed, copied test POM to temporary file: " + tempFile);
    }

    /**
     * Tests the removeOffendingProperties method of PomModifier.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void testRemoveOffendingProperties() throws Exception {
        PomModifier pomModifier = new PomModifier(OUTPUT_POM_PATH);
        pomModifier.removeOffendingProperties();
        pomModifier.savePom(OUTPUT_POM_PATH);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new File(OUTPUT_POM_PATH));
        doc.getDocumentElement().normalize();

        Element propertiesElement =
                (Element) doc.getElementsByTagName("properties").item(0);
        assertEquals(
                0,
                propertiesElement.getElementsByTagName("java.level").getLength(),
                "java.level property should be removed");
        assertEquals(
                0,
                propertiesElement
                        .getElementsByTagName("jenkins-test-harness.version")
                        .getLength(),
                "jenkins-test-harness.version property should be removed");

        // Verify that comments associated with the removed properties are also removed
        String pomContent = new String(Files.readAllBytes(Paths.get(OUTPUT_POM_PATH)));
        assertFalse(
                pomContent.contains("Java Level to use. Java 7 required when using core >= 1.612"),
                "Comment for java.level should be removed");
        assertFalse(
                pomContent.contains("Jenkins Test Harness version you use to test the plugin."),
                "Comment for jenkins-test-harness.version should be removed");
    }

    /**
     * Tests the updateParentPom method of PomModifier.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void testUpdateParentPom() throws Exception {
        PomModifier pomModifier = new PomModifier(OUTPUT_POM_PATH);
        pomModifier.updateParentPom("org.jenkins-ci.plugins", "plugin", "4.80");
        pomModifier.savePom(OUTPUT_POM_PATH);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new File(OUTPUT_POM_PATH));
        doc.getDocumentElement().normalize();

        Element parentElement = (Element) doc.getElementsByTagName("parent").item(0);
        assertEquals(
                "org.jenkins-ci.plugins",
                parentElement.getElementsByTagName("groupId").item(0).getTextContent());
        assertEquals(
                "plugin",
                parentElement.getElementsByTagName("artifactId").item(0).getTextContent());
        assertEquals(
                "4.80", parentElement.getElementsByTagName("version").item(0).getTextContent());
    }

    /**
     * Tests the updateJenkinsMinimalVersion method of PomModifier.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void testUpdateJenkinsMinimalVersion() throws Exception {
        PomModifier pomModifier = new PomModifier(OUTPUT_POM_PATH);
        pomModifier.updateJenkinsMinimalVersion("2.462.2");
        pomModifier.savePom(OUTPUT_POM_PATH);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new File(OUTPUT_POM_PATH));
        doc.getDocumentElement().normalize();

        Element propertiesElement =
                (Element) doc.getElementsByTagName("properties").item(0);
        String jenkinsVersion = propertiesElement
                .getElementsByTagName("jenkins.version")
                .item(0)
                .getTextContent();
        logger.info("Jenkins version found: " + jenkinsVersion);
        assertEquals("2.462.2", jenkinsVersion);
    }

    /**
     * Tests the replaceHttpWithHttps method of PomModifier.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void testReplaceHttpWithHttps() throws Exception {
        PomModifier pomModifier = new PomModifier(OUTPUT_POM_PATH);
        pomModifier.replaceHttpWithHttps();
        pomModifier.savePom(OUTPUT_POM_PATH);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new File(OUTPUT_POM_PATH));
        doc.getDocumentElement().normalize();

        NodeList repositoryUrls = doc.getElementsByTagName("url");
        for (int i = 0; i < repositoryUrls.getLength(); i++) {
            Node urlNode = repositoryUrls.item(i);
            String url = urlNode.getTextContent();
            assertTrue(url.startsWith("https://"), "URL should start with https://");
        }
    }

    /**
     * Tests the addRelativePath method of PomModifier.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void testAddRelativePath() throws Exception {
        PomModifier pomModifier = new PomModifier(OUTPUT_POM_PATH);
        pomModifier.addRelativePath();
        pomModifier.savePom(OUTPUT_POM_PATH);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new File(OUTPUT_POM_PATH));
        doc.getDocumentElement().normalize();

        NodeList relativePathList = doc.getElementsByTagName("relativePath");
        assertEquals(1, relativePathList.getLength(), "There should be one relativePath element");
        Node relativePathNode = relativePathList.item(0);
        assertTrue(relativePathNode.getTextContent().isEmpty(), "The relativePath element should be self-closing");
    }
}
