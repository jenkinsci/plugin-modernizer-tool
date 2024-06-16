package io.jenkins.tools.pluginmodernizer.core.extractor;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.Markers;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.License;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.tree.Xml;

import java.util.List;
import java.util.Optional;

public class PomParser extends Recipe {
    private static String jenkinsVersion;
    private static boolean isLicenses = false; // To add license template
    private static boolean isDevelopers = false; // To remove developers section
    @Override
    public String getDisplayName() {
        return "Pom Parser";
    }

    @Override
    public String getDescription() {
        return "Extracts Metadata from pom file.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Markers markers = document.getMarkers();
                Optional<MavenResolutionResult>mavenResolutionResult = markers.findFirst(MavenResolutionResult.class);
                if(mavenResolutionResult.isEmpty()) {
                    return document;
                }
                MavenResolutionResult resolutionResult = mavenResolutionResult.orElse(null);
                ResolvedPom resolvedPom = resolutionResult.getPom();
                Pom pom = resolvedPom.getRequested();

                jenkinsVersion = resolvedPom.getManagedVersion("org.jenkins-ci.main",
                        "jenkins-core", null, null);
                System.out.println(jenkinsVersion);
                TagExtractor tagExtractor = new TagExtractor();
                tagExtractor.visit(document, ctx);
                if(isLicenses) {
                    List<License> licenses = pom.getLicenses();
                    for(License license : licenses) {
                        System.out.println(license.getType());
                    }
                }

                // Action: search for java.level in props, Usage: remove it

                return document;
            }
        };
    }

    private static class TagExtractor extends MavenIsoVisitor<ExecutionContext> {
        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag t = super.visitTag(tag, ctx);
            if ("licenses".equals(tag.getName())) {
                isLicenses = true;
            } else if ("developers".equals(tag.getName())) {
                isDevelopers = true;
            }
            // Action: fetch scm connection, Usage: Update scm url from git to http protocol
            return t;
        }
    }
}
