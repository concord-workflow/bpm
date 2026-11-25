package io.takari.bpm;

import io.takari.bpm.api.Engine;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.event.InMemEventStorage;
import io.takari.bpm.model.*;
import io.takari.bpm.persistence.InMemPersistenceManager;
import io.takari.bpm.persistence.StaleStateException;
import io.takari.bpm.task.ServiceTaskRegistryImpl;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Two engines (e.g. two nodes) share the same storage but not the locks.
 */
public class ConcurrentModificationTest {

    /**
     * start --> gw1 --> ev1 --> slow --> gw2 --> after --> end
     *              \                    /
     *               --> ev2 -----------
     */
    @Test
    public void testConcurrentResumeOnDifferentEngines() throws Exception {
        CountDownLatch slowStarted = new CountDownLatch(1);
        CountDownLatch slowRelease = new CountDownLatch(1);
        AtomicInteger slowCalls = new AtomicInteger();
        AtomicInteger afterCalls = new AtomicInteger();

        ServiceTaskRegistryImpl tasks = new ServiceTaskRegistryImpl();
        tasks.register("slow", (JavaDelegate) ctx -> {
            if (slowCalls.incrementAndGet() == 1) {
                slowStarted.countDown();
                slowRelease.await(10, TimeUnit.SECONDS);
            }
        });
        tasks.register("after", (JavaDelegate) ctx -> afterCalls.incrementAndGet());

        ProcessDefinition pd = new ProcessDefinition("test", Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw1"),
                new ParallelGateway("gw1"),
                new SequenceFlow("f2", "gw1", "ev1"),
                new IntermediateCatchEvent("ev1", "ev1"),
                new SequenceFlow("f3", "ev1", "slow"),
                new ServiceTask("slow", ExpressionType.DELEGATE, "${slow}"),
                new SequenceFlow("f4", "slow", "gw2"),
                new SequenceFlow("f5", "gw1", "ev2"),
                new IntermediateCatchEvent("ev2", "ev2"),
                new SequenceFlow("f6", "ev2", "gw2"),
                new ParallelGateway("gw2"),
                new SequenceFlow("f7", "gw2", "after"),
                new ServiceTask("after", ExpressionType.DELEGATE, "${after}"),
                new SequenceFlow("f8", "after", "end"),
                new EndEvent("end")));

        InMemPersistenceManager pm = new InMemPersistenceManager();
        InMemEventStorage events = new InMemEventStorage();

        Engine a = new EngineBuilder().withDefinitionProvider(id -> pd).withTaskRegistry(tasks)
                .withPersistenceManager(pm).withEventStorage(events).build();
        Engine b = new EngineBuilder().withDefinitionProvider(id -> pd).withTaskRegistry(tasks)
                .withPersistenceManager(pm).withEventStorage(events).build();

        String key = UUID.randomUUID().toString();
        a.start(key, "test", null);

        // engine A resumes ev1 and blocks in the task
        CompletableFuture<Void> first = CompletableFuture.runAsync(() -> {
            try {
                a.resume(key, "ev1", null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        assertTrue(slowStarted.await(10, TimeUnit.SECONDS));

        // meanwhile engine B resumes ev2 and stores the state
        b.resume(key, "ev2", null);

        // engine A's changes are rejected
        slowRelease.countDown();
        Exception e = assertThrows(Exception.class, () -> first.get(10, TimeUnit.SECONDS));
        assertInstanceOf(StaleStateException.class, e.getCause().getCause());

        assertEquals(0, afterCalls.get());

        // ev1 is still there and can be retried
        a.resume(key, "ev1", null);

        assertEquals(1, afterCalls.get());
        assertTrue(events.find(key).isEmpty());
    }
}
