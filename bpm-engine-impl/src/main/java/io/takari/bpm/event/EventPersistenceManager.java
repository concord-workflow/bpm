package io.takari.bpm.event;

import io.takari.bpm.api.ExecutionException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface EventPersistenceManager {

    Event get(UUID id);

    Event remove(UUID id);

    Collection<Event> find(String processBusinessKey, String eventName);

    void clearGroup(String processBusinessKey, UUID groupId);

    void add(Event event) throws ExecutionException;

    List<ExpiredEvent> findNextExpiredEvent(int maxEvents) throws ExecutionException;
}
