package io.takari.bpm.actions;

import io.takari.bpm.misc.CoverageIgnore;

public class UnsetVariableAction implements Action {

    private static final long serialVersionUID = 1L;

    private final String key;

    public UnsetVariableAction(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    @Override
    @CoverageIgnore
    public String toString() {
        return "UnsetVariableAction [key=" + key + "]";
    }
}
