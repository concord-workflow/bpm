package io.takari.bpm.state;

import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.io.Serializable;
import java.util.Objects;

public class Activations implements Serializable {

    private static final long serialVersionUID = 1L;

    private final PVector<Activation> values;

    public Activations() {
        this.values = TreePVector.empty();
    }

    public Activations(PVector<Activation> values) {
        this.values = values;
    }

    public Activations inc(String definitionId, String elementId, int count) {
        return new Activations(values.plus(new Activation(definitionId, elementId, count)));
    }

    public int count(String definitionId, String elementId) {
        int cnt = 0;
        for (Activation a : values) {
            if (definitionId.equals(a.definitionId) && elementId.equals(a.elementId)) {
                cnt += a.count;
            }
        }
        return cnt;
    }

    private static final class Activation implements Serializable {

        private final String definitionId;
        private final String elementId;
        private final int count;

        public Activation(String definitionId, String elementId, int count) {
            this.definitionId = definitionId;
            this.elementId = elementId;
            this.count = count;
        }
    }
}
