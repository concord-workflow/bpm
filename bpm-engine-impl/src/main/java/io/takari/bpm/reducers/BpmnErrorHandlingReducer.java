package io.takari.bpm.reducers;

import io.takari.bpm.Configuration;
import io.takari.bpm.actions.SetVariableAction;
import io.takari.bpm.api.BpmnError;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.Command;
import io.takari.bpm.commands.CommandStack;
import io.takari.bpm.commands.PerformActionsCommand;
import io.takari.bpm.state.BpmnErrorHelper;
import io.takari.bpm.state.ProcessInstance;

import java.util.Map;

public abstract class BpmnErrorHandlingReducer implements Reducer {

    private final Configuration cfg;

    protected BpmnErrorHandlingReducer(Configuration cfg) {
        this.cfg = cfg;
    }

    protected ProcessInstance handleException(ProcessInstance state, String definitionId, String elementId,
                                              Exception e, Map<String, Command> routes, Command defaultCmd) throws ExecutionException {
        Throwable cause = e;

        while (true) {
            cause = cause.getCause();
            if (cause == null) {
                break;
            }

            if (cause instanceof BpmnError) {
                break;
            }
        }

        if (cause == null) {
            if (cfg.isWrapAllExceptionsAsBpmnErrors()) {
                cause = new BpmnError(null, e);
            } else {
                throw new ExecutionException("Unhandled execution exception: " + elementId, e);
            }
        }

        return handleBpmnError(state, definitionId, elementId, (BpmnError) cause, routes, defaultCmd);
    }

    protected ProcessInstance handleBpmnError(ProcessInstance state, String definitionId, String elementId,
                                              BpmnError e, Map<String, Command> routes, Command defaultCmd) throws ExecutionException {
        Command nextCmd = null;

        // add call point information to the error
        if (e.getDefinitionId() == null) {
            e = new BpmnError(definitionId, elementId, e.getErrorRef(), e.getCause());
        }

        String errorRef = e.getErrorRef();

        if (errorRef != null) {
            if (routes != null) {
                nextCmd = routes.get(errorRef);
            }
        }

        if (nextCmd == null) {
            nextCmd = defaultCmd;
        }

        if (nextCmd == null) {
            // no boundary error events were found - an error will be raised to
            // the parent execution

            CommandStack stack = state.getStack()
                    .push(new PerformActionsCommand(BpmnErrorHelper.raiseError(definitionId, elementId, errorRef, e.getCause())));
            state = state.setStack(stack);
        } else {
            // the element has an boundary error event - the process execution
            // will follow its flow

            CommandStack stack = state.getStack()
                    .push(nextCmd)
                    .push(new PerformActionsCommand(new SetVariableAction(ExecutionContext.LAST_ERROR_KEY, e)));
            state = state.setStack(stack);
        }

        return state;
    }
}
