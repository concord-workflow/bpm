package io.takari.bpm.actions;

import io.takari.bpm.commands.Command;

public class PushCommandAction implements Action {

    private static final long serialVersionUID = 1L;

    private final Command command;

    public PushCommandAction(Command command) {
        this.command = command;
    }

    public Command getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return "PushCommandAction [command=" + command + "]";
    }
}
