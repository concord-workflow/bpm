package io.takari.bpm;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.model.*;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class ScriptTaskTest extends AbstractEngineTest {

    /**
     * start --> t1 --> t2 --> end
     */
    @Test
    public void testJs() throws Exception {
        double a = System.currentTimeMillis();
        double b = 1234;

        String script = "execution.setVariable('c', a + b)";

        // ---

        JavaDelegate t2 = spy(new JavaDelegate() {

            @Override
            public void execute(ExecutionContext ctx) throws Exception {
                Double c = (Double) ctx.getVariable("c");
                assertNotNull(c);
                assertEquals(c, (Double) (a + b));
            }
        });
        getServiceTaskRegistry().register("t2", t2);

        // --

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ScriptTask("t1", ScriptTask.Type.CONTENT, "javascript", script),
                new SequenceFlow("f2", "t1", "t2"),
                new ServiceTask("t2", ExpressionType.DELEGATE, "${t2}"),
                new SequenceFlow("f3", "t2", "end"),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();

        Map<String, Object> args = new HashMap<>();
        args.put("a", a);
        args.put("b", b);

        getEngine().start(key, processId, args);

        // ---

        assertActivations(key, processId,
                "start",
                "f1",
                "t1",
                "f2",
                "t2",
                "f3",
                "end");
        assertNoMoreActivations();

        verify(t2, times(1)).execute(any(ExecutionContext.class));
    }

    /**
     * start --> t1 --> t2 --> end
     */
    @Test
    public void testJsInOut() throws Exception {
        double outerA = System.currentTimeMillis();
        double innerB = 2345;

        String script = "execution.setVariable('innerC', execution.getVariable('innerA') + execution.getVariable('innerB'))";

        // ---

        JavaDelegate t2 = spy(new JavaDelegate() {

            @Override
            public void execute(ExecutionContext ctx) throws Exception {
                Double c = (Double) ctx.getVariable("outerC");
                assertNotNull(c);
                assertEquals(c, (Double) (outerA + innerB));
            }
        });
        getServiceTaskRegistry().register("t2", t2);

        // --

        Set<VariableMapping> in = new HashSet<>();
        in.add(VariableMapping.copy("outerA", "innerA"));
        in.add(VariableMapping.set(innerB, "innerB"));
        Set<VariableMapping> out = Collections.singleton(VariableMapping.copy("innerC", "outerC"));

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ScriptTask("t1", ScriptTask.Type.CONTENT, "javascript", script, in, out),
                new SequenceFlow("f2", "t1", "t2"),
                new ServiceTask("t2", ExpressionType.DELEGATE, "${t2}"),
                new SequenceFlow("f3", "t2", "end"),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();

        Map<String, Object> args = new HashMap<>();
        args.put("outerA", outerA);

        getEngine().start(key, processId, args);

        // ---

        assertActivations(key, processId,
                "start",
                "f1",
                "t1",
                "f2",
                "t2",
                "f3",
                "end");
        assertNoMoreActivations();

        verify(t2, times(1)).execute(any(ExecutionContext.class));
    }

    /**
     * start --> t1 --> t2 --> end
     */
    @Test
    public void testJsWithCopyAllVariables() throws Exception {

        String script = "execution.setVariable('inner', execution.getVariable('main') + 10)";

        // --

        Set<VariableMapping> in = new HashSet<>();
        in.add(VariableMapping.set("whatever", "invar1"));
        Set<VariableMapping> out = Collections.singleton(VariableMapping.copy("inner", "outer"));

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ScriptTask("t1", ScriptTask.Type.CONTENT, "javascript", script, in, out, true),
                new SequenceFlow("f2", "t1", "t2"),
                new ServiceTask("t2", ExpressionType.DELEGATE, "${t2}"),
                new SequenceFlow("f3", "t2", "end"),
                new EndEvent("end")
        )));

        // ---

        JavaDelegate t2 = spy(new JavaDelegate() {

            @Override
            public void execute(ExecutionContext ctx) throws Exception {
                Double c = (Double) ctx.getVariable("outer");
                assertNotNull(c);
                assertEquals((Double)Double.sum(5,10), c);
            }
        });
        getServiceTaskRegistry().register("t2", t2);

        // ---

        String key = UUID.randomUUID().toString();

        Map<String, Object> args = new HashMap<>();
        args.put("main", 5.0);

        getEngine().start(key, processId, args);

        verify(t2, times(1)).execute(any(ExecutionContext.class));
    }

    /**
     * start --> t1 --> end
     */
    @Test
    public void testExternalJs() throws Exception {
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ScriptTask("t1", ScriptTask.Type.REFERENCE, null, "test.js"),
                new SequenceFlow("f2", "t1", "end"),
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
                "f2",
                "end");
        assertNoMoreActivations();
    }

    /**
     * start --> sub1 --------> end
     * \       /
     * bev1 --
     */
    @Test
    public void testWrappedException() throws Exception {
        getConfiguration().setWrapAllExceptionsAsBpmnErrors(true);

        // ---

        String script = "throw new io.takari.bpm.api.BpmnError('boom')";

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "sub1"),
                new SubProcess("sub1",
                        new StartEvent("substart"),
                        new SequenceFlow("f2", "substart", "t1"),
                        new ScriptTask("t1", ScriptTask.Type.CONTENT, "groovy", script),
                        new SequenceFlow("f3", "t1", "subend"),
                        new EndEvent("subend")),
                new BoundaryEvent("bev1", "sub1", null),
                new SequenceFlow("f4", "bev1", "end"),
                new SequenceFlow("f5", "sub1", "end"),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);

        // ---

        assertActivations(key, processId,
                "start",
                "f1",
                "sub1",
                "substart",
                "f2",
                "t1",
                "bev1",
                "f4",
                "end");
        assertNoMoreActivations();
    }

    /**
     * start --> t1 --> t2 --> end
     */
    @Test
    public void testTasks() throws Exception {
        double a = System.currentTimeMillis();

        String script = "tasks.get('t1').doSomething(a)";

        // ---

        TestTask t1 = spy(new TestTask() {

            @Override
            public void doSomething(Object o) {
                assertEquals(a, o);
            }
        });
        getServiceTaskRegistry().register("t1", t1);

        // --

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ScriptTask("t1", ScriptTask.Type.CONTENT, "javascript", script),
                new SequenceFlow("f2", "t1", "end"),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();

        Map<String, Object> args = new HashMap<>();
        args.put("a", a);
        getEngine().start(key, processId, args);

        // ---

        assertActivations(key, processId,
                "start",
                "f1",
                "t1",
                "f2",
                "end");
        assertNoMoreActivations();

        verify(t1, times(1)).doSomething(anyObject());
    }

    public interface TestTask {

        void doSomething(Object o);
    }
}
