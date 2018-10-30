package io.takari.bpm;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.model.*;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class OutVariablesTest extends AbstractEngineTest {

    @Test
    public void test() throws Exception {
        getConfiguration().setWrapAllExceptionsAsBpmnErrors(true);

        // ---

        JavaDelegate helloTask = spy(new JavaDelegate() {

            @Override
            public void execute(ExecutionContext ctx) throws ExecutionException {
                if (true == true) {
                    throw new RuntimeException("oops");
                }
                ctx.setVariable("result", Collections.singletonMap("k", "v"));
            }
        });
        getServiceTaskRegistry().register("hello", helloTask);

        // ---
        VariableMapping out = new VariableMapping(null, "${result.k}", "myResult");

        String processId = "test";
        deploy(new ProcessDefinition(processId,
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${hello}", null, Collections.singleton(out)),
                new SequenceFlow("f2", "t1", "end"),
                new EndEvent("end")
        ));

        // ---

        String key = UUID.randomUUID().toString();
        try {
            getEngine().start(key, processId, Collections.emptyMap());
        } catch (Exception e) {
            assertEquals("oops", e.getCause().getCause().getMessage());
        }
    }
}
