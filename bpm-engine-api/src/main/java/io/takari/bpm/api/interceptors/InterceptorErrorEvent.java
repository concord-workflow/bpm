package io.takari.bpm.api.interceptors;

import java.io.Serializable;
import java.util.UUID;

public class InterceptorErrorEvent implements Serializable {

    private final String processBusinessKey;
    private final String processDefinitionId;
    private final UUID executionId;
    private final UUID scopeId;
    private final Throwable cause;

    public InterceptorErrorEvent(String processBusinessKey, String processDefinitionId, UUID executionId, UUID scopeId, Throwable cause) {
        this.processBusinessKey = processBusinessKey;
        this.processDefinitionId = processDefinitionId;
        this.executionId = executionId;
        this.scopeId = scopeId;
        this.cause = cause;
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

    public Throwable getCause() {
        return cause;
    }
}
