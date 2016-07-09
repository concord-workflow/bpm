package io.takari.bpm.commands;

import io.takari.bpm.AbstractEngine;
import io.takari.bpm.DefaultExecution;
import io.takari.bpm.EventMapHelper;
import io.takari.bpm.api.ExecutionException;

public class ProcessEventMappingCommand implements ExecutionCommand {
	
	private static final long serialVersionUID = 1L;

    @Override
    public DefaultExecution exec(AbstractEngine engine, DefaultExecution execution) throws ExecutionException {
        execution.pop();
        
        if (!EventMapHelper.isEmpty(execution)) {
            execution.push(new SuspendExecutionCommand());
        }
        
        return execution;
    }
}
