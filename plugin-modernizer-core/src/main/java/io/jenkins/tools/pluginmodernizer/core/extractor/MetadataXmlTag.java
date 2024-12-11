package io.jenkins.tools.pluginmodernizer.core.extractor;

import java.util.List;
import java.util.Optional;

/**
 * To transport information form an OpenRewrite XML tag back to plugin modernizer
 * This avoid create a dependency between OpenRewrite internals and Plugin Modernizer
 */
public final class MetadataXmlTag {

    private String name;
    private Optional<String> value;
    private List<MetadataXmlTag> children = List.of();

    public MetadataXmlTag() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Optional<String> getValue() {
        return value;
    }

    public void setValue(Optional<String> value) {
        this.value = value;
    }

    public List<MetadataXmlTag> getChildren() {
        return children;
    }

    public void setChildren(List<MetadataXmlTag> children) {
        this.children = children;
    }

    public Optional<MetadataXmlTag> getChild(String name) {
        return children.stream().filter(c -> c.getName().equals(name)).findFirst();
    }

    public Optional<String> getChildValue(String name) {
        return getChild(name).flatMap(MetadataXmlTag::getValue);
    }
}
