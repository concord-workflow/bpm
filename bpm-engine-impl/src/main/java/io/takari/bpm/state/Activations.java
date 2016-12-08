package io.takari.bpm.state;

import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public class Activations implements Serializable {

    private static final long serialVersionUID = 1L;

    private final PVector<Activation> values;

    public Activations() {
        this.values = TreePVector.empty();
    }

    public Activations(PVector<Activation> values) {
        this.values = values;
    }

    public Activations inc(UUID scopeId, String elementId, int count) {
        return new Activations(values.plus(new Activation(scopeId, elementId, count)));
    }

    public int count(UUID scopeId, String elementId) {
        int cnt = 0;
        for (Activation a : values) {
            if (scopeId.equals(a.scopeId) && elementId.equals(a.elementId)) {
                cnt += a.count;
            }
        }
        return cnt;
    }

    public List<Activation> values() {
        return values;
    }

    public static final class Activation implements Serializable {

        private final UUID scopeId;
        private final String elementId;
        private final int count;

        public Activation(UUID scopeId, String elementId, int count) {
            this.scopeId = scopeId;
            this.elementId = elementId;
            this.count = count;
        }

        public UUID getScopeId() {
            return scopeId;
        }

        public String getElementId() {
            return elementId;
        }

        public int getCount() {
            return count;
        }
    }
}
