package io.jenkins.tools.pluginmodernizer.core.extractor;

import com.google.gson.Gson;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Parent;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.tree.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetadataCollector extends ScanningRecipe<MetadataCollector.Metadata> {

    /**
     * LOGGER.
     */
    private static final Logger LOG = LoggerFactory.getLogger(MetadataCollector.class);

    @Override
    public @NotNull String getDisplayName() {
        return "Plugin metadata extractor";
    }

    @Override
    public @NotNull String getDescription() {
        return "Extracts metadata from plugin.";
    }

    public static class Metadata {
        boolean hasJenkinsfile = false;
    }

    @Override
    public @NotNull Metadata getInitialValue(@NotNull ExecutionContext ctx) {
        return new Metadata();
    }

    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getScanner(@NotNull Metadata acc) {
        return new TreeVisitor<>() {
            @Override
            public Tree visit(@Nullable Tree tree, @NotNull ExecutionContext ctx) {
                if (tree == null) {
                    return null;
                }
                SourceFile sourceFile = (SourceFile) tree;
                if (sourceFile.getSourcePath().endsWith("Jenkinsfile")) {
                    acc.hasJenkinsfile = true;
                }
                return tree;
            }
        };
    }

    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getVisitor(@NotNull Metadata acc) {
        return new MavenIsoVisitor<>() {
            @Override
            public Xml.@NotNull Document visitDocument(Xml.@NotNull Document document, @NotNull ExecutionContext ctx) {

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

                TagExtractor tagExtractor = new TagExtractor();
                tagExtractor.visit(document, ctx);

                // Remove the properties that are not needed and specific to the build environment
                Map<String, String> properties = pom.getProperties();
                properties.remove("project.basedir");
                properties.remove("basedir");

                // Construct the plugin metadata
                PluginMetadata pluginMetadata = new PluginMetadata();
                pluginMetadata.setPluginName(pom.getName());
                Parent parent = pom.getParent();
                if (parent != null) {
                    pluginMetadata.setParentVersion(parent.getVersion());
                }
                pluginMetadata.setProperties(properties);
                pluginMetadata.setJenkinsVersion(
                        resolvedPom.getManagedVersion("org.jenkins-ci.main", "jenkins-core", null, null));
                pluginMetadata.setHasJavaLevel(pom.getProperties().get("java.level") != null);
                pluginMetadata.setUsesScmHttps(tagExtractor.usesScmHttps());
                pluginMetadata.setUsesRepositoriesHttps(tagExtractor.usesRepositoriesHttps());
                pluginMetadata.setHasJenkinsfile(acc.hasJenkinsfile);

                // Write the metadata to a file for later use by the plugin modernizer.
                Path path = Paths.get("target/pluginMetadata.json");
                try (OutputStreamWriter writer =
                        new OutputStreamWriter(new FileOutputStream(path.toFile()), StandardCharsets.UTF_8)) {
                    Gson gson = new Gson();
                    gson.toJson(pluginMetadata, writer);
                    LOG.debug("Plugin metadata written to {}", path);
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }

                return document;
            }
        };
    }

    /**
     * Maven visitor to extract tags from pom.xml.
     */
    private static class TagExtractor extends MavenIsoVisitor<ExecutionContext> {

        /**
         * If the plugin used SCM HTTPS protocol.
         */
        private boolean usesScmHttps = false;

        /**
         * If the plugin used repositories with HTTPS protocol.
         */
        private boolean usesRepositoriesHttps = false;

        @Override
        public Xml.@NotNull Tag visitTag(Xml.@NotNull Tag tag, @NotNull ExecutionContext ctx) {
            Xml.Tag t = super.visitTag(tag, ctx);
            if ("scm".equals(tag.getName())) {
                Optional<String> connection = tag.getChildValue("connection");
                connection.ifPresent(s -> usesScmHttps = s.startsWith("scm:git:https"));
            }
            if ("repositories".equals(tag.getName())) {
                usesRepositoriesHttps = tag.getChildren().stream()
                        .filter(c -> "repository".equals(c.getName()))
                        .map(Xml.Tag.class::cast)
                        .map(r -> r.getChildValue("url").orElseThrow())
                        .allMatch(url -> url.startsWith("https"));
            }
            return t;
        }

        public boolean usesScmHttps() {
            return usesScmHttps;
        }

        public boolean usesRepositoriesHttps() {
            return usesRepositoriesHttps;
        }
    }
}
