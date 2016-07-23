package io.takari.bpm;

import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.event.Event;
import io.takari.bpm.model.AbstractElement;
import io.takari.bpm.model.EndEvent;
import io.takari.bpm.model.ExpressionType;
import io.takari.bpm.model.IntermediateCatchEvent;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.model.ServiceTask;
import io.takari.bpm.model.StartEvent;

import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import static org.junit.Assert.*;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;

public class IntermediateCatchEventTest extends AbstractEngineTest {
    
    /**
     * start --> ev --> t1 --> end
     */
    @Test
    public void testSingleEvent() throws Exception {
        getServiceTaskRegistry().register("t1", mock(JavaDelegate.class));
        
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "ev"),
                new IntermediateCatchEvent("ev", "ev"),
                new SequenceFlow("f2", "ev", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
                new SequenceFlow("f3", "t1", "end"),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);

        // ---

        getEngine().resume(key, "ev", null);

        // ---

        assertActivations(key, processId,
                "start",
                "f1",
                "ev",
                "f2",
                "t1",
                "f3",
                "end");
        assertNoMoreActivations();
    }
    
    @Test
    public void testTimerWithDurationIso() throws Exception {
        testTimerWithDuration("PT30S");
    }
    
    @Test
    public void testTimerWithDurationExpression() throws Exception {
        getServiceTaskRegistry().register("bean", new TestBean("PT30S"));
        testTimerWithDuration("${bean.getDuration()}");
    }
    
    /**
     * start --> ev --> end
     */
    private void testTimerWithDuration(String duration) throws Exception {
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "ev"),
                new IntermediateCatchEvent("ev", "ev", null, duration),
                new SequenceFlow("f2", "ev", "end"),
                new EndEvent("end")
        )));
        
        long t1 = Instant.now().toEpochMilli();

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);

        // ---

        getEngine().resume(key, "ev", null);

        ArgumentCaptor<Event> args = ArgumentCaptor.forClass(Event.class);
        verify(getEventManager(), times(1)).add(args.capture());
        
        Event ev = args.getValue();
        assertNotNull(ev);
        assertNotNull(ev.getExpiredAt());
        
        long t2 = ev.getExpiredAt().toInstant().toEpochMilli();
        
        // the event should have an expiration date plus ~30sec in the future
        // we assume a generous range of ±10sec just to be sure that no
        // one confuses seconds and milliseconds
        long dt = t2 - t1;
        assertTrue(dt >= 30 * 1000);
        assertTrue(dt <= 40 * 1000);
    }
    
    public static class TestBean implements Serializable {
    	
    	private static final long serialVersionUID = 1L;
        
        private final String duration;

        public TestBean(String duration) {
            this.duration = duration;
        }

        public String getDuration() {
            return duration;
        }
    }
}
