package io.takari.bpm.context;

import java.io.Serializable;

public final class Change implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ChangeType type;
    private final Object value;

    public Change(ChangeType type, Object value) {
        this.type = type;
        this.value = value;
    }

    public ChangeType getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }
}