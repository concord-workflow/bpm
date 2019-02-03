package io.takari.bpm;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.api.Variables;
import io.takari.bpm.model.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class InitialVariablesTest extends AbstractEngineTest {

    /**
     * start --> t1 --> end
     */
    @Test
    public void testSimple() throws Exception {
        String value = "test" + System.currentTimeMillis();

        JavaDelegate helloTask = spy(new JavaDelegate() {

            @Override
            public void execute(ExecutionContext ctx) throws ExecutionException {
                Object v = ctx.getVariable("x");
                assertEquals(value, v);
            }
        });
        getServiceTaskRegistry().register("test", helloTask);

        // ---

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${test}"),
                new SequenceFlow("f2", "t1", "end"),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();

        Variables vars = new Variables().setVariable("x", value);
        getEngine().start(key, processId, vars, null);

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
}
