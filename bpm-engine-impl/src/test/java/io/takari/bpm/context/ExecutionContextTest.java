package io.takari.bpm.context;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.SetVariableAction;
import io.takari.bpm.actions.UnsetVariableAction;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.Variables;
import io.takari.bpm.el.ExpressionManager;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

public class ExecutionContextTest {

    @Test
    public void test() throws Exception {
        ExpressionManager em = mock(ExpressionManager.class);

        Variables vars = new Variables()
                .setVariable("a", 123)
                .setVariable("b", false);

        ExecutionContextImpl ctx = new ExecutionContextImpl(new DefaultExecutionContextFactory(em), em, vars);

        // ---

        assertEquals(123, ctx.getVariable("a"));

        ctx.setVariable("a", 234);
        assertEquals(234, ctx.getVariable("a"));

        ctx.removeVariable("b");
        assertEquals(null, ctx.getVariable("b"));

        // ---

        List<Action> ls = ctx.toActions();
        assertEquals(2, ls.size());

        SetVariableAction a1 = (SetVariableAction) ls.get(0);
        assertEquals("a", a1.getKey());
        assertEquals(234, a1.getValue());

        UnsetVariableAction a2 = (UnsetVariableAction) ls.get(1);
        assertEquals("b", a2.getKey());
    }

    @Test
    public void testToMap() throws Exception {
        ExpressionManager em = mock(ExpressionManager.class);

        Variables vars1 = new Variables()
                .setVariable("a", 123)
                .setVariable("b", false);

        Variables vars2 = new Variables(vars1)
                .setVariable("b", true)
                .setVariable("c", "hello");

        ExecutionContext ctx = new ExecutionContextImpl(new DefaultExecutionContextFactory(em), em, vars2);

        ctx.removeVariable("b");
        ctx.setVariable("d", "bye");

        Map<String, Object> m = ctx.toMap();
        assertNotNull(m);
        assertEquals(3, m.size());
        assertEquals(123, m.get("a"));
        assertNull(m.get("b"));
        assertEquals("hello", m.get("c"));
        assertEquals("bye", m.get("d"));
    }
}
