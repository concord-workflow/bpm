package io.takari.bpm.leveldb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.nio.ByteBuffer;
import java.util.UUID;

import org.iq80.leveldb.DBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.takari.bpm.persistence.PersistenceManager;
import io.takari.bpm.state.ProcessInstance;

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
        byte[] bytes = db.get(key);
        ProcessInstance e = unmarshallValue(bytes);
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
            return null;
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
            log.error("marshallValue -> error", e);
            return null;
        }
    }
}
