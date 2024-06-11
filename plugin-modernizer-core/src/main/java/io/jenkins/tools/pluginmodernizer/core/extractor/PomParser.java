package io.jenkins.tools.pluginmodernizer.core.extractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class PomParser {

    private static final Logger logger = Logger.getLogger(PomParser.class.getName());

    private final Document doc;
    private final XPath xpath;
    private final Map<String, String> properties;

    public PomParser(String filePath) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // Secure configuration to prevent XXE
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.parse(filePath);
            XPathFactory xPathFactory = XPathFactory.newInstance();
            xpath = xPathFactory.newXPath();
            properties = parseProperties();
            properties.putAll(parseElementsForPlaceholders());
        } catch (Exception e) {
            handleException("Error while parsing the XML file", e);
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> parseProperties() {
        try {
            Map<String, String> props = new HashMap<>();
            NodeList nodeList = (NodeList) xpath.evaluate("//*[local-name()='project']/*[local-name()='properties']/*", doc, XPathConstants.NODESET);
            for (int i = 0; i < nodeList.getLength(); i++) {
                String name = nodeList.item(i).getNodeName();
                String value = nodeList.item(i).getTextContent();
                props.put(name, value);
            }
            return props;
        } catch (Exception e) {
            handleException("Error while parsing properties", e);
            return Collections.emptyMap(); // Return empty map to avoid null reference
        }
    }

    private Map<String, String> parseElementsForPlaceholders() throws Exception {
        Map<String, String> elements = new HashMap<>();
        elements.put("project.groupId", parseSingleElement("//*[local-name()='project']/*[local-name()='groupId']"));
        elements.put("project.artifactId", parseSingleElement("//*[local-name()='project']/*[local-name()='artifactId']"));
        elements.put("project.version", parseSingleElement("//*[local-name()='project']/*[local-name()='version']"));
        elements.put("project.parent.groupId", parseSingleElement("//*[local-name()='project']/*[local-name()='parent']/*[local-name()='groupId']"));
        elements.put("project.parent.artifactId", parseSingleElement("//*[local-name()='project']/*[local-name()='parent']/*[local-name()='artifactId']"));
        elements.put("project.parent.version", parseSingleElement("//*[local-name()='project']/*[local-name()='parent']/*[local-name()='version']"));
        return elements;
    }

    private String resolvePlaceholders(String value) {
        if (value == null || !value.contains("${")) {
            return value;
        }
        String resolvedValue = value;
        boolean placeholdersResolved;
        do {
            placeholdersResolved = false;
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String placeholder = "${" + entry.getKey() + "}";
                if (resolvedValue.contains(placeholder)) {
                    resolvedValue = resolvedValue.replace(placeholder, entry.getValue());
                    placeholdersResolved = true;
                }
            }
        } while (placeholdersResolved && resolvedValue.contains("${"));
        return resolvedValue;
    }

    public Parent getParentDetails() throws Exception {
        String groupId = resolvePlaceholders(parseSingleElement("//*[local-name()='project']/*[local-name()='parent']/*[local-name()='groupId']"));
        String artifactId = resolvePlaceholders(parseSingleElement("//*[local-name()='project']/*[local-name()='parent']/*[local-name()='artifactId']"));
        String version = resolvePlaceholders(parseSingleElement("//*[local-name()='project']/*[local-name()='parent']/*[local-name()='version']"));
        return new Parent(groupId, artifactId, version);
    }

    public String getArtifactId() throws Exception {
        return resolvePlaceholders(parseSingleElement("//*[local-name()='project']/*[local-name()='artifactId']"));
    }

    public String getVersion() throws Exception {
        return resolvePlaceholders(parseSingleElement("//*[local-name()='project']/*[local-name()='version']"));
    }

    public String getUrl() throws Exception {
        return resolvePlaceholders(parseSingleElement("//*[local-name()='project']/*[local-name()='url']"));
    }

    public String getPackagingType() throws Exception {
        return resolvePlaceholders(parseSingleElement("//*[local-name()='project']/*[local-name()='packaging']"));
    }

    public List<String> getLicenses() throws Exception {
        return parseMultipleElements("//*[local-name()='project']/*[local-name()='licenses']/*[local-name()='license']/*[local-name()='name']");
    }

    public List<Developer> getDevelopers() throws Exception {
        List<Developer> developers = new ArrayList<>();
        NodeList nodeList = (NodeList) xpath.evaluate("//*[local-name()='project']/*[local-name()='developers']/*[local-name()='developer']", doc, XPathConstants.NODESET);
        for (int i = 0; i < nodeList.getLength(); i++) {
            String id = resolvePlaceholders(xpath.evaluate("*[local-name()='id']", nodeList.item(i)));
            String name = resolvePlaceholders(xpath.evaluate("*[local-name()='name']", nodeList.item(i)));
            String email = resolvePlaceholders(xpath.evaluate("*[local-name()='email']", nodeList.item(i)));
            String role = resolvePlaceholders(xpath.evaluate("*[local-name()='roles']/*[local-name()='role']", nodeList.item(i)));
            developers.add(new Developer(id, name, email, role));
        }
        return developers;
    }

    public Scm getScmDetails() throws Exception {
        String connection = resolvePlaceholders(parseSingleElement("//*[local-name()='project']/*[local-name()='scm']/*[local-name()='connection']"));
        String developerConnection = resolvePlaceholders(parseSingleElement("//*[local-name()='project']/*[local-name()='scm']/*[local-name()='developerConnection']"));
        String url = resolvePlaceholders(parseSingleElement("//*[local-name()='project']/*[local-name()='scm']/*[local-name()='url']"));
        String tag = resolvePlaceholders(parseSingleElement("//*[local-name()='project']/*[local-name()='scm']/*[local-name()='tag']"));
        return new Scm(connection, developerConnection, url, tag);
    }

    public JenkinsBom getBomDetails() throws Exception {
        String groupId = resolvePlaceholders(parseSingleElement("//*[local-name()='project']/*[local-name()='dependencyManagement']/*[local-name()='dependencies']/*[local-name()='dependency']/*[local-name()='groupId']"));
        String artifactId = resolvePlaceholders(parseSingleElement("//*[local-name()='project']/*[local-name()='dependencyManagement']/*[local-name()='dependencies']/*[local-name()='dependency']/*[local-name()='artifactId']"));
        String version = resolvePlaceholders(parseSingleElement("//*[local-name()='project']/*[local-name()='dependencyManagement']/*[local-name()='dependencies']/*[local-name()='dependency']/*[local-name()='version']"));
        String type = resolvePlaceholders(parseSingleElement("//*[local-name()='project']/*[local-name()='dependencyManagement']/*[local-name()='dependencies']/*[local-name()='dependency']/*[local-name()='type']"));
        String scope = resolvePlaceholders(parseSingleElement("//*[local-name()='project']/*[local-name()='dependencyManagement']/*[local-name()='dependencies']/*[local-name()='dependency']/*[local-name()='scope']"));
        return new JenkinsBom(groupId, artifactId, version, type, scope);
    }

    public List<Dependency> getDependencies() throws Exception {
        List<Dependency> dependencies = new ArrayList<>();
        NodeList nodeList = (NodeList) xpath.evaluate("//*[local-name()='project']/*[local-name()='dependencies']/*[local-name()='dependency']", doc, XPathConstants.NODESET);
        for (int i = 0; i < nodeList.getLength(); i++) {
            String groupId = resolvePlaceholders(xpath.evaluate("*[local-name()='groupId']", nodeList.item(i)));
            String artifactId = resolvePlaceholders(xpath.evaluate("*[local-name()='artifactId']", nodeList.item(i)));
            String version = resolvePlaceholders(xpath.evaluate("*[local-name()='version']", nodeList.item(i)));
            String scope = resolvePlaceholders(xpath.evaluate("*[local-name()='scope']", nodeList.item(i)));
            String type = resolvePlaceholders(xpath.evaluate("*[local-name()='type']", nodeList.item(i)));
            dependencies.add(new Dependency(groupId, artifactId, version, scope, type));
        }
        return dependencies;
    }

    public String getJenkinsVersion() {
        return properties.get("jenkins.version");
    }

    private String parseSingleElement(String expression) throws Exception {
        return (String) xpath.evaluate(expression, doc, XPathConstants.STRING);
    }

    private List<String> parseMultipleElements(String expression) throws Exception {
        List<String> elements = new ArrayList<>();
        NodeList nodeList = (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);
        for (int i = 0; i < nodeList.getLength(); i++) {
            elements.add(resolvePlaceholders(nodeList.item(i).getTextContent()));
        }
        return elements;
    }

    private void handleException(String message, Exception e) {
        logger.log(Level.SEVERE, message, e);
    }
}
