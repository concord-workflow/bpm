package io.takari.bpm.model;

public class EndEvent extends AbstractElement {

    private static final long serialVersionUID = 1L;

    private final String errorRef;
    private final String causeExpression;

    public EndEvent(String id) {
        this(id, null);
    }

    public EndEvent(String id, String errorRef) {
        this(id, errorRef, null);
    }

    public EndEvent(String id, String errorRef, String causeExpression) {
        super(id);
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
        return "EndEvent (" + getId() + ") {" +
                "errorRef='" + errorRef + '\'' +
                ", causeExpression='" + causeExpression + '\'' +
                '}';
    }
}
