package io.takari.bpm;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import org.junit.Test;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.model.AbstractElement;
import io.takari.bpm.model.BoundaryEvent;
import io.takari.bpm.model.EndEvent;
import io.takari.bpm.model.ExpressionType;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.model.ServiceTask;
import io.takari.bpm.model.StartEvent;
import io.takari.bpm.model.SubProcess;

public class IndexedProcessDefinitionTest {

    @Test
    public void testSubprocess() throws Exception {
        ProcessDefinition pd = new ProcessDefinition("test", Arrays.<AbstractElement>asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "sub"),
                new SubProcess("sub", Arrays.<AbstractElement>asList(
                        new StartEvent("substart"),
                        new SequenceFlow("f2", "substart", "t1"),
                        new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
                        new SequenceFlow("f3", "t1", "subend"),
                        new EndEvent("subend")
                )),
                new BoundaryEvent("be1", "sub", "errorRef"),
                new SequenceFlow("f4", "be1", "t3"),
                new ServiceTask("t3", ExpressionType.DELEGATE, "${t3}"),
                new SequenceFlow("f5", "t3", "end"),

                new SequenceFlow("f6", "sub", "t2"),
                new ServiceTask("t2", ExpressionType.DELEGATE, "${t2}"),
                new SequenceFlow("f7", "t2", "end"),
                new EndEvent("end")
        ));
        
        IndexedProcessDefinition ipd = new IndexedProcessDefinition(pd);
        
        // ---
        
        List<SequenceFlow> flows = ipd.findOptionalOutgoingFlows("sub");
        
        // ---
        
        assertNotNull(flows);
        assertEquals(1, flows.size());
        
        SequenceFlow f = flows.get(0);
        assertEquals("f6", f.getId());
    }
}
