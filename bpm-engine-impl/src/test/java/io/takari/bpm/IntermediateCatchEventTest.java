package io.takari.bpm;

import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.model.AbstractElement;
import io.takari.bpm.model.EndEvent;
import io.takari.bpm.model.ExpressionType;
import io.takari.bpm.model.IntermediateCatchEvent;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.model.ServiceTask;
import io.takari.bpm.model.StartEvent;
import java.util.Arrays;
import java.util.UUID;
import org.junit.Test;
import static org.mockito.Mockito.mock;

public class IntermediateCatchEventTest extends AbstractEngineTest {
    
    /**
     * start --> ev --> end
     */
    @Test
    public void testSingleEvent() throws Exception {
        getServiceTaskRegistry().register("t1", mock(JavaDelegate.class));
        
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.<AbstractElement>asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "ev"),
                new IntermediateCatchEvent("ev", "ev"),
                new SequenceFlow("f2", "ev", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
                new SequenceFlow("f3", "t1", "end"),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);

        // ---

        getEngine().resume(key, "ev", null);

        // ---

        assertActivations(key, processId,
                "start",
                "f1",
                "ev",
                "f2",
                "t1",
                "f3",
                "end");
        assertNoMoreActivations();
    }
}
