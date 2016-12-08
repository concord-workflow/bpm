package io.takari.bpm.actions;

import io.takari.bpm.misc.CoverageIgnore;

public class FireOnFailureInterceptorsAction implements Action {

    private static final long serialVersionUID = 1L;

    private final String errorRef;

    public FireOnFailureInterceptorsAction(String errorRef) {
        this.errorRef = errorRef;
    }

    public String getErrorRef() {
        return errorRef;
    }

    @Override
    @CoverageIgnore
    public String toString() {
        return "FireOnFailureInterceptorsAction [errorRef=" + errorRef + "]";
    }
}
