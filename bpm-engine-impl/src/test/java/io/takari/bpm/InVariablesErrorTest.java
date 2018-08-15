package io.takari.bpm;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.model.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class InVariablesErrorTest extends AbstractEngineTest {

    /**
     * start --> t1 --------> end
     *            \         /
     *             - ev1 -->
     */
    @Test
    public void testSimple() throws Exception {
        getConfiguration().setWrapAllExceptionsAsBpmnErrors(true);

        JavaDelegate helloTask = spy(new JavaDelegate() {

            @Override
            public void execute(ExecutionContext ctx) throws ExecutionException {
            }
        });
        getServiceTaskRegistry().register("hello", helloTask);

        VarTask varTask = spy(new VarTask() {
            @Override
            public Object getValue() {
                throw new RuntimeException("KABOOM!");
            }
        });
        getServiceTaskRegistry().register("var", varTask);

        // ---

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${hello}", Collections.singleton(new VariableMapping(null, "${var.getValue()}", "var")), null),
                new BoundaryEvent("ev1", "t1", null),
                new SequenceFlow("f2", "t1", "end"),
                new SequenceFlow("f3", "ev1", "end"),
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
                "ev1",
                "f3",
                "end");
        assertNoMoreActivations();

        // ---

        verify(varTask, times(1)).getValue();
        verifyZeroInteractions(helloTask);
    }

    public interface VarTask {

        Object getValue();
    }
}
