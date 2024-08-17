package io.jenkins.tools.pluginmodernizer.core.model;

public class ModernizerException extends RuntimeException {

    public ModernizerException(String message) {
        super(message);
    }

    public ModernizerException(String message, Throwable cause) {
        super(message, cause);
    }
}
