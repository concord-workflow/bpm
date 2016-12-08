package io.takari.bpm;

import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.model.*;
import org.junit.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;

public class ParallelRepeatingCallsTest extends AbstractEngineTest {

    @Test
    public void test() throws Exception {
        JavaDelegate t1 = mock(JavaDelegate.class);
        getServiceTaskRegistry().register("t1", t1);

        String masterId = "master";
        String par1Id = "par1";
        String par2Id = "par2";

        deploy(parallelCalls(masterId, par1Id));
        deploy(parallelCalls(masterId, par2Id));
        deploy(new ProcessDefinition(par2Id,
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
                new SequenceFlow("f2", "t1", "end"),
                new EndEvent("end")));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, masterId, null);
    }

    private static ProcessDefinition parallelCalls(String id, String calledElement) {
        return new ProcessDefinition(id,
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw1"),
                new ParallelGateway("gw1"),

                new SequenceFlow("f2", "gw1", "call1"),
                new CallActivity("call1", calledElement),
                new SequenceFlow("f3", "call1", "gw2"),

                new SequenceFlow("f4", "gw1", "call2"),
                new CallActivity("call2", calledElement),
                new SequenceFlow("f5", "call2", "gw2"),

                new ParallelGateway("gw2"),
                new SequenceFlow("f6", "gw2", "end"),
                new EndEvent("end"));
    }
}
