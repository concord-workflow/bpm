package io.takari.bpm.utils;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;

public class PersistentStack<T extends Serializable> implements Serializable {

    private static final long serialVersionUID = 1L;

    // TODO optimize
    private final Deque<T> stack;

    public PersistentStack() {
        this.stack = new ArrayDeque<>();
    }

    private PersistentStack(Deque<T> stack) {
        this.stack = stack;
    }

    public PersistentStack<T> push(T el) {
        Deque<T> n = new ArrayDeque<>(stack);
        n.push(el);

        return new PersistentStack<T>(n);
    }

    public T peek() {
        return stack.peek();
    }

    public PersistentStack<T> pop() {
        if (stack.isEmpty()) {
            return this;
        }

        Deque<T> n = new ArrayDeque<>(stack);
        T el = n.pop();

        return new PersistentStack<T>(n);
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }
}
