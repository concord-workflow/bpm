package io.takari.bpm.reducers;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.RemoveInstanceAction;
import io.takari.bpm.actions.SuspendAndPersistAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.persistence.PersistenceManager;
import io.takari.bpm.state.BpmnErrorHelper;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.state.ProcessStatus;
import io.takari.bpm.state.Variables;

@Impure
public class PersistenceReducer implements Reducer {

    private final PersistenceManager persistenceManager;

    public PersistenceReducer(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    @Override
    public ProcessInstance reduce(ProcessInstance state, Action action) throws ExecutionException {
        if (action instanceof SuspendAndPersistAction) {
            state = state.setStatus(ProcessStatus.SUSPENDED);

            // clear any raised errors before saving the state
            // they should be handled by the engine after we suspend
            // the process
            ProcessInstance s = clearErrors(state);
            persistenceManager.save(s);
        } else if (action instanceof RemoveInstanceAction) {
            RemoveInstanceAction a = (RemoveInstanceAction) action;
            persistenceManager.remove(a.getInstanceId());
        }

        return state;
    }

    private static ProcessInstance clearErrors(ProcessInstance state) {
        Variables vars = state.getVariables();
        if (vars == null) {
            return state;
        }

        return state.setVariables(BpmnErrorHelper.clear(vars));
    }
}
