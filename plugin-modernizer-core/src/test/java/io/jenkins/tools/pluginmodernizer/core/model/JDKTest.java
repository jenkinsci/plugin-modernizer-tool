package io.jenkins.tools.pluginmodernizer.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class JDKTest {

    // Write tests for JDK enum here
    @Test
    public void shouldNext() {
        assertEquals(JDK.JAVA_11, JDK.JAVA_8.next());
        assertEquals(JDK.JAVA_17, JDK.JAVA_11.next());
        assertEquals(JDK.JAVA_21, JDK.JAVA_17.next());
        assertNull(JDK.JAVA_21.next());
    }

    @Test
    public void shouldBefore() {
        assertNull(JDK.JAVA_8.previous());
        assertEquals(JDK.JAVA_8, JDK.JAVA_11.previous());
        assertEquals(JDK.JAVA_11, JDK.JAVA_17.previous());
        assertEquals(JDK.JAVA_17, JDK.JAVA_21.previous());
    }

    @Test
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

    @Test
    public void all() {
        assertEquals(4, JDK.all().size());
        assertEquals(JDK.JAVA_8, JDK.all().get(0));
        assertEquals(JDK.JAVA_11, JDK.all().get(1));
        assertEquals(JDK.JAVA_17, JDK.all().get(2));
        assertEquals(JDK.JAVA_21, JDK.all().get(3));
    }

    @Test
    public void getBuildableJdk() {

        assertEquals(1, JDK.get("2.163").size());
        assertEquals(JDK.JAVA_8, JDK.get("2.163").get(0));

        assertEquals(2, JDK.get("2.164.1").size());
        assertEquals(JDK.JAVA_8, JDK.get("2.164.1").get(0));
        assertEquals(JDK.JAVA_11, JDK.get("2.164.1").get(1));

        assertEquals(2, JDK.get("2.339").size());
        assertEquals(JDK.JAVA_8, JDK.get("2.339").get(0));
        assertEquals(JDK.JAVA_11, JDK.get("2.339").get(1));

        assertEquals(3, JDK.get("2.346.1").size());
        assertEquals(JDK.JAVA_8, JDK.get("2.346.1").get(0));
        assertEquals(JDK.JAVA_11, JDK.get("2.346.1").get(1));
        assertEquals(JDK.JAVA_17, JDK.get("2.346.1").get(2));

        assertEquals(2, JDK.get("2.357").size());
        assertEquals(JDK.JAVA_11, JDK.get("2.357").get(0));
        assertEquals(JDK.JAVA_17, JDK.get("2.357").get(1));

        assertEquals(2, JDK.get("2.361.1").size());
        assertEquals(JDK.JAVA_11, JDK.get("2.361.1").get(0));
        assertEquals(JDK.JAVA_17, JDK.get("2.361.1").get(1));

        assertEquals(3, JDK.get("2.426.1").size());
        assertEquals(JDK.JAVA_11, JDK.get("2.426.1").get(0));
        assertEquals(JDK.JAVA_17, JDK.get("2.426.1").get(1));
        assertEquals(JDK.JAVA_21, JDK.get("2.426.1").get(2));

        assertEquals(3, JDK.get("2.462.3").size());
        assertEquals(JDK.JAVA_11, JDK.get("2.462.3").get(0));
        assertEquals(JDK.JAVA_17, JDK.get("2.462.3").get(1));
        assertEquals(JDK.JAVA_21, JDK.get("2.462.3").get(2));

        assertEquals(2, JDK.get("2.463").size());
        assertEquals(JDK.JAVA_17, JDK.get("2.463").get(0));
        assertEquals(JDK.JAVA_21, JDK.get("2.463").get(1));

        assertEquals(2, JDK.get("2.479.1").size());
        assertEquals(JDK.JAVA_17, JDK.get("2.479.1").get(0));
        assertEquals(JDK.JAVA_21, JDK.get("2.479.1").get(1));
    }
}
