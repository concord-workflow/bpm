package io.takari.bpm.actions;

public class ProcessEventGatewayAction implements Action {

    private final String definitionId;
    private final String elementId;

    public ProcessEventGatewayAction(String definitionId, String elementId) {
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
        return "ProcessEventGatewayAction[" +
                "definitionId='" + definitionId + '\'' +
                ", elementId='" + elementId + '\'' +
                ']';
    }
}
