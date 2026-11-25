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

    public PersistenceChanges(ProcessInstance savedState, UUID removedStateId, List<Event> addedEvents, List<UUID> removedEvents) {
        this.savedState = savedState;
        this.removedStateId = removedStateId;
        this.addedEvents = List.copyOf(addedEvents);
        this.removedEvents = List.copyOf(removedEvents);
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
