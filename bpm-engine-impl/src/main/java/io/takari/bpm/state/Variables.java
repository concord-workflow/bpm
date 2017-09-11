package io.takari.bpm.state;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Process-level variables.
 */
public class Variables implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Variables parent;
    private final LinkedHashMap<String, Object> values;

    public Variables() {
        this((Variables) null);
    }

    public Variables(Variables parent) {
        this.parent = parent;
        this.values = new LinkedHashMap<>();
    }

    public Variables(Map<String, Object> values) {
        this.parent = null;
        this.values = new LinkedHashMap<>(values);
    }

    private Variables(Variables parent, LinkedHashMap<String, Object> values) {
        this.parent = parent;
        this.values = values;
    }

    public Variables getParent() {
        return parent;
    }

    public Variables setVariable(String key, Object value) {
        LinkedHashMap<String, Object> next = new LinkedHashMap<>(values);
        next.put(key, value);
        return new Variables(parent, next);
    }

    public Variables setVariables(Map<String, Object> m) {
        LinkedHashMap<String, Object> next = new LinkedHashMap<>(values);
        next.putAll(m);
        return new Variables(parent, next);
    }

    public Object getVariable(String key) {
        if (values.containsKey(key)) {
            return values.get(key);
        }
        return parent != null ? parent.getVariable(key) : null;
    }

    public Set<String> getVariableNames() {
        Set<String> result = new HashSet<>();
        result.addAll(values.keySet());
        return result;
    }

    public boolean hasVariable(String key) {
        return values.containsKey(key) || (parent != null && parent.hasVariable(key));
    }

    public Variables removeVariable(String key) {
        if (values.containsKey(key)) {
            LinkedHashMap<String, Object> next = new LinkedHashMap<>(values);
            next.remove(key);
            return new Variables(parent, next);
        }

        if (parent == null) {
            return this;
        }

        return new Variables(parent.removeVariable(key), values);
    }

    public java.util.Map<String, Object> asMap() {
        return values;
    }
}
