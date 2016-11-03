package io.takari.bpm.context;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.SetVariableAction;
import io.takari.bpm.actions.UnsetVariableAction;
import io.takari.bpm.el.ExpressionManager;
import io.takari.bpm.state.Variables;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class ExecutionContextTest {

    @Test
    public void test() throws Exception {
        ExpressionManager em = mock(ExpressionManager.class);

        Variables vars = new Variables()
                .setVariable("a", 123)
                .setVariable("b", false);

        ExecutionContextImpl ctx = new ExecutionContextImpl(em, vars);

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
}
