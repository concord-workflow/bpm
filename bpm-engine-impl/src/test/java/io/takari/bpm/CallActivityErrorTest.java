package io.takari.bpm;

import io.takari.bpm.Configuration.UnhandledBpmnErrorStrategy;
import io.takari.bpm.api.BpmnError;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.model.*;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class CallActivityErrorTest extends AbstractEngineTest {

    /**
     * start1 --> call                             end1
     *                \                           /
     *                 start2 --> t1 --> errorend2
     */
    @Test
    public void testPropagation() throws Exception {
        getConfiguration().setUnhandledBpmnErrorStrategy(UnhandledBpmnErrorStrategy.PROPAGATE);

        // ---

        String processA = "testA";
        String processB = "testB";
        String errorRef = "error#" + System.currentTimeMillis();

        deploy(new ProcessDefinition(processA,
                new StartEvent("start1"),
                new SequenceFlow("f1", "start1", "call"),
                new CallActivity("call", processB),
                new SequenceFlow("f2", "call", "end1"),
                new EndEvent("end1")));

        deploy(new ProcessDefinition(processB,
                new StartEvent("start2"),
                new SequenceFlow("f1", "start2", "errorend2"),
                new EndEvent("errorend2", errorRef)));

        // ---

        String key = UUID.randomUUID().toString();
        try {
            getEngine().start(key, processA, null);
            fail("Should fail");
        } catch (ExecutionException e) {
            assertNotNull(e.getCause());
            BpmnError err = (BpmnError) e.getCause();
            assertEquals(errorRef, err.getErrorRef());
            assertEquals(processB, err.getDefinitionId());
        }
    }

    /**
     * start1 --> gw1 ---> call                              ---> gw2 --> end1
     *               \         \                            /    /
     *                \         start2 --> ev1 --> errorend2    /
     *                 \                                       /
     *                  -> call                               /
     *                         \                             /
     *                          start2 --> ev1 --> errorend2
     */
    @Test
    public void testPropagationWithSuspend() throws Exception {
        getConfiguration().setUnhandledBpmnErrorStrategy(UnhandledBpmnErrorStrategy.PROPAGATE);
        getServiceTaskRegistry().register("gen", new MessageRefGenerator());

        // ---

        String processA = "testA";
        String processB = "testB";
        String errorRef = "error#" + System.currentTimeMillis();

        deploy(new ProcessDefinition(processA,
                new StartEvent("start1"),
                new SequenceFlow("f1", "start1", "gw1"),
                new InclusiveGateway("gw1"),

                new SequenceFlow("f2", "gw1", "call1"),
                new CallActivity("call1", processB),
                new SequenceFlow("f3", "call1", "gw2"),

                new SequenceFlow("f4", "gw1", "call2"),
                new CallActivity("call2", processB),
                new SequenceFlow("f5", "call2", "gw2"),

                new InclusiveGateway("gw2"),
                new SequenceFlow("f6", "gw2", "end1"),
                new EndEvent("end1")));

        deploy(new ProcessDefinition(processB,
                new StartEvent("start2"),
                new SequenceFlow("f1", "start2", "ev1"),
                IntermediateCatchEvent.messageExpr("ev1", "${gen.nextRef()}"),
                new SequenceFlow("f2", "ev1", "errorend2"),
                new EndEvent("errorend2", errorRef)));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processA, null);

        // ---

        getEngine().resume(key, "test1", null);

        // ---

        try {
            getEngine().resume(key, "test2", null);
            fail("Should fail");
        } catch (ExecutionException e) {
            assertNotNull(e.getCause());
            BpmnError err = (BpmnError) e.getCause();
            assertEquals(errorRef, err.getErrorRef());
            assertEquals(processB, err.getDefinitionId());
        }
    }

    public static class MessageRefGenerator {

        private final AtomicLong counter = new AtomicLong();

        public String nextRef() {
            return "test" + counter.incrementAndGet();
        }
    }
}
