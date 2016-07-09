package io.takari.bpm.commands;

import io.takari.bpm.AbstractEngine;
import io.takari.bpm.BpmnErrorHelper;
import io.takari.bpm.DefaultExecution;
import io.takari.bpm.ExecutionContextHelper;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.model.VariableMapping;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command to merge contexts of a parent and child processes. Designed to run
 * after the child process ends. On its execution, out variables of the child
 * process will become variables in the parent process.
 */
public class MergeExecutionContextCommand implements ExecutionCommand {
	
	private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(MergeExecutionContextCommand.class);

    private final ExecutionContext target;
    private final boolean copyAllVariables;
    private final Set<VariableMapping> outVariables;

    public MergeExecutionContextCommand(ExecutionContext target, Set<VariableMapping> outVariables) {
        this.target = target;
        this.outVariables = outVariables;
        this.copyAllVariables = false;
    }
    
    public MergeExecutionContextCommand(ExecutionContext target) {
        this.target = target;
        this.outVariables = null;
        this.copyAllVariables = true;
    }

    @Override
    public DefaultExecution exec(AbstractEngine engine, DefaultExecution execution) throws ExecutionException {
        execution.pop();
        
        ExecutionContext source = execution.getContext();
        execution.setContext(target);
        
        // TODO: refactor as a conditional command?
        String errorRef = BpmnErrorHelper.getRaisedError(source);
        if (errorRef != null) {
            // raise the error to the parent process
            BpmnErrorHelper.raiseError(target, errorRef);
            log.debug("raising error '{}'", errorRef);
            return execution;
        }
        
        if (copyAllVariables) {
            ExecutionContextHelper.copyVariables(source, target);
        } else {
            ExecutionContextHelper.copyVariables(engine.getExpressionManager(), source, target, outVariables);
        }
        
        return execution;
    }
}
