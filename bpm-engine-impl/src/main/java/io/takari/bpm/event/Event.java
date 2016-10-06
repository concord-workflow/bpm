package io.takari.bpm.event;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public final class Event implements Serializable {
	
	private static final long serialVersionUID = 1L;

    private final UUID id;
    private final UUID executionId;
    private final String definitionId;
    private final UUID scopeId;
    private final String name;
    private final String processBusinessKey;
    private final boolean exclusive;
    private final Date expiredAt;

    public Event(UUID id, UUID executionId, String definitionId, UUID scopeId, String name, String processBusinessKey, boolean exclusive,
                 Date expiredAt) {
        this.id = id;
        this.executionId = executionId;
        this.definitionId = definitionId;
        this.scopeId = scopeId;
        this.name = name;
        this.processBusinessKey = processBusinessKey;
        this.exclusive = exclusive;
        this.expiredAt = expiredAt;
    }

    public UUID getId() {
        return id;
    }
    
    public UUID getExecutionId() {
        return executionId;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public UUID getScopeId() {
        return scopeId;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public Date getExpiredAt() {
        return expiredAt;
    }

    public String getProcessBusinessKey() {
        return processBusinessKey;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + Objects.hashCode(this.id);
        hash = 43 * hash + Objects.hashCode(this.name);
        hash = 43 * hash + Objects.hashCode(this.executionId);
        hash = 43 * hash + Objects.hashCode(this.scopeId);
        hash = 43 * hash + Objects.hashCode(this.processBusinessKey);
        hash = 43 * hash + (this.exclusive ? 1 : 0);
        hash = 43 * hash + Objects.hashCode(this.expiredAt);
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
        final Event other = (Event) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.executionId, other.executionId)) {
            return false;
        }
        if (!Objects.equals(this.scopeId, other.scopeId)) {
            return false;
        }
        if (!Objects.equals(this.processBusinessKey, other.processBusinessKey)) {
            return false;
        }
        if (this.exclusive != other.exclusive) {
            return false;
        }
        return Objects.equals(this.expiredAt, other.expiredAt);
    }

    // TODO replace
    @Override
    public String toString() {
        return "Event{" + "id=" + id  + ", name=" + name + ", executionId=" + executionId + ", scopeId=" + scopeId + ", processBusinessKey=" + processBusinessKey + ", exclusive=" + exclusive + ", expiredAt=" + expiredAt + '}';
    }
}
