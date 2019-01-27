package io.takari.bpm;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.model.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class ForceSuspendTest extends AbstractEngineTest {

    /**
     * start --> t1 --> end
     */
    @Test
    public void testSimple() throws Exception {
        String messageRef = "test#" + System.currentTimeMillis();

        JavaDelegate t1 = spy(new JavaDelegate() {

            @Override
            public void execute(ExecutionContext ctx) throws ExecutionException {
                ctx.suspend(messageRef);
            }
        });
        getServiceTaskRegistry().register("t1", t1);

        // ---

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
        getEngine().start(key, processId, null);

        // ---

        assertActivations(key, processId,
                "start",
                "f1",
                "t1");
        assertNoMoreActivations();

        // ---

        verify(t1, times(1)).execute(any(ExecutionContext.class));

        // ---

        getEngine().resume(key, messageRef, null);

        // ---

        assertActivations(key, processId,
                "f2",
                "end");
        assertNoMoreActivations();
    }

    /**
     * start --> t1 --> end
     */
    @Test
    public void testSimpleWithScript() throws Exception {
        String messageRef = "test#" + System.currentTimeMillis();

        // ---

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ScriptTask("t1", ScriptTask.Type.CONTENT, "javascript", "execution.suspend('" + messageRef + "')"),
                new SequenceFlow("f2", "t1", "end"),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);

        // ---

        assertActivations(key, processId,
                "start",
                "f1",
                "t1");
        assertNoMoreActivations();

        // ---

        getEngine().resume(key, messageRef, null);

        // ---

        assertActivations(key, processId,
                "f2",
                "end");
        assertNoMoreActivations();
    }

    /**
     * start --> gw1 --> ev1 --> gw2 --> end
     *              \           /
     *               --> ev2 -->
     */
    @Test
    public void testDuoEvent() throws Exception {
        String messageRef = "test#" + System.currentTimeMillis();

        JavaDelegate t1 = spy(new JavaDelegate() {

            @Override
            public void execute(ExecutionContext ctx) throws ExecutionException {
                ctx.suspend(messageRef);
            }
        });
        getServiceTaskRegistry().register("t1", t1);

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw1"),
                new InclusiveGateway("gw1"),

                new SequenceFlow("f2", "gw1", "ev1"),
                new ServiceTask("ev1", ExpressionType.DELEGATE, "${t1}"),
                new SequenceFlow("f3", "ev1", "gw2"),

                new SequenceFlow("f4", "gw1", "ev2"),
                new IntermediateCatchEvent("ev2", "ev2"),
                new SequenceFlow("f5", "ev2", "gw2"),

                new InclusiveGateway("gw2"),
                new SequenceFlow("f6", "gw2", "end"),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);

        // ---

        assertActivations(key, processId,
                "start",
                "f1",
                "gw1",
                "f2",
                "ev1",
                "f4",
                "ev2");
        assertNoMoreActivations();

        // ---

        getEngine().resume(key, messageRef, null);

        // ---

        assertActivations(key, processId,
                "f3",
                "gw2");
        assertNoMoreActivations();

        // ---

        getEngine().resume(key, "ev2", null);

        // ---

        assertActivations(key, processId,
                "f5",
                "gw2",
                "f6",
                "end");
        assertNoMoreActivations();
    }
}
