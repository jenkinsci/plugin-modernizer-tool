package io.jenkins.tools.pluginmodernizer.core.extractor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import com.google.gson.Gson;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.Markers;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.tree.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PomParser extends Recipe {

    private static final Logger LOG = LoggerFactory.getLogger(PomParser.class);

    PluginMetadata pluginMetadata = PluginMetadata.getInstance();

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
                Optional<MavenResolutionResult> mavenResolutionResult = markers.findFirst(MavenResolutionResult.class);
                if (mavenResolutionResult.isEmpty()) {
                    return document;
                }
                MavenResolutionResult resolutionResult = mavenResolutionResult.get();
                ResolvedPom resolvedPom = resolutionResult.getPom();
                Pom pom = resolvedPom.getRequested();

                TagExtractor tagExtractor = new TagExtractor();
                tagExtractor.visit(document, ctx);

                pluginMetadata.setPluginName(pom.getName());
                pluginMetadata.setPluginParent(pom.getParent());
                pluginMetadata.setDependencies(pom.getDependencies());
                pluginMetadata.setProperties(pom.getProperties());
                pluginMetadata.setJenkinsVersion(resolvedPom.getManagedVersion("org.jenkins-ci.main", "jenkins-core", null, null));
                pluginMetadata.setHasJavaLevel(pom.getProperties().get("java.level") != null);
                pluginMetadata.setHasDevelopersTag(tagExtractor.hasDevelopersTag());
                pluginMetadata.setLicensed(!pom.getLicenses().isEmpty());
                pluginMetadata.setUsesHttps(tagExtractor.usesHttps());

                try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream("pluginMetadata.json"), StandardCharsets.UTF_8)) {
                    Gson gson = new Gson();
                    gson.toJson(pluginMetadata, writer);
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }

                return document;
            }
        };
    }

    private static class TagExtractor extends MavenIsoVisitor<ExecutionContext> {
        private boolean hasDevelopersTag = false;
        private boolean usesHttps = false;

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag t = super.visitTag(tag, ctx);
            if ("developers".equals(tag.getName())) {
                hasDevelopersTag = true;
            } else if ("scm".equals(tag.getName())) {
                Optional<String> connection = tag.getChildValue("connection");
                connection.ifPresent(s -> usesHttps = s.startsWith("scm:git:https"));
            }
            return t;
        }

        public boolean hasDevelopersTag() {
            return hasDevelopersTag;
        }

        public boolean usesHttps() {
            return usesHttps;
        }
    }
}
