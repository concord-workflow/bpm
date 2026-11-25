package io.takari.bpm.event;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.persistence.AtomicPersistenceManager;
import io.takari.bpm.persistence.PersistenceChanges;
import io.takari.bpm.persistence.PersistenceManager;
import io.takari.bpm.persistence.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Event manager backed by an {@link EventStorage}.
 * <p>
 * While an engine run is in progress (see {@link UnitOfWork}) the changes are buffered and applied at the
 * end of the run together with the process state. Reads take the buffered changes into account.
 */
public class EventPersistenceManagerImpl implements EventPersistenceManager {

    private static final Logger log = LoggerFactory.getLogger(EventPersistenceManagerImpl.class);

    private final EventStorage eventStorage;

    public EventPersistenceManagerImpl(EventStorage eventStorage) {
        this.eventStorage = eventStorage;
    }

    @Override
    public Event get(UUID id) {
        UnitOfWork uow = UnitOfWork.current();
        if (uow != null) {
            if (uow.isRemoved(id)) {
                return null;
            }
            for (Event e : uow.getAddedEvents()) {
                if (e.getId().equals(id)) {
                    return e;
                }
            }
        }

        Event result = eventStorage.get(id);
        log.debug("get ['{}'] -> done ({})", id, result != null);
        return result;
    }

    @Override
    public Event remove(UUID id) {
        UnitOfWork uow = UnitOfWork.current();
        if (uow != null) {
            Event e = get(id);
            uow.removeEvent(id);
            log.debug("remove ['{}'] -> deferred", id);
            return e;
        }

        Event result = eventStorage.remove(id);
        log.debug("remove ['{}'] -> done ({})", id, result != null);
        return result;
    }

    @Override
    public Collection<Event> find(String processBusinessKey, String eventName) {
        Collection<Event> result = overlay(eventStorage.find(processBusinessKey, eventName), processBusinessKey, eventName);
        log.debug("find ['{}', '{}'] -> done ({})", processBusinessKey, eventName, result.size());
        return result;
    }

    @Override
    public void add(Event event) throws ExecutionException {
        UnitOfWork uow = UnitOfWork.current();
        if (uow != null) {
            uow.addEvent(event);
            log.debug("add ['{}'] -> deferred", event);
            return;
        }

        eventStorage.add(event);
        log.debug("add ['{}'] -> done", event);
    }

    @Override
    public void clearGroup(String processBusinessKey, UUID groupId) {
        int removed = 0;
        Collection<Event> evs = overlay(eventStorage.find(processBusinessKey), processBusinessKey, null);
        for (Event e : evs) {
            UUID otherGid = e.getScopeId();
            if (groupId.equals(otherGid)) {
                remove(e.getId());
                removed++;
            }
        }
        log.debug("clearGroup ['{}', '{}'] -> total: {}, removed: {}", processBusinessKey, groupId, evs.size(), removed);
    }

    @Override
    public List<ExpiredEvent> findNextExpiredEvent(int maxEvents) throws ExecutionException {
        List<ExpiredEvent> result = eventStorage.findNextExpiredEvent(maxEvents);
        log.debug("findNextExpiredEvent [{}] -> done ({})", maxEvents, result.size());
        return result;
    }

    @Override
    public List<ExpiredEvent> findNextExpiredEvent(int maxEvents, long leaseMillis) throws ExecutionException {
        List<ExpiredEvent> result = eventStorage.findNextExpiredEvent(maxEvents, leaseMillis);
        log.debug("findNextExpiredEvent [{}, {}] -> done ({})", maxEvents, leaseMillis, result.size());
        return result;
    }

    @Override
    public void commit(PersistenceManager persistenceManager, PersistenceChanges changes) throws ExecutionException {
        if (changes.isEmpty()) {
            return;
        }

        if (persistenceManager instanceof AtomicPersistenceManager apm && apm.commit(changes, eventStorage)) {
            log.debug("commit -> done (atomic)");
            return;
        }

        // not atomic: new events, the state, removed events
        // a crash in the middle leaves orphaned events (ignored on resume) or stale events
        // (removed on resume), but the state remains resumable
        for (Event e : changes.getAddedEvents()) {
            eventStorage.add(e);
        }

        if (changes.getSavedState() != null) {
            persistenceManager.save(changes.getSavedState(), changes.getExpectedVersion());
        }

        if (changes.getRemovedStateId() != null) {
            persistenceManager.remove(changes.getRemovedStateId(), changes.getExpectedVersion());
        }

        for (UUID id : changes.getRemovedEvents()) {
            eventStorage.remove(id);
        }

        log.debug("commit -> done");
    }

    private static Collection<Event> overlay(Collection<Event> stored, String processBusinessKey, String eventName) {
        UnitOfWork uow = UnitOfWork.current();
        if (uow == null) {
            return stored;
        }

        List<Event> result = new ArrayList<>(stored.size());
        for (Event e : stored) {
            if (!uow.isRemoved(e.getId())) {
                result.add(e);
            }
        }

        for (Event e : uow.getAddedEvents()) {
            if (!Objects.equals(processBusinessKey, e.getProcessBusinessKey())) {
                continue;
            }
            if (eventName != null && !eventName.equals(e.getName())) {
                continue;
            }
            result.removeIf(x -> x.getId().equals(e.getId()));
            result.add(e);
        }

        return result;
    }
}
