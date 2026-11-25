package io.takari.bpm;

import io.takari.bpm.api.BpmnError;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.model.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class TimerBoundaryFailureTest extends AbstractEngineTest {

    /**
     * start --> t1 (throws) --> end
     *            \
     *             timer --> end
     */
    @Test
    public void testExceptionInTaskWithTimer() {
        getServiceTaskRegistry().register("boom", (JavaDelegate) ctx -> {
            throw new RuntimeException("boom");
        });

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${boom}"),
                new BoundaryEvent("timer", "t1", null, "PT30S"),
                new SequenceFlow("f2", "timer", "end"),
                new SequenceFlow("f3", "t1", "end"),
                new EndEvent("end"))));

        ExecutionException e = assertTimeoutPreemptively(Duration.ofSeconds(5),
                () -> assertThrows(ExecutionException.class, () -> getEngine().start(UUID.randomUUID().toString(), processId, null)));
        assertTrue(e.getCause() instanceof RuntimeException);
        assertEquals("boom", e.getCause().getMessage());
    }

    /**
     * start --> t1 (throws a BPMN error) --> end
     *            \  \
     *             \  error --> t2 --> end
     *              timer --> end
     */
    @Test
    public void testBpmnErrorInTaskWithTimer() throws Exception {
        List<String> seen = new CopyOnWriteArrayList<>();
        getServiceTaskRegistry().register("fail", (JavaDelegate) ctx -> {
            throw new BpmnError("e1");
        });
        getServiceTaskRegistry().register("t2", (JavaDelegate) ctx -> seen.add("t2"));

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${fail}"),
                new BoundaryEvent("timer", "t1", null, "PT30S"),
                new SequenceFlow("f2", "timer", "end"),
                new BoundaryEvent("error", "t1", "e1"),
                new SequenceFlow("f3", "error", "t2"),
                new ServiceTask("t2", ExpressionType.DELEGATE, "${t2}"),
                new SequenceFlow("f4", "t2", "end"),
                new SequenceFlow("f5", "t1", "end"),
                new EndEvent("end"))));

        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> getEngine().start(UUID.randomUUID().toString(), processId, null));
        assertEquals(List.of("t2"), seen);
    }

    /**
     * The variables set by a timed out task are discarded.
     * <pre>
     * start --> t1 (slow) --> end
     *            \
     *             timer --> t2 --> end
     * </pre>
     */
    @Test
    public void testTimedOutTaskChangesAreDiscarded() throws Exception {
        List<Object> seen = new CopyOnWriteArrayList<>();
        getServiceTaskRegistry().register("slow", (JavaDelegate) ctx -> {
            ctx.setVariable("early", true);
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                // the engine cancels the task, set a variable anyway
            }
            ctx.setVariable("late", true);
        });
        getServiceTaskRegistry().register("t2", (JavaDelegate) ctx -> {
            seen.add(ctx.getVariable("early"));
            seen.add(ctx.getVariable("late"));
        });

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${slow}"),
                new BoundaryEvent("timer", "t1", null, "PT0.2S"),
                new SequenceFlow("f2", "timer", "t2"),
                new ServiceTask("t2", ExpressionType.DELEGATE, "${t2}"),
                new SequenceFlow("f3", "t2", "end"),
                new SequenceFlow("f4", "t1", "end"),
                new EndEvent("end"))));

        getEngine().start(UUID.randomUUID().toString(), processId, null);

        assertEquals(Arrays.asList(null, null), seen);
    }

    /**
     * Timer boundary events are not supported for subprocesses, they must not be ignored silently.
     */
    @Test
    public void testTimerOnSubprocessIsRejected() {
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "sub"),
                new SubProcess("sub", Arrays.asList(
                        new StartEvent("sstart"),
                        new SequenceFlow("s1", "sstart", "ev"),
                        new IntermediateCatchEvent("ev", "ev"),
                        new SequenceFlow("s2", "ev", "send"),
                        new EndEvent("send"))),
                new BoundaryEvent("timer", "sub", null, "PT1S"),
                new SequenceFlow("f2", "timer", "end"),
                new SequenceFlow("f3", "sub", "end"),
                new EndEvent("end"))));

        ExecutionException e = assertThrows(ExecutionException.class,
                () -> getEngine().start(UUID.randomUUID().toString(), processId, null));
        assertTrue(e.getMessage().contains("supported only for service tasks"), e.getMessage());
    }
}
