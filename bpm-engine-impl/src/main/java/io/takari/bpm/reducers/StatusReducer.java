package io.takari.bpm.reducers;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.SetStatusAction;
import io.takari.bpm.state.ProcessInstance;

public class StatusReducer implements Reducer {

    @Override
    public ProcessInstance reduce(ProcessInstance state, Action action) {
        if (action instanceof SetStatusAction) {
            SetStatusAction a = (SetStatusAction) action;
            return state.setStatus(a.getStatus());
        }

        return state;
    }
}
