package io.takari.bpm.utils;

import org.pcollections.ConsPStack;
import org.pcollections.PStack;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;

public class PersistentStack<T extends Serializable> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final PStack<T> stack;

    public PersistentStack(){
        this.stack = ConsPStack.empty();
    }

    private PersistentStack(PStack<T> stack) {
        this.stack = stack;
    }

    public PersistentStack<T> push(T el) {
        return new PersistentStack<T>(stack.plus(el));
    }

    public T peek() {
        return stack.isEmpty() ? null : stack.get(0);
    }

    public PersistentStack<T> pop() {
        if (stack.isEmpty()) {
            return this;
        }

        return new PersistentStack<T>(stack.minus(0));
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }
}
