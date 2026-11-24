package io.takari.bpm.api;

import io.takari.bpm.api.Variables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VariablesTest {

    @Test
    public void testParent() throws Exception {
        Variables p = new Variables()
                .setVariable("a", 123)
                .setVariable("b", false);
        
        Variables v = new Variables(p);
        
        // ---
        
        Assertions.assertEquals(123, p.getVariable("a"));
        Assertions.assertEquals(123, v.getVariable("a"));
        
        // ---
        
        v = v.removeVariable("a");
        
        Assertions.assertNull(v.getVariable("a"));
        Assertions.assertNotNull(p.getVariable("a"));
        Assertions.assertNull(v.getParent().getVariable("a"));
        
        // ---
        
        v = v.setVariable("b", true);
        
        Assertions.assertEquals(true, v.getVariable("b"));
        Assertions.assertEquals(false, v.getParent().getVariable("b"));
    }
}
