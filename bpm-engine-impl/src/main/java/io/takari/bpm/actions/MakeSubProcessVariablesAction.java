package io.takari.bpm.actions;

import java.util.Set;

import io.takari.bpm.model.VariableMapping;

public class MakeSubProcessVariablesAction implements Action {

    private static final long serialVersionUID = 1L;

    private final Set<VariableMapping> inVariables;

    public MakeSubProcessVariablesAction(Set<VariableMapping> inVariables) {
        this.inVariables = inVariables;
    }

    public Set<VariableMapping> getInVariables() {
        return inVariables;
    }

    @Override
    public String toString() {
        return "MakeSubProcessVariablesAction [inVariables=" + inVariables + "]";
    }
}
