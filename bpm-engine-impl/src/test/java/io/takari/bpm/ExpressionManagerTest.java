package io.takari.bpm;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.el.DefaultExpressionManager;
import io.takari.bpm.el.ExpressionManager;
import io.takari.bpm.task.ServiceTaskRegistry;
import static org.junit.Assert.*;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class ExpressionManagerTest {

    @Test
    public void testMethodCall() throws Exception {
        ServiceTaskRegistry registry = mock(ServiceTaskRegistry.class);
        when(registry.getByKey(anyString())).thenReturn(new TestBean());
        
        ExpressionManager em = new DefaultExpressionManager(registry);
        String expr = "${bean.sayHello('world')}";
        String result = em.eval(mock(ExecutionContext.class), expr, String.class);
        
        assertEquals("Hello, world!", result);
    }
    
    @Test
    public void testEvalString() throws Exception {
        ExpressionManager em = new DefaultExpressionManager(mock(ServiceTaskRegistry.class));
        String s1 = "PT30S";
        String s2 = em.eval(mock(ExecutionContext.class), s1, String.class);
        assertEquals(s1, s2);
    }
    
    public static final class TestBean {
        
        public String sayHello(String s) {
            return "Hello, " + s + "!";
        }
    }
}
