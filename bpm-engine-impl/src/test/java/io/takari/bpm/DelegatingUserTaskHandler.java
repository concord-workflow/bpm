package io.takari.bpm;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.task.NoopUserTaskHandler;
import io.takari.bpm.task.UserTaskHandler;

public class DelegatingUserTaskHandler implements UserTaskHandler {

    private UserTaskHandler delegate = new NoopUserTaskHandler();

    public void set(UserTaskHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public ProcessInstance handle(ProcessInstance state, String definitionId, String elementId) throws ExecutionException {
        return delegate.handle(state, definitionId, elementId);
    }
}
