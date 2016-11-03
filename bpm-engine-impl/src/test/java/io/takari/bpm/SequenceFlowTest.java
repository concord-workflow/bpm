package io.takari.bpm;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionListener;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.model.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class SequenceFlowTest extends AbstractEngineTest {

    /**
     * start --delegate--> end
     */
    @Test
    public void testDelegateListener() throws Exception {
        ExecutionListener l = mock(ExecutionListener.class);
        getServiceTaskRegistry().register("hello", l);

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "end", new SequenceFlow.ExecutionListener("taken", ExpressionType.DELEGATE, "${hello}")),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);

        // ---

        assertActivations(key, processId,
                "start",
                "f1",
                "end");
        assertNoMoreActivations();

        // ---

        verify(l, times(1)).notify(any(ExecutionContext.class));
    }

    /**
     * start --simple--> end
     */
    @Test
    public void testSimpleListener() throws Exception {
        SampleListener l = mock(SampleListener.class);
        getServiceTaskRegistry().register("hello", l);

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "end", new SequenceFlow.ExecutionListener("taken", ExpressionType.SIMPLE, "${hello.doIt()}")),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);

        // ---

        assertActivations(key, processId,
                "start",
                "f1",
                "end");
        assertNoMoreActivations();

        // ---

        verify(l, times(1)).doIt();
    }

    /**
     * start --delegate--> t1 --> end
     */
    @Test
    public void testExpressionSideEffects() throws Exception {
        String varKey = "key#" + System.currentTimeMillis();
        String varVal = "val#" + System.currentTimeMillis();

        ExecutionListener l1 = spy(new ExecutionListener() {
            @Override
            public void notify(ExecutionContext ctx) {
                ctx.setVariable(varKey, "somethingElse");
            }
        });
        getServiceTaskRegistry().register("l1", l1);

        JavaDelegate t1 = spy(new JavaDelegate() {
            @Override
            public void execute(ExecutionContext ctx) throws Exception {
                assertEquals(varVal, ctx.getVariable(varKey));
            }
        });
        getServiceTaskRegistry().register("t1", t1);

        // ---

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1", new SequenceFlow.ExecutionListener("taken", ExpressionType.DELEGATE, "${l1}")),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
                new SequenceFlow("f2", "t1", "end"),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        Map<String, Object> args = Collections.singletonMap(varKey, varVal);
        getEngine().start(key, processId, args);

        // ---

        verify(t1, times(1)).execute(any(ExecutionContext.class));
    }

    public interface SampleListener {

        void doIt();
    }
}
