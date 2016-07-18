package io.takari.bpm.commands;

import io.takari.bpm.AbstractEngine;
import io.takari.bpm.DefaultExecution;
import io.takari.bpm.EventMapHelper;
import io.takari.bpm.api.ExecutionException;

public class ProcessEventMappingCommand implements ExecutionCommand {
	
	private static final long serialVersionUID = 1L;

    private final String definitionId;

    public ProcessEventMappingCommand(String definitionId) {
        this.definitionId = definitionId;
    }

    @Override
    public DefaultExecution exec(AbstractEngine engine, DefaultExecution execution) throws ExecutionException {
        execution.pop();
        
        if (!EventMapHelper.isEmpty(execution, definitionId)) {
            // we suspend if and only we are still waiting for the events of the
            // current (sub)process
            execution.push(new SuspendExecutionCommand());
        }
        
        return execution;
    }
}
