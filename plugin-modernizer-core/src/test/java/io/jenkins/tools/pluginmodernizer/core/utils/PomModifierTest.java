package io.jenkins.tools.pluginmodernizer.core.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PomModifierTest {

    private static final String TEST_POM_PATH = "src/test/resources/test-pom.xml";
    private static final String OUTPUT_POM_PATH = "src/test/resources/output-pom.xml";

    @BeforeEach
    public void setUp() throws Exception {
        Files.copy(Path.of(TEST_POM_PATH), Path.of(OUTPUT_POM_PATH));
    }

    @Test
    public void testRemoveOffendingProperties() throws Exception {
        PomModifier pomModifier = new PomModifier(OUTPUT_POM_PATH);
        pomModifier.removeOffendingProperties();
        pomModifier.savePom(OUTPUT_POM_PATH);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new File(OUTPUT_POM_PATH));
        doc.getDocumentElement().normalize();

        Element propertiesElement = (Element) doc.getElementsByTagName("properties").item(0);
        assertTrue(propertiesElement.getElementsByTagName("project.basedir").getLength() == 0);
        assertTrue(propertiesElement.getElementsByTagName("basedir").getLength() == 0);
    }

    @Test
    public void testAddParentPom() throws Exception {
        PomModifier pomModifier = new PomModifier(OUTPUT_POM_PATH);
        pomModifier.addParentPom("io.jenkins.plugin-modernizer", "plugin-modernizer-parent-pom", "1.0");
        pomModifier.savePom(OUTPUT_POM_PATH);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new File(OUTPUT_POM_PATH));
        doc.getDocumentElement().normalize();

        Element parentElement = (Element) doc.getElementsByTagName("parent").item(0);
        assertEquals("io.jenkins.plugin-modernizer", parentElement.getElementsByTagName("groupId").item(0).getTextContent());
        assertEquals("plugin-modernizer-parent-pom", parentElement.getElementsByTagName("artifactId").item(0).getTextContent());
        assertEquals("1.0", parentElement.getElementsByTagName("version").item(0).getTextContent());
    }

    @Test
    public void testUpdateJenkinsMinimalVersion() throws Exception {
        PomModifier pomModifier = new PomModifier(OUTPUT_POM_PATH);
        pomModifier.updateJenkinsMinimalVersion("2.289.1");
        pomModifier.savePom(OUTPUT_POM_PATH);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new File(OUTPUT_POM_PATH));
        doc.getDocumentElement().normalize();

        Element propertiesElement = (Element) doc.getElementsByTagName("properties").item(0);
        assertEquals("2.289.1", propertiesElement.getElementsByTagName("jenkins.version").item(0).getTextContent());
    }
}
