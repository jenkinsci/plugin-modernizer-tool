package io.jenkins.tools.pluginmodernizer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.Test;
import org.slf4j.Marker;

public class PluginLoggerDiscriminatorTest {

    @Test
    void testGetDiscriminatingValueNoMarkers() {
        PluginLoggerDiscriminator discriminator = new PluginLoggerDiscriminator();
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getMarkerList()).thenReturn(null);

        String discriminatingValue = discriminator.getDiscriminatingValue(event);
        assertEquals("modernizer", discriminatingValue);
    }

    @Test
    void testGetDiscriminatingValueEmptyMarkers() {
        PluginLoggerDiscriminator discriminator = new PluginLoggerDiscriminator();
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getMarkerList()).thenReturn(Collections.emptyList());

        String discriminatingValue = discriminator.getDiscriminatingValue(event);
        assertEquals("modernizer", discriminatingValue);
    }

    @Test
    void testGetDiscriminatingValueWithMarkers() {
        PluginLoggerDiscriminator discriminator = new PluginLoggerDiscriminator();
        ILoggingEvent event = mock(ILoggingEvent.class);
        Marker marker = mock(Marker.class);
        when(marker.getName()).thenReturn("testMarker");
        when(event.getMarkerList()).thenReturn(Collections.singletonList(marker));

        String discriminatingValue = discriminator.getDiscriminatingValue(event);
        assertEquals("testMarker", discriminatingValue);
    }

    @Test
    void testGetKey() {
        PluginLoggerDiscriminator discriminator = new PluginLoggerDiscriminator();
        String key = discriminator.getKey();
        assertEquals("filename", key);
    }
}
