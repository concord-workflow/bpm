package io.takari.bpm;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.model.ProcessDefinition;

public interface ProcessDefinitionProvider {

    /**
     * Returns a process definition object by its ID. Result must be the
     * same for each call of this method (idempotency).
     * @param id
     * @return
     * @throws ExecutionException if process definition not found.
     */
    public ProcessDefinition getById(String id) throws ExecutionException;
}
