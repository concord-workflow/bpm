package io.takari.bpm.actions;

import java.util.Set;

import io.takari.bpm.model.VariableMapping;

public class MakeSubProcessVariablesAction implements Action {

    private static final long serialVersionUID = 1L;

    private final Set<VariableMapping> inVariables;
    private final boolean copyAllVariables;

    public MakeSubProcessVariablesAction(Set<VariableMapping> inVariables, boolean copyAllVariables) {
        this.inVariables = inVariables;
        this.copyAllVariables = copyAllVariables;
    }

    public Set<VariableMapping> getInVariables() {
        return inVariables;
    }

    public boolean isCopyAllVariables() {
        return copyAllVariables;
    }

    @Override
    public String toString() {
        return "MakeSubProcessVariablesAction [inVariables=" + inVariables + ", copyAllVariables=" + copyAllVariables + "]";
    }
}
