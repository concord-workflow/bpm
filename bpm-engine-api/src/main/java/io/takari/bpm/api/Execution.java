package io.takari.bpm.api;

import java.io.Serializable;
import java.util.UUID;

/**
 * Instance of the process execution.
 */
public interface Execution extends Serializable {

    /**
     * The ID of process instance. 
     */
    UUID getId();

    /**
     * The business key of this process instance.
     */
    String getBusinessKey();

    /**
     * Indicates when the execution is done (no more process steps to run).
     */
    boolean isDone();
    
    /**
     * Indicates when the execution is suspended (waiting for an event).
     */
    boolean isSuspended();
}
