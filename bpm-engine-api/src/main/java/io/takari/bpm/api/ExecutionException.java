package io.takari.bpm.api;

/**
 * Generic execution exception.
 */
public class ExecutionException extends Exception {
	
	private static final long serialVersionUID = 1L;

    public ExecutionException(String message) {
        super(message);
    }
    
    public ExecutionException(String format, Object ... args) {
        super(String.format(format, args));
    }

    public ExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
