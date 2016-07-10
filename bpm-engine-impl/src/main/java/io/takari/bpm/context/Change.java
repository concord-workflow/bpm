package io.takari.bpm.context;

import java.io.Serializable;

public final class Change implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ChangeType type;
    private final Serializable value;

    public Change(ChangeType type, Serializable value) {
        this.type = type;
        this.value = value;
    }

    public ChangeType getType() {
        return type;
    }

    public Serializable getValue() {
        return value;
    }
}