package io.takari.bpm.actions;

import io.takari.bpm.misc.CoverageIgnore;
import io.takari.bpm.model.VariableMapping;
import io.takari.bpm.state.Variables;

import java.util.Set;

public class MergeVariablesAction implements Action {

    private static final long serialVersionUID = 1L;

    private final Variables source;
    private final Set<VariableMapping> outVariables;
    private final boolean copyAllVariables;

    public MergeVariablesAction(Variables source, Set<VariableMapping> outVariables, boolean copyAllVariables) {
        this.source = source;
        this.outVariables = outVariables;
        this.copyAllVariables = copyAllVariables;
    }

    public Variables getSource() {
        return source;
    }

    public Set<VariableMapping> getOutVariables() {
        return outVariables;
    }

    public boolean isCopyAllVariables() {
        return copyAllVariables;
    }

    @Override
    @CoverageIgnore
    public String toString() {
        return "MergeVariablesAction [" +
                "source=" + source +
                ", outVariables=" + outVariables +
                ", copyAllVariables=" + copyAllVariables +
                ']';
    }
}
