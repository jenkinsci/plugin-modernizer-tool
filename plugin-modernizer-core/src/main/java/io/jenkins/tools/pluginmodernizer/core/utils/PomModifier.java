package io.jenkins.tools.pluginmodernizer.core.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

public class PomModifier {

    private Document document;

    public PomModifier(String pomFilePath) {
        try {
            File pomFile = new File(pomFilePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            document = dBuilder.parse(pomFile);
            document.getDocumentElement().normalize();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeOffendingProperties() {
        NodeList propertiesList = document.getElementsByTagName("properties");
        if (propertiesList.getLength() > 0) {
            Node propertiesNode = propertiesList.item(0);
            NodeList childNodes = propertiesNode.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node node = childNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    String nodeName = node.getNodeName();
                    if (nodeName.equals("project.basedir") || nodeName.equals("basedir")) {
                        propertiesNode.removeChild(node);
                    }
                }
            }
        }
    }

    public void addParentPom(String groupId, String artifactId, String version) {
        Element parentElement = document.createElement("parent");

        Element groupIdElement = document.createElement("groupId");
        groupIdElement.appendChild(document.createTextNode(groupId));
        parentElement.appendChild(groupIdElement);

        Element artifactIdElement = document.createElement("artifactId");
        artifactIdElement.appendChild(document.createTextNode(artifactId));
        parentElement.appendChild(artifactIdElement);

        Element versionElement = document.createElement("version");
        versionElement.appendChild(document.createTextNode(version));
        parentElement.appendChild(versionElement);

        document.getDocumentElement().appendChild(parentElement);
    }

    public void updateJenkinsMinimalVersion(String version) {
        NodeList propertiesList = document.getElementsByTagName("properties");
        if (propertiesList.getLength() > 0) {
            Node propertiesNode = propertiesList.item(0);
            Element jenkinsVersionElement = document.createElement("jenkins.version");
            jenkinsVersionElement.appendChild(document.createTextNode(version));
            propertiesNode.appendChild(jenkinsVersionElement);
        }
    }

    public void savePom(String outputPath) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(new File(outputPath));
            transformer.transform(source, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
