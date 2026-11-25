package io.takari.bpm.event;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.persistence.PersistenceChanges;
import io.takari.bpm.persistence.PersistenceManager;

public interface EventPersistenceManager {

    Event get(UUID id);

    Event remove(UUID id);

    Collection<Event> find(String processBusinessKey, String eventName);

    void clearGroup(String processBusinessKey, UUID groupId);

    void add(Event event) throws ExecutionException;

    List<ExpiredEvent> findNextExpiredEvent(int maxEvents) throws ExecutionException;

    /**
     * Applies the changes of an engine run. The default implementation applies them one by one, in the order
     * that keeps the data recoverable if the process crashes in the middle: new events, the state,
     * removed events.
     */
    default void commit(PersistenceManager persistenceManager, PersistenceChanges changes) throws ExecutionException {
        for (Event e : changes.getAddedEvents()) {
            add(e);
        }

        if (changes.getSavedState() != null) {
            persistenceManager.save(changes.getSavedState());
        }

        if (changes.getRemovedStateId() != null) {
            persistenceManager.remove(changes.getRemovedStateId());
        }

        for (UUID id : changes.getRemovedEvents()) {
            remove(id);
        }
    }
}
