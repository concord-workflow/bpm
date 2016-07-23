package io.takari.bpm.state;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.Command;
import io.takari.bpm.commands.CommandStack;
import io.takari.bpm.event.Event;
import io.takari.bpm.model.VariableMapping;

public final class EventMapHelper {

    private static final Logger log = LoggerFactory.getLogger(EventMapHelper.class);
    private static final String EVENT_MAP_KEY = "__bpmn_event_map";

    public static ProcessInstance put(ProcessInstance state, Event ev, Command... commands) throws ExecutionException {
        Variables vars = state.getVariables();

        EventMap m = getMap(vars);
        if (m == null) {
            m = new EventMap();
        }

        if (m.containsEvent(ev.getDefinitionId(), ev.getId())) {
            throw new ExecutionException("Duplicate event mapping key '%s/%s'", ev.getDefinitionId(), ev.getId());
        }

        // we need to be sure that the used list implementation is serializable
        List<Command> l = new ArrayList<Command>(Arrays.asList(commands));
        m.putEvent(ev.getDefinitionId(), ev.getId(), new EventRecord(ev.getGroupId(), l));

        return state.setVariables(vars.setVariable(EVENT_MAP_KEY, m));
    }

    public static ProcessInstance remove(ProcessInstance state, String definitionId, UUID eventId) {
        Variables vars = state.getVariables();

        EventMap m = getMap(vars);
        if (m == null) {
            return state;
        }

        m.remove(definitionId, eventId);

        return state.setVariables(vars.setVariable(EVENT_MAP_KEY, m));
    }

    public static boolean isEmpty(ProcessInstance state) {
        Variables vars = state.getVariables();
        EventMap m = getMap(vars);
        return m == null || m.isEmpty();
    }

    public static boolean isEmpty(ProcessInstance state, String definitionId) {
        Variables vars = state.getVariables();
        EventMap m = getMap(vars);
        return m == null || m.isEmpty(definitionId);
    }

    public static ProcessInstance pushCommands(ProcessInstance state, String definitionId, UUID eventId) {
        EventMap m = getMap(state.getVariables());
        if (m == null) {
            // TODO isn't this an error?
            log.warn("pushCommands ['{}', '{}'] -> event map not found", state.getBusinessKey(), eventId);
            return state;
        }

        EventRecord r = m.get(definitionId, eventId);
        if (r == null) {
            // TODO an error?
            log.warn("pushCommands ['{}', '{}'] -> event record not found", state.getBusinessKey(), eventId);
            return state;
        }

        List<Command> commands = r.getCommands();
        if (commands == null) {
            // TODO really?
            return state;
        }

        CommandStack stack = state.getStack();
        for (Command c : commands) {
            stack = stack.push(c);
            log.debug("pushCommands ['{}', '{}'] -> pushed '{}'", state.getBusinessKey(), eventId, c);
        }

        return state.setStack(stack);
    }

    public static ProcessInstance clearGroup(ProcessInstance state, String definitionId, UUID groupId) {
        if (groupId == null) {
            // TODO huh?
            return state;
        }

        Variables vars = state.getVariables();
        EventMap m = getMap(vars);
        if (m == null) {
            // TODO eh?
            return state;
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
            vars = vars.removeVariable(EVENT_MAP_KEY);
        } else {
            vars = vars.setVariable(EVENT_MAP_KEY, m);
        }

        return state.setVariables(vars);
    }

    public static EventMap getMap(Variables vars) {
        Object m = vars.getVariable(EVENT_MAP_KEY);
        if (m == null) {
            return null;
        }

        if (m instanceof EventMap) {
            return (EventMap) m;
        } else {
            throw new IllegalStateException("Invalid object type, expected an instance of " + EventMap.class.getName());
        }
    }

    public static Variables set(Variables dst, EventMap m) {
        return dst.setVariable(EVENT_MAP_KEY, m);
    }

    public static VariableMapping createOutMapping() {
        return new VariableMapping(EVENT_MAP_KEY, null, EVENT_MAP_KEY);
    }

    private EventMapHelper() {
    }

    public static final class EventRecord implements Serializable {

        private static final long serialVersionUID = 1L;

        private final UUID groupId;
        private final List<Command> commands;

        public EventRecord(UUID groupId, List<Command> commands) {
            this.groupId = groupId;
            this.commands = commands;
        }

        public UUID getGroupId() {
            return groupId;
        }

        public List<Command> getCommands() {
            return commands;
        }
    }

    public static final class EventMap implements Serializable {

        private static final long serialVersionUID = 1L;

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
}
