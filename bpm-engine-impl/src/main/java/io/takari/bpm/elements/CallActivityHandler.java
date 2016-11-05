package io.takari.bpm.elements;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.*;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.*;
import io.takari.bpm.model.CallActivity;
import io.takari.bpm.model.VariableMapping;
import io.takari.bpm.state.ProcessInstance;

public class CallActivityHandler implements ElementHandler {

    @Override
    public List<Action> handle(ProcessInstance state, ProcessElementCommand cmd, List<Action> actions) throws ExecutionException {
        actions.add(new PopCommandAction());

        // add an error handling command to the stack
        Command errorHandlingCmd = new ActivityFinalizerCommand(cmd.getDefinitionId(), cmd.getElementId());
        actions.add(new PushCommandAction(errorHandlingCmd));

        List<Command> finishers = new ArrayList<>();
        finishers.add(errorHandlingCmd);

        // retrieve the variables mapping
        IndexedProcessDefinition pd = state.getDefinition(cmd.getDefinitionId());
        CallActivity a = (CallActivity) ProcessDefinitionUtils.findElement(pd, cmd.getElementId());

        Set<VariableMapping> inVariables = a.getIn();
        if (inVariables == null) {
            inVariables = new HashSet<>();
        }
        inVariables.add(new VariableMapping(ExecutionContext.PROCESS_BUSINESS_KEY, null, ExecutionContext.PROCESS_BUSINESS_KEY));

        Set<VariableMapping> outVariables = a.getOut();

        // set a new variables container (aka child's "ExecutionContext") as our
        // current
        actions.add(new MakeSubProcessVariablesAction(inVariables, a.isCopyAllVariables()));

        // copy the events map from the parent process to the child, so the
        // child process can track all external events
        //actions.add(new CopyEventMapAction(EventMapHelper.getMap(state.getVariables())));

        // add a variables merging command to the stack
        Command mergeCommand = new MergeVariablesCommand(state.getVariables(), outVariables);
        finishers.add(mergeCommand);
        actions.add(new PushCommandAction(mergeCommand));

        // find the called process and start its execution
        CallActivity e = (CallActivity) ProcessDefinitionUtils.findElement(pd, cmd.getElementId());
        actions.add(new FindAndCallActivityAction(e.getCalledElement()));

        // push the new scope when the call begins
        // the scope will be "popped" by one of the finishing actions
        actions.add(new PushCommandAction(new PerformActionsCommand(new PushScopeAction(false, finishers.toArray(new Command[finishers.size()])))));

        return actions;
    }
}
