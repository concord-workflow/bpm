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
    public void testEvalString() throws Exception {
        ExpressionManager em = new DefaultExpressionManager(mock(ServiceTaskRegistry.class));
        String s1 = "PT30S";
        String s2 = em.eval(mock(ExecutionContext.class), s1, String.class);
        assertEquals(s1, s2);
    }
}
