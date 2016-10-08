package io.takari.bpm;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.model.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class LoopingTest extends AbstractEngineTest {

    /**
     * start --> t1 --> gw --> sub                        -
     *           ^      \         \                     /  \
     *           \       -> end    substart --> subend     |
     *            \                                       /
     *             --------------------------------------
     */
    @Test
    public void testSubprocessLooping() throws Exception {
        int loops = 500;
        String counterKey = "counter_" + System.currentTimeMillis();
        String expr = "${" + counterKey + " >= " + loops + "}";

        JavaDelegate t1 = spy(new JavaDelegate() {
            @Override
            public void execute(ExecutionContext ctx) throws Exception {
                Long l = (Long) ctx.getVariable(counterKey);
                if (l == null) {
                    l = 0L;
                }
                ctx.setVariable(counterKey, l + 1);
            }
        });
        getServiceTaskRegistry().register("t1", t1);

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
                new SequenceFlow("f2", "t1", "gw"),

                new ExclusiveGateway("gw", "f3"),
                new SequenceFlow("f3", "gw", "sub"),
                new SubProcess("sub", Arrays.asList(
                        new StartEvent("substart"),
                        new SequenceFlow("f4", "substart", "subend"),
                        new EndEvent("subend")
                )),
                new SequenceFlow("f5", "sub", "t1"),

                new SequenceFlow("f6", "gw", "end", expr),
                new EndEvent("end"))));

        // ---

        String key = randomUuid().toString();
        getEngine().start(key, processId, null);

        // ---

        verify(t1, times(loops)).execute(any(ExecutionContext.class));
    }

    /**
     * start --> ev --
     *             \  \
     *              --/
     */
    @Test
    public void testEventLooping() throws Exception {
        int loops = 500;
        String evKey = "nextId";
        String expr = "${\"event_\".concat(" + evKey + ")}";

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "ev"),
                IntermediateCatchEvent.messageExpr("ev", expr),
                new SequenceFlow("f2", "ev", "ev"))));

        // ---

        String key = randomUuid().toString();
        Map<String, Object> args = Collections.singletonMap(evKey, 0);
        getEngine().start(key, processId, args);

        // ---

        for (int i = 0; i < loops; i++) {
            String ev = "event_" + i;
            args = Collections.singletonMap(evKey, i + 1);
            getEngine().resume(key, ev, args);
        }
    }
}
