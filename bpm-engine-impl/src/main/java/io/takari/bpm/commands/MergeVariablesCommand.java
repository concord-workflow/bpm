package io.takari.bpm.commands;

import java.util.Set;

import io.takari.bpm.misc.CoverageIgnore;
import io.takari.bpm.model.VariableMapping;
import io.takari.bpm.state.Variables;

public class MergeVariablesCommand implements Command {

    private static final long serialVersionUID = 1L;

    private final Variables target;
    private final Set<VariableMapping> outVariables;

    public MergeVariablesCommand(Variables target, Set<VariableMapping> outVariables) {
        this.target = target;
        this.outVariables = outVariables;
    }

    public Variables getTarget() {
        return target;
    }

    public Set<VariableMapping> getOutVariables() {
        return outVariables;
    }

    @Override
    @CoverageIgnore
    public String toString() {
        return "MergeVariablesCommand [target=" + target + ", outVariables=" + outVariables + "]";
    }
}
