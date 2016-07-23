package io.takari.bpm.planner;

import java.util.List;

import io.takari.bpm.actions.Action;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.state.ProcessInstance;

/**
 * Planner creates a list of actions that are required to transition the
 * specified process instance into a new state.
 */
public interface Planner {

    List<Action> eval(ProcessInstance instance) throws ExecutionException;
}
