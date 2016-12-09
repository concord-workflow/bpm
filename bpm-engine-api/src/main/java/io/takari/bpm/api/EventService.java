package io.takari.bpm.api;

import java.util.Collection;

public interface EventService {

    /**
     * Returns a collection of currently waiting events for the specified process instance.
     * @param processBusinessKey external business key of a process instance.
     * @return collection of events.
     */
    Collection<Event> getEvents(String processBusinessKey);
}
