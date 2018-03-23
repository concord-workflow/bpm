package io.takari.bpm.reducers;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.TerminateProcessAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.CommandStack;
import io.takari.bpm.event.EventPersistenceManager;
import io.takari.bpm.event.EventStorage;
import io.takari.bpm.state.Events;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.state.Scopes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Impure
public class TerminateEventReducer  implements Reducer {

    private static final Logger log = LoggerFactory.getLogger(EventsReducer.class);

    private final EventPersistenceManager eventManager;

    public TerminateEventReducer(EventPersistenceManager eventManager) {
        this.eventManager = eventManager;
    }

    @Override
    public ProcessInstance reduce(ProcessInstance state, Action action) {
        if (!(action instanceof TerminateProcessAction)) {
            return state;
        }

        state.getEvents().values().forEach((k, v) -> v.forEach((ek, ev) -> eventManager.remove(ek)));

        return state
                .setStack(new CommandStack())
                .setEvents(new Events())
                .setScopes(new Scopes());
    }
}
