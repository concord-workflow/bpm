package io.takari.bpm.state;

import io.takari.bpm.api.Variables;
import io.takari.bpm.commands.Command;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;

import java.io.Serializable;
import java.util.*;

public class Scopes implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID currentId;
    private final PMap<UUID, Scope> scopes;

    public Scopes() {
        this(null, HashTreePMap.empty());
    }

    private Scopes(UUID currentId, PMap<UUID, Scope> scopes) {
        this.currentId = currentId;
        this.scopes = scopes;
    }

    public UUID getCurrentId() {
        return currentId;
    }

    public Scopes setCurrentId(UUID scopeId) {
        if (!scopes.containsKey(scopeId)) {
            throw new IllegalArgumentException("Unknown scope: " + scopeId);
        }
        return new Scopes(scopeId, this.scopes);
    }

    public Scopes push(UUID id, boolean exclusive, Command... finishers) {
        Scope parent = currentId != null ? scopes.get(currentId) : null;
        UUID parentId = parent != null ? parent.getId() : null;
        Scope s = new Scope(parentId, id, exclusive, finishers);
        return new Scopes(id, this.scopes.plus(id, s));
    }

    public Scope peek() {
        if (currentId == null) {
            throw new IllegalArgumentException("No current scope");
        }

        Scope s = scopes.get(currentId);
        if (s == null) {
            throw new IllegalStateException("Invalid current scope ID: " + currentId);
        }

        return s;
    }

    public Scopes pop() {
        if (currentId == null) {
            throw new IllegalStateException("Invalid state: no scope to pop");
        }

        Scope s = scopes.get(currentId);
        if (s == null) {
            throw new IllegalStateException("Invalid current scope ID: " + currentId);
        }

        return new Scopes(s.getParentId(), this.scopes);
    }

    public Map<UUID, Scope> values() {
        return this.scopes;
    }

    /**
     * @param scopeId ID of a starting scope
     * @return a list of parent scopes, including the specified one.
     */
    public List<Scope> traverse(UUID scopeId) {
        List<Scope> result = new ArrayList<>();

        UUID id = scopeId;
        while (true) {
            if (id == null) {
                break;
            }

            Scope s = scopes.get(id);
            if (s == null) {
                throw new IllegalStateException("Scope not found: " + id);
            }

            result.add(s);

            id = s.getParentId();
        }

        return result;
    }

    /**
     * @return the ID of the scope that owns the variables of the specified scope: the nearest scope
     * (including the specified one) that has its own variables (a call activity or a subprocess with
     * a separate context) or the root scope.
     */
    public UUID getVariablesOwnerId(UUID scopeId) {
        UUID id = scopeId;
        while (id != null) {
            Scope s = scopes.get(id);
            if (s == null) {
                return null;
            }
            if (s.isOwnsVariables() || s.getParentId() == null) {
                return id;
            }
            id = s.getParentId();
        }
        return null;
    }

    /**
     * @return the ID of the root scope or {@code null}.
     */
    public UUID getRootId() {
        for (Scope s : scopes.values()) {
            if (s.getParentId() == null) {
                return s.getId();
            }
        }
        return null;
    }

    /**
     * Marks the scope as the owner of its own variables (see {@link #getVariablesOwnerId(UUID)}).
     */
    public Scopes markVariablesOwner(UUID scopeId) {
        Scope s = scopes.get(scopeId);
        if (s == null) {
            throw new IllegalArgumentException("Unknown scope: " + scopeId);
        }
        return new Scopes(currentId, scopes.plus(scopeId, s.withVariables(true, s.getVariables())));
    }

    /**
     * Stores the variables of the scope while the scope isn't the current one.
     */
    public Scopes setVariables(UUID scopeId, Variables variables) {
        Scope s = scopes.get(scopeId);
        if (s == null) {
            throw new IllegalArgumentException("Unknown scope: " + scopeId);
        }
        return new Scopes(currentId, scopes.plus(scopeId, s.withVariables(s.isOwnsVariables(), variables)));
    }

    public Scopes remove(UUID scopeId) {
        if (scopeId == currentId) {
            throw new IllegalArgumentException("Can't remove the current scope: " + scopeId);
        }
        return new Scopes(currentId, scopes.minus(scopeId));
    }

    @Override
    public String toString() {
        return "Scopes[" +
                "currentId=" + currentId +
                ", scopes=" + scopes +
                ']';
    }

    public static class Scope implements Serializable {

        private static final long serialVersionUID = 1L;

        private final UUID parentId;
        private final UUID id;
        private final boolean exclusive;
        private final Command[] finishers;
        private final boolean ownsVariables;
        private final Variables variables;

        public Scope(UUID parentId, UUID id, boolean exclusive, Command[] finishers) {
            this(parentId, id, exclusive, finishers, false, null);
        }

        private Scope(UUID parentId, UUID id, boolean exclusive, Command[] finishers, boolean ownsVariables, Variables variables) {
            this.parentId = parentId;
            this.id = id;
            this.exclusive = exclusive;
            this.finishers = finishers;
            this.ownsVariables = ownsVariables;
            this.variables = variables;
        }

        private Scope withVariables(boolean ownsVariables, Variables variables) {
            return new Scope(parentId, id, exclusive, finishers, ownsVariables, variables);
        }

        /**
         * @return {@code true} if the scope has its own variables, e.g. a call activity.
         */
        public boolean isOwnsVariables() {
            return ownsVariables;
        }

        /**
         * @return the scope's variables saved while the scope isn't the current one or {@code null}.
         */
        public Variables getVariables() {
            return variables;
        }

        public UUID getParentId() {
            return parentId;
        }

        public UUID getId() {
            return id;
        }

        public boolean isExclusive() {
            return exclusive;
        }

        public Command[] getFinishers() {
            return finishers;
        }

        @Override
        public String toString() {
            return "Scope[" +
                    "parentId=" + parentId +
                    ", id=" + id +
                    ", exclusive=" + exclusive +
                    ", finishers=" + Arrays.toString(finishers) +
                    ']';
        }
    }
}
