package io.jenkins.tools.pluginmodernizer.cli;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.FilterReply;
import io.jenkins.tools.pluginmodernizer.core.config.Config;

public class ApplicationLogFilter extends ThresholdFilter {

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (Config.DEBUG) {
            setLevel(Level.DEBUG.levelStr);
        }
        return super.decide(event);
    }
}
