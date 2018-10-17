package io.takari.bpm;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.model.*;
import org.junit.Test;

import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class GetVariableNamesInTaskTest extends AbstractEngineTest {

    @Test
    public void test() throws Exception {
        getConfiguration().setInterpolateInputVariables(true);

        // ---

        String varKey = "var_" + System.currentTimeMillis();
        String varValue = "${testBean.getValue(execution)}";

        getServiceTaskRegistry().register("testBean", new TestBean());

        JavaDelegate helloTask = spy(new JavaDelegate() {

            @Override
            public void execute(ExecutionContext ctx) throws ExecutionException {
                Object v = ctx.getVariable(varKey);
                assertEquals(ctx.getVariableNames(), v);
            }
        });
        getServiceTaskRegistry().register("hello", helloTask);

        // ---

        String processId = "test";
        deploy(new ProcessDefinition(processId,
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${hello}"),
                new SequenceFlow("f2", "t1", "end"),
                new EndEvent("end")
        ));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, Collections.singletonMap(varKey, varValue));

        // ---

        verify(helloTask, times(1)).execute(any(ExecutionContext.class));
    }

    public static final class TestBean {

        public Object getValue(ExecutionContext ctx) {
            return ctx.getVariableNames();
        }
    }
}
