package io.takari.bpm.actions;

import java.util.List;

public class FollowNewGroupAction implements Action {

    private static final long serialVersionUID = 1L;

    private final String definitionId;
    private final String elementId;
    private final boolean exclusive;
    private final List<String> flowIds;

    public FollowNewGroupAction(String definitionId, String elementId, boolean exclusive) {
        this(definitionId, elementId, exclusive, null);
    }

    public FollowNewGroupAction(String definitionId, String elementId, boolean exclusive, List<String> flowIds) {
        this.definitionId = definitionId;
        this.elementId = elementId;
        this.exclusive = exclusive;
        this.flowIds = flowIds;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public String getElementId() {
        return elementId;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public List<String> getFlowIds() {
        return flowIds;
    }

    @Override
    public String toString() {
        return "FollowNewGroupAction [definitionId=" + definitionId + ", elementId=" + elementId + ", exclusive=" + exclusive + ", flowIds="
                + flowIds + "]";
    }
}
