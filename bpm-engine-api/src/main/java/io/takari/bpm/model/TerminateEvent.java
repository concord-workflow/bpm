package io.takari.bpm.model;

public class TerminateEvent extends AbstractElement {

    private static final long serialVersionUID = 1L;

    public TerminateEvent(String id) {
        super(id);
    }

    @Override
    public String toString() {
        return "TerminateEvent (" + getId() + ")";
    }
}
