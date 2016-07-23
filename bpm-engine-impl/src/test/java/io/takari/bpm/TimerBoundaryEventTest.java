package io.takari.bpm;

import io.takari.bpm.api.BpmnError;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.model.AbstractElement;
import io.takari.bpm.model.BoundaryEvent;
import io.takari.bpm.model.EndEvent;
import io.takari.bpm.model.ExpressionType;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.model.ServiceTask;
import io.takari.bpm.model.StartEvent;
import java.util.Arrays;
import java.util.UUID;
import static org.junit.Assert.fail;
import org.junit.Test;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TimerBoundaryEventTest extends AbstractEngineTest {
    
    /**
     * start --> t1 --------> end1
     *            \
     *             timer1 --> end2
     */
    @Test(timeout = 10000)
    public void testAsUsual() throws Exception {
        JavaDelegate longTask = spy(new JavaDelegate() {

            @Override
            public void execute(ExecutionContext ctx) throws ExecutionException {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        
        getServiceTaskRegistry().register("longTask", longTask);
        
        // ---
        
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${longTask}"),
                new BoundaryEvent("timer1", "t1", null, "PT7S"),
                new SequenceFlow("f2", "t1", "end1"),
                new SequenceFlow("f3", "timer1", "end2"),
                new EndEvent("end1"),
                new EndEvent("end2")
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
                "end1");
        assertNoMoreActivations();

        // ---

        verify(longTask, times(1)).execute(any(ExecutionContext.class));
    }
    
    /**
     * start --> t1 --------> end1
     *            \
     *             timer1 --> end2
     */
    @Test(timeout = 10000)
    public void testTimeout() throws Exception {
        JavaDelegate longTask = spy(new JavaDelegate() {

            @Override
            public void execute(ExecutionContext ctx) throws ExecutionException {
                try {
                    Thread.sleep(5000);
                    fail("Should've been interrupted by the timer");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        
        getServiceTaskRegistry().register("longTask", longTask);
        
        // ---
        
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${longTask}"),
                new BoundaryEvent("timer1", "t1", null, "PT3S"),
                new SequenceFlow("f2", "t1", "end1"),
                new SequenceFlow("f3", "timer1", "end2"),
                new EndEvent("end1"),
                new EndEvent("end2")
        )));

        // ---
        
        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);

        // ---

        assertActivations(key, processId,
                "start",
                "f1",
                "t1",
                "timer1",
                "f3",
                "end2");
        assertNoMoreActivations();

        // ---

        verify(longTask, times(1)).execute(any(ExecutionContext.class));
    }
    
    /**
     * start --> t1 --------> end1
     *            \
     *             timer1 --> end2
     */
    @Test(timeout = 10000)
    public void testTimeoutWithExpression() throws Exception {
        TestBean testBean = spy(new TestBean());
        getServiceTaskRegistry().register("bean", testBean);
        
        // ---
        
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ServiceTask("t1", ExpressionType.SIMPLE, "${bean.doIt()}"),
                new BoundaryEvent("timer1", "t1", null, "PT3S"),
                new SequenceFlow("f2", "t1", "end1"),
                new SequenceFlow("f3", "timer1", "end2"),
                new EndEvent("end1"),
                new EndEvent("end2")
        )));

        // ---
        
        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);

        // ---

        assertActivations(key, processId,
                "start",
                "f1",
                "t1",
                "timer1",
                "f3",
                "end2");
        assertNoMoreActivations();

        // ---

        verify(testBean, times(1)).doIt();
    }
    
    /**
     * start --> t1 ---------> end1
     *            |\
     *            | timer1 --> end2
     *            \
     *             error1 ----> end3
     */
    @Test
    public void testMixedEvents() throws Exception {
        JavaDelegate failingTask = spy(new JavaDelegate() {

            @Override
            public void execute(ExecutionContext ctx) throws ExecutionException {
                throw new BpmnError("error1");
            }
        });
        
        getServiceTaskRegistry().register("failingTask", failingTask);
        
        // ---
        
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${failingTask}"),
                new BoundaryEvent("timer1", "t1", null, "PT3S"),
                new BoundaryEvent("error1", "t1", "error1"),
                new SequenceFlow("f2", "t1", "end1"),
                new SequenceFlow("f3", "timer1", "end2"),
                new SequenceFlow("f4", "error1", "end3"),
                new EndEvent("end1"),
                new EndEvent("end2"),
                new EndEvent("end3")
        )));

        // ---
        
        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);

        // ---

        assertActivations(key, processId,
                "start",
                "f1",
                "t1",
                "error1",
                "f4",
                "end3");
        assertNoMoreActivations();

        // ---

        verify(failingTask, times(1)).execute(any(ExecutionContext.class));
    }
    
    public static class TestBean {
        
        public void doIt() {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
