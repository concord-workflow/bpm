package io.takari.bpm.actions;

public class FireOnStartInterceptorsAction implements Action {

    private static final long serialVersionUID = 1L;

    private final String definitionId;

    public FireOnStartInterceptorsAction(String definitionId) {
        this.definitionId = definitionId;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    @Override
    public String toString() {
        return "FireOnStartInterceptorsAction [definitionId=" + definitionId + "]";
    }
}
