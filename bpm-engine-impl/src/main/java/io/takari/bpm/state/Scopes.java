package io.takari.bpm.state;

import io.takari.bpm.commands.Command;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;

import java.io.Serializable;
import java.util.*;

public class Scopes {

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

        private final UUID parentId;
        private final UUID id;
        private final boolean exclusive;
        private final Command[] finishers;

        public Scope(UUID parentId, UUID id, boolean exclusive, Command[] finishers) {
            this.parentId = parentId;
            this.id = id;
            this.exclusive = exclusive;
            this.finishers = finishers;
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
