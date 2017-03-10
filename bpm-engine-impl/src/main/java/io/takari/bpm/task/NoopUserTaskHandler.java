package io.takari.bpm.task;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.state.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoopUserTaskHandler implements UserTaskHandler {

    private static final Logger log = LoggerFactory.getLogger(NoopUserTaskHandler.class);

    @Override
    public ProcessInstance handle(ProcessInstance state, String definitionId, String elementId) throws ExecutionException {
        log.debug("handle ['{}', '{}', '{}'] -> noop", state.getId(), definitionId, elementId);
        return state;
    }
}
