package io.takari.bpm.commands;

import io.takari.bpm.api.Variables;
import io.takari.bpm.misc.CoverageIgnore;
import io.takari.bpm.model.VariableMapping;

import java.util.Set;

public class MergeVariablesCommand implements Command {

    private static final long serialVersionUID = 1L;

    private final Variables target;
    private final Set<VariableMapping> outVariables;
    private final boolean copyAllVariables;

    public MergeVariablesCommand(Variables target, Set<VariableMapping> outVariables) {
        this(target, outVariables, false);
    }

    public MergeVariablesCommand(Variables target, Set<VariableMapping> outVariables, boolean copyAllVariables) {
        this.target = target;
        this.outVariables = outVariables;
        this.copyAllVariables = copyAllVariables;
    }

    public Variables getTarget() {
        return target;
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
        return "MergeVariablesCommand [" +
                "target=" + target +
                ", outVariables=" + outVariables +
                ", copyAllVariables=" + copyAllVariables +
                ']';
    }
}
