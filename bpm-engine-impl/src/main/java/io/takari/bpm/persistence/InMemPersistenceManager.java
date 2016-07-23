package io.takari.bpm.persistence;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.state.ProcessInstance;

public class InMemPersistenceManager implements PersistenceManager {

    private static final Logger log = LoggerFactory.getLogger(InMemPersistenceManager.class);

    private final Map<UUID, ProcessInstance> data = new ConcurrentHashMap<>();

    @Override
    public void save(ProcessInstance state) throws ExecutionException {
        data.put(state.getId(), state);
        log.debug("save ['{}'] -> done", state.getId());
    }

    @Override
    public ProcessInstance get(UUID id) {
        ProcessInstance i = data.get(id);
        if (i == null) {
            log.warn("get ['{}'] -> not found", id);
        } else {
            log.debug("get ['{}'] -> found", id);
        }
        return i;
    }

    @Override
    public void remove(UUID id) {
        data.remove(id);
        log.debug("remove ['{}'] -> done", id);
    }
}
