package io.takari.bpm.mvstore;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.event.Event;
import io.takari.bpm.event.EventStorage;
import io.takari.bpm.persistence.AtomicPersistenceManager;
import io.takari.bpm.persistence.PersistenceChanges;
import io.takari.bpm.persistence.StaleStateException;
import io.takari.bpm.state.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Stores process instances in an {@link MvStoreDatabase}. When used together with the database's
 * {@link MvStoreEventStorage}, the engine commits the state and the events of a run in a single MVStore commit.
 */
public class MvStorePersistenceManager implements AtomicPersistenceManager {

    private static final Logger log = LoggerFactory.getLogger(MvStorePersistenceManager.class);
    /**
     * Format 1: [1][payload]. Format 2: [2][state version, 8 bytes][payload].
     */
    private static final byte FORMAT_V1 = 1;
    private static final byte FORMAT_V2 = 2;
    private static final int V2_HEADER_SIZE = 1 + Long.BYTES;

    private final MvStoreDatabase database;

    public MvStorePersistenceManager(MvStoreDatabase database) {
        this.database = Objects.requireNonNull(database, "database must not be null");
    }

    @Override
    public void save(ProcessInstance state) throws ExecutionException {
        Objects.requireNonNull(state, "state must not be null");
        UUID id = Objects.requireNonNull(state.getId(), "state.id must not be null");

        byte[] record = encode(state);

        try {
            database.write(() -> {
                database.executionsMap().put(id, record);
                return null;
            });
        } catch (PersistenceException e) {
            throw new ExecutionException("Failed to persist process instance: " + id, e);
        }

        log.debug("save ['{}'] -> done", id);
    }

    @Override
    public void save(ProcessInstance state, long expectedVersion) throws ExecutionException {
        Objects.requireNonNull(state, "state must not be null");
        UUID id = Objects.requireNonNull(state.getId(), "state.id must not be null");

        byte[] record = encode(state);

        try {
            database.write(() -> {
                checkVersion(id, expectedVersion);
                database.executionsMap().put(id, record);
                return null;
            });
        } catch (PersistenceException e) {
            throw unwrap(e, "Failed to persist process instance: " + id);
        }

        log.debug("save ['{}', {}] -> done", id, expectedVersion);
    }

    @Override
    public void remove(UUID id, long expectedVersion) throws ExecutionException {
        Objects.requireNonNull(id, "id must not be null");

        try {
            database.write(() -> {
                checkVersion(id, expectedVersion);
                database.executionsMap().remove(id);
                return null;
            });
        } catch (PersistenceException e) {
            throw unwrap(e, "Failed to remove process instance: " + id);
        }

        log.debug("remove ['{}', {}] -> done", id, expectedVersion);
    }

    private static ExecutionException unwrap(PersistenceException e, String message) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof StaleStateRuntimeException s) {
                return (StaleStateException) s.getCause();
            }
        }
        return new ExecutionException(message, e);
    }

    @Override
    public boolean commit(PersistenceChanges changes, EventStorage eventStorage) throws ExecutionException {
        MvStoreEventStorage events = database.eventStorage();
        if (eventStorage != events) {
            return false;
        }

        // serialize outside of the I/O thread
        ProcessInstance state = changes.getSavedState();
        byte[] record = state != null ? encode(state) : null;

        List<Event> addedEvents = changes.getAddedEvents();
        List<byte[]> encodedEvents = new ArrayList<>(addedEvents.size());
        for (Event e : addedEvents) {
            encodedEvents.add(EventCodec.encode(e, database.getSerializer()));
        }

        try {
            database.write(() -> {
                UUID instanceId = state != null ? state.getId() : changes.getRemovedStateId();
                if (instanceId != null) {
                    checkVersion(instanceId, changes.getExpectedVersion());
                }

                for (int i = 0; i < addedEvents.size(); i++) {
                    events.putInTx(addedEvents.get(i), encodedEvents.get(i));
                }

                if (state != null) {
                    database.executionsMap().put(state.getId(), record);
                }

                if (changes.getRemovedStateId() != null) {
                    database.executionsMap().remove(changes.getRemovedStateId());
                }

                for (UUID id : changes.getRemovedEvents()) {
                    events.removeInTx(id);
                }

                return null;
            });
        } catch (PersistenceException e) {
            throw unwrap(e, "Failed to commit the process changes");
        }

        log.debug("commit -> done (state: {}, added events: {}, removed events: {})",
                state != null ? state.getId() : changes.getRemovedStateId(), addedEvents.size(), changes.getRemovedEvents().size());

        return true;
    }

    private byte[] encode(ProcessInstance state) throws ExecutionException {
        UUID id = state.getId();

        byte[] payload;
        try {
            payload = database.getSerializer().serialize(state);
        } catch (PersistenceException e) {
            throw new ExecutionException("Failed to serialize process instance: " + id, e);
        }

        if (payload == null) {
            throw new ExecutionException("Serializer returned null for process instance: " + id);
        }

        return ByteBuffer.allocate(V2_HEADER_SIZE + payload.length)
                .put(FORMAT_V2)
                .putLong(state.getVersion())
                .put(payload)
                .array();
    }

    /**
     * @return the version of the stored state, {@code 0} if there's no state or the record doesn't have a version.
     */
    private static long storedVersion(byte[] record) {
        if (record == null || record.length < V2_HEADER_SIZE || record[0] != FORMAT_V2) {
            return 0;
        }
        return ByteBuffer.wrap(record, 1, Long.BYTES).getLong();
    }

    private void checkVersion(UUID id, long expectedVersion) {
        long actual = storedVersion(database.executionsMap().get(id));
        if (actual != expectedVersion) {
            throw new StaleStateRuntimeException(new StaleStateException(id, expectedVersion, actual));
        }
    }

    /**
     * Carries {@link StaleStateException} out of the database's write action.
     */
    private static final class StaleStateRuntimeException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        private StaleStateRuntimeException(StaleStateException cause) {
            super(cause);
        }
    }

    @Override
    public ProcessInstance get(UUID id) {
        Objects.requireNonNull(id, "id must not be null");

        byte[] record = database.read(() -> database.executionsMap().get(id));
        if (record == null) {
            log.debug("get ['{}'] -> not found", id);
            return null;
        }
        if (record.length == 0) {
            throw new PersistenceException("Empty process instance record for id " + id);
        }
        byte[] payload;
        if (record[0] == FORMAT_V1) {
            payload = Arrays.copyOfRange(record, 1, record.length);
        } else if (record[0] == FORMAT_V2 && record.length >= V2_HEADER_SIZE) {
            payload = Arrays.copyOfRange(record, V2_HEADER_SIZE, record.length);
        } else {
            throw new PersistenceException("Unsupported process instance format version " + record[0] + " for id " + id);
        }
        Object obj = database.getSerializer().deserialize(payload);
        if (!(obj instanceof ProcessInstance instance)) {
            throw new PersistenceException("Expected ProcessInstance for id " + id + ", got: "
                    + (obj != null ? obj.getClass().getName() : "null"));
        }

        log.debug("get ['{}'] -> found", id);
        return instance;
    }

    @Override
    public void remove(UUID id) {
        Objects.requireNonNull(id, "id must not be null");

        database.write(() -> {
            database.executionsMap().remove(id);
            return null;
        });

        log.debug("remove ['{}'] -> done", id);
    }
}
