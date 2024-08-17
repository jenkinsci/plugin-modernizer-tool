package io.jenkins.tools.pluginmodernizer.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class JDKTest {

    // Write tests for JDK enum here
    @Test
    public void shouldNext() {
        assertEquals(JDK.JAVA_11, JDK.JAVA_8.next());
        assertEquals(JDK.JAVA_17, JDK.JAVA_11.next());
        assertEquals(JDK.JAVA_21, JDK.JAVA_17.next());
        assertEquals(null, JDK.JAVA_21.next());
    }

    public void shouldGet() {
        assertEquals(JDK.JAVA_8, JDK.get(8));
        assertEquals(JDK.JAVA_11, JDK.get(11));
        assertEquals(JDK.JAVA_17, JDK.get(17));
        assertEquals(JDK.JAVA_21, JDK.get(21));
    }

    @Test
    public void currentMax() {
        // Adapt when new JDK are added
        assertEquals(JDK.JAVA_21, JDK.max());
    }
}
