package io.takari.bpm.state;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

import org.pcollections.HashTreePMap;
import org.pcollections.OrderedPSet;
import org.pcollections.PMap;
import org.pcollections.POrderedSet;

public class Forks implements Serializable {
    private static final long serialVersionUID = 1L;

    private final PMap<ForkKey, Fork> values;

    public Forks() {
        this(HashTreePMap.empty());
    }

    private Forks(PMap<ForkKey, Fork> values) {
        this.values = values;
    }

    public boolean containsFork(UUID scopeId, String elementId) {
        return values.containsKey(new ForkKey(scopeId, elementId));
    }

    public Fork getFork(UUID scopeId, String elementId) {
        return values.get(new ForkKey(scopeId, elementId));
    }

    public Forks removeFork(UUID scopeId, String elementId) {
        return new Forks(values.minus(new ForkKey(scopeId, elementId)));
    }

    public Forks incrementFlow(UUID scopeId, String elementId, String flowId, int diff) {
        ForkKey k = new ForkKey(scopeId, elementId);
        Fork f = values.get(k);
        if (f == null) {
            f = new Fork();
        }
        return new Forks(values.plus(k, f.increment(flowId, diff)));
    }

    public static class Fork implements Serializable {
        private static final long serialVersionUID = 1L;

        private final POrderedSet<String> flows;
        private final PMap<String, Integer> flowCounts;

        public Fork() {
            this(OrderedPSet.empty(), HashTreePMap.empty());
        }

        public Set<String> getFlows() {
            return flows;
        }

        public int getFlowCount(String flowId) {
            return flowCounts.get(flowId);
        }

        private Fork(POrderedSet<String> flows, PMap<String, Integer> flowCounts) {
            this.flows = flows;
            this.flowCounts = flowCounts;
        }

        public Fork increment(String flowId, int diff) {
            int c;
            Integer cobj = flowCounts.get(flowId);
            if (cobj == null) {
                c = 0;
            } else {
                c = cobj.intValue();
            }
            return new Fork(flows.plus(flowId), flowCounts.plus(flowId, c + diff));
        }
    }

    public static final class ForkKey implements Serializable {

        private static final long serialVersionUID = 1L;

        private final UUID scopeId;
        private final String elementId;

        public ForkKey(UUID scopeId, String elementId) {
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

            ForkKey that = (ForkKey) o;

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
