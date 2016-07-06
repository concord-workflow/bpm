package io.takari.bpm.commands;

import io.takari.bpm.AbstractEngine;
import io.takari.bpm.DefaultExecution;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.persistence.PersistenceManager;

public class PersistExecutionCommand implements ExecutionCommand {

    @Override
    public DefaultExecution exec(AbstractEngine engine, DefaultExecution execution) throws ExecutionException {
        execution.pop();
        
        PersistenceManager pm = engine.getPersistenceManager();
        pm.save(execution);
        
        return execution;
    }
}
