package io.takari.bpm.state;

import java.io.Serializable;
import java.util.Objects;

import org.organicdesign.fp.collections.ImMap;
import org.organicdesign.fp.collections.PersistentHashMap;

public class Activations implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ImMap<ActivationKey, Integer> values;

    public Activations() {
        this.values = PersistentHashMap.empty();
    }

    private Activations(ImMap<ActivationKey, Integer> values) {
        this.values = values;
    }

    public Activations inc(String definitionId, String elementId, int count) {
        ActivationKey k = new ActivationKey(definitionId, elementId);

        Integer i = values.get(k);
        if (i == null) {
            i = 0;
        }
        i = i + count;

        return new Activations(values.assoc(k, i));
    }
    
    public int count(String definitionId, String elementId) {
        ActivationKey k = new ActivationKey(definitionId, elementId);
        Integer i = values.get(k);
        return i != null ? i : 0;
    }

    private static final class ActivationKey implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String definitionId;
        private final String elementId;

        public ActivationKey(String definitionId, String elementId) {
            this.definitionId = definitionId;
            this.elementId = elementId;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 79 * hash + Objects.hashCode(this.definitionId);
            hash = 79 * hash + Objects.hashCode(this.elementId);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ActivationKey other = (ActivationKey) obj;
            if (!Objects.equals(this.definitionId, other.definitionId)) {
                return false;
            }
            return Objects.equals(this.elementId, other.elementId);
        }

        @Override
        public String toString() {
            return "ActivationKey [definitionId=" + definitionId + ", elementId=" + elementId + "]";
        }
    }
}
