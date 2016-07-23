package io.takari.bpm.reducers;

import io.takari.bpm.actions.Action;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.state.ProcessInstance;

public class CombiningReducer implements Reducer {

    private final Reducer[] reducers;

    public CombiningReducer(Reducer... reducers) {
        this.reducers = reducers;
    }

    @Override
    public ProcessInstance reduce(ProcessInstance state, Action action) throws ExecutionException {
        for (Reducer r : reducers) {
            state = r.reduce(state, action);
        }

        return state;
    }
}
