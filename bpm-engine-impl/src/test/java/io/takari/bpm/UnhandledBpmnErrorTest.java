package io.takari.bpm;

import io.takari.bpm.Configuration.UnhandledBpmnErrorStrategy;
import io.takari.bpm.api.BpmnError;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.model.*;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class UnhandledBpmnErrorTest extends AbstractEngineTest {

    private static final String PROCESS_ID = "test";
    private static final String ERROR_REF = "kaboom!";

    private JavaDelegate t1;
    private JavaDelegate t2;

    @Before
    public void setUp() {
        t1 = spy(new JavaDelegate() {
            @Override
            public void execute(ExecutionContext ctx) throws Exception {
                throw new BpmnError(ERROR_REF);
            }
        });
        getServiceTaskRegistry().register("t1", t1);

        t2 = spy(new JavaDelegate() {
            @Override
            public void execute(ExecutionContext ctx) throws Exception {
                BpmnError e = (BpmnError) ctx.getVariable(ExecutionContext.LAST_ERROR_KEY);
                assertNotNull(e);
                assertEquals(ERROR_REF, e.getErrorRef());
            }
        });
        getServiceTaskRegistry().register("t2", t2);
    }

    @Test
    public void testPropagate() throws Exception {
        getConfiguration().setUnhandledBpmnErrorStrategy(UnhandledBpmnErrorStrategy.PROPAGATE);
        deployProcessWithBoundaryError();

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, PROCESS_ID, null);

        // ---

        verify(t1, times(1)).execute(any(ExecutionContext.class));
        verify(t2, times(1)).execute(any(ExecutionContext.class));
    }

    @Test
    public void testException() throws Exception {
        getConfiguration().setUnhandledBpmnErrorStrategy(UnhandledBpmnErrorStrategy.EXCEPTION);
        deployProcessWithBoundaryError();

        // ---

        String key = UUID.randomUUID().toString();

        try {
            getEngine().start(key, PROCESS_ID, null);
            fail("Should fail");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof BpmnError);

            BpmnError err = (BpmnError) cause;
            assertEquals(ERROR_REF, err.getErrorRef());
        }

        // ---

        verify(t1, times(1)).execute(any(ExecutionContext.class));
        verifyZeroInteractions(t2);
    }

    @Test
    public void testIgnore() throws Exception {
        getConfiguration().setUnhandledBpmnErrorStrategy(UnhandledBpmnErrorStrategy.IGNORE);
        deployProcessWithBoundaryError();

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, PROCESS_ID, null);

        // ---

        verify(t1, times(1)).execute(any(ExecutionContext.class));
        verifyZeroInteractions(t2);
    }

    @Test
    public void testPropagateWithoutBoundaryError() throws Exception {
        getConfiguration().setUnhandledBpmnErrorStrategy(UnhandledBpmnErrorStrategy.PROPAGATE);
        deployProcessWithoutBoundaryError();

        // ---

        String key = UUID.randomUUID().toString();
        try {
            getEngine().start(key, PROCESS_ID, null);
            fail("Should fail");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof BpmnError);

            BpmnError err = (BpmnError) cause;
            assertEquals(ERROR_REF, err.getErrorRef());
        }

        // ---

        verify(t1, times(1)).execute(any(ExecutionContext.class));
    }

    /*
     * start --> sub1                                                         ---------------> end
     *              \                                                        /
     *               sub1start --> sub2                               sub1end
     *                                 \                             /
     *                                  sub2start --> t1 --> sub2end
     */
    private void deployProcessWithoutBoundaryError() {
        deploy(new ProcessDefinition(PROCESS_ID,
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "sub1"),
                new SubProcess("sub1",
                        new StartEvent("sub1start"),
                        new SequenceFlow("f2", "sub1start", "sub2"),
                        new SubProcess("sub2",
                                new StartEvent("sub2start"),
                                new SequenceFlow("f3", "sub2start", "t1"),
                                new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
                                new SequenceFlow("f4", "t1", "sub2end"),
                                new EndEvent("sub2end"))),
                new SequenceFlow("f7", "sub1", "end"),
                new EndEvent("end")));
    }

    /*
     * start --> sub1                                                         ---------------> end
     *              \                                                        /                /
     *               sub1start --> sub2                               sub1end --> error --> t2
     *                                 \                             /
     *                                  sub2start --> t1 --> sub2end
     */
    private void deployProcessWithBoundaryError() {
        deploy(new ProcessDefinition(PROCESS_ID,
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "sub1"),
                new SubProcess("sub1",
                        new StartEvent("sub1start"),
                        new SequenceFlow("f2", "sub1start", "sub2"),
                        new SubProcess("sub2",
                                new StartEvent("sub2start"),
                                new SequenceFlow("f3", "sub2start", "t1"),
                                new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
                                new SequenceFlow("f4", "t1", "sub2end"),
                                new EndEvent("sub2end"))),
                new BoundaryEvent("ev1", "sub1", null),
                new SequenceFlow("f5", "ev1", "t2", "end"),
                new ServiceTask("t2", ExpressionType.DELEGATE, "${t2}"),
                new SequenceFlow("f6", "t2", "end"),
                new SequenceFlow("f7", "sub1", "end"),
                new EndEvent("end")));
    }
}
