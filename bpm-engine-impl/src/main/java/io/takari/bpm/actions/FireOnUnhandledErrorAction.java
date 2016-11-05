package io.takari.bpm.actions;

import io.takari.bpm.api.BpmnError;

public class FireOnUnhandledErrorAction implements Action {

    private static final long serialVersionUID = 1L;

    private final BpmnError error;

    public FireOnUnhandledErrorAction(BpmnError error) {
        this.error = error;
    }

    public BpmnError getError() {
        return error;
    }

    @Override
    public String toString() {
        return "FireOnUnhandledErrorAction[" +
                "error=" + error +
                ']';
    }
}
