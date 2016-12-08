package io.takari.bpm.actions;

import java.util.Collection;

public class ActivateFlowsAction implements Action {

    private static final long serialVersionUID = 1L;

    private final String definitionId;
    private final Collection<String> elementIds;
    private final int count;

    public ActivateFlowsAction(String definitionId, Collection<String> elementIds, int count) {
        this.definitionId = definitionId;
        this.elementIds = elementIds;
        this.count = count;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public Collection<String> getElementIds() {
        return elementIds;
    }

    public int getCount() {
        return count;
    }

    @Override
    public String toString() {
        return "ActivateFlowsAction [definitionId=" + definitionId + ", elementIds=" + elementIds + ", count=" + count + "]";
    }
}
