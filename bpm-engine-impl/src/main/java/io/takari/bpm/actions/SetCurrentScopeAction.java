package io.takari.bpm.actions;

import io.takari.bpm.misc.CoverageIgnore;

import java.util.UUID;

public class SetCurrentScopeAction implements Action {

    private final UUID scopeId;

    public SetCurrentScopeAction(UUID scopeId) {
        this.scopeId = scopeId;
    }

    public UUID getScopeId() {
        return scopeId;
    }

    @Override
    @CoverageIgnore
    public String toString() {
        return "SetCurrentScopeAction [" +
                "scopeId=" + scopeId +
                ']';
    }
}
