package io.jenkins.tools.pluginmodernizer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.FilterReply;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Marker;

public class ConsoleLogFilterTest {

    @Mock
    private ILoggingEvent mockEvent;

    @Mock
    private Marker mockMarker;

    private ConsoleLogFilter consoleLogFilter;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        consoleLogFilter = new ConsoleLogFilter();
    }

    @Test
    public void testDecideEventBelowThreshold() {
        when(mockEvent.getLevel()).thenReturn(ch.qos.logback.classic.Level.INFO);
        FilterReply reply = consoleLogFilter.decide(mockEvent);
        assertEquals(FilterReply.NEUTRAL, reply);
    }

    @Test
    public void testDecideMarkerListEmptyDebugEnabled() {
        when(mockEvent.getMarkerList()).thenReturn(null);
        Config.DEBUG = true;
        FilterReply reply = consoleLogFilter.decide(mockEvent);
        assertEquals(FilterReply.NEUTRAL, reply);
    }

    @Test
    public void testDecideMarkerListEmptyDebugDisabled() {
        when(mockEvent.getMarkerList()).thenReturn(null);
        Config.DEBUG = false;
        FilterReply reply = consoleLogFilter.decide(mockEvent);
        assertEquals(FilterReply.NEUTRAL, reply);
    }

    @Test
    public void testDecideMarkerListNotEmpty() {
        List<Marker> markers = Collections.singletonList(mockMarker);
        when(mockEvent.getMarkerList()).thenReturn(markers);
        FilterReply reply = consoleLogFilter.decide(mockEvent);
        assertEquals(FilterReply.DENY, reply);
    }
}
