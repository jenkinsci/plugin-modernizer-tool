package io.jenkins.tools.pluginmodernizer.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jenkins.tools.pluginmodernizer.core.extractor.MetadataFlag;
import io.jenkins.tools.pluginmodernizer.core.extractor.PluginMetadata;
import io.jenkins.tools.pluginmodernizer.core.model.PreconditionError;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class JsonUtilsTest {

    @Test
    public void testMetaDataToJson() {
        PluginMetadata metadata = new PluginMetadata();
        metadata.setKey("plugin-api-key");
        metadata.setFlags(Set.of(MetadataFlag.IS_API_PLUGIN));
        metadata.setJenkinsVersion("2.479.1");
        metadata.setErrors(Set.of(PreconditionError.MAVEN_REPOSITORIES_HTTP));
        assertEquals(
                "{\"flags\":[\"IS_API_PLUGIN\"],\"errors\":[\"MAVEN_REPOSITORIES_HTTP\"],\"jenkinsVersion\":\"2.479.1\",\"key\":\"plugin-api-key\",\"path\":\".\"}",
                JsonUtils.toJson(metadata));
    }
}
