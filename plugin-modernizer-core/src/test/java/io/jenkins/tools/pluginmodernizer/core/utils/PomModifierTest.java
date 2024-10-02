package io.jenkins.tools.pluginmodernizer.core.utils;

import static java.nio.file.Files.createTempFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jenkins.tools.pluginmodernizer.core.config.Settings;
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
     * Sets up the test environment by copying the test POM to a temporary file.
     *
     * @throws Exception if an error occurs during setup
     */
    @BeforeEach
    public void setUp() throws Exception {
        Path tempFile = new File(OUTPUT_POM_PATH).toPath();
        Files.copy(Path.of(TEST_POM_PATH), tempFile, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Setup completed, copied test POM to temporary file: " + tempFile.toString());
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
        assertTrue(propertiesElement.getElementsByTagName("java.level").getLength() == 0);
        assertTrue(propertiesElement
                        .getElementsByTagName("jenkins-test-harness.version")
                        .getLength()
                == 0);
        logger.info("Offending properties removed successfully");
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
        pomModifier.updateJenkinsMinimalVersion(Settings.JENKINS_MINIMUM_VERSION);
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
        assertEquals(Settings.JENKINS_MINIMUM_VERSION, jenkinsVersion);
    }

    /**
     * Tests the addBom method of PomModifier.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void testAddBom() throws Exception {
        PomModifier pomModifier = new PomModifier(OUTPUT_POM_PATH);
        pomModifier.addBom("io.jenkins.tools.bom", Settings.BOM_BASE, Settings.BOM_VERSION);
        pomModifier.savePom(OUTPUT_POM_PATH);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new File(OUTPUT_POM_PATH));
        doc.getDocumentElement().normalize();

        NodeList dependencyManagementList = doc.getElementsByTagName("dependencyManagement");
        assertTrue(dependencyManagementList.getLength() > 0);

        Element dependencyManagementElement = (Element) dependencyManagementList.item(0);
        NodeList dependenciesList = dependencyManagementElement.getElementsByTagName("dependencies");
        assertTrue(dependenciesList.getLength() > 0);

        Element dependenciesElement = (Element) dependenciesList.item(0);
        NodeList dependencyList = dependenciesElement.getElementsByTagName("dependency");
        boolean bomFound = false;

        for (int i = 0; i < dependencyList.getLength(); i++) {
            Element dependencyElement = (Element) dependencyList.item(i);
            String groupId =
                    dependencyElement.getElementsByTagName("groupId").item(0).getTextContent();
            String artifactId =
                    dependencyElement.getElementsByTagName("artifactId").item(0).getTextContent();
            String version =
                    dependencyElement.getElementsByTagName("version").item(0).getTextContent();
            String scope =
                    dependencyElement.getElementsByTagName("scope").item(0).getTextContent();
            String type = dependencyElement.getElementsByTagName("type").item(0).getTextContent();

            if (groupId.equals("io.jenkins.tools.bom")
                    && artifactId.equals(Settings.BOM_BASE)
                    && version.equals(Settings.BOM_VERSION)
                    && scope.equals("import")
                    && type.equals("pom")) {
                bomFound = true;
                break;
            }
        }

        assertTrue(bomFound, "BOM dependency not found in the POM file");
    }
}
