package io.takari.bpm;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.model.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ExclusiveGatewayTest extends AbstractEngineTest {
    
    /**
     * start --> gw --fA--> end
     *             \       /
     *              --fB-->
     */
    @Test
    public void testDefaultFlow() throws Exception {
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw"),
                new ExclusiveGateway("gw", "fB"),
                new SequenceFlow("fA", "gw", "end"),
                new SequenceFlow("fB", "gw", "end"),
                new EndEvent("end")
        )));
        
        // ---
        
        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);
        
        // ---
        
        assertActivations(key, processId,
                "start",
                "f1",
                "gw",
                "fB",
                "end");
        assertNoMoreActivations();
    }

    /**
     * start --> gw --fA--> end
     *             \       /
     *              --fB-->
     */
    @Test
    public void testNoDefaultFlow() throws Exception {
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw"),
                new ExclusiveGateway("gw"),
                new SequenceFlow("fA", "gw", "end"),
                new SequenceFlow("fB", "gw", "end"),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);

        // ---

        assertActivations(key, processId,
                "start",
                "f1",
                "gw",
                "fA",
                "end");
        assertNoMoreActivations();
    }
    
    @Test
    public void testExpressions() throws Exception {
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw"),
                new ExclusiveGateway("gw", "fA"),
                new SequenceFlow("fA", "gw", "end", "${0 == 1}"),
                new SequenceFlow("fB", "gw", "end", "${1 == 1}"),
                new EndEvent("end")
        )));
        
        // ---
        
        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);
        
        // ---
        
        assertActivations(key, processId,
                "start",
                "f1",
                "gw",
                "fB",
                "end");
        assertNoMoreActivations();
    }

    @Test
    public void testExpressionsSideEffects() throws Exception {
        String varKey = "key#" + System.currentTimeMillis();
        String varVal = "val#" + System.currentTimeMillis();

        JavaDelegate t1 = spy(new JavaDelegate() {
            @Override
            public void execute(ExecutionContext ctx) throws Exception {
                ctx.setVariable(varKey, "something else");
            }
        });
        getServiceTaskRegistry().register("t1", t1);

        JavaDelegate t2 = spy(new JavaDelegate() {
            @Override
            public void execute(ExecutionContext ctx) throws Exception {
                Object v = ctx.getVariable(varKey);
                assertEquals(varVal, v);
            }
        });
        getServiceTaskRegistry().register("t2", t2);

        // ---

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw"),
                new ExclusiveGateway("gw", "fB"),

                    new SequenceFlow("fA", "gw", "t2", "${t1.execute(execution) || true}"),
                    new ServiceTask("t2", ExpressionType.DELEGATE, "${t2}"),
                    new SequenceFlow("f3", "t2", "end"),

                    new SequenceFlow("fB", "gw", "end"),

                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, Collections.singletonMap(varKey, varVal));

        // ---

        assertActivations(key, processId,
                "start",
                "f1",
                "gw",
                "fA",
                "t2",
                "f3",
                "end");
        assertNoMoreActivations();

        // ---

        verify(t1, times(1)).execute(any(ExecutionContext.class));
        verify(t2, times(1)).execute(any(ExecutionContext.class));
    }
}
