package io.takari.bpm;

import static org.junit.Assert.*;
import org.junit.Test;

import io.takari.bpm.state.Variables;

public class VariablesTest {

    @Test
    public void testParent() throws Exception {
        Variables p = new Variables()
                .setVariable("a", 123)
                .setVariable("b", false);
        
        Variables v = new Variables(p);
        
        // ---
        
        assertEquals(123, p.getVariable("a"));
        assertEquals(123, v.getVariable("a"));
        
        // ---
        
        v = v.removeVariable("a");
        
        assertNull(v.getVariable("a"));
        assertNotNull(p.getVariable("a"));
        assertNull(v.getParent().getVariable("a"));
        
        // ---
        
        v = v.setVariable("b", true);
        
        assertEquals(true, v.getVariable("b"));
        assertEquals(false, v.getParent().getVariable("b"));
    }
}
