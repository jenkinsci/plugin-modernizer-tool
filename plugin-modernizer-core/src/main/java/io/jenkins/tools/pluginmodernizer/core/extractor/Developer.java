package io.jenkins.tools.pluginmodernizer.core.extractor;

public record Developer(String id, String name, String email, String role) {

    @Override
    public String toString() {
        return "Developer{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}