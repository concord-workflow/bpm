package io.takari.bpm.api;

public class NoEventFoundException extends ExecutionException {
	
	private static final long serialVersionUID = 1L;

    public NoEventFoundException(String format, Object... args) {
        super(format, args);
    }
}
