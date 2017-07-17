package io.takari.bpm;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.model.BoundaryEvent;
import io.takari.bpm.model.EndEvent;
import io.takari.bpm.model.EventBasedGateway;
import io.takari.bpm.model.ExpressionType;
import io.takari.bpm.model.InclusiveGateway;
import io.takari.bpm.model.IntermediateCatchEvent;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.model.ServiceTask;
import io.takari.bpm.model.StartEvent;
import io.takari.bpm.model.SubProcess;

import java.util.Arrays;
import java.util.UUID;

import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class InclusiveGatewayTest extends AbstractEngineTest {

    /**
     * start --> gw1 --> ev --> gw2 --> end
     */
    @Test
    public void testSingleEvent() throws Exception {
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw1"),
                new InclusiveGateway("gw1"),
                    new SequenceFlow("f2", "gw1", "ev"),
                    new IntermediateCatchEvent("ev", "ev"),
                    new SequenceFlow("f3", "ev", "gw2"),
                new InclusiveGateway("gw2"),
                new SequenceFlow("f4", "gw2", "end"),
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
                "gw1",
                "f2",
                "ev",
                "f3",
                "gw2",
                "f4",
                "end");
        assertNoMoreActivations();
    }

    /**
     * start --> gw1 --> ev1 --> gw2 --> end
     *              \           /
     *               --> ev2 -->
     */
    private ProcessDefinition makeDuoEventProcess(String processId) {
       return new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw1"),
                new InclusiveGateway("gw1"),

                    new SequenceFlow("f2", "gw1", "ev1"),
                    new IntermediateCatchEvent("ev1", "ev1"),
                    new SequenceFlow("f3", "ev1", "gw2"),

                    new SequenceFlow("f4", "gw1", "ev2"),
                    new IntermediateCatchEvent("ev2", "ev2"),
                    new SequenceFlow("f5", "ev2", "gw2"),

                new InclusiveGateway("gw2"),
                new SequenceFlow("f6", "gw2", "end"),
                new EndEvent("end")
        ));
    }

    @Test
    public void testDuoEvent() throws Exception {
        String processId = "test";
        deploy(makeDuoEventProcess(processId));

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

    @Test
    public void testDuoEventReverseOrder() throws Exception {
        String processId = "test";
        deploy(makeDuoEventProcess(processId));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);
        getEngine().resume(key, "ev2", null);
        getEngine().resume(key, "ev1", null);

        assertActivations(key, processId,
                "start",
                "f1",
                "gw1",
                "f2",
                "ev1",
                "f4",
                "ev2",
                "f5",
                "gw2",
                "f3",
                "gw2",
                "f6",
                "end");
        assertNoMoreActivations();
    }

    /**
     * start --> gw1 --> t1 --> ev1 --> gw2 --> t3 --> end
     *              \                  /
     *               --> t2 --> ev2 -->
     */
    @Test
    public void testDuoEventComplex() throws Exception {
        JavaDelegate taskA = mock(JavaDelegate.class);
        getServiceTaskRegistry().register("taskA", taskA);
        
        JavaDelegate taskB = mock(JavaDelegate.class);
        getServiceTaskRegistry().register("taskB", taskB);
        
        JavaDelegate taskC = mock(JavaDelegate.class);
        getServiceTaskRegistry().register("taskC", taskC);
        
        // ---
        
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw1"),
                new InclusiveGateway("gw1"),

                    new SequenceFlow("f2", "gw1", "t1"),
                    new ServiceTask("t1", ExpressionType.DELEGATE, "${taskA}"),
                    new SequenceFlow("f3", "t1", "ev1"),
                    new IntermediateCatchEvent("ev1", "ev1"),
                    new SequenceFlow("f4", "ev1", "gw2"),

                    new SequenceFlow("f5", "gw1", "t2"),
                    new ServiceTask("t2", ExpressionType.DELEGATE, "${taskB}"),
                    new SequenceFlow("f6", "t2", "ev2"),
                    new IntermediateCatchEvent("ev2", "ev2"),
                    new SequenceFlow("f7", "ev2", "gw2"),

                new InclusiveGateway("gw2"),
                new SequenceFlow("f8", "gw2", "t3"),
                new ServiceTask("t3", ExpressionType.DELEGATE, "${taskC}"),
                new SequenceFlow("f9", "t3", "end"),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);
        getEngine().resume(key, "ev1", null);
        getEngine().resume(key, "ev2", null);
        
        // ---
        
        verify(taskA, times(1)).execute(any(ExecutionContext.class));
        verify(taskB, times(1)).execute(any(ExecutionContext.class));
        verify(taskC, times(1)).execute(any(ExecutionContext.class));
    }
    
    /**
     * start --> gw1 --> t1 ------> gw2 --> end
     *              \             /
     *               \--> t2 ---->
     *                \          /
     *                 --> t3 -->
     */
    @Test
    public void testPartiallyInactive() throws Exception {
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw1"),
                new InclusiveGateway("gw1"),

                    new SequenceFlow("f2", "gw1", "t1"),
                    new ServiceTask("t1"),
                    new SequenceFlow("f3", "t1", "gw2"),

                    new SequenceFlow("f4", "gw1", "t2", "${false}"),
                    new ServiceTask("t2"),
                    new SequenceFlow("f5", "t2", "gw2"),

                    new SequenceFlow("f6", "gw1", "t3", "${false}"),
                    new ServiceTask("t3"),
                    new SequenceFlow("f7", "t3", "gw2"),

                new InclusiveGateway("gw2"),
                new SequenceFlow("f8", "gw2", "end"),
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
                "t1",
                "f3",
                "gw2",
                "f8",
                "end");
        assertNoMoreActivations();
    }
    
    /**
     * start --> gw1 --> sub1 -> evgw1 -> ev1 -> end(err) ---> gw2 --> end
     *              \                                         /
     *               \--> sub2 -> evgw2 -> ev2 -> end ------>
     *                                             \       /
     *                                             boundary
     */
    @Test
    public void testParallelSubEvents() throws Exception {
        String processId = "test";
        
        deploy(new ProcessDefinition(processId, Arrays.asList(
            new StartEvent("start"),
            new SequenceFlow("f1", "start", "gw1"),
            new InclusiveGateway("gw1"),

                new SequenceFlow("f2", "gw1", "sub1"),
                new SubProcess("sub1", Arrays.asList(
                    new StartEvent("sub1_start"),
                    new SequenceFlow("f3", "sub1_start", "sub1_gw"),
                    new EventBasedGateway("sub1_gw"),
                    new SequenceFlow("f4", "sub1_gw", "ev1"),
                    new IntermediateCatchEvent("ev1", "ev1"),
                    new SequenceFlow("f5", "ev1", "sub1_end"),
                    new EndEvent("sub1_end", "err")
                )),
                new SequenceFlow("f6", "sub1", "gw2"),
                
                new SequenceFlow("f7", "gw1", "sub2"),
                new SubProcess("sub2", Arrays.asList(
                    new StartEvent("sub2_start"),
                    new SequenceFlow("f8", "sub2_start", "sub2_gw"),
                    new EventBasedGateway("sub2_gw"),
                    new SequenceFlow("f9", "sub2_gw", "ev2"),
                    new IntermediateCatchEvent("ev2", "ev2"),
                    new SequenceFlow("f10", "ev2", "sub2_end"),
                    new EndEvent("sub2_end")
                )),
                new SequenceFlow("f11", "sub2", "gw2"),
                
                new BoundaryEvent("b1", "sub2", null),
                new SequenceFlow("f12", "b1", "gw2"),
                
            new InclusiveGateway("gw2"),
            new SequenceFlow("f13", "gw2", "end"),
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
            
            "f2", // ev1
            "sub1",
            "sub1_start",
            "f3",
            "sub1_gw",
            "f4",
            "ev1",
            
            "f7", // ev2
            "sub2",
            "sub2_start",
            "f8",
            "sub2_gw",
            "f9",
            "ev2");
        assertNoMoreActivations();
        
        // ---
        
        getEngine().resume(key, "ev1", null);
        
        assertActivations(key, processId,
            "f5",
            "sub1_end");
        assertNoMoreActivations();
        
        // ---
        
        getEngine().resume(key, "ev2", null);
        
        assertActivations(key, processId,
            "f10",
            "sub2_end",
            "f11",
            "gw2",
            "f6",
            "gw2",
            "f13",
            "end");
        assertNoMoreActivations();
    }
    
    /**
     *                 <------- (x<3?)
     *                /            \
     * start (x=0) -> t1 (x++) --> gw1 -> t2 -> gw2 -> end
     * 
     */
    @Test
    public void testSelfAgitatingLoop() throws Exception {

        JavaDelegate t1 = spy(new JavaDelegate() {
            int x = 0;
            @Override
            public void execute(ExecutionContext ctx) throws Exception {
                ctx.setVariable("x", ++x);
            }
        });
        JavaDelegate t2 = mock(JavaDelegate.class);
        
        getServiceTaskRegistry().register("t1", t1);
        getServiceTaskRegistry().register("t2", t2);
  
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
            new StartEvent("start"),
            
            new SequenceFlow("ft1", "start", "t1"),
            new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
            
            new SequenceFlow("fgw1", "t1", "gw1"),
            new InclusiveGateway("gw1"),
            
            new SequenceFlow("floop", "gw1", "t1", "${x < 3}"),
            new SequenceFlow("ft2", "gw1", "t2"),
            
            new ServiceTask("t2", ExpressionType.DELEGATE, "${t2}"),
            
            new SequenceFlow("fgw2", "t2", "gw2"),
            new InclusiveGateway("gw2"),
            
            new SequenceFlow("fend", "gw2", "end"),
            new EndEvent("end")
        )));
  
        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);
        
        verify(t1, times(3)).execute(any(ExecutionContext.class));
        verify(t2, times(3)).execute(any(ExecutionContext.class));
  
        assertActivations(key, processId,
            "start", 
            
            // x == 0
            "ft1", "t1",
            "fgw1", "gw1",
  
            // x == 1
            "floop", "t1",
            "fgw1", "gw1",
  
            // x == 2
            "floop", "t1",
            "fgw1", "gw1",
            
            // y == 0
            "ft2", "t2",
            "fgw2", "gw2",
            
            // y == 1
            "ft2", "t2",
            "fgw2", "gw2",
            
            // y == 2
            "ft2", "t2",
            "fgw2", "gw2",
            
            // done
            "fend", "end"
            );
        assertNoMoreActivations();
    }

}
