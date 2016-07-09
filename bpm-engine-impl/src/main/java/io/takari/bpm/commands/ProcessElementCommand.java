package io.takari.bpm.commands;

import io.takari.bpm.AbstractEngine;
import io.takari.bpm.DefaultExecution;
import io.takari.bpm.api.ExecutionException;
import java.util.UUID;

public class ProcessElementCommand implements ExecutionCommand {
	
	private static final long serialVersionUID = 1L;

    private final String processDefinitionId;
    private final String elementId;
    private final UUID groupId;
    private final boolean exclusive;

    public ProcessElementCommand(String processDefinitionId, String elementId) {
        this(processDefinitionId, elementId, null, false);
    }

    public ProcessElementCommand(String processDefinitionId, String elementId, UUID groupId, boolean exclusive) {
        this.processDefinitionId = processDefinitionId;
        this.elementId = elementId;
        this.groupId = groupId;
        this.exclusive = exclusive;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public String getElementId() {
        return elementId;
    }

    public UUID getGroupId() {
        return groupId;
    }

    /**
     * Indicates the exclusiveness of this flow. Used to mark exclusive branches
     * of parallel flows.
     */
    public boolean isExclusive() {
        return exclusive;
    }
    
    @Override
    public DefaultExecution exec(AbstractEngine engine, DefaultExecution execution) throws ExecutionException {
        engine.getElementHandler().handle(execution, this);

        // perform notification of an element activation
        execution.onActivation(execution, processDefinitionId, elementId);
        engine.getInterceptorHolder().fireOnElement(execution.getBusinessKey(), processDefinitionId, execution.getId(), elementId);
        
        return execution;
    }
}
