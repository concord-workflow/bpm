package io.takari.bpm.commands;

import io.takari.bpm.utils.PersistentStack;

import java.io.Serializable;
import java.util.Collection;

public class CommandStack implements Serializable {

    private static final long serialVersionUID = 1L;

    private final PersistentStack<Command> stack;

    public CommandStack() {
        this.stack = new PersistentStack<>();
    }

    private CommandStack(PersistentStack<Command> stack) {
        this.stack = stack;
    }

    public CommandStack push(Command cmd) {
        return new CommandStack(stack.push(cmd));
    }

    public Command peek() {
        return stack.peek();
    }

    public CommandStack pop() {
        return new CommandStack(stack.pop());
    }

    public Collection<Command> values() {
        return stack.values();
    }
}
