package io.takari.bpm;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.model.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ErrorEndEventTest extends AbstractEngineTest {

    @Test
    public void test() throws Exception {
        getConfiguration().setThrowExceptionOnErrorEnd(true);

        // ---

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "end"),
                new EndEvent("end", "error!")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        try {
            getEngine().start(key, processId, null);
            fail("should throw an exception");
        } catch (ExecutionException e) {
        }
    }
    
    @Test
    public void testWithCause() throws Exception {
        getConfiguration().setThrowExceptionOnErrorEnd(true);

        String errorMsg = "pow!";
        
        JavaDelegate t1 = spy(new JavaDelegate() {
            @Override
            public void execute(ExecutionContext ctx) throws ExecutionException {
                ctx.setVariable("ex", new RuntimeException(errorMsg));
            }
        });
        getServiceTaskRegistry().register("t1", t1);
        
        // ---

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
                new SequenceFlow("f2", "t1", "end"),
                new EndEvent("end", "error!", "ex")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        try {
            getEngine().start(key, processId, null);
            fail("should throw an exception");
        } catch (ExecutionException e) {
            assertNotNull(e.getCause());
            assertEquals(errorMsg, e.getCause().getMessage());
        }
        verify(t1, times(1)).execute(any(ExecutionContext.class));
    }
}
