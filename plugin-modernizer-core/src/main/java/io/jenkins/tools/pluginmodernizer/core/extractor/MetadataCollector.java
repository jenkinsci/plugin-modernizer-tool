package io.jenkins.tools.pluginmodernizer.core.extractor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.model.JDK;
import io.jenkins.tools.pluginmodernizer.core.utils.JsonUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.openrewrite.*;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Parent;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.tree.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressFBWarnings(
        value = {"RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"},
        justification = "Extrac checks harmless")
public class MetadataCollector extends ScanningRecipe<MetadataCollector.MetadataAccumulator> {

    /**
     * LOGGER.
     */
    private static final Logger LOG = LoggerFactory.getLogger(MetadataCollector.class);

    public MetadataCollector() {}

    public MetadataCollector(boolean force) {
        this.force = force;
    }

    @Option(
            displayName = "Force collection of metadata. Default to false",
            required = false,
            description = "Force the collection of metadata. If false will reuse the existing metadata if any.",
            example = "true")
    boolean force = false;

    @Override
    public String getDisplayName() {
        return "Plugin metadata extractor";
    }

    @Override
    public String getDescription() {
        return "Extracts metadata from plugin.";
    }

    /**
     * Accumulator to store metadata.
     */
    public static class MetadataAccumulator {
        private final List<ArchetypeCommonFile> commonFiles = new LinkedList<>();
        private final Set<MetadataFlag> flags = new LinkedHashSet<>();
        private final Set<JDK> jdkVersions = new LinkedHashSet<>();

        public List<ArchetypeCommonFile> getCommonFiles() {
            return commonFiles.stream().sorted().collect(Collectors.toList());
        }

        public Set<JDK> getJdkVersions() {
            return jdkVersions;
        }

        public void addCommonFile(ArchetypeCommonFile file) {
            commonFiles.add(file);
        }

        public void addJdk(JDK jdk) {
            jdkVersions.add(jdk);
        }

        public Set<MetadataFlag> getFlags() {
            return flags;
        }

        public void addFlags(Set<MetadataFlag> flags) {
            this.flags.addAll(flags);
        }
    }

