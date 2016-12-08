package io.takari.bpm.actions;

import io.takari.bpm.misc.CoverageIgnore;

public class EvaluateAndFollowFlowsAction implements Action {

    private static final long serialVersionUID = 1L;

    private final String definitionId;
    private final String elementId;
    private final String defaultFlow;

    public EvaluateAndFollowFlowsAction(String definitionId, String elementId, String defaultFlow) {
        this.definitionId = definitionId;
        this.elementId = elementId;
        this.defaultFlow = defaultFlow;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public String getElementId() {
        return elementId;
    }

    public String getDefaultFlow() {
        return defaultFlow;
    }

    @Override
    @CoverageIgnore
    public String toString() {
        return "EvaluateAndFollowFlowsAction [definitionId=" + definitionId + ", elementId=" + elementId + ", defaultFlow=" + defaultFlow
                + "]";
    }
}
