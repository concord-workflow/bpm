package io.takari.bpm.actions;

import java.io.Serializable;

public class SetVariableAction implements Action {

    private static final long serialVersionUID = 1L;

    private final String key;
    private final Object value;

    public SetVariableAction(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "SetVariableAction [key=" + key + ", value=" + value + "]";
    }
}
