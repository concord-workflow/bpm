package io.takari.bpm.persistence;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.event.EventStorage;

/**
 * A {@link PersistenceManager} that can store the process state and the process events in a single transaction.
 */
public interface AtomicPersistenceManager extends PersistenceManager {

    /**
     * Applies all changes atomically: either all of them are stored or none.
     *
     * @param changes      the changes to apply.
     * @param eventStorage the engine's event storage.
     * @return {@code false} if the changes can't be applied atomically using the specified event storage
     * (e.g. it is backed by a different database). Nothing is changed in that case.
     */
    boolean commit(PersistenceChanges changes, EventStorage eventStorage) throws ExecutionException;
}
