package io.takari.bpm.commands;

import java.io.Serializable;

import io.takari.bpm.utils.PersistentStack;

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

    public boolean isEmpty() {
        return stack.isEmpty();
    }
}
