package io.takari.bpm.state;

import com.github.andrewoma.dexx.collection.HashMap;
import com.github.andrewoma.dexx.collection.Map;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Process-level variables.
 */
public class Variables implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Variables parent;
    private final Map<String, Object> values;

    public Variables() {
        this(null);
    }

    public Variables(Variables parent) {
        this.parent = parent;
        this.values = HashMap.empty();
    }

    private Variables(Variables parent, Map<String, Object> values) {
        this.parent = parent;
        this.values = values;
    }

    public Variables getParent() {
        return parent;
    }

    public Variables setVariable(String key, Object value) {
        return new Variables(parent, values.put(key, value));
    }

    public Object getVariable(String key) {
        if (values.containsKey(key)) {
            return values.get(key);
        }
        return parent != null ? parent.getVariable(key) : null;
    }

    public Set<String> getVariableNames() {
        Set<String> result = new HashSet<>();
        for (String s : values.keys()) {
            result.add(s);
        }
        return result;
    }

    public boolean hasVariable(String key) {
        return values.containsKey(key) || (parent != null && parent.hasVariable(key));
    }

    public Variables removeVariable(String key) {
        if (values.containsKey(key)) {
            return new Variables(parent, values.remove(key));
        }

        if (parent == null) {
            return this;
        }

        return new Variables(parent.removeVariable(key), values);
    }

    public java.util.Map<String, Object> asMap() {
        return values.asMap();
    }
}
