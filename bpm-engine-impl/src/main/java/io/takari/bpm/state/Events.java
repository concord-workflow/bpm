package io.takari.bpm.state;

import io.takari.bpm.commands.Command;
import io.takari.bpm.commands.CommandStack;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.takari.bpm.state.Scopes.Scope;

public class Events implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(Events.class);

    private final PMap<UUID, PMap<UUID, EventRecord>> events;

    public Events() {
        this(HashTreePMap.empty());
    }

    private Events(PMap<UUID, PMap<UUID, EventRecord>> events) {
        this.events = events;
    }

    public Events addEvent(UUID scopeId, UUID eventId, String name, Command... commands) {
        PMap<UUID, EventRecord> scope = events.get(scopeId);
        if (scope == null) {
            scope = HashTreePMap.empty();
        }

        EventRecord r = new EventRecord(name, commands);
        scope = scope.plus(eventId, r);

        return new Events(this.events.plus(scopeId, scope));
    }

    public Events clearScope(UUID scopeId) {
        return new Events(this.events.minus(scopeId));
    }

    public Events removeEvent(UUID scopeId, UUID eventId) {
        PMap<UUID, EventRecord> scope = this.events.get(scopeId);
        if (scope == null) {
            throw new IllegalArgumentException("Scope not found: " + scopeId);
        }

        scope = scope.minus(eventId);
        if (scope.isEmpty()) {
            return new Events(this.events.minus(scopeId));
        } else {
            return new Events(this.events.plus(scopeId, scope));
        }
    }

    public ProcessInstance pushCommands(ProcessInstance state, UUID scopeId, UUID eventId) {
        Map<UUID, EventRecord> scope = events.get(scopeId);
        if (scope == null) {
            throw new IllegalArgumentException("Scope not found: " + scopeId);
        }

        EventRecord r = scope.get(eventId);
        if (r == null) {
            throw new IllegalArgumentException("Event not found: " + eventId);
        }

        if (r.getCommands() == null || r.getCommands().length == 0) {
            log.warn("pushCommands ['{}', '{}'] -> event has no commands", state.getBusinessKey(), eventId);
            return state;
        }

        CommandStack stack = state.getStack();
        for (Command c : r.getCommands()) {
            stack = stack.push(c);
        }
        return state.setStack(stack);
    }

    public boolean isEmpty() {
        int count = 0;
        for (Map.Entry<?, PMap<UUID, EventRecord>> e : events.entrySet()) {
            count += e.getValue().size();
        }
        return count == 0;
    }

    public boolean isEmpty(Scopes scopes, UUID scopeId) {
        Map<UUID, Scope> m = scopes.values();

        Map<?, ?> em = events.get(scopeId);
        if (em != null && !em.isEmpty()) {
            log.debug("isEmpty ['{}'] -> false", scopeId);
            return false;
        }

        for (Map.Entry<?, Scope> e : m.entrySet()) {
            Scope next = e.getValue();
            if (scopeId.equals(next.getParentId())) {
                if (!isEmpty(scopes, next.getId())) {
                    return false;
                }
            }
        }

        log.debug("isEmpty ['{}'] -> true", scopeId);
        return true;
    }

    public Map<UUID, Map<UUID, EventRecord>> values() {
        Map<UUID, Map<UUID, EventRecord>> result = new HashMap<>(events.size());
        for (Map.Entry<UUID, PMap<UUID, EventRecord>> e : events.entrySet()) {
            UUID k = e.getKey();
            PMap<UUID, EventRecord> v = e.getValue();
            result.put(k, v);
        }
        return result;
    }

    public static final class EventRecord implements Serializable {

        private final String name;
        private final Command[] commands;

        public EventRecord(String name, Command... commands) {
            this.name = name;
            this.commands = commands;
        }

        public Command[] getCommands() {
            return commands;
        }

        @Override
        public String toString() {
            return "EventRecord[" +
                    "name=" + name +
                    ", commands=" + Arrays.toString(commands) +
                    ']';
        }
    }
}
