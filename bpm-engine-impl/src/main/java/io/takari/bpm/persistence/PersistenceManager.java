package io.takari.bpm.persistence;

import java.util.UUID;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.state.ProcessInstance;

public interface PersistenceManager {

    void save(ProcessInstance state) throws ExecutionException;

    ProcessInstance get(UUID id);

    void remove(UUID id);
}
