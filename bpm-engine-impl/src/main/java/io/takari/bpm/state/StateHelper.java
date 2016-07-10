package io.takari.bpm.state;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.takari.bpm.Executor;
import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.FireOnStartInterceptorsAction;
import io.takari.bpm.actions.SetVariableAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.CommandStack;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.model.StartEvent;

public final class StateHelper {

    public static ProcessInstance createInitialState(Executor executor, UUID id, String businessKey,
            IndexedProcessDefinition pd, Map<String, Object> externalVariables) throws ExecutionException {

        ProcessInstance state = new ProcessInstance(id, businessKey, pd);

        // add the first command to the stack
        StartEvent start = ProcessDefinitionUtils.findStartEvent(pd);
        CommandStack stack = state.getStack();
        state = state.setStack(stack.push(new ProcessElementCommand(pd.getId(), start.getId())));

        // set external variables
        List<Action> actions = new ArrayList<>();
        if (externalVariables != null) {
            for (Map.Entry<String, Object> e : externalVariables.entrySet()) {
                String key = e.getKey();
                Object v = e.getValue();

                if (!(v instanceof Serializable)) {
                    throw new ExecutionException("Process variables must be serializable. Got: " + v);
                }

                actions.add(new SetVariableAction(key, (Serializable) v));
            }
        }

        // fire interceptors
        actions.add(new FireOnStartInterceptorsAction(pd.getId()));

        return executor.eval(state, actions);
    }

    public static boolean isDone(ProcessInstance state) {
        return state.getStack().isEmpty();
    }

    private StateHelper() {
    }
}
