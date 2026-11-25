package io.takari.bpm.persistence;

import io.takari.bpm.api.ExecutionException;

import java.util.UUID;

/**
 * The process state was modified concurrently (e.g. by another engine instance) after it was loaded.
 * The changes of the current run are discarded, the run can be retried.
 */
public class StaleStateException extends ExecutionException {

    private static final long serialVersionUID = 1L;

    public StaleStateException(UUID instanceId, long expectedVersion, long actualVersion) {
        super("Process instance '%s' was modified concurrently: expected version %d, got %d",
                instanceId, expectedVersion, actualVersion);
    }
}
