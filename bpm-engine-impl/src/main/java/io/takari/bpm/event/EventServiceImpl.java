package io.takari.bpm.event;

import io.takari.bpm.api.*;
import io.takari.bpm.api.Event;
import io.takari.bpm.lock.LockManager;

import java.util.Collection;

public class EventServiceImpl implements EventService {

    private final LockManager lockManager;
    private final EventStorage eventStorage;

    public EventServiceImpl(LockManager lockManager, EventStorage eventStorage) {
        this.lockManager = lockManager;
        this.eventStorage = eventStorage;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Event> getEvents(String processBusinessKey) {
        lockManager.lock(processBusinessKey);
        try {
            return (Collection) eventStorage.find(processBusinessKey);
        } finally {
            lockManager.unlock(processBusinessKey);
        }
    }
}
