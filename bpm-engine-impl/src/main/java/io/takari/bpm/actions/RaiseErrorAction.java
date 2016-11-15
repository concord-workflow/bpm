package io.takari.bpm.actions;

public class RaiseErrorAction implements Action {

    private static final long serialVersionUID = 1L;

    private final String definitionId;
    private final String elementId;
    private final String errorRef;
    private final String causeExpression;

    public RaiseErrorAction(String definitionId, String elementId, String errorRef, String causeExpression) {
        this.definitionId = definitionId;
        this.elementId = elementId;
        this.errorRef = errorRef;
        this.causeExpression = causeExpression;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public String getElementId() {
        return elementId;
    }

    public String getErrorRef() {
        return errorRef;
    }

    public String getCauseExpression() {
        return causeExpression;
    }

    @Override
    public String toString() {
        return "RaiseErrorAction[" +
                "definitionId=" + definitionId +
                ", elementId=" + elementId +
                ", errorRef=" + errorRef +
                ", causeExpression=" + causeExpression +
                ']';
    }
}
