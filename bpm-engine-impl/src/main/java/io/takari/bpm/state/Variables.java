package io.takari.bpm.state;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.organicdesign.fp.collections.ImMap;
import org.organicdesign.fp.collections.PersistentHashMap;

/**
 * Process-level variables.
 */
public class Variables implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Variables parent;
    private final ImMap<String, Object> values;

    public Variables() {
        this(null);
    }

    public Variables(Variables parent) {
        this.parent = parent;
        this.values = PersistentHashMap.empty();
    }

    private Variables(Variables parent, ImMap<String, Object> values) {
        this.parent = parent;
        this.values = values;
    }

    public Variables getParent() {
        return parent;
    }

    public Variables setVariable(String key, Object value) {
        return new Variables(parent, values.assoc(key, value));
    }

    public Object getVariable(String key) {
        if (values.containsKey(key)) {
            return values.get(key);
        }
        return parent != null ? parent.getVariable(key) : null;
    }

    public Set<String> getVariableNames() {
        return values.keySet();
    }

    public boolean hasVariable(String key) {
        return values.containsKey(key) || (parent != null && parent.hasVariable(key));
    }

    public Variables removeVariable(String key) {
        if (values.containsKey(key)) {
            return new Variables(parent, values.without(key));
        }

        if (parent == null) {
            return this;
        }

        return new Variables(parent.removeVariable(key), values);
    }

    public Map<String, Object> asMap() {
        return values;
    }
}
