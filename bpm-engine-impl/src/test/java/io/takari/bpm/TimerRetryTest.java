package io.takari.bpm;

import io.takari.bpm.api.Engine;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.event.DirectEventDispatcher;
import io.takari.bpm.event.Event;
import io.takari.bpm.event.EventPersistenceManagerImpl;
import io.takari.bpm.event.EventScheduler;
import io.takari.bpm.event.ExpiredEvent;
import io.takari.bpm.event.InMemEventStorage;
import io.takari.bpm.model.*;
import io.takari.bpm.task.ServiceTaskRegistryImpl;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TimerRetryTest {

    @Test
    public void testLeasedEventsAreReacquired() throws Exception {
        InMemEventStorage storage = new InMemEventStorage();
        Event ev = new Event(UUID.randomUUID(), UUID.randomUUID(), "test", UUID.randomUUID(), "ev", "bk", false,
                new Date(System.currentTimeMillis() - 1000), null);
        storage.add(ev);

        List<ExpiredEvent> first = storage.findNextExpiredEvent(10, 200);
        assertEquals(1, first.size());

        // leased
        assertTrue(storage.findNextExpiredEvent(10, 200).isEmpty());

        // the lease expires, the event is still there
        Thread.sleep(300);
        assertEquals(1, storage.findNextExpiredEvent(10, 200).size());

        // processed
        storage.remove(ev.getId());
        Thread.sleep(300);
        assertTrue(storage.findNextExpiredEvent(10, 200).isEmpty());
    }

    /**
     * start --> timer --> t1 (fails once) --> end
     */
    @Test
    public void testTimerIsRetriedAfterFailure() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        ServiceTaskRegistryImpl tasks = new ServiceTaskRegistryImpl();
        tasks.register("flaky", (JavaDelegate) ctx -> {
            if (calls.incrementAndGet() == 1) {
                throw new RuntimeException("transient");
            }
        });

        ProcessDefinition pd = new ProcessDefinition("test", Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "timer"),
                IntermediateCatchEvent.timeDuration("timer", "PT0S"),
                new SequenceFlow("f2", "timer", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${flaky}"),
                new SequenceFlow("f3", "t1", "end"),
                new EndEvent("end")));

        InMemEventStorage storage = new InMemEventStorage();
        Engine engine = new EngineBuilder()
                .withDefinitionProvider(id -> pd)
                .withTaskRegistry(tasks)
                .withEventStorage(storage)
                .build();

        EventScheduler scheduler = new EventScheduler(new EventPersistenceManagerImpl(storage), 10,
                new DirectEventDispatcher((AbstractEngine) engine));
        scheduler.setAcquisitionDelay(50);
        scheduler.setExecutionErrorDelay(10);
        scheduler.setLeaseDuration(200);
        scheduler.setEventExecutorsCount(1);

        String key = UUID.randomUUID().toString();
        engine.start(key, "test", null);

        scheduler.start();
        try {
            long deadline = System.currentTimeMillis() + 10_000;
            while (!storage.find(key).isEmpty() && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }
        } finally {
            scheduler.stop();
        }

        assertEquals(2, calls.get());
        assertTrue(storage.find(key).isEmpty());
    }
}
