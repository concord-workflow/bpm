package io.takari.bpm.reducers;

import io.takari.bpm.UuidGenerator;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.PopScopeAction;
import io.takari.bpm.actions.PushScopeAction;
import io.takari.bpm.actions.SetCurrentScopeAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.state.ProcessInstance;

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
            return state.setScopes(state.getScopes().pop());
        } else if (action instanceof SetCurrentScopeAction) {
            SetCurrentScopeAction a = (SetCurrentScopeAction) action;
            return state.setScopes(state.getScopes().setCurrentId(a.getScopeId()));
        }

        return state;
    }
}
