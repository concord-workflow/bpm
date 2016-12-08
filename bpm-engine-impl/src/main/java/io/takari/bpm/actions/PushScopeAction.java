package io.takari.bpm.actions;

import io.takari.bpm.commands.Command;
import io.takari.bpm.misc.CoverageIgnore;

import java.util.Arrays;

public class PushScopeAction implements Action {

    private final String definitionId;
    private final String elementId;
    private final boolean exclusive;
    private final Command[] finishers;

    public PushScopeAction(String definitionId, String elementId, boolean exclusive) {
        this(definitionId, elementId, exclusive, (Command[]) null);
    }

    public PushScopeAction(String definitionId, String elementId, boolean exclusive, Command... finishers) {
        this.definitionId = definitionId;
        this.elementId = elementId;
        this.exclusive = exclusive;
        this.finishers = finishers;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public String getElementId() {
        return elementId;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public Command[] getFinishers() {
        return finishers;
    }

    @Override
    @CoverageIgnore
    public String toString() {
        return "PushScopeAction [" +
                "definitionId=" + definitionId +
                ", elementId=" + elementId +
                ", exclusive=" + exclusive +
                ", finishers=" + Arrays.toString(finishers) +
                ']';
    }
}
