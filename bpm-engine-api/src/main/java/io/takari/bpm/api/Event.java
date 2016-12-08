package io.takari.bpm.api;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public interface Event extends Serializable {

    /**
     * @return Unique ID of the event.
     */
    UUID getId();

    /**
     * @return Name of the event. Can be non-unique.
     */
    String getName();

    /**
     * @return ID of the execution (process instance).
     */
    UUID getExecutionId();

    /**
     * @return external business key of the execution.
     */
    String getProcessBusinessKey();

    /**
     * @return ID of the event's process definition.
     */
    String getDefinitionId();

    /**
     * @return ID of the event's scope.
     */
    UUID getScopeId();

    /**
     * @return {@code true} if the event is unique in its scope.
     * Only one unique event per scope can be activated.
     * @see io.takari.bpm.model.EventBasedGateway
     * @see io.takari.bpm.model.ExclusiveGateway
     */
    boolean isExclusive();

    /**
     * @return expiration date of the event. Can be null.
     */
    Date getExpiredAt();
}
