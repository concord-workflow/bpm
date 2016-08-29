package io.takari.bpm.actions;

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
    public String toString() {
        return "FireOnFailureInterceptorsAction [errorRef=" + errorRef + "]";
    }
}
