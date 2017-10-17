package io.takari.bpm.leveldb;

import io.takari.bpm.persistence.PersistenceManager;
import io.takari.bpm.state.ProcessInstance;
import org.iq80.leveldb.DBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.UUID;

public class LevelDbPersistenceManager implements PersistenceManager {

    private static final Logger log = LoggerFactory.getLogger(LevelDbPersistenceManager.class);

    private final LevelDb db;

    public LevelDbPersistenceManager(Configuration cfg, DBFactory dbFactory) {
        this.db = new LevelDb(dbFactory, cfg.getExecutionPath(), cfg.isSyncWrite());
    }

    public void init() {
        db.init();
    }

    public void close() {
        db.close();
    }

    @Override
    public void save(ProcessInstance execution) {
        byte[] key = marshallKey(execution.getId());
        db.put(key, marshallValue(execution));
        log.debug("save ['{}'] -> done", execution.getId());
    }

    @Override
    public ProcessInstance get(UUID id) {
        byte[] key = marshallKey(id);
        byte[] bytes = db.get(key);

        return unmarshallValue(bytes);
    }

    @Override
    public void remove(UUID id) {
        byte[] key = marshallKey(id);
        db.delete(key);
    }

    private static byte[] marshallKey(UUID id) {
        long mostSigBits = id.getMostSignificantBits();
        long leastSigBits = id.getLeastSignificantBits();
        return ByteBuffer.allocate(8 + 8)
                .putLong(mostSigBits)
                .putLong(leastSigBits)
                .array();
    }

    private byte[] marshallValue(ProcessInstance execution) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(execution);
            return bos.toByteArray();
        } catch (IOException e) {
            log.error("marshallValue -> error", e);
            throw new RuntimeException(e);
        }
    }

    private ProcessInstance unmarshallValue(byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                ObjectInput in = new ObjectInputStream(bis) {

                    @Override
                    protected Class<?> resolveClass(final ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                        final String name = desc.getName();
                        try {
                            return Class.forName(name);
                        } catch (final ClassNotFoundException ex) {
                            return Class.forName(name, false, Thread.currentThread().getContextClassLoader());
                        }
                    }

                }) {
            return (ProcessInstance) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            log.error("unmarshallValue -> error", e);
            throw new RuntimeException(e);
        }
    }
}
