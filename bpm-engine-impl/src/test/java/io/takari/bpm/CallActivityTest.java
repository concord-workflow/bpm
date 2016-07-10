package io.takari.bpm;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.model.AbstractElement;
import io.takari.bpm.model.BoundaryEvent;
import io.takari.bpm.model.CallActivity;
import io.takari.bpm.model.EndEvent;
import io.takari.bpm.model.EventBasedGateway;
import io.takari.bpm.model.ExpressionType;
import io.takari.bpm.model.InclusiveGateway;
import io.takari.bpm.model.IntermediateCatchEvent;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.model.ServiceTask;
import io.takari.bpm.model.StartEvent;
import io.takari.bpm.model.VariableMapping;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import static org.junit.Assert.*;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class CallActivityTest extends AbstractEngineTest {

    /**
     * start --> call               end
     *               \             /
     *                start --> end
     */
    @Test
    public void testSimple() throws Exception {
        String aId = "testA";
        String bId = "testB";

        deploy(new ProcessDefinition(aId, Arrays.<AbstractElement>asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "call"),
                new CallActivity("call", bId),
                new SequenceFlow("f2", "call", "end"),
                new EndEvent("end")
        )));

        deploy(new ProcessDefinition(bId, Arrays.<AbstractElement>asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "end"),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, aId, null);

        // ---

        assertActivations(key, aId,
                "start",
                "f1",
                "call");

        assertActivations(key, bId,
                "start",
                "f1",
                "end");

        assertActivations(key, aId,
                "f2",
                "end");

        assertNoMoreActivations();
    }

    /**
     * start --> call                              end
     *               \                            /
     *                start --> gw --> ev1 --> end
     *                            \          /
     *                             --> ev2 --
     */
    @Test
    public void testEventGateway() throws Exception {
        String aId = "testA";
        String bId = "testB";
        String ev1 = "ev1";
        String ev2 = "ev2";

        deploy(new ProcessDefinition(aId, Arrays.<AbstractElement>asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "call"),
                new CallActivity("call", bId),
                new SequenceFlow("f2", "call", "end"),
                new EndEvent("end")
        )));

        deploy(new ProcessDefinition(bId, Arrays.<AbstractElement>asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw"),
                new EventBasedGateway("gw"),
                    new SequenceFlow("f2", "gw", ev1),
                    new IntermediateCatchEvent(ev1, ev1),
                    new SequenceFlow("f3", ev1, "end"),

                    new SequenceFlow("f4", "gw", ev2),
                    new IntermediateCatchEvent(ev2, ev2),
                    new SequenceFlow("f5", ev2, "end"),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, aId, null);

        assertActivations(key, aId,
                "start",
                "f1",
                "call");

        assertActivations(key, bId,
                "start",
                "f1",
                "gw",
                "f2",
                ev1,
                "f4",
                ev2);

        assertNoMoreActivations();

        // ---

        getEngine().resume(key, ev1, null);

        assertActivations(key, bId,
                "f3",
                "end");
        assertActivations(key, aId,
                "f2",
                "end");

        assertNoMoreActivations();
    }
    
    /**
     * start --> call               t1 --> end
     *               \             /
     *                start --> end
     */
    @Test
    public void testOutVariables() throws Exception {
        String aId = "testA";
        String bId = "testB";
        final String beforeK = "beforeK" + System.currentTimeMillis();
        final String insideK = "insideK" + System.currentTimeMillis();
        final String outsidek = "outsideK" + System.currentTimeMillis();
        final Object v = "v" + System.currentTimeMillis();
        
        Set<VariableMapping> ins = new HashSet<>();
        ins.add(new VariableMapping(null, "${" + beforeK + "}", insideK));
        
        Set<VariableMapping> outs = new HashSet<>();
        outs.add(new VariableMapping(null, "${" + insideK + "}", outsidek));

        deploy(new ProcessDefinition(aId, Arrays.<AbstractElement>asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "call"),
                new CallActivity("call", bId, ins, outs),
                new SequenceFlow("f2", "call", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
                new SequenceFlow("f3", "t1", "end"),
                new EndEvent("end")
        )));

        deploy(new ProcessDefinition(bId, Arrays.<AbstractElement>asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "end"),
                new EndEvent("end")
        )));
        
        JavaDelegate t1Task = spy(new JavaDelegate() {

            @Override
            public void execute(ExecutionContext ctx) throws Exception {
                Object o = ctx.getVariable(outsidek);
                assertEquals(v, o);
            }
        });
        getServiceTaskRegistry().register("t1", t1Task);

        // ---

        String key = UUID.randomUUID().toString();
        Map<String, Object> input = new HashMap<>();
        input.put(beforeK, v);
        getEngine().start(key, aId, input);

        // ---

        assertActivations(key, aId,
                "start",
                "f1",
                "call");

        assertActivations(key, bId,
                "start",
                "f1",
                "end");

        assertActivations(key, aId,
                "f2",
                "t1",
                "f3",
                "end");

        assertNoMoreActivations();
        
        // ---
        
        verify(t1Task, times(1)).execute(any(ExecutionContext.class));
    }
    
    /**
     * start --> call                       end
     *               \                     /
     *                start --> ev1 --> end
     */
    @Test
    public void testError() throws Exception {
        String aId = "testA";
        String bId = "testB";
        String errorRef = "e" + System.currentTimeMillis();
        String messageRef = "m" + System.currentTimeMillis();

        deploy(new ProcessDefinition(aId, Arrays.<AbstractElement>asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "call"),
                new CallActivity("call", bId),
                new BoundaryEvent("be", "call", errorRef),
                new SequenceFlow("f2", "call", "end"),
                new SequenceFlow("f3", "be", "end"),
                new EndEvent("end")
        )));

        deploy(new ProcessDefinition(bId, Arrays.<AbstractElement>asList(
                new StartEvent("bstart"),
                new SequenceFlow("bf1", "bstart", "bev1"),
                new IntermediateCatchEvent("bev1", messageRef),
                new SequenceFlow("bf2", "bev1", "bend"),
                new EndEvent("bend", errorRef)
        )));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, aId, null);

        // ---

        assertActivations(key, aId,
                "start",
                "f1",
                "call");

        assertActivations(key, bId,
                "bstart",
                "bf1",
                "bev1");
        
        getEngine().resume(key, messageRef, null);

        assertActivations(key, bId,
                "bf2",
                "bend");
        
        assertActivations(key, aId,
                "f3",
                "end");

        assertNoMoreActivations();
    }
    
    /**
     * start --> call                                               --------------> t1 --> end
     *                \                                            /                      /
     *                 start --> gw1 --> ev1 --> gw2 --> t2 --> end                      /
     *                              \                                                   /
     *                               call                                              /
     *                                   \                                            /
     *                                    start --> gw1 --> ev2 --> gw2 --> t3 --> end
     *                                                 \          /
     *                                                  --> ev3 --
     */
    @Test
    public void testDeeplyNested() throws Exception {
        JavaDelegate t1 = mock(JavaDelegate.class);
        getServiceTaskRegistry().register("t1", t1);
        JavaDelegate t2 = mock(JavaDelegate.class);
        getServiceTaskRegistry().register("t2", t2);
        JavaDelegate t3 = mock(JavaDelegate.class);
        getServiceTaskRegistry().register("t3", t3);
        
        String outerProcId = "outer";
        String nestedProcId = "nested";
        String deeplyNestedProcId = "deep";

        deploy(new ProcessDefinition(outerProcId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "call"),
                new CallActivity("call", nestedProcId),
                new SequenceFlow("f2", "call", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
                new SequenceFlow("f3", "t1", "endOuter"),
                new EndEvent("endOuter"))));

        deploy(new ProcessDefinition(nestedProcId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw1"),
                new InclusiveGateway("gw1"),
                    new SequenceFlow("f2", "gw1", "ev1"),
                    new IntermediateCatchEvent("ev1", "ev1"),
                    new SequenceFlow("f3", "ev1", "gw2"),

                    new SequenceFlow("f4", "gw1", "call"),
                    new CallActivity("call", deeplyNestedProcId),
                    new SequenceFlow("f5", "call", "gw2"),

                new InclusiveGateway("gw2"),
                new SequenceFlow("f6", "gw2", "t2"),
                new ServiceTask("t2", ExpressionType.DELEGATE, "${t2}"),
                new SequenceFlow("f7", "t2", "endNested"),
                new EndEvent("endNested"))));

        deploy(new ProcessDefinition(deeplyNestedProcId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw1"),
                new InclusiveGateway("gw1"),
                    new SequenceFlow("f2", "gw1", "ev2"),
                    new IntermediateCatchEvent("ev2", "ev2"),
                    new SequenceFlow("f3", "ev2", "gw2"),

                    new SequenceFlow("f4", "gw1", "ev3"),
                    new IntermediateCatchEvent("ev3", "ev3"),
                    new SequenceFlow("f5", "ev3", "gw2"),

                new InclusiveGateway("gw2"),
                new SequenceFlow("f6", "gw2", "t3"),
                new ServiceTask("t3", ExpressionType.DELEGATE, "${t3}"),
                new SequenceFlow("f7", "t3", "endDeep"),
                new EndEvent("endDeep"))));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, outerProcId, null);

        assertActivations(key, outerProcId,
                "start",
                "f1",
                "call");

        assertActivations(key, nestedProcId,
                "start",
                "f1",
                "gw1",
                "f2",
                "ev1",
                "f4",
                "call");

        assertActivations(key, deeplyNestedProcId,
                "start",
                "f1",
                "gw1",
                "f2",
                "ev2",
                "f4",
                "ev3");

        assertNoMoreActivations();

        // ---

        getEngine().resume(key, "ev1", null);

        assertActivations(key, nestedProcId,
                "f3",
                "gw2");

        assertNoMoreActivations();

        // ---

        getEngine().resume(key, "ev2", null);

        assertActivations(key, deeplyNestedProcId,
                "f3",
                "gw2");

        assertNoMoreActivations();

        // ---

        getEngine().resume(key, "ev3", null);

        assertActivations(key, deeplyNestedProcId,
                "f5",
                "gw2",
                "f6",
                "t3",
                "f7",
                "endDeep");

        assertActivations(key, nestedProcId,
                "f5",
                "gw2",
                "f6",
                "t2",
                "f7",
                "endNested");

        assertActivations(key, outerProcId,
                "f2",
                "t1",
                "f3",
                "endOuter");

        assertNoMoreActivations();

        // ---

        verify(t1, times(1)).execute(any(ExecutionContext.class));
        verify(t2, times(1)).execute(any(ExecutionContext.class));
        verify(t3, times(1)).execute(any(ExecutionContext.class));
    }
}
