package io.takari.bpm.persistence;

import io.takari.bpm.event.Event;
import io.takari.bpm.state.ProcessInstance;

import java.util.List;
import java.util.UUID;

/**
 * Changes made by a single engine run: the state to save (or the ID of the instance to remove), the events
 * created by the run and the events consumed or discarded by it. The changes must be applied together.
 */
public final class PersistenceChanges {

    private final ProcessInstance savedState;
    private final UUID removedStateId;
    private final List<Event> addedEvents;
    private final List<UUID> removedEvents;
    private final long expectedVersion;

    public PersistenceChanges(ProcessInstance savedState, UUID removedStateId, List<Event> addedEvents, List<UUID> removedEvents) {
        this(savedState, removedStateId, addedEvents, removedEvents, savedState != null ? Math.max(0, savedState.getVersion() - 1) : 0);
    }

    public PersistenceChanges(ProcessInstance savedState, UUID removedStateId, List<Event> addedEvents, List<UUID> removedEvents,
                              long expectedVersion) {
        this.savedState = savedState;
        this.removedStateId = removedStateId;
        this.addedEvents = List.copyOf(addedEvents);
        this.removedEvents = List.copyOf(removedEvents);
        this.expectedVersion = expectedVersion;
    }

    /**
     * @return the version of the state that must be currently stored for the changes to be applied:
     * the version of the state loaded by the run, {@code 0} for a new instance.
     * See {@link PersistenceManager#save(ProcessInstance, long)}.
     */
    public long getExpectedVersion() {
        return expectedVersion;
    }

    /**
     * @return the state to save or {@code null}.
     */
    public ProcessInstance getSavedState() {
        return savedState;
    }

    /**
     * @return the ID of the instance to remove or {@code null}.
     */
    public UUID getRemovedStateId() {
        return removedStateId;
    }

    public List<Event> getAddedEvents() {
        return addedEvents;
    }

    public List<UUID> getRemovedEvents() {
        return removedEvents;
    }

    public boolean isEmpty() {
        return savedState == null && removedStateId == null && addedEvents.isEmpty() && removedEvents.isEmpty();
    }
}
