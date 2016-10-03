package io.takari.bpm.state;

import io.takari.bpm.Executor;
import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.FireOnStartInterceptorsAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.CommandStack;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.StartEvent;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public final class StateHelper {

    public static ProcessInstance createInitialState(Executor executor, UUID id, String businessKey,
            IndexedProcessDefinition pd, Map<String, Object> args) throws ExecutionException {

        ProcessInstance state = new ProcessInstance(id, businessKey, pd);

        // add the first command to the stack
        StartEvent start = ProcessDefinitionUtils.findStartEvent(pd);
        CommandStack stack = state.getStack();
        state = state.setStack(stack.push(new ProcessElementCommand(pd.getId(), start.getId())));

        // set external variables
        state = applyVariables(state, pd.getAttributes(), args);

        // fire interceptors
        Action a = new FireOnStartInterceptorsAction(pd.getId());
        state = executor.eval(state, Arrays.asList(a));

        return state;
    }

    public static ProcessInstance applyVariables(ProcessInstance state, Map<String, String> attr, Map<String, Object> args) {
        Variables vars = state.getVariables();

        if (attr != null) {
            for (Map.Entry<String, String> e : attr.entrySet()) {
                String k = ProcessDefinition.ATTRIBUTE_KEY_PREFIX + e.getKey();
                vars = vars.setVariable(k, e.getValue());
            }
        }

        if (args != null) {
            for (Map.Entry<String, Object> e : args.entrySet()) {
                vars = vars.setVariable(e.getKey(), e.getValue());
            }
        }

        return state.setVariables(vars);
    }

    public static boolean isDone(ProcessInstance state) {
        return state.getStack().isEmpty();
    }

    private StateHelper() {
    }
}
