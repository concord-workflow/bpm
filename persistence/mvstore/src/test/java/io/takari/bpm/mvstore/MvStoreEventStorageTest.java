package io.takari.bpm.mvstore;

import io.takari.bpm.event.Event;
import io.takari.bpm.event.ExpiredEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class MvStoreEventStorageTest {

    @TempDir
    Path tempDir;

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> now;

        MutableClock(Instant initial) {
            this.now = new AtomicReference<>(initial);
        }

        void set(Instant instant) {
            now.set(instant);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    }

    private static Event event(UUID id, String businessKey, String name, Date expiredAt, Object payload) {
        return new Event(id, UUID.randomUUID(), "proc-1", UUID.randomUUID(), name, businessKey, false, expiredAt, payload);
    }

    @Test
    void crudAndPrefixSafeBusinessKeyLookups() {
        try (MvStoreDatabase db = MvStoreDatabase.open(MvStoreConfig.inMemory().build())) {
            MvStoreEventStorage storage = db.eventStorage();

            Event e1 = event(UUID.randomUUID(), "order", "approve", null, "p1");
            Event e2 = event(UUID.randomUUID(), "order", "cancel", null, "p2");
            Event e3 = event(UUID.randomUUID(), "order", "approve", null, "p3");
            Event e4 = event(UUID.randomUUID(), "order-1", "approve", null, "p4");
            Event e5 = event(UUID.randomUUID(), "order\0x", "approve", null, "p5");
            Event e6 = event(UUID.randomUUID(), "order", null, null, "p6");

            for (Event e : List.of(e1, e2, e3, e4, e5, e6)) {
                storage.add(e);
            }

            assertEquals(e1, storage.get(e1.getId()));
            assertEquals("p1", storage.get(e1.getId()).getPayload());
            assertNull(storage.get(UUID.randomUUID()));

            Collection<Event> byOrder = storage.find("order");
            assertEquals(Set.of(e1.getId(), e2.getId(), e3.getId(), e6.getId()),
                    byOrder.stream().map(Event::getId).collect(Collectors.toSet()));

            Collection<Event> byOrderApprove = storage.find("order", "approve");
            assertEquals(Set.of(e1.getId(), e3.getId()),
                    byOrderApprove.stream().map(Event::getId).collect(Collectors.toSet()));

            Collection<Event> byOrderNullName = storage.find("order", null);
            assertEquals(Set.of(e6.getId()),
                    byOrderNullName.stream().map(Event::getId).collect(Collectors.toSet()));

            assertEquals(1, storage.find("order-1", "approve").size());
            assertEquals(1, storage.find("order\0x", "approve").size());
            assertTrue(storage.find("missing").isEmpty());
            assertTrue(storage.find("order", "missing").isEmpty());

            Event removed = storage.remove(e1.getId());
            assertEquals(e1, removed);
            assertNull(storage.remove(e1.getId()));
            assertNull(storage.get(e1.getId()));
            assertEquals(Set.of(e3.getId()),
                    storage.find("order", "approve").stream().map(Event::getId).collect(Collectors.toSet()));
        }
    }

    @Test
    void overwriteExistingEventCleansUpOldIndexes() {
        MutableClock clock = new MutableClock(Instant.ofEpochMilli(10_000));
        try (MvStoreDatabase db = MvStoreDatabase.open(MvStoreConfig.inMemory().clock(clock).build())) {
            MvStoreEventStorage storage = db.eventStorage();

            UUID id = UUID.randomUUID();
            Event original = event(id, "bk-old", "ev-old", new Date(5_000), "old");
            storage.add(original);

            Event replacement = event(id, "bk-new", "ev-new", new Date(20_000), "new");
            storage.add(replacement);

            assertTrue(storage.find("bk-old").isEmpty());
            assertTrue(storage.find("bk-old", "ev-old").isEmpty());
            assertEquals(1, storage.find("bk-new", "ev-new").size());
            assertEquals("new", storage.get(id).getPayload());

            // Old expiration at 5_000 must not fire at t=10_000
            assertTrue(storage.findNextExpiredEvent(10).isEmpty());

            // New expiration at 20_000 must fire at t=20_000
            clock.set(Instant.ofEpochMilli(20_000));
            List<ExpiredEvent> expired = storage.findNextExpiredEvent(10);
            assertEquals(1, expired.size());
            assertEquals(id, expired.getFirst().geId());
            assertEquals(20_000L, expired.getFirst().getExpiredAt().getTime());
        }
    }

    @Test
    void expiredEventOrderingLimitsAndClaimSemantics() {
        MutableClock clock = new MutableClock(Instant.ofEpochMilli(5_000));
        try (MvStoreDatabase db = MvStoreDatabase.open(MvStoreConfig.inMemory().clock(clock).build())) {
            MvStoreEventStorage storage = db.eventStorage();

            Event eLate = event(UUID.randomUUID(), "bk", "late", new Date(6_000), null);
            Event eExact = event(UUID.randomUUID(), "bk", "exact", new Date(5_000), null);
            Event eEarly1 = event(UUID.randomUUID(), "bk", "early1", new Date(1_000), null);
            Event eEarly2 = event(UUID.randomUUID(), "bk", "early2", new Date(1_000), null);
            Event eRemoved = event(UUID.randomUUID(), "bk", "removed", new Date(500), null);
            Event eNoExpiry = event(UUID.randomUUID(), "bk", "no-expiry", null, null);

            for (Event e : List.of(eLate, eExact, eEarly1, eEarly2, eRemoved, eNoExpiry)) {
                storage.add(e);
            }
            storage.remove(eRemoved.getId());

            assertTrue(storage.findNextExpiredEvent(0).isEmpty());
            assertTrue(storage.findNextExpiredEvent(-5).isEmpty());

            List<ExpiredEvent> batch1 = storage.findNextExpiredEvent(2);
            assertEquals(2, batch1.size());
            assertEquals(1_000L, batch1.get(0).getExpiredAt().getTime());
            assertEquals(1_000L, batch1.get(1).getExpiredAt().getTime());
            assertEquals(Set.of(eEarly1.getId(), eEarly2.getId()),
                    Set.of(batch1.get(0).geId(), batch1.get(1).geId()));

            // Claimed events remain readable via get/find until explicitly removed
            assertNotNull(storage.get(eEarly1.getId()));
            assertNotNull(storage.get(eEarly2.getId()));

            List<ExpiredEvent> batch2 = storage.findNextExpiredEvent(10);
            assertEquals(1, batch2.size());
            assertEquals(eExact.getId(), batch2.getFirst().geId());

            // Nothing more expired at t=5_000
            assertTrue(storage.findNextExpiredEvent(10).isEmpty());

            // Advance clock to 6_000 -> eLate becomes eligible
            clock.set(Instant.ofEpochMilli(6_000));
            List<ExpiredEvent> batch3 = storage.findNextExpiredEvent(10);
            assertEquals(1, batch3.size());
            assertEquals(eLate.getId(), batch3.getFirst().geId());
        }
    }

    @Test
    void persistsEventsAndIndexesAcrossRestart() {
        Path dbFile = tempDir.resolve("events.mv");
        MutableClock clock = new MutableClock(Instant.ofEpochMilli(10_000));

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        try (MvStoreDatabase db = MvStoreDatabase.open(MvStoreConfig.file(dbFile).clock(clock).build())) {
            MvStoreEventStorage storage = db.eventStorage();
            storage.add(event(id1, "bk-a", "ev-1", new Date(5_000), "payload-1"));
            storage.add(event(id2, "bk-a", "ev-2", new Date(15_000), "payload-2"));
        }

        try (MvStoreDatabase reopened = MvStoreDatabase.open(MvStoreConfig.file(dbFile).clock(clock).build())) {
            MvStoreEventStorage storage = reopened.eventStorage();

            assertEquals("payload-1", storage.get(id1).getPayload());
            assertEquals(2, storage.find("bk-a").size());
            assertEquals(1, storage.find("bk-a", "ev-2").size());

            List<ExpiredEvent> expired = storage.findNextExpiredEvent(10);
            assertEquals(1, expired.size());
            assertEquals(id1, expired.getFirst().geId());
        }

        // Verify the claim was persisted across another restart
        try (MvStoreDatabase reopenedAgain = MvStoreDatabase.open(MvStoreConfig.file(dbFile).clock(clock).build())) {
            assertTrue(reopenedAgain.eventStorage().findNextExpiredEvent(10).isEmpty());
        }
    }

    @Test
    void concurrentReadsWritesAndExpirationClaims() throws Exception {
        MutableClock clock = new MutableClock(Instant.ofEpochMilli(100_000));
        int threads = 8;
        int perThread = 50;

        try (MvStoreDatabase db = MvStoreDatabase.open(
                MvStoreConfig.inMemory().clock(clock).syncWrite(false).build());
             ExecutorService pool = Executors.newFixedThreadPool(threads)) {

            MvStoreEventStorage storage = db.eventStorage();
            List<Callable<List<UUID>>> tasks = new ArrayList<>();

            for (int t = 0; t < threads; t++) {
                int threadIdx = t;
                tasks.add(() -> {
                    List<UUID> added = new ArrayList<>(perThread);
                    for (int i = 0; i < perThread; i++) {
                        UUID id = UUID.randomUUID();
                        storage.add(event(id, "bk-" + threadIdx, "ev-" + (i % 5), new Date(1_000L + i), i));
                        added.add(id);
                        storage.find("bk-" + threadIdx);
                    }
                    return added;
                });
            }

            List<UUID> allIds = new ArrayList<>(threads * perThread);
            for (Future<List<UUID>> f : pool.invokeAll(tasks)) {
                allIds.addAll(f.get());
            }

            // Concurrently claim all expired events across multiple threads
            List<Callable<List<UUID>>> claimers = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                claimers.add(() -> {
                    List<UUID> claimed = new ArrayList<>();
                    while (true) {
                        List<ExpiredEvent> batch = storage.findNextExpiredEvent(7);
                        if (batch.isEmpty()) {
                            break;
                        }
                        for (ExpiredEvent e : batch) {
                            claimed.add(e.geId());
                        }
                    }
                    return claimed;
                });
            }

            List<UUID> totalClaimed = new ArrayList<>();
            for (Future<List<UUID>> f : pool.invokeAll(claimers)) {
                totalClaimed.addAll(f.get());
            }

            // Every expired event must be claimed exactly once
            assertEquals(allIds.size(), totalClaimed.size());
            assertEquals(Set.copyOf(allIds), Set.copyOf(totalClaimed));
        }
    }

    @Test
    void leasedEventsAreReacquiredUntilRemoved() {
        MutableClock clock = new MutableClock(Instant.ofEpochMilli(10_000));
        try (MvStoreDatabase db = MvStoreDatabase.open(MvStoreConfig.inMemory().clock(clock).build())) {
            MvStoreEventStorage storage = db.eventStorage();

            Event e1 = event(UUID.randomUUID(), "bk", "e1", new Date(5_000), null);
            Event e2 = event(UUID.randomUUID(), "bk", "e2", new Date(6_000), null);
            storage.add(e1);
            storage.add(e2);

            List<ExpiredEvent> first = storage.findNextExpiredEvent(10, 1_000);
            assertEquals(2, first.size());

            // leased
            assertTrue(storage.findNextExpiredEvent(10, 1_000).isEmpty());

            // e1 is processed, e2 isn't (e.g. its resume failed)
            storage.remove(e1.getId());

            clock.set(Instant.ofEpochMilli(11_000));
            List<ExpiredEvent> second = storage.findNextExpiredEvent(10, 1_000);
            assertEquals(1, second.size());
            assertEquals(e2.getId(), second.getFirst().geId());

            // the stale index entry of e1 is gone
            storage.remove(e2.getId());
            clock.set(Instant.ofEpochMilli(20_000));
            assertTrue(storage.findNextExpiredEvent(10, 1_000).isEmpty());
            assertTrue(db.read(() -> db.expiredEventsIndex().isEmpty()));
        }
    }
}
