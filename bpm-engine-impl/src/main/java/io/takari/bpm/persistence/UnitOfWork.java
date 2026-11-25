package io.takari.bpm.persistence;

import io.takari.bpm.event.Event;
import io.takari.bpm.state.ProcessInstance;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Collects the persistence changes of an engine run (see {@link PersistenceChanges}).
 * The changes are applied only if the run succeeds. If the run fails, the stored state and events remain
 * untouched and the run can be retried.
 * <p>
 * A unit of work is bound to the current thread. Units of work can be nested, e.g. when a task starts
 * another process.
 */
public final class UnitOfWork {

    private static final ThreadLocal<Deque<UnitOfWork>> CURRENT = ThreadLocal.withInitial(ArrayDeque::new);

    public static UnitOfWork begin() {
        UnitOfWork uow = new UnitOfWork();
        CURRENT.get().push(uow);
        return uow;
    }

    /**
     * @return the current unit of work or {@code null}.
     */
    public static UnitOfWork current() {
        return CURRENT.get().peek();
    }

    private final Map<UUID, Event> addedEvents = new LinkedHashMap<>();
    private final Set<UUID> removedEvents = new LinkedHashSet<>();
    private ProcessInstance savedState;
    private UUID removedStateId;
    private long expectedVersion;

    private UnitOfWork() {
    }

    public void addEvent(Event e) {
        removedEvents.remove(e.getId());
        addedEvents.put(e.getId(), e);
    }

    public void removeEvent(UUID id) {
        if (addedEvents.remove(id) == null) {
            removedEvents.add(id);
        }
    }

    public boolean isRemoved(UUID eventId) {
        return removedEvents.contains(eventId);
    }

    public List<Event> getAddedEvents() {
        return new ArrayList<>(addedEvents.values());
    }

    /**
     * @param state the state to save. Its version must be the version of the loaded state,
     *              the saved state gets the next version.
     */
    public void saveState(ProcessInstance state) {
        this.expectedVersion = state.getVersion();
        this.savedState = state.setVersion(state.getVersion() + 1);
        this.removedStateId = null;
    }

    /**
     * @param state the state to remove.
     */
    public void removeState(ProcessInstance state) {
        this.expectedVersion = state.getVersion();
        this.savedState = null;
        this.removedStateId = state.getId();
    }

    public PersistenceChanges toChanges() {
        return new PersistenceChanges(savedState, removedStateId, new ArrayList<>(addedEvents.values()), new ArrayList<>(removedEvents),
                expectedVersion);
    }

    /**
     * Unbinds the unit of work from the current thread.
     */
    public void end() {
        Deque<UnitOfWork> stack = CURRENT.get();
        stack.remove(this);
        if (stack.isEmpty()) {
            CURRENT.remove();
        }
    }
}
