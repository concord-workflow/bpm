package io.takari.bpm;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionListener;
import io.takari.bpm.model.EndEvent;
import io.takari.bpm.model.ExpressionType;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.model.StartEvent;
import java.util.Arrays;
import java.util.UUID;
import org.junit.Test;
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
    
    public interface SampleListener {
        
        void doIt();
    }
}
