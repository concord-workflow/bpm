package io.takari.bpm.event;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface EventStorage {
    
    Event get(UUID id);

    Event remove(UUID id);

    Collection<Event> find(String processBusinessKey);
    
    Collection<Event> find(String processBusinessKey, String eventName);

    void add(Event event);

    List<ExpiredEvent> findNextExpiredEvent(int maxEvents);

    /**
     * Claims up to {@code maxEvents} expired events with a lease. The claimed events are not returned again
     * until the lease expires: if the event is still in the storage at that point (e.g. the process crashed
     * or the resume failed), it is returned again. Events are removed only when their process is resumed.
     * <p>
     * The default implementation falls back to {@link #findNextExpiredEvent(int)}, i.e. no retries.
     *
     * @param maxEvents   max number of events to claim.
     * @param leaseMillis lease duration.
     */
    default List<ExpiredEvent> findNextExpiredEvent(int maxEvents, long leaseMillis) {
        return findNextExpiredEvent(maxEvents);
    }
}
