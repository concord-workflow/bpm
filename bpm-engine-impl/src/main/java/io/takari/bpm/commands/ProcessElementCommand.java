package io.takari.bpm.commands;

import java.util.UUID;

public class ProcessElementCommand implements Command {

    private static final long serialVersionUID = 1L;

    private final String definitionId;
    private final String elementId;
    private final UUID groupId;
    private final boolean exclusive;

    public ProcessElementCommand(String definitionId, String elementId) {
        this(definitionId, elementId, null, false);
    }

    public ProcessElementCommand(String definitionId, String elementId, UUID groupId, boolean exclusive) {
        this.definitionId = definitionId;
        this.elementId = elementId;
        this.groupId = groupId;
        this.exclusive = exclusive;
    }

    public String getDefinitionId() {
        return definitionId;
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
        return "ProcessElementCommand [definitionId=" + definitionId + ", elementId=" + elementId + ", groupId=" + groupId + ", exclusive="
                + exclusive + "]";
    }
}
