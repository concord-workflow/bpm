package io.takari.bpm.actions;

import java.util.UUID;

public class FollowFlowsAction implements Action {

    private static final long serialVersionUID = 1L;

    private final String definitionId;
    private final String elementId;
    private final UUID groupId;
    private final boolean exclusive;

    public FollowFlowsAction(String definitionId, String elementId, UUID groupId, boolean exclusive) {
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
        return "FollowFlowsAction [definitionId=" + definitionId + ", elementId=" + elementId + ", groupId=" + groupId + ", exclusive="
                + exclusive + "]";
    }
}
