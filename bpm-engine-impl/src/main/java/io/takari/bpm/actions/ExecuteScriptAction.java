package io.takari.bpm.actions;

public class ExecuteScriptAction implements Action {

    private final String definitionId;
    private final String elementId;

    public ExecuteScriptAction(String definitionId, String elementId) {
        this.definitionId = definitionId;
        this.elementId = elementId;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public String getElementId() {
        return elementId;
    }

    @Override
    public String toString() {
        return "ExecuteScriptAction [" +
                "definitionId=" + definitionId +
                ", elementId=" + elementId +
                ']';
    }
}
