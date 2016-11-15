package io.takari.bpm.actions;

import java.util.List;

public class FollowFlowsAction implements Action {

    private static final long serialVersionUID = 1L;

    private final String definitionId;
    private final String elementId;
    private final List<String> flowIds;

    public FollowFlowsAction(String definitionId, String elementId) {
        this(definitionId, elementId, null);
    }

    public FollowFlowsAction(String definitionId, String elementId, List<String> flowIds) {
        this.definitionId = definitionId;
        this.elementId = elementId;
        this.flowIds = flowIds;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public String getElementId() {
        return elementId;
    }

    public List<String> getFlowIds() {
        return flowIds;
    }

    @Override
    public String toString() {
        return "FollowFlowsAction[" +
                "definitionId=" + definitionId +
                ", elementId=" + elementId +
                ", flowIds=" + flowIds +
                ']';
    }
}
