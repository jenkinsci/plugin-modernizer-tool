package io.jenkins.tools.pluginmodernizer.core.extractor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FindSourceFiles;
import org.openrewrite.Preconditions;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.Markers;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Parent;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.tree.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "false, positive")
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
        int jdkVersion;
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
                    groovyIsoVisitor.visit(tree, ctx);
                }
                return tree;
            }

            final TreeVisitor<?, ExecutionContext> groovyIsoVisitor = Preconditions.check(
                    new FindSourceFiles("**/Jenkinsfile"), new GroovyIsoVisitor<ExecutionContext>() {
                        @Override
                        public J.@NotNull MethodInvocation visitMethodInvocation(
                                J.@NotNull MethodInvocation method, @NotNull ExecutionContext ctx) {
                            int jdkVersion = Integer.MAX_VALUE;
                            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                            if ("buildPlugin".equals(m.getSimpleName())) {
                                List<Expression> args = m.getArguments();

                                for (Expression arg : args) {
                                    if (arg instanceof G.MapEntry entry) {
                                        if ("configurations".equals(((J.Literal) entry.getKey()).getValue())) {
                                            if (entry.getValue() instanceof G.ListLiteral listLiteral) {
                                                for (Expression expression : listLiteral.getElements()) {
                                                    if (expression instanceof G.MapLiteral mapLiteral) {

                                                        for (Expression mapExpr : mapLiteral.getElements()) {
                                                            if (mapExpr instanceof G.MapEntry mapEntry) {

                                                                J.Literal key = (J.Literal) mapEntry.getKey();
                                                                J.Literal value = (J.Literal) mapEntry.getValue();

                                                                if ("jdk".equals(key.getValue())) {
                                                                    int currentJdkVersion =
                                                                            Integer.parseInt(value.getValue()
                                                                                    .toString());
                                                                    if (currentJdkVersion < jdkVersion) {
                                                                        jdkVersion = currentJdkVersion;
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            acc.jdkVersion = jdkVersion;
                            return m;
                        }
                    });
        };
    }

    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getVisitor(@NotNull Metadata acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, @NotNull ExecutionContext ctx) {
                return mavenIsoVisitor.visit(tree, ctx);
            }

            final TreeVisitor<?, ExecutionContext> mavenIsoVisitor = new MavenIsoVisitor<>() {
                @Override
                public Xml.@NotNull Document visitDocument(
                        Xml.@NotNull Document document, @NotNull ExecutionContext ctx) {

                    // Ensure maven resolution result is present
                    Markers markers = document.getMarkers();
                    Optional<MavenResolutionResult> mavenResolutionResult =
                            markers.findFirst(MavenResolutionResult.class);
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

                    PluginMetadata pluginMetadata = new PluginMetadata();

                    // Construct the plugin metadata
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
                    pluginMetadata.setJdkVersion(acc.jdkVersion);

                    // Write the metadata to a file for later use by the plugin modernizer.
                    pluginMetadata.save();
                    LOG.debug("Plugin metadata written to {}", pluginMetadata.getRelativePath());

                    return document;
                }
            };
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
