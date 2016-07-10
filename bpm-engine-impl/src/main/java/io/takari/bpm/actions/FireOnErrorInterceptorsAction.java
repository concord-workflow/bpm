package io.takari.bpm.actions;

public class FireOnErrorInterceptorsAction implements Action {

    private static final long serialVersionUID = 1L;

    private final Throwable cause;

    public FireOnErrorInterceptorsAction(Throwable cause) {
        this.cause = cause;
    }

    public Throwable getCause() {
        return cause;
    }

    @Override
    public String toString() {
        return "FireOnSuspendIterceptorsAction []";
    }
}
