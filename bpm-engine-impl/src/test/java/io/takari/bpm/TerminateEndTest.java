package io.takari.bpm;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.api.NoEventFoundException;
import io.takari.bpm.model.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class TerminateEndTest extends AbstractEngineTest {

     /**
     * start --> t1 --> terminate
     */
    @Test
    public void testSimple() throws Exception {
        JavaDelegate helloTask = spy(new JavaDelegate() {

            @Override
            public void execute(ExecutionContext ctx) throws ExecutionException {
                System.out.println("Hello, " + ctx.getVariable("name") + "!");
            }
        });
        getServiceTaskRegistry().register("hello", helloTask);

        // ---

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${hello}"),
                new SequenceFlow("f2", "t1", "end"),
                new TerminateEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", "world");
        getEngine().start(key, processId, vars);

        // ---

        assertActivations(key, processId,
                "start",
                "f1",
                "t1",
                "f2",
                "end");
        assertNoMoreActivations();

        // ---

        verify(helloTask, times(1)).execute(any(ExecutionContext.class));
    }

    /**
     * start --> call                          /-> t1 --> end
     *               \                        /
     *                start --> terminate-end
     */
    @Test
    public void testTerminateInCallActiviti() throws Exception {
        JavaDelegate helloTask = spy(new JavaDelegate() {

            @Override
            public void execute(ExecutionContext ctx) throws ExecutionException {
                System.out.println("Hello, " + ctx.getVariable("name") + "!");
            }
        });
        getServiceTaskRegistry().register("hello", helloTask);

        // ---

        String processId = "testA";
        String bId = "testB";

        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "call"),
                new CallActivity("call", bId),
                new SequenceFlow("f2", "call", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${hello}"),
                new SequenceFlow("f3", "t1", "end"),
                new EndEvent("end")
        )));

        deploy(new ProcessDefinition(bId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "terminate-end"),
                new TerminateEvent("terminate-end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", "world");
        getEngine().start(key, processId, vars);

        // ---

        assertActivations(key, bId,
                "start",
                "f1",
                "terminate-end");

        assertActivations(key, processId,
                "start",
                "f1",
                "call");

        assertNoMoreActivations();

        // ---

        verify(helloTask, times(0)).execute(any(ExecutionContext.class));
    }

    /**
     * start --> gw1 --> ev1 --> terminate
     *              \
     *               --> ev2 --> end
     */
    private static ProcessDefinition makeEventProcess(String processId) {
        return new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw1"),
                new InclusiveGateway("gw1"),

                new SequenceFlow("f2", "gw1", "ev1"),
                new IntermediateCatchEvent("ev1", "ev1"),
                new SequenceFlow("f3", "ev1", "terminate-end"),
                new TerminateEvent("terminate-end"),

                new SequenceFlow("f4", "gw1", "ev2"),
                new IntermediateCatchEvent("ev2", "ev2"),
                new SequenceFlow("f5", "ev2", "end"),

                new EndEvent("end")
        ));
    }

    @Test
    public void testInEvent() throws Exception {
        String processId = "test";
        deploy(makeEventProcess(processId));

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

        getEngine().resume(key, "ev1", null);

        // ---

        assertActivations(key, processId,
                "f3",
                "terminate-end");
        assertNoMoreActivations();

        // ---

        try {
            getEngine().resume(key, "ev2", null);
            fail("exception expected");
        } catch (NoEventFoundException e) {
            // expected
        }
    }

    /**
     *
     * start --> gw1 --> p1_gate --> p1_ev -------------------------------------------------------------> gw2 --> end
     *           \--> p2                                                                                /
     *                \--> p2_start --> p2sub                                                   /--> p2_end
     *                                     \--> p2sub_start --> p2sub_gate --> p2sub_ev --> p2sub_terminate
     */
    private static ProcessDefinition makeSubprocessProcess1(String processId) {
        return new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw1"),
                new InclusiveGateway("gw1"),
                // parallel lane 1
                new SequenceFlow("f2", "gw1", "p1_gate"),
                new EventBasedGateway("p1_gate"),
                new SequenceFlow("p1_f1", "p1_gate", "p1_ev"),
                new IntermediateCatchEvent("p1_ev"),
                new SequenceFlow("f3", "p1_ev", "gw2"),

                // parallel lane 2
                new SequenceFlow("f4", "gw1", "p2"),
                new SubProcess("p2", Arrays.asList(
                        new StartEvent("p2_start"),
                        new SequenceFlow("p2_f1", "p2_start", "p2sub"),
                        new SubProcess("p2sub", Arrays.asList(
                                new StartEvent("p2sub_start"),
                                new SequenceFlow("p2sub_f1", "p2sub_start", "p2sub_gate"),
                                new EventBasedGateway("p2sub_gate"),
                                new SequenceFlow("p2sub_f2", "p2sub_gate", "p2sub_ev"),
                                new IntermediateCatchEvent("p2sub_ev"),
                                new SequenceFlow("p2sub_f3", "p2sub_ev", "p2sub_terminate"),
                                new TerminateEvent("p2sub_terminate")
                        )),
                        new SequenceFlow("p2_f2", "p2sub", "p2_end"),
                        new EndEvent("p2_end")
                )),
                new SequenceFlow("f5", "p2", "gw2"),

                new InclusiveGateway("gw2"),
                new SequenceFlow("f6", "gw2", "end"),
                new EndEvent("end")
        ));
    }

    @Test
    public void testSubprocess1() throws Exception {
        String processId = "test";
        deploy(makeSubprocessProcess1(processId));

        // ---

        String key = randomUuid().toString();
        getEngine().start(key, processId, null);

        assertActivations(key, processId,
                "start",
                "f1",
                "gw1",

                "f2",
                "p1_gate",
                "p1_f1",
                "p1_ev",

                "f4",
                "p2",
                "p2_start",
                "p2_f1",
                "p2sub",
                "p2sub_start",
                "p2sub_f1",
                "p2sub_gate",
                "p2sub_f2",
                "p2sub_ev");
        assertNoMoreActivations();

        // ---

        getEngine().resume(key, "p2sub_ev", null);
        assertActivations(key, processId,
                "p2sub_f3",
                "p2sub_terminate");

        // ---

        assertNoMoreActivations();

        // ---

        try {
            getEngine().resume(key, "p1_ev", null);
            fail("exception expected");
        } catch (NoEventFoundException e) {
            // expected
        }
    }

    /**
     *
     * start --> gw1 --> p1_gate --> p1_ev -----------------------------> terminate                      gw2 --> end
     *           \--> p2                                                                                /
     *                \--> p2_start --> p2sub                                                   /--> p2_end
     *                                     \--> p2sub_start --> p2sub_gate --> p2sub_ev --> p2sub_end
     */
    private static ProcessDefinition makeSubprocessProcess2(String processId) {
        return new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw1"),
                new InclusiveGateway("gw1"),
                // parallel lane 1
                new SequenceFlow("f2", "gw1", "p1_gate"),
                new EventBasedGateway("p1_gate"),
                new SequenceFlow("p1_f1", "p1_gate", "p1_ev"),
                new IntermediateCatchEvent("p1_ev"),
                new SequenceFlow("f3", "p1_ev", "terminate_end"),
                new TerminateEvent("terminate_end"),

                // parallel lane 2
                new SequenceFlow("f4", "gw1", "p2"),
                new SubProcess("p2", Arrays.asList(
                        new StartEvent("p2_start"),
                        new SequenceFlow("p2_f1", "p2_start", "p2sub"),
                        new SubProcess("p2sub", Arrays.asList(
                                new StartEvent("p2sub_start"),
                                new SequenceFlow("p2sub_f1", "p2sub_start", "p2sub_gate"),
                                new EventBasedGateway("p2sub_gate"),
                                new SequenceFlow("p2sub_f2", "p2sub_gate", "p2sub_ev"),
                                new IntermediateCatchEvent("p2sub_ev"),
                                new SequenceFlow("p2sub_f3", "p2sub_ev", "p2sub_end"),
                                new EndEvent("p2sub_end")
                        )),
                        new SequenceFlow("p2_f2", "p2sub", "p2_end"),
                        new EndEvent("p2_end")
                )),
                new SequenceFlow("f5", "p2", "gw2"),

                new InclusiveGateway("gw2"),
                new SequenceFlow("f6", "gw2", "end"),
                new EndEvent("end")
        ));
    }

    @Test
    public void testSubprocess2() throws Exception {
        String processId = "test";
        deploy(makeSubprocessProcess2(processId));

        // ---

        String key = randomUuid().toString();
        getEngine().start(key, processId, null);

        assertActivations(key, processId,
                "start",
                "f1",
                "gw1",

                "f2",
                "p1_gate",
                "p1_f1",
                "p1_ev",

                "f4",
                "p2",
                "p2_start",
                "p2_f1",
                "p2sub",
                "p2sub_start",
                "p2sub_f1",
                "p2sub_gate",
                "p2sub_f2",
                "p2sub_ev");
        assertNoMoreActivations();

        // ---

        getEngine().resume(key, "p1_ev", null);
        assertActivations(key, processId,
                "f3",
                "terminate_end");

        // ---

        assertNoMoreActivations();

        // ---

        try {
            getEngine().resume(key, "p2sub_ev", null);
            fail("exception expected");
        } catch (NoEventFoundException e) {
            // expected
        }
    }
}
