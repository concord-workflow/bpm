package io.takari.bpm.mvstore;

import io.takari.bpm.AbstractEngine;
import io.takari.bpm.EngineBuilder;
import io.takari.bpm.ProcessDefinitionProvider;
import io.takari.bpm.api.Engine;
import io.takari.bpm.api.Event;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.event.DirectEventDispatcher;
import io.takari.bpm.event.EventPersistenceManagerImpl;
import io.takari.bpm.event.EventScheduler;
import io.takari.bpm.event.ExpiredEvent;
import io.takari.bpm.model.EndEvent;
import io.takari.bpm.model.EventBasedGateway;
import io.takari.bpm.model.ExpressionType;
import io.takari.bpm.model.IntermediateCatchEvent;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.model.ServiceTask;
import io.takari.bpm.model.StartEvent;
import io.takari.bpm.task.ServiceTaskRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MvStoreEngineIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void suspendsAndResumesProcessAcrossDatabaseRestart() throws Exception {
        Path dbFile = tempDir.resolve("engine.mv");

        ProcessDefinition pd = new ProcessDefinition("approval-flow", List.of(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${step1}"),
                new SequenceFlow("f2", "t1", "wait"),
                new IntermediateCatchEvent("wait", "approved"),
                new SequenceFlow("f3", "wait", "t2"),
                new ServiceTask("t2", ExpressionType.DELEGATE, "${step2}"),
                new SequenceFlow("f4", "t2", "end"),
                new EndEvent("end")
        ));

        ProcessDefinitionProvider pdp = id -> "approval-flow".equals(id) ? pd : null;

        AtomicReference<String> step1Seen = new AtomicReference<>();
        AtomicReference<String> step2Seen = new AtomicReference<>();

        ServiceTaskRegistry tasks = key -> switch (key) {
            case "step1" -> (JavaDelegate) ctx -> {
                step1Seen.set((String) ctx.getVariable("input"));
                ctx.setVariable("fromStep1", "computed-" + ctx.getVariable("input"));
            };
            case "step2" -> (JavaDelegate) ctx -> {
                step2Seen.set(ctx.getVariable("fromStep1") + ":" + ctx.getVariable("approver"));
            };
            default -> null;
        };

        String businessKey = "order-123";

        // Phase 1: start and suspend at IntermediateCatchEvent, then close DB
        try (MvStoreDatabase db = MvStoreDatabase.open(MvStoreConfig.file(dbFile).build())) {
            Engine engine = new EngineBuilder()
                    .withDefinitionProvider(pdp)
                    .withTaskRegistry(tasks)
                    .withPersistenceManager(db.persistenceManager())
                    .withEventStorage(db.eventStorage())
                    .build();

            engine.start(businessKey, "approval-flow", Map.of("input", "alpha"));

            assertEquals("alpha", step1Seen.get());
            assertNull(step2Seen.get());

            Collection<Event> pending = engine.getEventService().getEvents(businessKey);
            assertEquals(1, pending.size());
            assertEquals("approved", pending.iterator().next().getName());
        }

        // Phase 2: reopen DB in a brand new Engine instance and resume to completion
        try (MvStoreDatabase reopened = MvStoreDatabase.open(MvStoreConfig.file(dbFile).build())) {
            Engine engine = new EngineBuilder()
                    .withDefinitionProvider(pdp)
                    .withTaskRegistry(tasks)
                    .withPersistenceManager(reopened.persistenceManager())
                    .withEventStorage(reopened.eventStorage())
                    .build();

            Collection<Event> pending = engine.getEventService().getEvents(businessKey);
            assertEquals(1, pending.size());

            engine.resume(businessKey, "approved", Map.of("approver", "alice"));

            assertEquals("computed-alpha:alice", step2Seen.get());
            assertTrue(engine.getEventService().getEvents(businessKey).isEmpty());
        }
    }

    @Test
    void timerEventPersistsAndExpiresAcrossDatabaseRestart() throws Exception {
        Path dbFile = tempDir.resolve("timer.mv");

        ProcessDefinition pd = new ProcessDefinition("timer-flow", List.of(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "timer"),
                new IntermediateCatchEvent("timer", "timerEv", null, "PT5S"),
                new SequenceFlow("f2", "timer", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${afterTimer}"),
                new SequenceFlow("f3", "t1", "end"),
                new EndEvent("end")
        ));

        ProcessDefinitionProvider pdp = id -> "timer-flow".equals(id) ? pd : null;
        AtomicReference<Boolean> fired = new AtomicReference<>(false);
        ServiceTaskRegistry tasks = key -> "afterTimer".equals(key)
                ? (JavaDelegate) ctx -> fired.set(true)
                : null;

        String businessKey = "timer-bk-1";

        // Phase 1: start timer process and shut down
        try (MvStoreDatabase db = MvStoreDatabase.open(MvStoreConfig.file(dbFile).build())) {
            Engine engine = new EngineBuilder()
                    .withDefinitionProvider(pdp)
                    .withTaskRegistry(tasks)
                    .withPersistenceManager(db.persistenceManager())
                    .withEventStorage(db.eventStorage())
                    .build();

            engine.start(businessKey, "timer-flow", null);
            assertFalse(fired.get());
            assertTrue(db.eventStorage().findNextExpiredEvent(10).isEmpty());
        }

        // Phase 2: reopen DB with a clock 1 minute in the future and trigger expired event
        Clock futureClock = Clock.fixed(Instant.now().plusSeconds(60), ZoneOffset.UTC);
        try (MvStoreDatabase reopened = MvStoreDatabase.open(
                MvStoreConfig.file(dbFile).clock(futureClock).build())) {
            Engine engine = new EngineBuilder()
                    .withDefinitionProvider(pdp)
                    .withTaskRegistry(tasks)
                    .withPersistenceManager(reopened.persistenceManager())
                    .withEventStorage(reopened.eventStorage())
                    .build();

            List<ExpiredEvent> expired = reopened.eventStorage().findNextExpiredEvent(10);
            assertEquals(1, expired.size());

            engine.resume(expired.getFirst().geId(), null);

            assertTrue(fired.get());
            assertTrue(engine.getEventService().getEvents(businessKey).isEmpty());
        }
    }

    @Test
    void exclusiveEventBasedGatewayClearsSiblingEventsAcrossRestart() throws Exception {
        Path dbFile = tempDir.resolve("gateway.mv");

        ProcessDefinition pd = new ProcessDefinition("gw-flow", List.of(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw"),
                new EventBasedGateway("gw"),
                new SequenceFlow("f2", "gw", "evA"),
                new SequenceFlow("f3", "gw", "evB"),
                new IntermediateCatchEvent("evA", "approve"),
                new IntermediateCatchEvent("evB", "reject"),
                new SequenceFlow("f4", "evA", "tApprove"),
                new SequenceFlow("f5", "evB", "tReject"),
                new ServiceTask("tApprove", ExpressionType.DELEGATE, "${approveTask}"),
                new ServiceTask("tReject", ExpressionType.DELEGATE, "${rejectTask}"),
                new SequenceFlow("f6", "tApprove", "end"),
                new SequenceFlow("f7", "tReject", "end"),
                new EndEvent("end")
        ));

        ProcessDefinitionProvider pdp = id -> "gw-flow".equals(id) ? pd : null;
        AtomicReference<String> branch = new AtomicReference<>();
        ServiceTaskRegistry tasks = key -> switch (key) {
            case "approveTask" -> (JavaDelegate) ctx -> branch.set("approved");
            case "rejectTask" -> (JavaDelegate) ctx -> branch.set("rejected");
            default -> null;
        };

        String bk = "gw-bk-1";

        try (MvStoreDatabase db = MvStoreDatabase.open(MvStoreConfig.file(dbFile).build())) {
            Engine engine = new EngineBuilder()
                    .withDefinitionProvider(pdp)
                    .withTaskRegistry(tasks)
                    .withPersistenceManager(db.persistenceManager())
                    .withEventStorage(db.eventStorage())
                    .build();

            engine.start(bk, "gw-flow", null);
            assertEquals(2, engine.getEventService().getEvents(bk).size());
        }

        try (MvStoreDatabase reopened = MvStoreDatabase.open(MvStoreConfig.file(dbFile).build())) {
            Engine engine = new EngineBuilder()
                    .withDefinitionProvider(pdp)
                    .withTaskRegistry(tasks)
                    .withPersistenceManager(reopened.persistenceManager())
                    .withEventStorage(reopened.eventStorage())
                    .build();

            engine.resume(bk, "approve", null);

            assertEquals("approved", branch.get());
            assertTrue(engine.getEventService().getEvents(bk).isEmpty());
        }
    }

    @Test
    void eventSchedulerDispatchesTimerAndStopsCleanlyWithoutBreakingStore() throws Exception {
        Path dbFile = tempDir.resolve("scheduler.mv");

        ProcessDefinition pd = new ProcessDefinition("sched-flow", List.of(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "timer"),
                new IntermediateCatchEvent("timer", "tEv", null, "PT0.1S"),
                new SequenceFlow("f2", "timer", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${done}"),
                new SequenceFlow("f3", "t1", "end"),
                new EndEvent("end")
        ));

        ProcessDefinitionProvider pdp = id -> "sched-flow".equals(id) ? pd : null;
        CountDownLatch finished = new CountDownLatch(1);
        ServiceTaskRegistry tasks = key -> "done".equals(key)
                ? (JavaDelegate) ctx -> finished.countDown()
                : null;

        try (MvStoreDatabase db = MvStoreDatabase.open(MvStoreConfig.file(dbFile).build())) {
            EventPersistenceManagerImpl eventManager = new EventPersistenceManagerImpl(db.eventStorage());
            Engine engine = new EngineBuilder()
                    .withDefinitionProvider(pdp)
                    .withTaskRegistry(tasks)
                    .withPersistenceManager(db.persistenceManager())
                    .withEventStorage(db.eventStorage())
                    .withEventManager(eventManager)
                    .build();

            EventScheduler scheduler = new EventScheduler(eventManager, 16, new DirectEventDispatcher((AbstractEngine) engine));
            scheduler.setEventExecutorsCount(2);
            scheduler.setAcquisitionDelay(50);
            scheduler.start();

            try {
                engine.start("sched-bk-1", "sched-flow", null);
                assertTrue(finished.await(10, TimeUnit.SECONDS), "timer should have fired via EventScheduler");
            } finally {
                // EventScheduler.stop() interrupts all acquisition/executor threads; DB must remain healthy
                scheduler.stop();
            }

            assertFalse(db.isClosed());
            assertTrue(engine.getEventService().getEvents("sched-bk-1").isEmpty());
        }
    }
}
