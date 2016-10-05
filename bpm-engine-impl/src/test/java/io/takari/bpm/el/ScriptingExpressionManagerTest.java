package io.takari.bpm.el;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.task.KeyAwareServiceTaskRegistry;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class ScriptingExpressionManagerTest {

    @Test
    public void testNashornSimple() throws Exception {
        String taskKey = "t";
        String taskArg = "arg#" + System.currentTimeMillis();

        TestBean testBean = spy(new TestBean());

        KeyAwareServiceTaskRegistry taskRegistry = mock(KeyAwareServiceTaskRegistry.class);
        when(taskRegistry.getByKey(eq(taskKey))).thenReturn(testBean);
        when(taskRegistry.containsKey(eq(taskKey))).thenReturn(true);

        // ---

        ExpressionManager em = new ScriptingExpressionManager("nashorn", taskRegistry);

        ExecutionContext ctx = mock(ExecutionContext.class);
        String expr = String.format("%s.doIt('%s')", taskKey, taskArg);
        em.eval(ctx, expr, Object.class);

        // ---

        verify(testBean, times(1)).doIt(eq(taskArg));
    }
    
    @Test
    public void testNashornParameters() throws Exception {
        String taskKey = "t";
        String taskArg = "arg#" + System.currentTimeMillis();

        KeyAwareServiceTaskRegistry taskRegistry = mock(KeyAwareServiceTaskRegistry.class);

        // ---

        ExpressionManager em = new ScriptingExpressionManager("nashorn", taskRegistry);

        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.hasVariable(eq("test"))).thenReturn(true);
        when(ctx.getVariable(eq("test"))).thenReturn("hello");
        Object result = em.eval(ctx, "test", Object.class);

        // ---

        assertEquals("hello", result);
    }

    @Test
    public void testNashornDelegate() throws Exception {
        String taskKey = "t";

        TestBean testBean1 = new TestBean();

        KeyAwareServiceTaskRegistry taskRegistry = mock(KeyAwareServiceTaskRegistry.class);
        when(taskRegistry.getByKey(eq(taskKey))).thenReturn(testBean1);
        when(taskRegistry.containsKey(eq(taskKey))).thenReturn(true);

        // ---

        ExpressionManager em = new ScriptingExpressionManager("nashorn", taskRegistry);

        ExecutionContext ctx = mock(ExecutionContext.class);
        String expr = String.format("%s", taskKey);
        TestBean testBean2 = em.eval(ctx, expr, TestBean.class);
        assertEquals(testBean1, testBean2);
    }

    public static class TestBean {

        public void doIt(String arg) {
        }
    }
}
