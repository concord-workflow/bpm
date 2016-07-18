package io.takari.bpm;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.ExecutionCommand;
import io.takari.bpm.event.Event;
import io.takari.bpm.model.VariableMapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class EventMapHelper {

    private static final String EVENT_MAP_KEY = "__bpmn_event_map";

    public static void put(DefaultExecution e, String definitionId, Event ev, ExecutionCommand... commands) throws ExecutionException {
        ExecutionContext ctx = e.getContext();
        EventMap m = (EventMap) ctx.getVariable(EVENT_MAP_KEY);
        if (m == null) {
            m = new EventMap();
        }
        
        if (m.containsEvent(definitionId, ev.getId())) {
            throw new ExecutionException("Duplicate event mapping key '%s/%s'", definitionId, ev.getId());
        }
        
        List<ExecutionCommand> l = new ArrayList<>();
        l.addAll(Arrays.asList(commands));
        
        m.putEvent(definitionId, ev.getId(), new EventRecord(ev.getGroupId(), l));
        ctx.setVariable(EVENT_MAP_KEY, m);
    }
    
    public static void remove(DefaultExecution e, String definitionId, UUID eventId) {
        ExecutionContext ctx = e.getContext();
        EventMap m = (EventMap) ctx.getVariable(EVENT_MAP_KEY);
        if (m == null) {
            return;
        }

        m.remove(definitionId, eventId);
        ctx.setVariable(EVENT_MAP_KEY, m);
    }

    public static boolean isEmpty(DefaultExecution e) {
        ExecutionContext ctx = e.getContext();
        EventMap m = (EventMap) ctx.getVariable(EVENT_MAP_KEY);
        return m == null || m.isEmpty();
    }
    
    public static boolean isEmpty(DefaultExecution e, String definitionId) {
        ExecutionContext ctx = e.getContext();
        EventMap m = (EventMap) ctx.getVariable(EVENT_MAP_KEY);
        return m == null || m.isEmpty(definitionId);
    }

    public static void pushCommands(DefaultExecution e, String definitionId, UUID eventId) {
        ExecutionContext ctx = e.getContext();

        EventMap m = (EventMap) ctx.getVariable(EVENT_MAP_KEY);
        if (m == null) {
            return;
        }

        EventRecord r = m.get(definitionId, eventId);
        if (r == null) {
            return;
        }

        List<ExecutionCommand> l = r.getCommands();
        if (l == null) {
            return;
        }
        
        for (ExecutionCommand c : l) {
            e.push(c);
        }
    }

    public static void clearGroup(DefaultExecution s, String definitionId, UUID groupId) {
        if (groupId == null) {
            return;
        }

        ExecutionContext ctx = s.getContext();
        EventMap m = (EventMap) ctx.getVariable(EVENT_MAP_KEY);
        if (m == null) {
            return;
        }

        List<UUID> toDelete = new ArrayList<>();
        for (Map.Entry<UUID, EventRecord> e : m.entrySet(definitionId)) {
            EventRecord r = e.getValue();
            if (groupId.equals(r.getGroupId())) {
                toDelete.add(e.getKey());
            }
        }

        for (UUID id : toDelete) {
            m.remove(definitionId, id);
        }

        if (m.isEmpty()) {
            ctx.removeVariable(EVENT_MAP_KEY);
        } else {
            ctx.setVariable(EVENT_MAP_KEY, m);
        }
    }

    public static void link(ExecutionContext src, ExecutionContext dst) {
        dst.setVariable(EVENT_MAP_KEY, src.getVariable(EVENT_MAP_KEY));
    }

    public static void addOutMapping(Set<VariableMapping> out) {
        out.add(new VariableMapping(EVENT_MAP_KEY, null, EVENT_MAP_KEY));
    }

    public static final class EventMap implements Serializable {

        private final Map<String, Map<UUID, EventRecord>> events = new HashMap<>();

        public boolean containsEvent(String definitionId, UUID eventId) {
            Map<UUID, EventRecord> m = events.get(definitionId);
            return m != null && m.containsKey(eventId);
        }

        public void putEvent(String definitionId, UUID eventId, EventRecord r) {
            Map<UUID, EventRecord> m = events.get(definitionId);
            if (m == null) {
                m = new HashMap<>();
                events.put(definitionId, m);
            }
            m.put(eventId, r);
        }

        public EventRecord get(String definitionId, UUID eventId) {
            Map<UUID, EventRecord> m = events.get(definitionId);
            return m != null ? m.get(eventId) : null;
        }

        public Set<Map.Entry<UUID, EventRecord>> entrySet(String definitionId) {
            Map<UUID, EventRecord> m = events.get(definitionId);
            return m != null ? m.entrySet() : Collections.emptySet();
        }

        public void remove(String definitionId, UUID eventId) {
            Map<UUID, EventRecord> m = events.get(definitionId);
            if (m == null) {
                return;
            }
            m.remove(eventId);
        }

        public boolean isEmpty() {
            int count = 0;
            for (Map.Entry<?, Map<UUID, EventRecord>> e : events.entrySet()) {
                Map<?, ?> m = e.getValue();
                count += m.size();
            }
            return count == 0;
        }

        public boolean isEmpty(String definitionId) {
            Map<UUID, EventRecord> m = events.get(definitionId);
            return m == null || m.isEmpty();
        }
    }

    public static final class EventRecord implements Serializable {
    	
    	private static final long serialVersionUID = 1L;

        private final UUID groupId;
        private final List<ExecutionCommand> commands;

        public EventRecord(UUID groupId, List<ExecutionCommand> commands) {
            this.groupId = groupId;
            this.commands = commands;
        }

        public UUID getGroupId() {
            return groupId;
        }

        public List<ExecutionCommand> getCommands() {
            return commands;
        }
    }
}
