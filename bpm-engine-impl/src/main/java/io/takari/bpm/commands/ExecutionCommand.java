package io.takari.bpm.commands;

import io.takari.bpm.AbstractEngine;
import io.takari.bpm.DefaultExecution;
import io.takari.bpm.api.ExecutionException;
import java.io.Serializable;

public interface ExecutionCommand extends Serializable {

    /**
     * Executes a command with the specified process state.
     * @param engine
     * @param execution
     * @return modified or new process state.
     * @throws ExecutionException
     */
    DefaultExecution exec(AbstractEngine engine, DefaultExecution execution) throws ExecutionException;
}
