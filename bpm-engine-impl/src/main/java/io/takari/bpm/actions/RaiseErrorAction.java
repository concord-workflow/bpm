package io.takari.bpm.actions;

public class RaiseErrorAction implements Action {

    private static final long serialVersionUID = 1L;

    private final String errorRef;
    private final String causeExpression;

    public RaiseErrorAction(String errorRef, String causeExpression) {
        this.errorRef = errorRef;
        this.causeExpression = causeExpression;
    }

    public String getErrorRef() {
        return errorRef;
    }

    public String getCauseExpression() {
        return causeExpression;
    }

    @Override
    public String toString() {
        return "RaiseErrorAction [errorRef=" + errorRef + ", causeExpression=" + causeExpression + "]";
    }

}
