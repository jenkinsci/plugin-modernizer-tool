package io.jenkins.tools.pluginmodernizer.core.recipes;

import java.util.Optional;
import org.openrewrite.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;

/**
 * Determines if this project is using a BOM in it's bom file
 */
public class IsUsingBom extends Recipe {

    @Override
    public String getDisplayName() {
        return "Is the project a using Jenkins bom?";
    }

    @Override
    public String getDescription() {
        return "Checks if the project is a using a Jenkins bom by the presence of io.jenkins.tools.bom group ID as managed dependency.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlVisitor<ExecutionContext>() {
            @Override
            public Xml visitDocument(Xml.Document document, ExecutionContext ctx) {

                // Ensure maven resolution result is present
                Markers markers = document.getMarkers();
                Optional<MavenResolutionResult> mavenResolutionResult = markers.findFirst(MavenResolutionResult.class);
                if (mavenResolutionResult.isEmpty()) {
                    return document;
                }
                // Get the pom
                MavenResolutionResult resolutionResult = mavenResolutionResult.get();
                ResolvedPom resolvedPom = resolutionResult.getPom();
                Pom pom = resolvedPom.getRequested();

                // Check if the project is using Jenkins bom
                boolean isUsingBom = pom.getDependencyManagement().stream()
                        .anyMatch(dependency -> dependency.getGroupId().equals("io.jenkins.tools.bom"));

                if (isUsingBom) {
                    return SearchResult.found(document, "Project is using Jenkins bom");
                }

                return document;
            }
        };
    }
}
