package io.takari.bpm.actions;

import io.takari.bpm.misc.CoverageIgnore;

import java.util.UUID;

public class RemoveInstanceAction implements Action {

    private static final long serialVersionUID = 1L;

    private final UUID instanceId;

    public RemoveInstanceAction(UUID instanceId) {
        this.instanceId = instanceId;
    }

    public UUID getInstanceId() {
        return instanceId;
    }

    @Override
    @CoverageIgnore
    public String toString() {
        return "RemoveInstanceAction [instanceId=" + instanceId + "]";
    }
}
