package io.jenkins.tools.pluginmodernizer.cli;

import java.util.List;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.sift.AbstractDiscriminator;
import org.slf4j.Marker;

public class PluginLoggerDiscriminator extends AbstractDiscriminator<ILoggingEvent> {

    @Override
    public String getDiscriminatingValue(ILoggingEvent iLoggingEvent) {
        List<Marker> markers = iLoggingEvent.getMarkerList();
        if (markers == null || markers.isEmpty()) {
            return "modernizer";
        }
        final Marker marker = markers.get(0);
        return marker.getName();
    }

    @Override
    public String getKey() {
        return "filename";
    }

}
