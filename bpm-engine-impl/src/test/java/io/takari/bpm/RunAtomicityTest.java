package io.takari.bpm;

import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.api.NoEventFoundException;
import io.takari.bpm.event.Event;
import io.takari.bpm.model.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The state and the events of a run are stored only if the run succeeds.
 */
public class RunAtomicityTest extends AbstractEngineTest {

    /**
     * start --> ev --> t1 --> end
     */
    @Test
    public void testFailedResumeCanBeRetried() throws Exception {
        AtomicBoolean fail = new AtomicBoolean(true);
        AtomicInteger calls = new AtomicInteger();
        getServiceTaskRegistry().register("flaky", (JavaDelegate) ctx -> {
            calls.incrementAndGet();
            if (fail.get()) {
                throw new RuntimeException("transient");
            }
        });

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "ev"),
                new IntermediateCatchEvent("ev", "ev"),
                new SequenceFlow("f2", "ev", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${flaky}"),
                new SequenceFlow("f3", "t1", "end"),
                new EndEvent("end"))));

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);

        assertThrows(Exception.class, () -> getEngine().resume(key, "ev", null));

        // the event is still there
        assertEquals(1, getEventManager().find(key, "ev").size());

        fail.set(false);
        getEngine().resume(key, "ev", null);

        assertEquals(2, calls.get());
        assertTrue(getEventManager().find(key, "ev").isEmpty());
    }

    /**
     * start --> gw --> ev --> end
     *              \
     *               --> t1 (fails) --> end
     */
    @Test
    public void testFailedStartLeavesNoEvents() throws Exception {
        getServiceTaskRegistry().register("boom", (JavaDelegate) ctx -> {
            throw new RuntimeException("boom");
        });

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw"),
                new ParallelGateway("gw"),
                new SequenceFlow("f2", "gw", "ev"),
                new IntermediateCatchEvent("ev", "ev"),
                new SequenceFlow("f3", "ev", "end"),
                new SequenceFlow("f4", "gw", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${boom}"),
                new SequenceFlow("f5", "t1", "end"),
                new EndEvent("end"))));

        String key = UUID.randomUUID().toString();
        assertThrows(Exception.class, () -> getEngine().start(key, processId, null));

        assertTrue(getEventManager().find(key, "ev").isEmpty());
    }

    /**
     * An event that was already processed (e.g. the engine crashed before removing it) is removed on resume.
     * <pre>
     * start --> ev1 --> ev2 --> end
     * </pre>
     */
    @Test
    public void testStaleEventIsRemoved() throws Exception {
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "ev1"),
                new IntermediateCatchEvent("ev1", "ev1"),
                new SequenceFlow("f2", "ev1", "ev2"),
                new IntermediateCatchEvent("ev2", "ev2"),
                new SequenceFlow("f3", "ev2", "end"),
                new EndEvent("end"))));

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);

        Collection<Event> evs = getEventManager().find(key, "ev1");
        assertEquals(1, evs.size());
        Event ev1 = evs.iterator().next();

        getEngine().resume(key, "ev1", null);

        // simulate a crash between storing the state and removing the event
        getEventManager().add(ev1);

        assertThrows(NoEventFoundException.class, () -> getEngine().resume(key, "ev1", null));
        assertTrue(getEventManager().find(key, "ev1").isEmpty());

        // the process can continue
        getEngine().resume(key, "ev2", null);
        assertTrue(getEventManager().find(key, "ev2").isEmpty());
    }
}
