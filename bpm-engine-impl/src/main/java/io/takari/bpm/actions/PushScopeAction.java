package io.takari.bpm.actions;

import io.takari.bpm.commands.Command;

import java.util.Arrays;

public class PushScopeAction implements Action {

    private final boolean exclusive;
    private final Command[] finishers;

    public PushScopeAction(boolean exclusive) {
        this(exclusive, (Command[])null);
    }

    public PushScopeAction(boolean exclusive, Command... finishers) {
        this.exclusive = exclusive;
        this.finishers = finishers;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public Command[] getFinishers() {
        return finishers;
    }

    @Override
    public String toString() {
        return "PushScopeAction{" +
                "exclusive=" + exclusive +
                ", finishers=" + Arrays.toString(finishers) +
                '}';
    }
}
