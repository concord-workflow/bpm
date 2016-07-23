package io.takari.bpm.commands;

import java.util.UUID;

import io.takari.bpm.api.BpmnError;

public class ProcessBpmnErrorCommand implements Command {

    private static final long serialVersionUID = 1L;

    private final BpmnError bpmnError;
    private final String elementId;
    private final UUID groupId;
    private final boolean exclusive;

    public ProcessBpmnErrorCommand(BpmnError bpmnError, String elementId, UUID groupId, boolean exclusive) {
        this.bpmnError = bpmnError;
        this.elementId = elementId;
        this.groupId = groupId;
        this.exclusive = exclusive;
    }

    public BpmnError getBpmnError() {
        return bpmnError;
    }

    public String getElementId() {
        return elementId;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    @Override
    public String toString() {
        return "ProcessBpmnErrorCommand [bpmnError=" + bpmnError + ", elementId=" + elementId + ", groupId=" + groupId + ", exclusive="
                + exclusive + "]";
    }
}
