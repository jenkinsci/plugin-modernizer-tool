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
    WORKFLOW_CD(".github/workflows/cd.yml"),
    ;

    // TODO: More file here (dependabot, codeowners, readme, cd, release drafter, .mvn files, etc...)

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
