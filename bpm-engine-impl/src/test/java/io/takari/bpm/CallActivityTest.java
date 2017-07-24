package io.takari.bpm;

import io.takari.bpm.api.BpmnError;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.model.*;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

        deploy(new ProcessDefinition(aId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "call"),
                new CallActivity("call", bId),
                new SequenceFlow("f2", "call", "end"),
                new EndEvent("end")
        )));

        deploy(new ProcessDefinition(bId, Arrays.asList(
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

        deploy(new ProcessDefinition(aId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "call"),
                new CallActivity("call", bId),
                new SequenceFlow("f2", "call", "end"),
                new EndEvent("end")
        )));

        deploy(new ProcessDefinition(bId, Arrays.asList(
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
        ins.add(VariableMapping.eval("${" + beforeK + "}", insideK));

        Set<VariableMapping> outs = new HashSet<>();
        outs.add(VariableMapping.eval("${" + insideK + "}", outsidek));

        deploy(new ProcessDefinition(aId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "call"),
                new CallActivity("call", bId, ins, outs),
                new SequenceFlow("f2", "call", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
                new SequenceFlow("f3", "t1", "end"),
                new EndEvent("end")
        )));

        deploy(new ProcessDefinition(bId, Arrays.asList(
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
     * start --> call                             t1 --> end
     *               \                           /
     *                start --> gw --> ev --> end
     */
    @Test
    public void testEventAndOutVariables() throws Exception {
        String aId = "testA";
        String bId = "testB";

        String varKey = "key_" + System.currentTimeMillis();
        Object varVal = "val_" + System.currentTimeMillis();

        Set<VariableMapping> outs = new HashSet<>();
        outs.add(VariableMapping.copy(varKey, varKey));

        deploy(new ProcessDefinition(aId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "call"),
                new CallActivity("call", bId, null, outs),
                new SequenceFlow("f2", "call", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
                new SequenceFlow("f3", "t1", "end"),
                new EndEvent("end")
        )));

        deploy(new ProcessDefinition(bId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw"),
                new EventBasedGateway("gw"),
                new SequenceFlow("f2", "gw", "ev"),
                new IntermediateCatchEvent("ev", "ev"),
                new SequenceFlow("f3", "ev", "end"),
                new EndEvent("end")
        )));

        JavaDelegate t1Task = spy(new JavaDelegate() {

            @Override
            public void execute(ExecutionContext ctx) throws Exception {
                Object o = ctx.getVariable(varKey);
                assertEquals(varVal, o);
            }
        });
        getServiceTaskRegistry().register("t1", t1Task);

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, aId, null);

        // ---

        Map<String, Object> args = new HashMap<>();
        args.put(varKey, varVal);
        getEngine().resume(key, "ev", args);

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

        deploy(new ProcessDefinition(aId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "call"),
                new CallActivity("call", bId),
                new BoundaryEvent("be", "call", errorRef),
                new SequenceFlow("f2", "call", "end"),
                new SequenceFlow("f3", "be", "end"),
                new EndEvent("end")
        )));

        deploy(new ProcessDefinition(bId, Arrays.asList(
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
                "be",
                "f3",
                "end");

        assertNoMoreActivations();
    }


    /**
     * start --> gw --> call                ----------
     *                      \              /          \
     *                       start --> end ---error1-----> gw --> end
     *                                     \          /
     *                                      --error2--
     */
    @Test
    public void testMultipleErrorTypesGw() throws Exception {
        String aId = "testA";
        String bId = "testB";

        String errorA = "errorA#" + System.currentTimeMillis();
        String errorB = "errorB#" + System.currentTimeMillis();

        deploy(new ProcessDefinition(aId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw1"),
                new InclusiveGateway("gw1"),
                new SequenceFlow("f2", "gw1", "call"),
                new CallActivity("call", bId),

                // normal
                new SequenceFlow("f3", "call", "gw2"),

                // errorA
                new BoundaryEvent("beA", "call", errorA),
                new SequenceFlow("f4", "beA", "gw2"),

                // errorB
                new BoundaryEvent("beB", "call", errorB),
                new SequenceFlow("f5", "beB", "gw2"),

                new InclusiveGateway("gw2"),
                new SequenceFlow("f6", "gw2", "end"),
                new EndEvent("end")
        )));

        deploy(new ProcessDefinition(bId, Arrays.asList(
                new StartEvent("bstart"),
                new SequenceFlow("bf1", "bstart", "bend"),
                new EndEvent("bend", errorB)
        )));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, aId, null);

        // ---

        assertActivations(key, aId,
                "start",
                "f1",
                "gw1",
                "f2",
                "call");

        assertActivations(key, bId,
                "bstart",
                "bf1",
                "bend");

        assertActivations(key, aId,
                "beB",
                "f5",
                "gw2",
                "f6",
                "end");

        assertNoMoreActivations();
    }

    /**
     * start --> call                      end
     *               \                    /
     *                start --> t1 --> end
     */
    @Test
    public void testDefaultError() throws Exception {
        String aId = "testA";
        String bId = "testB";
        String errorRef = "e1-" + System.currentTimeMillis();

        JavaDelegate t = mock(JavaDelegate.class);
        doThrow(BpmnError.class).when(t).execute(any(ExecutionContext.class));

        getServiceTaskRegistry().register("t", t);

        deploy(new ProcessDefinition(aId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "call"),
                new CallActivity("call", bId),
                new BoundaryEvent("be1", "call", errorRef),
                new BoundaryEvent("be2", "call", null),
                new SequenceFlow("f2", "call", "end"),
                new SequenceFlow("f3", "be1", "end"),
                new SequenceFlow("f4", "be2", "end"),
                new EndEvent("end")
        )));

        deploy(new ProcessDefinition(bId, Arrays.asList(
                new StartEvent("bstart"),
                new SequenceFlow("bf1", "bstart", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${t}"),
                new SequenceFlow("bf2", "t1", "bend"),
                new EndEvent("bend")
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
                "t1");

        assertActivations(key, aId,
                "be2",
                "f4",
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

    /**
     * start --> call                      end
     *               \                    /
     *                start --> t1 --> end
     */
    @Test
    public void testCopyAllVariables() throws Exception {
        final String varKey = "key#" + System.currentTimeMillis();
        final String varValue = "value#" + System.currentTimeMillis();

        JavaDelegate t1 = spy(new JavaDelegate() {

            @Override
            public void execute(ExecutionContext ctx) throws Exception {
                Object v = ctx.getVariable(varKey);
                assertEquals(varValue, v);
            }
        });
        getServiceTaskRegistry().register("t1", t1);

        // ---

        String aId = "testA";
        String bId = "testB";

        deploy(new ProcessDefinition(aId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "call"),
                new CallActivity("call", bId, true),
                new SequenceFlow("f2", "call", "end"),
                new EndEvent("end"))));

        deploy(new ProcessDefinition(bId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
                new SequenceFlow("f2", "t1", "end"),
                new EndEvent("end"))));

        // ---

        Map<String, Object> args = new HashMap<>();
        args.put(varKey, varValue);

        String key = UUID.randomUUID().toString();
        getEngine().start(key, aId, args);

        // ---

        assertActivations(key, aId,
                "start",
                "f1",
                "call");

        assertActivations(key, bId,
                "start",
                "f1",
                "t1",
                "f2",
                "end");

        assertActivations(key, aId,
                "f2",
                "end");

        assertNoMoreActivations();

        // ---

        verify(t1, times(1)).execute(any(ExecutionContext.class));
    }

    /**
     * start --> call                      end
     *               \                    /
     *                start --> t1 --> end
     */
    @Test
    public void testInVariableValues() throws Exception {
        final String varKey = "key#" + System.currentTimeMillis();
        final Object varValue = new Double(3.1415);

        JavaDelegate t1 = spy(new JavaDelegate() {

            @Override
            public void execute(ExecutionContext ctx) throws Exception {
                Object v = ctx.getVariable(varKey);
                assertEquals(varValue, v);
            }
        });
        getServiceTaskRegistry().register("t1", t1);

        // ---

        String aId = "testA";
        String bId = "testB";

        Set<VariableMapping> inVars = new HashSet<>();
        inVars.add(new VariableMapping(null, null, varValue, varKey));

        deploy(new ProcessDefinition(aId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "call"),
                new CallActivity("call", bId, inVars, null),
                new SequenceFlow("f2", "call", "end"),
                new EndEvent("end"))));

        deploy(new ProcessDefinition(bId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
                new SequenceFlow("f2", "t1", "end"),
                new EndEvent("end"))));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, aId, null);

        // ---

        verify(t1, times(1)).execute(any(ExecutionContext.class));
    }

    /**
     * start --> gw --> call                        gw --> t1 --> end
     *              \       \                     / /
     *               \       start --> ev1 --> end /
     *                call                        /
     *                                           /
     *                     start --> ev2 --> end
     */
    @Test
    public void testParallelCallsWithEvents() throws Exception {
        String argKey = "arg_" + System.currentTimeMillis();
        Object argVal = "argVal_" + System.currentTimeMillis();
        String outKey = "out_" + System.currentTimeMillis();
        Object ev1Val = "ev1Val_" + System.currentTimeMillis();
        Object ev2Val = "ev2Val_" + System.currentTimeMillis();

        JavaDelegate t1 = spy(new JavaDelegate() {

            @Override
            public void execute(ExecutionContext ctx) throws Exception {
                Object v = ctx.getVariable(outKey);
                assertEquals(ev2Val, v);
            }
        });
        getServiceTaskRegistry().register("t1", t1);

        // ---

        String aId = "testA";
        String bId = "testB";
        String cId = "testC";

        Set<VariableMapping> inVars = new HashSet<>();
        inVars.add(VariableMapping.copy(argKey, outKey));

        Set<VariableMapping> outVars = new HashSet<>();
        outVars.add(VariableMapping.copy(outKey, outKey));

        deploy(new ProcessDefinition(aId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw1"),

                new ParallelGateway("gw1"),
                new SequenceFlow("f2", "gw1", "call1"),
                new CallActivity("call1", bId, inVars, outVars),
                new SequenceFlow("f3", "call1", "gw2"),

                new SequenceFlow("f4", "gw1", "call2"),
                new CallActivity("call2", cId, inVars, outVars),
                new SequenceFlow("f5", "call2", "gw2"),

                new ParallelGateway("gw2"),
                new SequenceFlow("f6", "gw2", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
                new SequenceFlow("f7", "t1", "end"),
                new EndEvent("end"))));

        deploy(new ProcessDefinition(bId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "ev1"),
                new IntermediateCatchEvent("ev1", "ev1"),
                new SequenceFlow("f2", "ev1", "end"),
                new EndEvent("end"))));

        deploy(new ProcessDefinition(cId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "ev2"),
                new IntermediateCatchEvent("ev2", "ev2"),
                new SequenceFlow("f2", "ev2", "end"),
                new EndEvent("end"))));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, aId, Collections.singletonMap(argKey, argVal));

        // ---

        getEngine().resume(key, "ev1", Collections.singletonMap(outKey, ev1Val));
        getEngine().resume(key, "ev2", Collections.singletonMap(outKey, ev2Val));

        // ---

        verify(t1, times(1)).execute(any(ExecutionContext.class));
    }

    /**
     * start --> call               call               end
     *               \             /    \             /
     *                start --> end      start --> end
     */
    @Test
    public void testScopeNesting() throws Exception {
        String aId = "testA";
        String bId = "testB";

        deploy(new ProcessDefinition(aId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "call1"),
                new CallActivity("call1", bId),
                new SequenceFlow("f2", "call1", "call2"),
                new CallActivity("call2", bId),
                new SequenceFlow("f3", "call2", "end"),
                new EndEvent("end")
        )));

        deploy(new ProcessDefinition(bId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "end"),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, aId, null);
    }

    /**
     * start --> call <------------ ev
     *               \             /
     *                start --> end
     */
    @Test
    public void testDefinitionReloading() throws Exception {
        getConfiguration().setAvoidDefinitionReloadingOnCall(false);

        // ---

        String aId = "testA";
        String bId = "testB";
        String ev = "ev";

        deploy(new ProcessDefinition(aId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "call"),
                new CallActivity("call", bId),
                new SequenceFlow("f2", "call", "ev"),
                new IntermediateCatchEvent(ev),
                new SequenceFlow("f3", ev, "call")
        )));

        deploy(new ProcessDefinition(bId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "end"),
                new EndEvent("end")
        )));

        // ---


        String key = UUID.randomUUID().toString();
        getEngine().start(key, aId, null);
        getEngine().resume(key, ev, null);
        getEngine().resume(key, ev, null);

        // ---

        verify(getProcessDefinitionProvider(), times(3)).getById(eq(bId));
        reset(getProcessDefinitionProvider());

        // ---

        getConfiguration().setAvoidDefinitionReloadingOnCall(true);

        getEngine().resume(key, ev, null);
        getEngine().resume(key, ev, null);

        verifyZeroInteractions(getProcessDefinitionProvider());
    }

    /**
     * start --> call                      end
     *               \                    /
     *                start --> t1 --> end
     */
    @Test
    public void testVariableInterpolationValues() throws Exception {
        int inputVal = 1000;

        JavaDelegate t1 = spy(new JavaDelegate() {

            @Override
            public void execute(ExecutionContext ctx) throws Exception {
                Object v1 = ctx.getVariable("a");
                assertEquals("test: 1001", v1);

                Object v2 = ctx.getVariable("b");
                Map<Object, Object> m = (Map<Object, Object>) v2;
                assertEquals("test: 1002", ((List) m.get("x")).get(0));
            }
        });
        getServiceTaskRegistry().register("t1", t1);

        // ---

        Set<VariableMapping> inVars = new HashSet<>();
        inVars.add(new VariableMapping(null, "test: ${inputVar + 1}", null, "a", true));

        Map<Object, Object> stuff = new HashMap<>();
        stuff.put("x", Arrays.asList("test: ${inputVar + 2}"));
        inVars.add(new VariableMapping(null, null, stuff, "b", true));

        // ---

        String aId = "testA";
        String bId = "testB";


        deploy(new ProcessDefinition(aId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "call"),
                new CallActivity("call", bId, inVars, null),
                new SequenceFlow("f2", "call", "end"),
                new EndEvent("end"))));

        deploy(new ProcessDefinition(bId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
                new SequenceFlow("f2", "t1", "end"),
                new EndEvent("end"))));

        // ---

        String key = UUID.randomUUID().toString();
        Map<String, Object> args = new HashMap<>();
        args.put("inputVar", inputVal);
        getEngine().start(key, aId, args);

        // ---

        verify(t1, times(1)).execute(any(ExecutionContext.class));
    }

    /**
     * start --> call                      t2 --> end
     *               \                    /
     *                start --> t1 --> end
     */
    @Test
    public void testNoImplicitOutVariables() throws Exception {
        getConfiguration().setCopyAllCallActivityOutVariables(false);
        String innerKey = "key#" + System.currentTimeMillis();
        String innerVal = "val#" + System.currentTimeMillis();

        testOutVariable(innerKey, innerVal, new JavaDelegate() {
            @Override
            public void execute(ExecutionContext ctx) throws Exception {
                assertFalse(ctx.hasVariable(innerKey));
            }
        });
    }

    /**
     * start --> call                      t2 --> end
     *               \                    /
     *                start --> t1 --> end
     */
    @Test
    public void testImplicitOutVariables() throws Exception {
        getConfiguration().setCopyAllCallActivityOutVariables(true);
        String innerKey = "key#" + System.currentTimeMillis();
        String innerVal = "val#" + System.currentTimeMillis();

        testOutVariable(innerKey, innerVal, new JavaDelegate() {
            @Override
            public void execute(ExecutionContext ctx) throws Exception {
                assertEquals(innerVal, ctx.getVariable(innerKey));
            }
        });
    }

    private void testOutVariable(String innerKey, String innerVal, JavaDelegate t2) throws Exception {
        JavaDelegate t1 = spy(new JavaDelegate() {
            @Override
            public void execute(ExecutionContext ctx) throws Exception {
                ctx.setVariable(innerKey, innerVal);
            }
        });
        getServiceTaskRegistry().register("t1", t1);

        t2 = spy(t2);
        getServiceTaskRegistry().register("t2", t2);

        // ---


        String aId = "testA";
        String bId = "testB";

        deploy(new ProcessDefinition(aId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "call"),
                new CallActivity("call", bId, true),
                new SequenceFlow("f2", "call", "t2"),
                new ServiceTask("t2", ExpressionType.DELEGATE, "${t2}"),
                new SequenceFlow("f3", "t2", "end"),
                new EndEvent("end")
        )));

        deploy(new ProcessDefinition(bId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
                new SequenceFlow("f2", "t1", "end"),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, aId, null);

        // ---

        verify(t1, times(1)).execute(any(ExecutionContext.class));
        verify(t2, times(1)).execute(any(ExecutionContext.class));
    }
}
