package io.takari.bpm.state;

import java.io.Serializable;
import java.util.Map;

import org.organicdesign.fp.collections.ImMap;
import org.organicdesign.fp.collections.PersistentHashMap;

/**
 * Process-level variables.
 */
public class Variables implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Variables parent;
    private final ImMap<String, Serializable> values;

    public Variables() {
        this(null);
    }

    public Variables(Variables parent) {
        this.parent = parent;
        this.values = PersistentHashMap.empty();
    }

    private Variables(Variables parent, ImMap<String, Serializable> values) {
        this.parent = parent;
        this.values = values;
    }

    public Variables getParent() {
        return parent;
    }

    public Variables setVariable(String key, Serializable value) {
        return new Variables(parent, values.assoc(key, value));
    }

    public Serializable getVariable(String key) {
        if (values.containsKey(key)) {
            return values.get(key);
        }
        return parent != null ? parent.getVariable(key) : null;
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

    public Map<String, Serializable> asMap() {
        return values;
    }
}
