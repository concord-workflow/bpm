package io.takari.bpm.api.interceptors;

import java.io.Serializable;
import java.util.UUID;

public class InterceptorElementEvent implements Serializable {
	
	private static final long serialVersionUID = 1L;

    private final String processBusinessKey;
    private final String processDefinitionId;
    private final UUID executionId;
    private final String elementId;
    private final UUID scopeId;

    public InterceptorElementEvent(String processBusinessKey, String processDefinitionId, UUID executionId, String elementId, UUID scopeId) {
        this.processBusinessKey = processBusinessKey;
        this.processDefinitionId = processDefinitionId;
        this.executionId = executionId;
        this.elementId = elementId;
        this.scopeId = scopeId;
    }

    public String getProcessBusinessKey() {
        return processBusinessKey;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public String getElementId() {
        return elementId;
    }

    public UUID getScopeId() {
        return scopeId;
    }
}
