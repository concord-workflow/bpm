package io.takari.bpm;

import io.takari.bpm.api.BpmnError;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.model.BoundaryEvent;
import io.takari.bpm.model.EndEvent;
import io.takari.bpm.model.ExpressionType;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.model.ServiceTask;
import io.takari.bpm.model.StartEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
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
    
    @Ignore
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
    
    @Ignore
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
                "f2",
                "end");
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
                Object v = ctx.getVariable(ExecutionContext.ERROR_CODE_KEY);
                Assert.assertEquals(errorRef, v);
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
    
    public interface SampleTask {
        
        void doIt(long i);
    }
}
