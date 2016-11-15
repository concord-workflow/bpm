package io.takari.bpm.api.interceptors;

import java.io.Serializable;
import java.util.UUID;

public class InterceptorScopeCreatedEvent implements Serializable {

    private final String processBusinessKey;
    private final String processDefinitionId;
    private final UUID executionId;
    private final UUID scopeId;
    private final String elementId;

    public InterceptorScopeCreatedEvent(String processBusinessKey, String processDefinitionId, UUID executionId, UUID scopeId, String elementId) {
        this.processBusinessKey = processBusinessKey;
        this.processDefinitionId = processDefinitionId;
        this.executionId = executionId;
        this.scopeId = scopeId;
        this.elementId = elementId;
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

    public UUID getScopeId() {
        return scopeId;
    }

    public String getElementId() {
        return elementId;
    }
}
