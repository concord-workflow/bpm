package io.takari.bpm;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.model.*;
import org.junit.Test;

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

        String script = "execution.setVariable('c', execution.getVariable('a') + execution.getVariable('b'))";

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
     * start --> t1 --> end
     */
    @Test
    public void testExternalJs() throws Exception {
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ScriptTask("t1", ScriptTask.Type.REFERENCE, "javascript", "test.js"),
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
}