    @Override
    public MetadataAccumulator getInitialValue(ExecutionContext ctx) {
        return new MetadataAccumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(MetadataAccumulator acc) {
        return new TreeVisitor<>() {
            @Override
            public Tree visit(Tree tree, ExecutionContext ctx) {
                if (tree == null) {
                    return null;
                }
                SourceFile sourceFile = (SourceFile) tree;
                ArchetypeCommonFile commonFile =
                        ArchetypeCommonFile.fromFile(sourceFile.getSourcePath().toString());
                if (commonFile != null) {
                    acc.addCommonFile(commonFile);
                    LOG.debug("File {} is a common file", sourceFile.getSourcePath());
                } else {
                    LOG.debug("File {} is not a common file", sourceFile.getSourcePath());
                }
                groovyIsoVisitor.visit(tree, ctx);
                return tree;
            }

            final TreeVisitor<?, ExecutionContext> groovyIsoVisitor = Preconditions.check(
                    new FindSourceFiles("**/Jenkinsfile"), new GroovyIsoVisitor<ExecutionContext>() {
                        private final Map<String, Object> variableMap = new HashMap<>();

                        @Override
                        public J.VariableDeclarations visitVariableDeclarations(
                                J.VariableDeclarations v, ExecutionContext ec) {
                            J.VariableDeclarations variableDeclarations = super.visitVariableDeclarations(v, ec);

                            for (J.VariableDeclarations.NamedVariable variable : variableDeclarations.getVariables()) {
                                variableMap.put(variable.getSimpleName(), variable.getInitializer());
                            }

                            return variableDeclarations;
                        }

                        @Override
                        public J.MethodInvocation visitMethodInvocation(
                                J.MethodInvocation method, ExecutionContext ctx) {
                            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                            if ("buildPlugin".equals(m.getSimpleName())) {
                                List<Expression> args = m.getArguments();

                                List<Integer> jdkVersions = args.stream()
                                        .flatMap(this::extractJdkVersions)
                                        .distinct()
                                        .toList();

                                jdkVersions.forEach(jdkVersion -> acc.addJdk(JDK.get(jdkVersion)));
                            }

                            return m;
                        }

                        private Stream<Integer> extractJdkVersions(Expression arg) {
                            if (arg instanceof G.MapEntry) {
                                return Stream.of(arg)
                                        .map(G.MapEntry.class::cast)
                                        .filter(entry ->
                                                "configurations".equals(((J.Literal) entry.getKey()).getValue()))
                                        .map(entry -> resolveConfigurations(entry.getValue()))
                                        .filter(value -> value instanceof G.ListLiteral)
                                        .flatMap(value -> ((G.ListLiteral) value).getElements().stream())
                                        .filter(expression -> expression instanceof G.MapLiteral)
                                        .flatMap(expression -> ((G.MapLiteral) expression).getElements().stream())
                                        .filter(mapExpr -> mapExpr instanceof G.MapEntry)
                                        .map(G.MapEntry.class::cast)
                                        .filter(mapEntry -> "jdk".equals(((J.Literal) mapEntry.getKey()).getValue()))
                                        .map(mapEntry -> Integer.parseInt(((J.Literal) mapEntry.getValue())
                                                .getValue()
                                                .toString()));
                            } else {
                                Expression resolvedArg = resolveVariable(arg);
                                return Stream.of(resolvedArg)
                                        .filter(resolved -> resolved instanceof G.MapLiteral)
                                        .flatMap(resolved -> ((G.MapLiteral) resolved).getElements().stream())
                                        .filter(entry -> entry instanceof G.MapEntry)
                                        .map(G.MapEntry.class::cast)
                                        .filter(entry ->
                                                "configurations".equals(((J.Literal) entry.getKey()).getValue()))
                                        .map(entry -> resolveConfigurations(entry.getValue()))
                                        .filter(value -> value instanceof G.ListLiteral)
                                        .flatMap(value -> ((G.ListLiteral) value).getElements().stream())
                                        .filter(expression -> expression instanceof G.MapLiteral)
                                        .flatMap(expression -> ((G.MapLiteral) expression).getElements().stream())
                                        .filter(mapExpr -> mapExpr instanceof G.MapEntry)
                                        .map(G.MapEntry.class::cast)
                                        .filter(mapEntry -> "jdk".equals(((J.Literal) mapEntry.getKey()).getValue()))
                                        .map(mapEntry -> Integer.parseInt(((J.Literal) mapEntry.getValue())
                                                .getValue()
                                                .toString()));
                            }
                        }

                        private Expression resolveVariable(Expression expression) {
                            if (expression instanceof J.Identifier) {
                                String variableName = ((J.Identifier) expression).getSimpleName();
                                if (variableMap.containsKey(variableName)) {
                                    return (Expression) variableMap.get(variableName);
                                }
                            }
                            return expression;
                        }

                        private Expression resolveConfigurations(Expression entry) {
                            return entry instanceof G.ListLiteral ? entry : resolveVariable(entry);
                        }
                    });
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(MetadataAccumulator acc) {
        return new MavenIsoVisitor<>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {

                final PluginMetadata pluginMetadata = new PluginMetadata();

                if (!force) {
                    // Check if metadata already exists
                    PluginMetadata refreshedMetadata = pluginMetadata.refresh();
                    if (refreshedMetadata != null && refreshedMetadata.getJenkinsVersion() != null) {
                        LOG.info("Metadata already exists, skipping metadata extraction");
                        LOG.info("Metadata: {}", JsonUtils.toJson(refreshedMetadata));
                        return SearchResult.found(document, JsonUtils.toJson(refreshedMetadata));
                    }
                }

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

                // Store flags on the accumulator
                acc.addFlags(tagExtractor.getFlags());
                LOG.info("Flags detected: {}", acc.getFlags());

                // Remove the properties that are not needed and specific to the build environment
                Map<String, String> properties = pom.getProperties();
                properties.remove("project.basedir");
                properties.remove("basedir");

                // Construct the plugin metadata
                pluginMetadata.setPluginName(pom.getName());
                Parent parent = pom.getParent();
                if (parent != null) {
                    pluginMetadata.setParentVersion(parent.getVersion());
                }
                // Lookup by group ID to set the BOM version if any
                pom.getDependencyManagement().stream()
                        .peek(dependency -> LOG.debug("Dependency: {}", dependency))
                        .filter(dependency -> "io.jenkins.tools.bom".equals(dependency.getGroupId()))
                        .findFirst()
                        .ifPresent(dependency -> pluginMetadata.setBomVersion(dependency.getVersion()));
                pluginMetadata.setProperties(properties);
                pluginMetadata.setJenkinsVersion(
                        resolvedPom.getManagedVersion("org.jenkins-ci.main", "jenkins-core", null, null));
                pluginMetadata.setFlags(acc.getFlags());
                pluginMetadata.setCommonFiles(acc.getCommonFiles());
                pluginMetadata.setJdks(acc.getJdkVersions());

                // Write the metadata to a file for later use by the plugin modernizer.
                pluginMetadata.save();
                LOG.debug("Plugin metadata written to {}", pluginMetadata.getRelativePath());
                LOG.debug(JsonUtils.toJson(pluginMetadata));

                return SearchResult.found(document, JsonUtils.toJson(pluginMetadata));
            }
        };
    }

    /**
     * Maven visitor to extract tags from pom.xml.
     */
    private static class TagExtractor extends MavenIsoVisitor<ExecutionContext> {

        /**
         * Detected flag
         */
        private final Set<MetadataFlag> flags = new LinkedHashSet<>();

        /**
         * Convert an OpenRewrite XML tag to a metadata XML tag.
         * @param tag OpenRewrite XML tag
         * @return metadata XML tag
         */
        private MetadataXmlTag convertToMetadataXmlTag(Xml.Tag tag) {
            MetadataXmlTag metadataXmlTag = new MetadataXmlTag();
            metadataXmlTag.setName(tag.getName());
            metadataXmlTag.setValue(tag.getValue());
            metadataXmlTag.setChildren(tag.getChildren().stream()
                    .map(this::convertToMetadataXmlTag)
                    .collect(Collectors.toList()));
            return metadataXmlTag;
        }

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag t = super.visitTag(tag, ctx);
            MetadataXmlTag metadataXmlTag = convertToMetadataXmlTag(tag);
            List<MetadataFlag> newFlags = Arrays.stream(MetadataFlag.values())
                    .filter(flag -> flag.isApplicable(metadataXmlTag))
                    .toList();
            flags.addAll(newFlags);
            if (!newFlags.isEmpty()) {
                LOG.debug(
                        "Flags detected for tag {} {}",
                        tag,
                        newFlags.stream().map(Enum::name).collect(Collectors.joining(", ")));
            }
            return t;
        }

        /**
         * Get the flags for this visitor.
         * @return flags for this visitor
         */
        public Set<MetadataFlag> getFlags() {
            return flags;
        }
    }
}
