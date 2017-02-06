package io.takari.bpm;

import io.takari.bpm.api.BpmnError;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.model.*;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

public class ServiceTaskTest extends AbstractEngineTest {

    /**
     * start --> t1 --> end
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
                new EndEvent("end")
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

    @Test
    public void testDelegateBoundaryError() throws Exception {
        final String errorRef = "test#" + System.currentTimeMillis();

        JavaDelegate t1 = spy(new JavaDelegate() {

            @Override
            public void execute(ExecutionContext ctx) throws ExecutionException {
                throw new BpmnError(errorRef);
            }
        });
        getServiceTaskRegistry().register("t1", t1);

        ServiceTask t = new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}");
        testBoundaryError(t, errorRef);

        verify(t1, times(1)).execute(any(ExecutionContext.class));
    }

    @Test
    public void testExpressionBoundaryError() throws Exception {
        final String errorRef = "test#" + System.currentTimeMillis();

        SampleTask t1 = spy(new SampleTask() {

            public void doIt(long i) {
                throw new BpmnError(errorRef);
            }
        });
        getServiceTaskRegistry().register("t1", t1);

        ServiceTask t = new ServiceTask("t1", ExpressionType.SIMPLE, "${t1.doIt(123)}");
        testBoundaryError(t, errorRef);

        verify(t1, times(1)).doIt(anyLong());
    }

    /**
     * start --> t1 ----------> end
     *             \        /
     *              error --
     */
    public void testBoundaryError(ServiceTask t, String errorRef) throws Exception {
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                t,
                new BoundaryEvent("be1", "t1", errorRef),
                new SequenceFlow("f2", "be1", "end"),
                new SequenceFlow("f3", "t1", "end"),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);

        // ---

        assertActivations(key, processId,
                "start",
                "f1",
                "t1",
                "be1",
                "f2",
                "end");
        assertNoMoreActivations();
    }

    /**
     * start --> t1 -------------------->  end1
     *             \                   /
     *              -> some error -----
     *              \
     *               -> default error --> end2
     *
     *
     */
    @Test
    public void testDefaultBoundaryError() throws Exception {
        JavaDelegate t1 = spy(new JavaDelegate() {
            @Override
            public void execute(ExecutionContext ctx) throws Exception {
                throw new BpmnError("kaboom!");
            }
        });

        getServiceTaskRegistry().register("t1", t1);

        // ---

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
                new SequenceFlow("f2", "t1", "end1"),

                new BoundaryEvent("be1", "t1", "some error"),
                new SequenceFlow("f3", "be1", "end1"),

                new BoundaryEvent("be2", "t1", null),
                new SequenceFlow("f4", "be2", "end2"),

                new EndEvent("end1"),
                new EndEvent("end2")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);

        // ---

        assertActivations(key, processId,
                "start",
                "f1",
                "t1",
                "be2",
                "f4",
                "end2");
        assertNoMoreActivations();
    }

    /**
     * start --> t1 --------- t2 --> end
     *             \        /
     *              error --
     */
    @Test
    @Ignore
    public void testErrorCodeStoring() throws Exception {
        final String errorRef = "test#" + System.currentTimeMillis();

        JavaDelegate t1 = spy(new JavaDelegate() {

            @Override
            public void execute(ExecutionContext ctx) throws ExecutionException {
                throw new BpmnError(errorRef);
            }
        });
        getServiceTaskRegistry().register("t1", t1);

        JavaDelegate t2 = spy(new JavaDelegate() {

            @Override
            public void execute(ExecutionContext ctx) throws ExecutionException {
                BpmnError e = (BpmnError) ctx.getVariable(ExecutionContext.LAST_ERROR_KEY);
                assertNotNull(e);
                assertEquals(errorRef, e.getErrorRef());
            }
        });
        getServiceTaskRegistry().register("t2", t2);

        // ---

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
                new SequenceFlow("f2", "t1", "end"),
                new BoundaryEvent("be1", "t1", errorRef),
                new SequenceFlow("f3", "be1", "t2"),
                new ServiceTask("t2", ExpressionType.DELEGATE, "${t2}"),
                new SequenceFlow("f4", "t2", "end"),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);

        // ---

        verify(t1, times(1)).execute(any(ExecutionContext.class));
        verify(t2, times(1)).execute(any(ExecutionContext.class));
    }

    /**
     * start --> t1 --> end
     */
    @Test
    @Ignore
    public void testSimpleTaskExpression() throws Exception {
        SampleTask t = mock(SampleTask.class);
        getServiceTaskRegistry().register("hello", t);

        // ---

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ServiceTask("t1", ExpressionType.SIMPLE, "${hello.doIt(123)}"),
                new SequenceFlow("f2", "t1", "end"),
                new EndEvent("end")
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

        verify(t, times(1)).doIt(eq(123L));
    }

    /**
     * start --> t1 --> t2 --> end
     */
    @Test
    public void testInOut() throws Exception {
        String outerKey = "outerKey_" + System.currentTimeMillis();
        String outerVal = "outerVal_" + System.currentTimeMillis();

        String outerKey2 = "outerKey2_" + System.currentTimeMillis();
        String outerVal2 = "outerVal2_" + System.currentTimeMillis();

        String innerKey = "innerKey_" + System.currentTimeMillis();
        String innerVal = "innerVal_" + System.currentTimeMillis();

        String outKey = "outKey_" + System.currentTimeMillis();

        JavaDelegate t1 = spy(new JavaDelegate() {

            @Override
            public void execute(ExecutionContext ctx) throws ExecutionException {
                assertNull(ctx.getVariable(outerKey2));
                assertEquals(outerVal, ctx.getVariable(innerKey));
                ctx.setVariable(innerKey, innerVal);
            }
        });
        getServiceTaskRegistry().register("t1", t1);

        JavaDelegate t2 = spy(new JavaDelegate() {
            @Override
            public void execute(ExecutionContext ctx) throws Exception {
                assertEquals(innerVal, ctx.getVariable(outKey));
            }
        });
        getServiceTaskRegistry().register("t2", t2);

        // ---

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}",
                        Collections.singleton(new VariableMapping(null, "${" + outerKey + "}", innerKey)),
                        Collections.singleton(new VariableMapping(null, "${" + innerKey + "}", outKey))),
                new SequenceFlow("f2", "t1", "t2"),
                new ServiceTask("t2", ExpressionType.DELEGATE, "${t2}"),
                new SequenceFlow("f3", "t2", "end"),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        Map<String, Object> vars = new HashMap<>();
        vars.put(outerKey, outerVal);
        vars.put(outerKey2, outerVal2);
        getEngine().start(key, processId, vars);

        // ---

        verify(t1, times(1)).execute(any(ExecutionContext.class));
        verify(t2, times(1)).execute(any(ExecutionContext.class));
    }

    public interface SampleTask {

        void doIt(long i);
    }
}
