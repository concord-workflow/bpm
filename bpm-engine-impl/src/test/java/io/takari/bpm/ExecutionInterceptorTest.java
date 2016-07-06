package io.takari.bpm;

import io.takari.bpm.api.interceptors.ExecutionInterceptor;
import io.takari.bpm.api.interceptors.InterceptorStartEvent;
import io.takari.bpm.model.AbstractElement;
import io.takari.bpm.model.EndEvent;
import io.takari.bpm.model.EventBasedGateway;
import io.takari.bpm.model.ExpressionType;
import io.takari.bpm.model.IntermediateCatchEvent;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.model.ServiceTask;
import io.takari.bpm.model.StartEvent;
import java.util.Arrays;
import java.util.UUID;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;

public class ExecutionInterceptorTest extends AbstractEngineTest {

    private ExecutionInterceptor interceptor;
    
    @Before
    public void setUp() {
        interceptor = mock(ExecutionInterceptor.class);
        getEngine().addInterceptor(interceptor);
    }
    
    /**
     * start --> gw --> ev --> end
     */
    @Test
    public void testSingleEvent() throws Exception {
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.<AbstractElement>asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw"),
                new EventBasedGateway("gw"),
                    new SequenceFlow("f2", "gw", "ev"),
                    new IntermediateCatchEvent("ev", "ev"),
                    new SequenceFlow("f3", "ev", "end"),
                    new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);
        
        ArgumentCaptor<InterceptorStartEvent> args = ArgumentCaptor.forClass(InterceptorStartEvent.class);
        verify(interceptor, times(1)).onStart(args.capture());
        assertEquals(key, args.getValue().getProcessBusinessKey());
        
        verify(interceptor, times(1)).onSuspend();

        // ---

        getEngine().resume(key, "ev", null);

        // ---

        verify(interceptor, times(1)).onResume();
        verify(interceptor, times(1)).onFinish(eq(key));
    }
    
    /**
     * start --> t1 (exception!) --> end
     */
    @Test
    public void testException() throws Exception {
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.<AbstractElement>asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
                new SequenceFlow("f2", "t1", "end"),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        
        try {
            getEngine().start(key, processId, null);
            fail("whoa there");
        } catch (Exception e) {
        }
        
        verify(interceptor).onError(eq(key), any(Throwable.class));
    }
}
