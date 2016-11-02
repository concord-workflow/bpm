package io.takari.bpm.api;

/**
 * BPMN error, wrapped in an exception.
 */
public class BpmnError extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String definitionId;
    private final String elementId;
    private final String errorRef;
    private final Throwable cause;

    public BpmnError(String errorRef) {
        this(null, null, errorRef, null);
    }

    public BpmnError(String errorRef, Throwable cause) {
        this(null, null, errorRef, cause);
    }

    public BpmnError(String definitionId, String elementId, String errorRef) {
        this(definitionId, elementId, errorRef, null);
    }

    public BpmnError(String definitionId, String elementId, String errorRef, Throwable cause) {
        super(String.format("Error at %s/%s: %s", definitionId, elementId, errorRef));
        this.definitionId = definitionId;
        this.elementId = elementId;
        this.errorRef = errorRef;
        this.cause = cause;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public String getElementId() {
        return elementId;
    }

    public String getErrorRef() {
        return errorRef;
    }

    @Override
    public Throwable getCause() {
        return cause;
    }
}
