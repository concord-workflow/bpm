package io.takari.bpm;

import io.takari.bpm.api.BpmnError;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.api.interceptors.ExecutionInterceptor;
import io.takari.bpm.api.interceptors.InterceptorScopeCreatedEvent;
import io.takari.bpm.api.interceptors.InterceptorScopeDestroyedEvent;
import io.takari.bpm.api.interceptors.InterceptorStartEvent;
import io.takari.bpm.model.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
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
        deploy(new ProcessDefinition(processId, Arrays.asList(
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
        deploy(new ProcessDefinition(processId, Arrays.asList(
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

    /**
     * start --> errorEnd
     */
    @Test
    public void testFailure() throws Exception {
        String processId = "test";
        String errorRef = "myError#" + System.currentTimeMillis();

        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "end"),
                new EndEvent("end", errorRef)
        )));

        // ---


        String key = UUID.randomUUID().toString();
        try {
            getEngine().start(key, processId, null);
            fail("Should fail");
        } catch (ExecutionException e) {
            assertBpmnError(e, errorRef);
        }

        verify(interceptor).onFailure(eq(key), eq(errorRef));
    }

    /**
     * start --> sub                            end
     *              \                          /
     *               substart --> t1 --> subend
     *                              \
     *                               be1
     *                               \
     *                                --------> errorEnd
     */
    @Test
    public void testBoundaryFailure() throws Exception {
        String processId = "test";
        String errorRef = "myError#" + System.currentTimeMillis();

        JavaDelegate t1 = mock(JavaDelegate.class);
        doThrow(new BpmnError("someError")).when(t1).execute(any(ExecutionContext.class));
        getServiceTaskRegistry().register("t1", t1);

        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "sub"),
                new SubProcess("sub", Arrays.asList(
                        new StartEvent("substart"),
                        new SequenceFlow("f2", "substart", "t1"),
                        new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
                        new SequenceFlow("f3", "t1", "subend"),
                        new EndEvent("subend")
                )),

                new BoundaryEvent("be1", "sub", null),
                new SequenceFlow("f4", "be1", "errorEnd"),
                new EndEvent("errorEnd", errorRef),

                new SequenceFlow("f5", "sub", "end"),
                new EndEvent("end")
        )));

        // ---


        String key = UUID.randomUUID().toString();
        try {
            getEngine().start(key, processId, null);
            fail("Should fail");
        } catch (ExecutionException e) {
            assertBpmnError(e, errorRef);
        }

        // ---

        verify(t1).execute(any(ExecutionContext.class));
        verify(interceptor).onFailure(eq(key), eq(errorRef));
    }

    /**
     * start --> call               end
     *               \             /
     *                start --> end
     */
    @Test
    public void testActivityScopes() throws Exception {
        String aId = "testA";
        String bId = "testB";

        deploy(new ProcessDefinition(aId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "call"),
                new CallActivity("call", bId),
                new SequenceFlow("f2", "call", "end"),
                new EndEvent("end")
        )));

        deploy(new ProcessDefinition(bId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "end"),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, aId, null);

        // ---

        ArgumentCaptor<InterceptorScopeCreatedEvent> createEvCapture = ArgumentCaptor.forClass(InterceptorScopeCreatedEvent.class);
        verify(interceptor, atLeastOnce()).onScopeCreated(createEvCapture.capture());

        List<InterceptorScopeCreatedEvent> createEvs = createEvCapture.getAllValues();
        assertEquals(2, createEvs.size());

        InterceptorScopeCreatedEvent createEv = createEvs.get(1);
        assertEquals(aId, createEv.getProcessDefinitionId());
        assertEquals("call", createEv.getElementId());

        UUID scopeId = createEv.getScopeId();

        // ---

        ArgumentCaptor<InterceptorScopeDestroyedEvent> destroyEvCapture = ArgumentCaptor.forClass(InterceptorScopeDestroyedEvent.class);
        verify(interceptor, atLeastOnce()).onScopeDestroyed(destroyEvCapture.capture());

        List<InterceptorScopeDestroyedEvent> destroyEvs = destroyEvCapture.getAllValues();
        assertEquals(2, createEvs.size());

        InterceptorScopeDestroyedEvent destroyEv = destroyEvs.get(0);
        assertEquals(scopeId, destroyEv.getScopeId());
    }
}
