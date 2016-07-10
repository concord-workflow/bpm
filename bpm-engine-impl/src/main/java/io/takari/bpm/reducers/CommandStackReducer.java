package io.takari.bpm.reducers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.ClearCommandsAction;
import io.takari.bpm.actions.PopCommandAction;
import io.takari.bpm.actions.PushCommandAction;
import io.takari.bpm.commands.CommandStack;
import io.takari.bpm.state.ProcessInstance;

public class CommandStackReducer implements Reducer {

    private static final Logger log = LoggerFactory.getLogger(CommandStackReducer.class);

    @Override
    public ProcessInstance reduce(ProcessInstance state, Action action) {
        if (action instanceof PushCommandAction) {
            PushCommandAction a = (PushCommandAction) action;
            log.debug("reduce ['{}'] -> push '{}'", state.getBusinessKey(), a.getCommand());
            return state.setStack(state.getStack().push(a.getCommand()));
        } else if (action instanceof PopCommandAction) {
            log.debug("reduce ['{}'] -> pop", state.getBusinessKey());
            return state.setStack(state.getStack().pop());
        } else if (action instanceof ClearCommandsAction) {
            log.debug("reduce ['{}'] -> clear", state.getBusinessKey());
            return state.setStack(new CommandStack());
        }

        return state;
    }
}
