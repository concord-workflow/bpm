package io.takari.bpm.actions;

public class CreateEventAction implements Action {

    private static final long serialVersionUID = 1L;

    private final String definitionId;
    private final String elementId;

    public CreateEventAction(String definitionId, String elementId) {
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
        return "CreateEventAction[" +
                "definitionId=" + definitionId +
                ", elementId=" + elementId +
                ']';
    }
}
