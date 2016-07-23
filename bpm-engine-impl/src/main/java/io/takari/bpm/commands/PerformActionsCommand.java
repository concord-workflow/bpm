package io.takari.bpm.commands;

import java.util.Arrays;
import java.util.List;

import io.takari.bpm.actions.Action;

public class PerformActionsCommand implements Command {

    private static final long serialVersionUID = 1L;

    private final List<Action> actions;

    public PerformActionsCommand(Action action) {
        this(Arrays.asList(action));
    }

    public PerformActionsCommand(List<Action> actions) {
        this.actions = actions;
    }

    public List<Action> getActions() {
        return actions;
    }

    @Override
    public String toString() {
        return "PerformActionsCommand [actions=" + actions + "]";
    }
}
