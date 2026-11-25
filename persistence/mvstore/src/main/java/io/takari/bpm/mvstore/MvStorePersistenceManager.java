package io.takari.bpm.mvstore;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.event.Event;
import io.takari.bpm.event.EventStorage;
import io.takari.bpm.persistence.AtomicPersistenceManager;
import io.takari.bpm.persistence.PersistenceChanges;
import io.takari.bpm.state.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final byte FORMAT_VERSION = 1;

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
            throw new ExecutionException("Failed to commit the process changes", e);
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

        byte[] record = new byte[1 + payload.length];
        record[0] = FORMAT_VERSION;
        System.arraycopy(payload, 0, record, 1, payload.length);
        return record;
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
        if (record[0] != FORMAT_VERSION) {
            throw new PersistenceException("Unsupported process instance format version " + record[0] + " for id " + id);
        }

        byte[] payload = Arrays.copyOfRange(record, 1, record.length);
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
