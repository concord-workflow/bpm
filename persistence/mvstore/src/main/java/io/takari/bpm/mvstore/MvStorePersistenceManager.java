package io.takari.bpm.mvstore;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.persistence.PersistenceManager;
import io.takari.bpm.state.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public class MvStorePersistenceManager implements PersistenceManager {

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
