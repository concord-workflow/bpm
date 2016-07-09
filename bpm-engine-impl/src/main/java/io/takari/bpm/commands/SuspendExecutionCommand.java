package io.takari.bpm.commands;

import io.takari.bpm.AbstractEngine;
import io.takari.bpm.DefaultExecution;
import io.takari.bpm.api.ExecutionException;

/**
 * Process suspension command. Invokes saving of the process state and
 * interrupts it execution.
 */
public class SuspendExecutionCommand implements ExecutionCommand {
	
	private static final long serialVersionUID = 1L;

    @Override
    public DefaultExecution exec(AbstractEngine e, DefaultExecution s) throws ExecutionException {
        s.pop();
        
        s.setSuspended(true);
        e.getPersistenceManager().save(s);
        
        return s;
    }
}
