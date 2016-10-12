package io.takari.bpm.actions;

import java.util.Set;

import io.takari.bpm.model.VariableMapping;
import io.takari.bpm.state.Variables;

public class MergeVariablesAction implements Action {

    private static final long serialVersionUID = 1L;

    private final Variables source;
    private final Set<VariableMapping> outVariables;

    public MergeVariablesAction(Variables source, Set<VariableMapping> outVariables) {
        this.source = source;
        this.outVariables = outVariables;
    }

    public Variables getSource() {
        return source;
    }

    public Set<VariableMapping> getOutVariables() {
        return outVariables;
    }

    @Override
    public String toString() {
        return "MergeVariablesAction [source=" + source + ", outVariables=" + outVariables + "]";
    }
}
