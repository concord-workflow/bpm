package io.takari.bpm.api;

/**
 * BPMN error, wrapped in an exception.
 */
public class BpmnError extends RuntimeException {
	
	private static final long serialVersionUID = 1L;

    private final String errorRef;
    private final Throwable cause;

    public BpmnError(String errorRef) {
        this(errorRef, null);
    }

    public BpmnError(String errorRef, Throwable cause) {
        this.errorRef = errorRef;
        this.cause = cause;
    }

    public String getErrorRef() {
        return errorRef;
    }

    @Override
    public Throwable getCause() {
        return cause;
    }
}
