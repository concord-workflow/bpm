package io.takari.bpm.persistence;

import io.takari.bpm.DefaultExecution;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DummyPersistenceManager extends MapPersistenceManager {

    public DummyPersistenceManager() {
        super(new ConcurrentHashMap<UUID, DefaultExecution>());
    }
}
