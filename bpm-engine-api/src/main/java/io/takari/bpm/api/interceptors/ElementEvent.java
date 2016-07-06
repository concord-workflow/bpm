package io.takari.bpm.api.interceptors;

import java.io.Serializable;
import java.util.UUID;

public class ElementEvent implements Serializable {

    private final String processBusinessKey;
    private final String processDefinitionId;
    private final UUID executionId;
    private final String elementId;

    public ElementEvent(String processBusinessKey, String processDefinitionId, UUID executionId, String elementId) {
        this.processBusinessKey = processBusinessKey;
        this.processDefinitionId = processDefinitionId;
        this.executionId = executionId;
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

    public String getElementId() {
        return elementId;
    }
}
