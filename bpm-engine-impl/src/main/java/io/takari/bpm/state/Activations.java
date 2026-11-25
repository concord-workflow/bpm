package io.takari.bpm.state;

import io.takari.bpm.state.Scopes.Scope;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

    /**
     * Returns the ID of the "container" scope for the specified scope. Join activations are tracked per container:
     * the root process, a subprocess or a call activity. Scopes created by forks and event-based gateways are
     * transparent: they share the join counters of their container. That makes the counters independent of the
     * order in which parallel branches (and their nested forks) are executed.
     */
    public static UUID containerOf(Scopes scopes, UUID scopeId) {
        UUID id = scopeId;
        while (id != null) {
            Scope s = scopes.values().get(id);
            if (s == null) {
                return scopeId;
            }
            if (s.getFinishers() != null || s.getParentId() == null) {
                return id;
            }
            id = s.getParentId();
        }
        return scopeId;
    }

    public Activations incExpectation(Scopes scopes, UUID scope, String elementId, int count) {
        ActivationKey k = find(scopes, scope, elementId);
        if (k == null) {
            k = new ActivationKey(containerOf(scopes, scope), elementId);
        }
        Activation v = Activation.notEmpty(values.get(k), k.scopeId + ":" + elementId);
        log.debug("Incrementing {} ({}/{}+{})", v.id, v.getReceived(), v.getExpected(), count);
        return new Activations(values.plus(k, v.incExpected(count)));
    }

    /**
     * Registers an arrival of a token. An arrival that wasn't predicted (no expectation or no expectation left)
     * is registered as a new expected activation, so the join waits for the remaining flows instead of failing.
     */
    public Activations inc(Scopes scopes, UUID scope, String elementId, int count) {
        ActivationKey k = find(scopes, scope, elementId);
        if (k == null) {
            k = new ActivationKey(containerOf(scopes, scope), elementId);
        }

        Activation v = values.get(k);
        if (v == null || !v.isActivated() || v.getReceived() + count > v.getExpected()) {
            Activation base = Activation.notEmpty(v, k.scopeId + ":" + elementId);
            log.debug("Unexpected activation of {} ({}+{}/{}), adjusting the expectation", base.id, base.getReceived(), count, base.getExpected());
            int received = base.getReceived() + count;
            return new Activations(values.plus(k, new Activation(base.id, received, received, true)));
        }

        log.debug("Incrementing {} ({}+{}/{})", v.id, v.getReceived(), count, v.getExpected());
        return new Activations(values.plus(k, v.incReceived(count)));
    }

    public Activation getActivation(Scopes scopes, UUID scope, String elementId) {
        ActivationKey k = find(scopes, scope, elementId);
        if (k == null) {
            return Activation.notEmpty(null, scope + ":" + elementId);
        }
        return values.get(k);
    }

    /**
     * Removes the activations of the specified flows (e.g. after a join consumed its tokens).
     */
    public Activations consume(Scopes scopes, UUID scope, Collection<String> elementIds) {
        PMap<ActivationKey, Activation> m = values;
        for (String elementId : elementIds) {
            ActivationKey k = find(scopes, scope, elementId);
            if (k != null) {
                m = m.minus(k);
            }
        }
        return new Activations(m);
    }

    /**
     * @return activations of the specified container scope that have received tokens. Such activations
     * represent tokens that are waiting in joins.
     */
    public List<Activation> findWaiting(UUID containerId) {
        List<Activation> result = new ArrayList<>();
        for (Map.Entry<ActivationKey, Activation> e : values.entrySet()) {
            if (e.getKey().getScopeId().equals(containerId) && e.getValue().getReceived() > 0) {
                result.add(e.getValue());
            }
        }
        return result;
    }

    /**
     * Removes all activations of the specified scope.
     */
    public Activations removeScope(UUID scopeId) {
        PMap<ActivationKey, Activation> m = values;
        for (ActivationKey k : values.keySet()) {
            if (k.getScopeId().equals(scopeId)) {
                m = m.minus(k);
            }
        }
        return m == values ? this : new Activations(m);
    }

    /**
     * Finds an existing activation key. Looks into the container scope first and then (for compatibility
     * with the states created by the previous versions) into the chain of the parent scopes.
     */
    private ActivationKey find(Scopes scopes, UUID scope, String elementId) {
        if (scope == null) {
            return null;
        }

        UUID containerId = containerOf(scopes, scope);
        ActivationKey k = new ActivationKey(containerId, elementId);
        if (values.containsKey(k)) {
            return k;
        }

        // legacy states: activations were stored in the fork scopes
        UUID id = scope;
        while (id != null && !id.equals(containerId)) {
            k = new ActivationKey(id, elementId);
            if (values.containsKey(k)) {
                return k;
            }
            Scope s = scopes.values().get(id);
            id = s != null ? s.getParentId() : null;
        }

        return null;
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

        public String getId() {
            return id;
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
