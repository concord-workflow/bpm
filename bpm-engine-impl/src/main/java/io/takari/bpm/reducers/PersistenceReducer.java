package io.takari.bpm.reducers;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.RemoveInstanceAction;
import io.takari.bpm.actions.SuspendAndPersistAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.persistence.PersistenceManager;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.state.ProcessStatus;

@Impure
public class PersistenceReducer implements Reducer {

    private final PersistenceManager persistenceManager;

    public PersistenceReducer(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    @Override
    public ProcessInstance reduce(ProcessInstance state, Action action) throws ExecutionException {
        if (action instanceof SuspendAndPersistAction) {
            // TODO split into two actions?
            state = state.setStatus(ProcessStatus.SUSPENDED);
            persistenceManager.save(state);
        } else if (action instanceof RemoveInstanceAction) {
            RemoveInstanceAction a = (RemoveInstanceAction) action;
            persistenceManager.remove(a.getInstanceId());
        }

        return state;
    }
}
