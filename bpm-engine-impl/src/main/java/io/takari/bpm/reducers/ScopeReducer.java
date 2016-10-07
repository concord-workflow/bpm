package io.takari.bpm.reducers;

import io.takari.bpm.UuidGenerator;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.PopScopeAction;
import io.takari.bpm.actions.PushScopeAction;
import io.takari.bpm.actions.SetCurrentScopeAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.state.Events;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.state.Scopes;

import java.util.UUID;

public class ScopeReducer implements Reducer {

    private final UuidGenerator uuidGenerator;

    public ScopeReducer(UuidGenerator uuidGenerator) {
        this.uuidGenerator = uuidGenerator;
    }

    @Override
    public ProcessInstance reduce(ProcessInstance state, Action action) throws ExecutionException {
        if (action instanceof PushScopeAction) {
            PushScopeAction a = (PushScopeAction) action;
            UUID id = uuidGenerator.generate();
            return state.setScopes(state.getScopes().push(id, a.isExclusive(), a.getFinishers()));
        } else if (action instanceof PopScopeAction) {
            Scopes s = state.getScopes();
            UUID id = s.getCurrentId();

            s = s.pop();

            // check if the last scope can be removed safely
            // TODO move into a separate action?
            Events e = state.getEvents();
            if (e.isEmpty(s, id)) {
                s = s.remove(id);
            }

            return state.setScopes(s);
        } else if (action instanceof SetCurrentScopeAction) {
            SetCurrentScopeAction a = (SetCurrentScopeAction) action;
            return state.setScopes(state.getScopes().setCurrentId(a.getScopeId()));
        }

        return state;
    }
}
