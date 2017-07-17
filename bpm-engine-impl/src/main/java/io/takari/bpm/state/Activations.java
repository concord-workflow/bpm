package io.takari.bpm.state;

import org.pcollections.HashTreePMap;
import org.pcollections.PMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

public class Activations implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(Activations.class);

    private static final long serialVersionUID = 1L;

    private final PMap<ActivationKey, Activation> values;

    public Activations() {
        this.values = HashTreePMap.empty();
    }

    private Activations(PMap<ActivationKey, Activation> values) {
        this.values = values;
    }

    public Activations incExpectation(Scopes scopes, UUID scope, String elementId, int count) {
        ActivationKey k = new ActivationKey(scope, elementId);
        Activation v = Activation.notEmpty(values.get(k), scope + ":" + elementId);
        log.debug("Incrementing {} ({}/{}+{})", v.id, v.getReceived(), v.getExpected(), count);
        return new Activations(values.plus(k, v.incExpected(count)));
    }

    public Activations inc(Scopes scopes, UUID scope, String elementId, int count) {
        while (scope != null) {
            ActivationKey k = new ActivationKey(scope, elementId);
            Activation v = values.get(k);
            if (v != null) {
                log.debug("Incrementing {} ({}+{}/{})", v.id, v.getReceived(), count, v.getExpected());
                return new Activations(values.plus(k, v.incReceived(count)));
            }
            scope = scopes.values().get(scope).getParentId();
        }
        throw new IllegalStateException("Cannot find activationKey for " + scope + ":" + elementId);
    }

    public Activation getActivation(Scopes scopes, UUID scope, String elementId) {
        while (scope != null) {
            ActivationKey k = new ActivationKey(scope, elementId);
            Activation v = values.get(k);
            if (v != null) {
                return v;
            }
            scope = scopes.values().get(scope).getParentId();
        }
        return Activation.notEmpty(null, scope + ":" + elementId);
    }

    public Map<ActivationKey, Activation> values() {
        return values;
    }

    public static final class Activation implements Serializable {

        private static final long serialVersionUID = 1L;

        public static final Activation notEmpty(Activation act, String id) {
            if (act == null) {
                act = new Activation(id, 0, 0, false);
            }
            return act;
        }

        private final String id;
        private final int expected;
        private final int received;
        private final boolean activated;

        Activation(String id, int expected, int received, boolean activated) {
            this.id = id;
            this.expected = expected;
            this.received = received;
            this.activated = activated;
        }

        public boolean isActivated() {
            return activated;
        }

        public int getExpected() {
            return expected;
        }

        public int getReceived() {
            return received;
        }

        public Activation incExpected(int count) {
            return new Activation(id, expected + count, received, true);
        }

        public Activation incReceived(int count) {
            if (!activated) {
                throw new IllegalStateException("Incrementing " + id + " 'received' but the flow hasn't been activated yet");
            }
            if (received + count > expected) {
                throw new IllegalStateException("Incrementing " + id + " 'received' to " + received + " + " + count + " but the flow expects " + expected + " max");
            }
            return new Activation(id, expected, received + count, true);
        }

        public String toString() {
            return "[" + id + "] " + received + "/" + expected;
        }
    }

    public static final class ActivationKey implements Serializable {

        private static final long serialVersionUID = 1L;

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
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            ActivationKey that = (ActivationKey) o;

            if (!scopeId.equals(that.scopeId))
                return false;
            return elementId.equals(that.elementId);
        }

        @Override
        public int hashCode() {
            int result = scopeId.hashCode();
            result = 31 * result + elementId.hashCode();
            return result;
        }
    }
}
