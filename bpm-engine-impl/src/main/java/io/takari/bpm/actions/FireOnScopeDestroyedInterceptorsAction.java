package io.takari.bpm.actions;

import java.util.UUID;

public class FireOnScopeDestroyedInterceptorsAction implements Action {

    private final UUID scopeId;

    public FireOnScopeDestroyedInterceptorsAction(UUID scopeId) {
        this.scopeId = scopeId;
    }

    public UUID getScopeId() {
        return scopeId;
    }

    @Override
    public String toString() {
        return "FireOnScopeDestroyedInterceptorsAction[" +
                "scopeId=" + scopeId +
                ']';
    }
}
