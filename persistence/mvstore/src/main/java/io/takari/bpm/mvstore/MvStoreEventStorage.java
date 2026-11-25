package io.takari.bpm.mvstore;

import io.takari.bpm.event.Event;
import io.takari.bpm.event.EventStorage;
import io.takari.bpm.event.ExpiredEvent;
import io.takari.bpm.mvstore.EventCodec.EventMetadata;
import io.takari.bpm.mvstore.StoreTypes.BusinessKeyIndexKey;
import io.takari.bpm.mvstore.StoreTypes.ExpiredEventIndexKey;
import io.takari.bpm.mvstore.StoreTypes.Unit;
import org.h2.mvstore.Cursor;
import org.h2.mvstore.MVMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MvStoreEventStorage implements EventStorage {

    private static final Logger log = LoggerFactory.getLogger(MvStoreEventStorage.class);

    private final MvStoreDatabase database;

    public MvStoreEventStorage(MvStoreDatabase database) {
        this.database = Objects.requireNonNull(database, "database must not be null");
    }

    @Override
    public Event get(UUID id) {
        Objects.requireNonNull(id, "id must not be null");

        byte[] bytes = database.read(() -> database.eventsMap().get(id));
        return EventCodec.decode(bytes, database.getSerializer());
    }

    @Override
    public Event remove(UUID id) {
        Objects.requireNonNull(id, "id must not be null");

        byte[] removed = database.write(() -> removeInTx(id));

        Event event = null;
        if (removed != null) {
            try {
                event = EventCodec.decode(removed, database.getSerializer());
            } catch (PersistenceException e) {
                log.warn("remove ['{}'] -> deleted record could not be decoded", id, e);
            }
        }

        log.debug("remove ['{}'] -> done ({})", id, removed != null);
        return event;
    }

    @Override
    public Collection<Event> find(String processBusinessKey) {
        Objects.requireNonNull(processBusinessKey, "processBusinessKey must not be null");

        List<byte[]> rawEvents = database.read(() -> {
            List<byte[]> results = new ArrayList<>();
            MVMap<BusinessKeyIndexKey, Unit> index = database.businessKeyIndex();
            MVMap<UUID, byte[]> events = database.eventsMap();

            BusinessKeyIndexKey fromKey = new BusinessKeyIndexKey(processBusinessKey, null, null);
            Cursor<BusinessKeyIndexKey, Unit> cursor = index.cursor(fromKey);
            while (cursor.hasNext()) {
                BusinessKeyIndexKey key = cursor.next();
                if (!processBusinessKey.equals(key.businessKey())) {
                    break;
                }
                byte[] eventBytes = events.get(key.eventId());
                if (eventBytes != null) {
                    results.add(eventBytes);
                }
            }
            return results;
        });

        return decodeAll(rawEvents);
    }

    @Override
    public Collection<Event> find(String processBusinessKey, String eventName) {
        Objects.requireNonNull(processBusinessKey, "processBusinessKey must not be null");

        List<byte[]> rawEvents = database.read(() -> {
            List<byte[]> results = new ArrayList<>();
            MVMap<BusinessKeyIndexKey, Unit> index = database.businessKeyIndex();
            MVMap<UUID, byte[]> events = database.eventsMap();

            BusinessKeyIndexKey fromKey = new BusinessKeyIndexKey(processBusinessKey, eventName, null);
            Cursor<BusinessKeyIndexKey, Unit> cursor = index.cursor(fromKey);
            while (cursor.hasNext()) {
                BusinessKeyIndexKey key = cursor.next();
                if (!processBusinessKey.equals(key.businessKey()) || !Objects.equals(eventName, key.eventName())) {
                    break;
                }
                byte[] eventBytes = events.get(key.eventId());
                if (eventBytes != null) {
                    results.add(eventBytes);
                }
            }
            return results;
        });

        return decodeAll(rawEvents);
    }

    /**
     * Persists an event and updates its secondary indexes. Events with a {@code null}
     * {@code processBusinessKey} are stored by ID and (if applicable) expiration time,
     * but are not indexed for {@link #find(String)} lookups.
     */
    @Override
    public void add(Event event) {
        Objects.requireNonNull(event, "event must not be null");
        UUID id = Objects.requireNonNull(event.getId(), "event.id must not be null");

        byte[] encoded = EventCodec.encode(event, database.getSerializer());

        database.write(() -> {
            putInTx(event, encoded);
            return null;
        });

        log.debug("add ['{}'] -> done", id);
    }

    /**
     * Stores an encoded event and its index entries. Must be called inside {@link MvStoreDatabase#write}.
     */
    void putInTx(Event event, byte[] encoded) {
        UUID id = event.getId();
        byte[] prev = database.eventsMap().put(id, encoded);
        if (prev != null) {
            removeIndexEntriesForPreviousRecord(id, prev);
        }

        if (event.getProcessBusinessKey() != null) {
            database.businessKeyIndex().put(
                    new BusinessKeyIndexKey(event.getProcessBusinessKey(), event.getName(), id),
                    Unit.INSTANCE);
        }

        if (event.getExpiredAt() != null) {
            database.expiredEventsIndex().put(
                    new ExpiredEventIndexKey(event.getExpiredAt().getTime(), id),
                    Unit.INSTANCE);
        }
    }

    /**
     * Removes an event and its index entries. Must be called inside {@link MvStoreDatabase#write}.
     *
     * @return the removed record or {@code null}.
     */
    byte[] removeInTx(UUID id) {
        byte[] prev = database.eventsMap().remove(id);
        if (prev == null) {
            return null;
        }
        removeIndexEntriesForPreviousRecord(id, prev);
        return prev;
    }

    @Override
    public List<ExpiredEvent> findNextExpiredEvent(int maxEvents) {
        if (maxEvents <= 0) {
            return new ArrayList<>();
        }

        long nowMillis = database.getClock().millis();

        List<ExpiredEvent> result = database.write(() -> {
            MVMap<ExpiredEventIndexKey, Unit> index = database.expiredEventsIndex();
            List<ExpiredEventIndexKey> claimedKeys = new ArrayList<>();
            List<ExpiredEvent> expired = new ArrayList<>();

            Cursor<ExpiredEventIndexKey, Unit> cursor = index.cursor(null);
            while (cursor.hasNext() && expired.size() < maxEvents) {
                ExpiredEventIndexKey key = cursor.next();
                if (key.expiredAtMillis() > nowMillis) {
                    break;
                }
                claimedKeys.add(key);
                expired.add(new ExpiredEvent(key.eventId(), new Date(key.expiredAtMillis())));
            }

            for (ExpiredEventIndexKey key : claimedKeys) {
                index.remove(key);
            }

            return expired;
        });

        log.debug("findNextExpiredEvent [{}] -> done ({})", maxEvents, result.size());
        return result;
    }

    @Override
    public List<ExpiredEvent> findNextExpiredEvent(int maxEvents, long leaseMillis) {
        if (maxEvents <= 0) {
            return new ArrayList<>();
        }

        long nowMillis = database.getClock().millis();
        long leaseUntil = nowMillis + Math.max(0, leaseMillis);

        List<ExpiredEvent> result = database.write(() -> {
            MVMap<ExpiredEventIndexKey, Unit> index = database.expiredEventsIndex();
            MVMap<UUID, byte[]> events = database.eventsMap();
            List<ExpiredEventIndexKey> claimedKeys = new ArrayList<>();
            List<ExpiredEvent> expired = new ArrayList<>();

            Cursor<ExpiredEventIndexKey, Unit> cursor = index.cursor(null);
            while (cursor.hasNext() && expired.size() < maxEvents) {
                ExpiredEventIndexKey key = cursor.next();
                if (key.expiredAtMillis() > nowMillis) {
                    break;
                }
                claimedKeys.add(key);
                if (events.containsKey(key.eventId())) {
                    expired.add(new ExpiredEvent(key.eventId(), new Date(key.expiredAtMillis())));
                }
            }

            // keep the claimed events scheduled: they are acquired again after the lease expires
            // unless they are removed (i.e. processed) first. Entries of removed events are dropped
            for (ExpiredEventIndexKey key : claimedKeys) {
                index.remove(key);
                if (events.containsKey(key.eventId())) {
                    index.put(new ExpiredEventIndexKey(leaseUntil, key.eventId()), Unit.INSTANCE);
                }
            }

            return expired;
        });

        log.debug("findNextExpiredEvent [{}, {}] -> done ({})", maxEvents, leaseMillis, result.size());
        return result;
    }

    private void removeIndexEntriesForPreviousRecord(UUID id, byte[] prevBytes) {
        try {
            EventMetadata metadata = EventCodec.decodeMetadata(prevBytes);
            if (metadata.processBusinessKey() != null) {
                database.businessKeyIndex().remove(
                        new BusinessKeyIndexKey(metadata.processBusinessKey(), metadata.name(), metadata.id()));
            }
            if (metadata.expiredAtMillis() != null) {
                database.expiredEventsIndex().remove(
                        new ExpiredEventIndexKey(metadata.expiredAtMillis(), metadata.id()));
            }
        } catch (PersistenceException e) {
            log.warn("Corrupt event record for id '{}', falling back to index scan cleanup", id, e);
            removeIndexEntriesByScan(id);
        }
    }

    private void removeIndexEntriesByScan(UUID id) {
        MVMap<BusinessKeyIndexKey, Unit> bkIndex = database.businessKeyIndex();
        List<BusinessKeyIndexKey> staleBkKeys = new ArrayList<>();
        Cursor<BusinessKeyIndexKey, Unit> bkCursor = bkIndex.cursor(null);
        while (bkCursor.hasNext()) {
            BusinessKeyIndexKey key = bkCursor.next();
            if (id.equals(key.eventId())) {
                staleBkKeys.add(key);
            }
        }
        for (BusinessKeyIndexKey key : staleBkKeys) {
            bkIndex.remove(key);
        }

        MVMap<ExpiredEventIndexKey, Unit> expIndex = database.expiredEventsIndex();
        List<ExpiredEventIndexKey> staleExpKeys = new ArrayList<>();
        Cursor<ExpiredEventIndexKey, Unit> expCursor = expIndex.cursor(null);
        while (expCursor.hasNext()) {
            ExpiredEventIndexKey key = expCursor.next();
            if (id.equals(key.eventId())) {
                staleExpKeys.add(key);
            }
        }
        for (ExpiredEventIndexKey key : staleExpKeys) {
            expIndex.remove(key);
        }
    }

    private List<Event> decodeAll(List<byte[]> rawEvents) {
        Serializer serializer = database.getSerializer();
        List<Event> events = new ArrayList<>(rawEvents.size());
        for (byte[] raw : rawEvents) {
            events.add(EventCodec.decode(raw, serializer));
        }
        return events;
    }
}
