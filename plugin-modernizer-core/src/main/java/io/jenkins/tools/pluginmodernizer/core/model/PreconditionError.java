package io.jenkins.tools.pluginmodernizer.core.model;

import java.util.function.BiFunction;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import org.w3c.dom.Document;

/**
 * Enum to represent the precondition errors preventing any modernization process
 * Generally, these are the errors that need to be fixed before applying any modernization (very old plugin)
 * We can provide in future version a way to fix these errors automatically (without OpenRewrite) by adding a fix function
 * on this enum
 */
public enum PreconditionError {

    /**
     * No pom file found
     */
    NO_POM(
            (document, xpath) -> {
                return document == null;
            },
            "No pom file found"),

    /**
     * If the plugin has HTTP repositories preventing modernization
     */
    MAVEN_REPOSITORIES_HTTP(
            (document, xpath) -> {
                if (document == null) {
                    return false;
                }
                try {
                    Double nonHttpsRepositories = (Double) xpath.evaluate(
                            "count(//*[local-name()='project']/*[local-name()='repositories']/*[local-name()='repository']/*[local-name()='url' and not(starts-with(., 'https'))])",
                            document,
                            XPathConstants.NUMBER);
                    return nonHttpsRepositories != null && !nonHttpsRepositories.equals(0.0);
                } catch (Exception e) {
                    return false;
                }
            },
            "Found non-https repository URL in pom file preventing maven older than 3.8.1"),

    /**
     * If the plugin has an older Java version preventing modernization
     */
    OLDER_JAVA_VERSION(
            (document, xpath) -> {
                if (document == null) {
                    return false;
                }
                try {
                    String javaVersion = (String) xpath.evaluate(
                            "//*[local-name()='project']/*[local-name()='properties']/*[local-name()='java.level']",
                            document,
                            XPathConstants.STRING);
                    return javaVersion != null
                            && (javaVersion.equals("7") || javaVersion.equals("6") || javaVersion.equals("5"));
                } catch (Exception e) {
                    return false;
                }
            },
            "Found older Java version in pom file preventing using recent Maven older than 3.9.x"),

    /**
     * If the plugin has missing relative path preventing modernization
     */
    MISSING_RELATIVE_PATH(
            (document, xpath) -> {
                try {
                    Double parentRelativePath = (Double) xpath.evaluate(
                            "count(//*[local-name()='project']/*[local-name()='parent']/*[local-name()='relativePath'])",
                            document,
                            XPathConstants.NUMBER);
                    return parentRelativePath == null || parentRelativePath.equals(0.0);
                } catch (Exception e) {
                    return false;
                }
            },
            "Missing relative path in pom file preventing parent download");

    /**
     * Predicate to check if the flag is applicable for the given Document and XPath
     */
    private final BiFunction<Document, XPath, Boolean> isApplicable;

    /**
     * Error message
     */
    private final String error;

    /**
     * Constructor
     * @param isApplicable Predicate to check if the flag is applicable for the given XML document
     */
    PreconditionError(BiFunction<Document, XPath, Boolean> isApplicable, String error) {
        this.isApplicable = isApplicable;
        this.error = error;
    }

    /**
     * Check if the flag is applicable for the given Document and XPath
     * @param Document the XML document
     * @param xpath the XPath object
     * @return true if the flag is applicable, false otherwise
     */
    public boolean isApplicable(Document Document, XPath xpath) {
        return isApplicable.apply(Document, xpath);
    }

    /**
     * Get the error message
     * @return the error message
     */
    public String getError() {
        return error;
    }
}
