package io.jenkins.tools.pluginmodernizer.core.extractor;

/**
 * An archetype repository file with location
 * Used to create metadata and store information about file presence or changes
 */
public enum ArchetypeCommonFile {

    /**
     * The Jenkinsfile in the root repository
     */
    JENKINSFILE("Jenkinsfile"),

    /**
     * The pom.xml file in the root repository
     */
    POM("pom.xml"),

    /**
     * The workflow CD file
     */
    WORKFLOW_CD(".github/workflows/cd.yaml"),

    /**
     * The workflow Jenkins security scan
     */
    WORKFLOW_SECURITY(".github/workflows/jenkins-security-scan.yml"),

    /**
     * Release drafter file
     */
    RELEASE_DRAFTER(".github/release-drafter.yml"),

    /**
     * Pull request template file
     */
    PULL_REQUEST_TEMPLATE(".github/PULL_REQUEST_TEMPLATE.md"),

    /**
     * Codeowners file
     */
    CODEOWNERS(".github/CODEOWNERS"),

    /**
     * Index jelly file
     */
    INDEX_JELLY("src/main/resources/index.jelly"),

    /**
     * .gitignore file
     */
    GITIGNORE(".gitignore"),

    /**
     * License file
     */
    LICENSE("LICENSE.md"),

    /**
     * Contributing file
     */
    CONTRIBUTING("CONTRIBUTING.md"),

    /**
     * Dependabot configuration file
     */
    DEPENDABOT(".github/dependabot.yml"),

    /**
     * Renovate configuration file.
     * Not in archetype but to skip plugins using a different bot for updates
     */
    RENOVATE("renovate.json"),

    /**
     * Maven extensions file
     */
    MAVEN_EXTENSIONS(".mvn/extensions.xml"),

    /**
     * Maven configuration file
     */
    MAVEN_CONFIG(".mvn/maven.config"),

    /**
     * README
     */
    README("README.md"),
    ;

    /**
     * Relative path
     */
    private final String path;

    /**
     * Private constructor
     * @param value the path
     */
    ArchetypeCommonFile(String value) {
        this.path = value;
    }

    /**
     * Return the enum from a file path or null if not found
     * @param file the file path
     * @return the enum or null
     */
    public static ArchetypeCommonFile fromFile(String file) {
        for (ArchetypeCommonFile f : ArchetypeCommonFile.values()) {
            if (f.getPath().equals(file)) {
                return f;
            }
        }
        return null;
    }

    /**
     * Get the path
     * @return the path
     */
    public String getPath() {
        return path;
    }
}
