package io.takari.bpm.persistence;

import java.util.UUID;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.state.ProcessInstance;

public interface PersistenceManager {

    void save(ProcessInstance state) throws ExecutionException;

    ProcessInstance get(UUID id);

    void remove(UUID id);

    /**
     * Saves the state if the currently stored version is {@code expectedVersion} ({@code 0} if the state
     * wasn't stored yet). Implementations that support optimistic locking must throw
     * {@link StaleStateException} otherwise. The default implementation doesn't check the version.
     */
    default void save(ProcessInstance state, long expectedVersion) throws ExecutionException {
        save(state);
    }

    /**
     * Removes the state if the currently stored version is {@code expectedVersion}.
     *
     * @see #save(ProcessInstance, long)
     */
    default void remove(UUID id, long expectedVersion) throws ExecutionException {
        remove(id);
    }
}
