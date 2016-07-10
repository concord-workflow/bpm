package io.takari.bpm;

import java.util.List;

import io.takari.bpm.actions.Action;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.state.ProcessInstance;

/**
 * Executor performs specified actions, changing the state of the process.
 */
public interface Executor {

    ProcessInstance eval(ProcessInstance instance, List<Action> actions) throws ExecutionException;
}
