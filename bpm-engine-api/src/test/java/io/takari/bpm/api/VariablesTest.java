package io.takari.bpm.api;

import io.takari.bpm.api.Variables;
import org.junit.Assert;
import org.junit.Test;

public class VariablesTest {

    @Test
    public void testParent() throws Exception {
        Variables p = new Variables()
                .setVariable("a", 123)
                .setVariable("b", false);
        
        Variables v = new Variables(p);
        
        // ---
        
        Assert.assertEquals(123, p.getVariable("a"));
        Assert.assertEquals(123, v.getVariable("a"));
        
        // ---
        
        v = v.removeVariable("a");
        
        Assert.assertNull(v.getVariable("a"));
        Assert.assertNotNull(p.getVariable("a"));
        Assert.assertNull(v.getParent().getVariable("a"));
        
        // ---
        
        v = v.setVariable("b", true);
        
        Assert.assertEquals(true, v.getVariable("b"));
        Assert.assertEquals(false, v.getParent().getVariable("b"));
    }
}
