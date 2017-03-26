package io.takari.bpm.commands;

import io.takari.bpm.actions.Action;
import io.takari.bpm.misc.CoverageIgnore;

import java.util.Collections;
import java.util.List;

public class PerformActionsCommand implements Command {

    private static final long serialVersionUID = 1L;

    private final List<Action> actions;

    public PerformActionsCommand(Action action) {
        this(Collections.singletonList(action));
    }

    public PerformActionsCommand(List<Action> actions) {
        this.actions = actions;
    }

    public List<Action> getActions() {
        return actions;
    }

    @Override
    @CoverageIgnore
    public String toString() {
        return "PerformActionsCommand [actions=" + actions + "]";
    }
}
