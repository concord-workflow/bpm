package io.takari.bpm;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.model.ProcessDefinition;

public interface ProcessDefinitionProvider {

    /**
     * Returns a process definition object by its ID. Result must be the
     * same for each call of this method (idempotent).
     * @param id
     * @return process definition or {@code null}
     * @throws ExecutionException
     */
    ProcessDefinition getById(String id) throws ExecutionException;
}
