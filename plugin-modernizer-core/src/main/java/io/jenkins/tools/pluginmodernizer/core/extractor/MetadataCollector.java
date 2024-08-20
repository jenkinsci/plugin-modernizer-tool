package io.jenkins.tools.pluginmodernizer.core.extractor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.utils.JsonUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.Markers;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Parent;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.tree.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", justification = "Extrac checks harmless")
public class MetadataCollector extends ScanningRecipe<MetadataCollector.MetadataAccumulator> {

    /**
     * LOGGER.
     */
    private static final Logger LOG = LoggerFactory.getLogger(MetadataCollector.class);

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
        private final List<ArchetypeCommonFile> commonFiles = new ArrayList<>();
        private final List<String> otherFiles = new ArrayList<>();
        private final List<MetadataFlag> flags = new LinkedList<>();

        public List<ArchetypeCommonFile> getCommonFiles() {
            return commonFiles;
        }

        public List<String> getOtherFiles() {
            return otherFiles;
        }

        public void addCommonFile(ArchetypeCommonFile file) {
            commonFiles.add(file);
        }

        public void addOtherFile(String file) {
            otherFiles.add(file);
        }

        public List<MetadataFlag> getFlags() {
            return flags;
        }

        public void addFlags(List<MetadataFlag> flags) {
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
                    acc.addOtherFile(sourceFile.getSourcePath().toString());
                    LOG.debug("File {} is not a common file", sourceFile.getSourcePath());
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(MetadataAccumulator acc) {
        return new MavenIsoVisitor<>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {

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
                PluginMetadata pluginMetadata = new PluginMetadata();
                pluginMetadata.setPluginName(pom.getName());
                Parent parent = pom.getParent();
                if (parent != null) {
                    pluginMetadata.setParentVersion(parent.getVersion());
                }
                pluginMetadata.setProperties(properties);
                pluginMetadata.setJenkinsVersion(
                        resolvedPom.getManagedVersion("org.jenkins-ci.main", "jenkins-core", null, null));
                pluginMetadata.setFlags(acc.getFlags());
                pluginMetadata.setCommonFiles(acc.getCommonFiles());
                pluginMetadata.setOtherFiles(acc.getOtherFiles());

                // Write the metadata to a file for later use by the plugin modernizer.
                pluginMetadata.save();
                LOG.debug("Plugin metadata written to {}", pluginMetadata.getRelativePath());
                LOG.debug(JsonUtils.toJson(pluginMetadata));

                return document;
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
        private final List<MetadataFlag> flags = new ArrayList<>();

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag t = super.visitTag(tag, ctx);
            List<MetadataFlag> newFlags = Arrays.stream(MetadataFlag.values())
                    .filter(flag -> flag.isApplicable(tag))
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
        public List<MetadataFlag> getFlags() {
            return flags;
        }
    }
}
