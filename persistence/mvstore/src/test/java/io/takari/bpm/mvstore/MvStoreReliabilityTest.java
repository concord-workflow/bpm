package io.takari.bpm.mvstore;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.api.Variables;
import io.takari.bpm.event.Event;
import io.takari.bpm.event.ExpiredEvent;
import io.takari.bpm.model.EndEvent;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.model.StartEvent;
import io.takari.bpm.mvstore.StoreTypes.BusinessKeyIndexKey;
import io.takari.bpm.mvstore.StoreTypes.ExpiredEventIndexKey;
import io.takari.bpm.mvstore.StoreTypes.Unit;
import io.takari.bpm.state.ProcessInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MvStoreReliabilityTest {

    @TempDir
    Path tempDir;

    private static ProcessInstance sampleInstance(UUID id, String bk) {
        ProcessDefinition pd = new ProcessDefinition("p1", List.of(
                new StartEvent("s"),
                new SequenceFlow("f", "s", "e"),
                new EndEvent("e")
        ));
        return new ProcessInstance(id, bk, new IndexedProcessDefinition(pd), new Variables(Map.of("k", "v")));
    }

    private static Event sampleEvent(UUID id, String bk, String name, Date expiredAt) {
        return new Event(id, UUID.randomUUID(), "p1", UUID.randomUUID(), name, bk, false, expiredAt, "payload");
    }

    @Test
    void preInterruptedThreadDoesNotDisableFileBackedStore() throws Exception {
        Path dbFile = tempDir.resolve("interrupted.mv");
        Clock clock = Clock.fixed(Instant.ofEpochMilli(10_000), ZoneOffset.UTC);
        UUID execId = UUID.randomUUID();
        UUID evId1 = UUID.randomUUID();
        UUID evId2 = UUID.randomUUID();

        // cacheSizeMb(0) forces page reads to touch the FileChannel
        try (MvStoreDatabase db = MvStoreDatabase.open(
                MvStoreConfig.file(dbFile).cacheSizeMb(0).clock(clock).build())) {

            // Set interrupt flag before each operation; verify flag is preserved and store stays healthy
            Thread.currentThread().interrupt();
            try {
                db.persistenceManager().save(sampleInstance(execId, "bk-int"));
                assertTrue(Thread.interrupted(), "interrupt flag must be preserved after save");
            } finally {
                Thread.interrupted();
            }

            Thread.currentThread().interrupt();
            try {
                db.eventStorage().add(sampleEvent(evId1, "bk-int", "e1", new Date(5_000)));
                assertTrue(Thread.interrupted(), "interrupt flag must be preserved after add");
            } finally {
                Thread.interrupted();
            }

            Thread.currentThread().interrupt();
            try {
                db.eventStorage().add(sampleEvent(evId2, "bk-int", "e2", new Date(20_000)));
                assertTrue(Thread.interrupted(), "interrupt flag must be preserved after second add");
            } finally {
                Thread.interrupted();
            }

            Thread.currentThread().interrupt();
            try {
                assertNotNull(db.persistenceManager().get(execId));
                assertTrue(Thread.interrupted(), "interrupt flag must be preserved after get");
            } finally {
                Thread.interrupted();
            }

            Thread.currentThread().interrupt();
            try {
                assertEquals(2, db.eventStorage().find("bk-int").size());
                assertTrue(Thread.interrupted(), "interrupt flag must be preserved after find");
            } finally {
                Thread.interrupted();
            }

            Thread.currentThread().interrupt();
            try {
                List<ExpiredEvent> expired = db.eventStorage().findNextExpiredEvent(10);
                assertEquals(1, expired.size());
                assertEquals(evId1, expired.getFirst().geId());
                assertTrue(Thread.interrupted(), "interrupt flag must be preserved after findNextExpiredEvent");
            } finally {
                Thread.interrupted();
            }

            Thread.currentThread().interrupt();
            try {
                db.close();
                assertTrue(Thread.interrupted(), "interrupt flag must be preserved after close");
            } finally {
                Thread.interrupted();
            }
        }

        // Reopen and verify every committed write survived cleanly
        try (MvStoreDatabase reopened = MvStoreDatabase.open(MvStoreConfig.file(dbFile).clock(clock).build())) {
            assertNotNull(reopened.persistenceManager().get(execId));
            assertNotNull(reopened.eventStorage().get(evId1));
            assertNotNull(reopened.eventStorage().get(evId2));
            assertTrue(reopened.eventStorage().findNextExpiredEvent(10).isEmpty());
        }
    }

    @Test
    void midWriteInterruptsDoNotCorruptOrCloseStore() throws Exception {
        Path dbFile = tempDir.resolve("mid-interrupt.mv");
        int writeCount = 40;

        try (MvStoreDatabase db = MvStoreDatabase.open(MvStoreConfig.file(dbFile).syncWrite(true).build())) {
            AtomicBoolean running = new AtomicBoolean(true);
            CountDownLatch started = new CountDownLatch(1);
            AtomicReference<Throwable> writerFailure = new AtomicReference<>();

            Thread writer = new Thread(() -> {
                started.countDown();
                try {
                    for (int i = 0; i < writeCount; i++) {
                        UUID id = UUID.randomUUID();
                        db.eventStorage().add(sampleEvent(id, "bk-mid", "ev-" + i, null));
                        Thread.interrupted(); // clear so test loop observes fresh interrupts
                    }
                } catch (Throwable t) {
                    writerFailure.set(t);
                } finally {
                    running.set(false);
                }
            }, "test-interrupted-writer");

            writer.start();
            assertTrue(started.await(5, TimeUnit.SECONDS));
            while (running.get()) {
                writer.interrupt();
                Thread.sleep(1);
            }
            writer.join(15_000);
            assertFalse(writer.isAlive());
            assertNull(writerFailure.get(), () -> "Writer failed: " + writerFailure.get());
            assertFalse(db.isClosed());
            assertEquals(writeCount, db.eventStorage().find("bk-mid").size());
        }

        try (MvStoreDatabase reopened = MvStoreDatabase.open(MvStoreConfig.file(dbFile).build())) {
            assertEquals(writeCount, reopened.eventStorage().find("bk-mid").size());
        }
    }

    @Test
    void readOnlyFileFailsFastWhenOpenedWritable() throws Exception {
        Path dbFile = tempDir.resolve("readonly-file.mv");
        UUID id = UUID.randomUUID();

        try (MvStoreDatabase db = MvStoreDatabase.open(MvStoreConfig.file(dbFile).build())) {
            db.eventStorage().add(sampleEvent(id, "bk-ro", "ev", null));
        }

        Set<PosixFilePermission> originalPerms = Files.getPosixFilePermissions(dbFile);
        Files.setPosixFilePermissions(dbFile, PosixFilePermissions.fromString("r--r--r--"));
        try {
            if (!Files.isWritable(dbFile)) {
                assertThrows(PersistenceException.class,
                        () -> MvStoreDatabase.open(MvStoreConfig.file(dbFile).readOnly(false).build()));
            }
            try (MvStoreDatabase ro = MvStoreDatabase.open(MvStoreConfig.file(dbFile).readOnly(true).build())) {
                assertNotNull(ro.eventStorage().get(id));
            }
        } finally {
            Files.setPosixFilePermissions(dbFile, originalPerms);
        }
    }

    @Test
    void failedWriteRollsBackAllPartialMutations() {
        try (MvStoreDatabase db = MvStoreDatabase.open(MvStoreConfig.inMemory().build())) {
            UUID existingId = UUID.randomUUID();
            db.eventStorage().add(sampleEvent(existingId, "bk-1", "existing", null));

            UUID failedId = UUID.randomUUID();
            assertThrows(PersistenceException.class, () -> db.write(() -> {
                db.eventsMap().put(failedId, new byte[]{1, 2, 3});
                db.businessKeyIndex().put(new BusinessKeyIndexKey("bk-1", "failed", failedId), Unit.INSTANCE);
                throw new RuntimeException("simulated failure");
            }));

            assertFalse(db.isClosed());
            assertNull(db.eventStorage().get(failedId));
            assertEquals(1, db.eventStorage().find("bk-1").size());
            assertEquals(existingId, db.eventStorage().find("bk-1").iterator().next().getId());

            // Store remains usable after rollback
            UUID afterId = UUID.randomUUID();
            db.eventStorage().add(sampleEvent(afterId, "bk-1", "after", null));
            assertEquals(2, db.eventStorage().find("bk-1").size());
        }
    }

    @Test
    void crashRecoveryPreservesCommittedStateAndIndexes() throws Exception {
        Path dbFile = tempDir.resolve("crash.mv");
        Clock clock = Clock.fixed(Instant.ofEpochMilli(10_000), ZoneOffset.UTC);
        UUID execId = UUID.randomUUID();
        UUID evId = UUID.randomUUID();

        MvStoreDatabase db = MvStoreDatabase.open(MvStoreConfig.file(dbFile).syncWrite(true).clock(clock).build());
        db.persistenceManager().save(sampleInstance(execId, "bk-crash"));
        db.eventStorage().add(sampleEvent(evId, "bk-crash", "ev-crash", new Date(5_000)));
        // Abrupt close without clean shutdown footer
        db.closeImmediatelyForTest();

        try (MvStoreDatabase recovered = MvStoreDatabase.open(MvStoreConfig.file(dbFile).clock(clock).build())) {
            ProcessInstance loaded = recovered.persistenceManager().get(execId);
            assertNotNull(loaded);
            assertEquals("bk-crash", loaded.getBusinessKey());

            assertEquals(1, recovered.eventStorage().find("bk-crash", "ev-crash").size());
            List<ExpiredEvent> expired = recovered.eventStorage().findNextExpiredEvent(10);
            assertEquals(1, expired.size());
            assertEquals(evId, expired.getFirst().geId());
        }
    }

    @Test
    void syncWriteRetentionAndCompactRetentionPreservation() {
        Path syncFile = tempDir.resolve("sync.mv");
        try (MvStoreDatabase db = MvStoreDatabase.open(MvStoreConfig.file(syncFile).syncWrite(true).build())) {
            assertEquals(0, db.getRetentionTimeForTest());
        }

        Path asyncFile = tempDir.resolve("async.mv");
        try (MvStoreDatabase db = MvStoreDatabase.open(MvStoreConfig.file(asyncFile).syncWrite(false).build())) {
            int before = db.getRetentionTimeForTest();
            assertTrue(before > 0, "default retention time should be > 0 when syncWrite=false");
            db.compact(50);
            assertEquals(before, db.getRetentionTimeForTest(), "compact() must restore prior retention time");
        }
    }

    @Test
    void unpairedSurrogateStringsRoundTripAcrossRestart() {
        Path dbFile = tempDir.resolve("surrogates.mv");
        String bk = "bk-\uD800-mid-\uDC00";
        String name = "ev-\uDBFF-end";
        UUID id = UUID.randomUUID();

        try (MvStoreDatabase db = MvStoreDatabase.open(MvStoreConfig.file(dbFile).build())) {
            db.eventStorage().add(sampleEvent(id, bk, name, null));
        }

        try (MvStoreDatabase reopened = MvStoreDatabase.open(MvStoreConfig.file(dbFile).build())) {
            Event loaded = reopened.eventStorage().get(id);
            assertNotNull(loaded);
            assertEquals(bk, loaded.getProcessBusinessKey());
            assertEquals(name, loaded.getName());

            assertEquals(1, reopened.eventStorage().find(bk).size());
            assertEquals(1, reopened.eventStorage().find(bk, name).size());

            Event removed = reopened.eventStorage().remove(id);
            assertEquals(bk, removed.getProcessBusinessKey());
            assertTrue(reopened.eventStorage().find(bk).isEmpty());
        }
    }

    @Test
    void corruptEventRecordCanBeRemovedOrOverwritten() {
        try (MvStoreDatabase db = MvStoreDatabase.open(MvStoreConfig.inMemory().build())) {
            UUID corruptRemoveId = UUID.randomUUID();
            UUID corruptOverwriteId = UUID.randomUUID();

            // Inject corrupt records directly alongside index entries
            db.write(() -> {
                db.eventsMap().put(corruptRemoveId, new byte[]{99, 0, 1});
                db.businessKeyIndex().put(new BusinessKeyIndexKey("bk-bad", "ev-bad", corruptRemoveId), Unit.INSTANCE);
                db.expiredEventsIndex().put(new ExpiredEventIndexKey(1_000L, corruptRemoveId), Unit.INSTANCE);

                db.eventsMap().put(corruptOverwriteId, new byte[]{99, 0, 2});
                db.businessKeyIndex().put(new BusinessKeyIndexKey("bk-old", "ev-old", corruptOverwriteId), Unit.INSTANCE);
                db.expiredEventsIndex().put(new ExpiredEventIndexKey(1_000L, corruptOverwriteId), Unit.INSTANCE);
                return null;
            });

            // remove() succeeds despite corrupt payload, returns null, and cleans up both indexes
            assertNull(db.eventStorage().remove(corruptRemoveId));
            assertNull(db.eventStorage().get(corruptRemoveId));
            assertTrue(db.eventStorage().find("bk-bad").isEmpty());

            // overwriting add() cleans up old corrupt indexes and stores the valid replacement
            Event replacement = sampleEvent(corruptOverwriteId, "bk-good", "ev-good", null);
            db.eventStorage().add(replacement);
            assertEquals(replacement, db.eventStorage().get(corruptOverwriteId));
            assertTrue(db.eventStorage().find("bk-old").isEmpty());
            assertEquals(1, db.eventStorage().find("bk-good", "ev-good").size());
            assertTrue(db.eventStorage().findNextExpiredEvent(10).isEmpty());
        }
    }

    @Test
    void fileBackedConcurrencyWithSyncWrite() throws Exception {
        Path dbFile = tempDir.resolve("concurrent.mv");
        Clock clock = Clock.fixed(Instant.ofEpochMilli(100_000), ZoneOffset.UTC);
        int threads = 4;
        int perThread = 20;

        try (MvStoreDatabase db = MvStoreDatabase.open(
                MvStoreConfig.file(dbFile).syncWrite(true).clock(clock).build());
             ExecutorService pool = Executors.newFixedThreadPool(threads)) {

            List<Callable<List<UUID>>> tasks = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                int idx = t;
                tasks.add(() -> {
                    List<UUID> ids = new ArrayList<>(perThread);
                    for (int i = 0; i < perThread; i++) {
                        UUID evId = UUID.randomUUID();
                        db.eventStorage().add(sampleEvent(evId, "bk-" + idx, "ev-" + i, new Date(1_000L + i)));
                        ids.add(evId);

                        UUID execId = UUID.randomUUID();
                        db.persistenceManager().save(sampleInstance(execId, "bk-" + idx));
                        assertNotNull(db.persistenceManager().get(execId));
                    }
                    return ids;
                });
            }

            List<UUID> allIds = new ArrayList<>();
            for (Future<List<UUID>> f : pool.invokeAll(tasks)) {
                allIds.addAll(f.get());
            }

            List<ExpiredEvent> claimed = db.eventStorage().findNextExpiredEvent(allIds.size() + 10);
            assertEquals(allIds.size(), claimed.size());
        }
    }
}
