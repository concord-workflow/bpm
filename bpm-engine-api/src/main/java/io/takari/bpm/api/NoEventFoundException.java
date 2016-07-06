package io.takari.bpm.api;

public class NoEventFoundException extends ExecutionException {

    public NoEventFoundException(String format, Object... args) {
        super(format, args);
    }
}
