package io.takari.bpm.state;

import org.pcollections.HashTreePMap;
import org.pcollections.PMap;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Process-level variables.
 */
public class Variables implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Variables parent;
    private final PMap<String, Object> values;

    public Variables() {
        this((Variables) null);
    }

    public Variables(Variables parent) {
        this.parent = parent;
        this.values = HashTreePMap.empty();
    }

    public Variables(Map<String, Object> values) {
        this.parent = null;
        this.values = HashTreePMap.from(values);
    }

    private Variables(Variables parent, PMap<String, Object> values) {
        this.parent = parent;
        this.values = values;
    }

    public Variables getParent() {
        return parent;
    }

    public Variables setVariable(String key, Object value) {
        return new Variables(parent, values.plus(key, value));
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
            return new Variables(parent, values.minus(key));
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
