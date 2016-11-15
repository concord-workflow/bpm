package io.takari.bpm.api.interceptors;

import java.io.Serializable;
import java.util.UUID;

public class InterceptorScopeDestroyedEvent implements Serializable {

    private final String processBusinessKey;
    private final UUID executionId;
    private final UUID scopeId;

    public InterceptorScopeDestroyedEvent(String processBusinessKey, UUID executionId, UUID scopeId) {
        this.processBusinessKey = processBusinessKey;
        this.executionId = executionId;
        this.scopeId = scopeId;
    }

    public String getProcessBusinessKey() {
        return processBusinessKey;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public UUID getScopeId() {
        return scopeId;
    }
}