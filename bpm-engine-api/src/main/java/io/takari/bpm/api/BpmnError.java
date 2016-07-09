package io.takari.bpm.api;

/**
 * BPMN error, wrapped in an exception.
 */
public class BpmnError extends RuntimeException {
	
	private static final long serialVersionUID = 1L;

    private final String errorRef;

    public BpmnError(String errorRef) {
        this.errorRef = errorRef;
    }

    public String getErrorRef() {
        return errorRef;
    }
}
