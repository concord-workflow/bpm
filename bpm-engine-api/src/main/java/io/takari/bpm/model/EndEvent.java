package io.takari.bpm.model;

public class EndEvent extends AbstractElement {

    private static final long serialVersionUID = 1L;

    private final String errorRef;
    private final String causeVariable;

    public EndEvent(String id) {
        this(id, null);
    }

    public EndEvent(String id, String errorRef) {
        this(id, errorRef, null);
    }

    public EndEvent(String id, String errorRef, String causeVariable) {
        super(id);
        this.errorRef = errorRef;
        this.causeVariable = causeVariable;
    }

    public String getErrorRef() {
        return errorRef;
    }

    public String getCauseVariable() {
        return causeVariable;
    }
}
