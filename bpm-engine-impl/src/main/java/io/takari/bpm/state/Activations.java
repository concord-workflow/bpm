package io.takari.bpm.state;

import org.pcollections.HashTreePMap;
import org.pcollections.PMap;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

public class Activations implements Serializable {

    private static final long serialVersionUID = 1L;

    private final PMap<ActivationKey, Integer> values;

    public Activations() {
        this.values = HashTreePMap.empty();
    }

    public Activations(PMap<ActivationKey, Integer> values) {
        this.values = values;
    }

    public Activations inc(UUID scopeId, String elementId, int count) {
        ActivationKey k = new ActivationKey(scopeId, elementId);
        Integer v = values.get(k);
        if (v == null) {
            v = 0;
        }
        return new Activations(values.plus(k, v + count));
    }

    public int count(UUID scopeId, String elementId) {
        ActivationKey k = new ActivationKey(scopeId, elementId);
        return values.getOrDefault(k, 0);
    }

    public Map<ActivationKey, Integer> values() {
        return values;
    }

    public static final class ActivationKey implements Serializable {

        private final UUID scopeId;
        private final String elementId;

        public ActivationKey(UUID scopeId, String elementId) {
            this.scopeId = scopeId;
            this.elementId = elementId;
        }

        public UUID getScopeId() {
            return scopeId;
        }

        public String getElementId() {
            return elementId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ActivationKey that = (ActivationKey) o;

            if (!scopeId.equals(that.scopeId)) return false;
            return elementId.equals(that.elementId);
        }

        @Override
        public int hashCode() {
            int result = scopeId.hashCode();
            result = 31 * result + elementId.hashCode();
            return result;
        }
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
