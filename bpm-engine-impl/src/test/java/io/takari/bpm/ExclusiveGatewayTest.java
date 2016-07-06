package io.takari.bpm;

import io.takari.bpm.model.AbstractElement;
import io.takari.bpm.model.EndEvent;
import io.takari.bpm.model.ExclusiveGateway;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.model.StartEvent;
import java.util.Arrays;
import java.util.UUID;
import org.junit.Test;

public class ExclusiveGatewayTest extends AbstractEngineTest {
    
    /**
     * start --> gw --fA--> end
     *             \       /
     *              --fB-->
     */
    @Test
    public void testDefaultFlow() throws Exception {
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.<AbstractElement>asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw"),
                new ExclusiveGateway("gw", "fB"),
                new SequenceFlow("fA", "gw", "end"),
                new SequenceFlow("fB", "gw", "end"),
                new EndEvent("end")
        )));
        
        // ---
        
        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);
        
        // ---
        
        assertActivations(key, processId,
                "start",
                "f1",
                "gw",
                "fB",
                "end");
        assertNoMoreActivations();
    }
    
    @Test
    public void testExpressions() throws Exception {
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.<AbstractElement>asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw"),
                new ExclusiveGateway("gw", "fA"),
                new SequenceFlow("fA", "gw", "end", "${0 == 1}"),
                new SequenceFlow("fB", "gw", "end", "${1 == 1}"),
                new EndEvent("end")
        )));
        
        // ---
        
        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);
        
        // ---
        
        assertActivations(key, processId,
                "start",
                "f1",
                "gw",
                "fB",
                "end");
        assertNoMoreActivations();
    }
}
