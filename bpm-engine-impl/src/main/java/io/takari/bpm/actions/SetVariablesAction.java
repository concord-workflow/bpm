package io.takari.bpm.actions;

import io.takari.bpm.api.Variables;
import io.takari.bpm.misc.CoverageIgnore;

public class SetVariablesAction implements Action {

    private static final long serialVersionUID = 1L;

    private final Variables variables;

    public SetVariablesAction(Variables variables) {
        this.variables = variables;
    }

    public Variables getVariables() {
        return variables;
    }

    @Override
    @CoverageIgnore
    public String toString() {
        return "SetVariablesAction [variables=" + variables + "]";
    }
}
