package io.takari.bpm.actions;

import io.takari.bpm.misc.CoverageIgnore;

import java.util.UUID;

public class FireOnScopeCreatedInterceptorsAction implements Action {

    private final UUID scopeId;
    private final String definitionId;
    private final String elementId;

    public FireOnScopeCreatedInterceptorsAction(UUID scopeId, String definitionId, String elementId) {
        this.scopeId = scopeId;
        this.definitionId = definitionId;
        this.elementId = elementId;
    }

    public UUID getScopeId() {
        return scopeId;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public String getElementId() {
        return elementId;
    }

    @Override
    @CoverageIgnore
    public String toString() {
        return "FireOnScopeCreatedInterceptorsAction [" +
                "scopeId=" + scopeId +
                ", definitionId=" + definitionId +
                ", elementId=" + elementId +
                ']';
    }
}
