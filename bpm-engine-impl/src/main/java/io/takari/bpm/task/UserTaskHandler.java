package io.takari.bpm.task;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.state.ProcessInstance;

public interface UserTaskHandler {

    ProcessInstance handle(ProcessInstance state, String definitionId, String elementId) throws ExecutionException;
}
