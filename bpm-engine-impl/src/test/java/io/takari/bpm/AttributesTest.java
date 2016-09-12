package io.takari.bpm;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.model.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class AttributesTest extends AbstractEngineTest {

    @Test
    public void test() throws Exception {
        String processId = "test";
        String attrKey = "key#" + System.currentTimeMillis();
        String attrVal = "val#" + System.currentTimeMillis();

        JavaDelegate t1 = spy(new JavaDelegate() {

            @Override
            public void execute(ExecutionContext ctx) throws Exception {
                String k = ProcessDefinition.ATTRIBUTE_KEY_PREFIX + attrKey;
                String v = (String) ctx.getVariable(k);
                assertEquals(attrVal, v);
            }
        });
        getServiceTaskRegistry().register("t1", t1);

        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
                new SequenceFlow("f2", "t1", "end"),
                new EndEvent("end")
        ), Collections.singletonMap(attrKey, attrVal)));

        // ---


        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);

        // ---

        verify(t1, times(1)).execute(any(ExecutionContext.class));
    }
}
