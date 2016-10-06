package io.takari.bpm;

import io.takari.bpm.model.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.UUID;

public class ParallelSubEventsTest extends AbstractEngineTest {

    @Test
    public void testParallelSubEvents() throws Exception {
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
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
        )));


        // ---

        String key = randomUuid().toString();
        getEngine().start(key, processId, null);

        // ---

        dumpActivations();
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
                "p2sub_end",
                "p2_f2",
                "p2_end",
                "f5",
                "gw2");

        // ---

        assertNoMoreActivations();

        // ---

        getEngine().resume(key, "p1_ev", null);
        assertActivations(key, processId,
                "f3",
                "gw2",
                "f6",
                "end");

        assertNoMoreActivations();
    }
}
