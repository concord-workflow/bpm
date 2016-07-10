package io.takari.bpm.elements;

import java.util.List;
import java.util.Set;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.CopyEventMapAction;
import io.takari.bpm.actions.FindAndCallActivityAction;
import io.takari.bpm.actions.MakeSubProcessVariablesAction;
import io.takari.bpm.actions.PopCommandAction;
import io.takari.bpm.actions.PushCommandAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.Command;
import io.takari.bpm.commands.HandleRaisedErrorCommand;
import io.takari.bpm.commands.MergeVariablesCommand;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.model.CallActivity;
import io.takari.bpm.model.VariableMapping;
import io.takari.bpm.state.EventMapHelper;
import io.takari.bpm.state.ProcessInstance;

public class CallActivityHandler implements ElementHandler {

    @Override
    public List<Action> handle(ProcessInstance state, ProcessElementCommand cmd, List<Action> actions) throws ExecutionException {
        actions.add(new PopCommandAction());

        // add an error handling command to the stack
        Command errorHandlingCmd = new HandleRaisedErrorCommand(cmd.getDefinitionId(), cmd.getElementId(), cmd.getGroupId(),
                cmd.isExclusive());
        actions.add(new PushCommandAction(errorHandlingCmd));

        // retrive the variables mapping
        IndexedProcessDefinition pd = state.getDefinition(cmd.getDefinitionId());
        CallActivity a = (CallActivity) ProcessDefinitionUtils.findElement(pd, cmd.getElementId());
        Set<VariableMapping> inVariables = a.getIn();
        Set<VariableMapping> outVariables = a.getOut();

        // set a new variables container (aka child's "ExecutionContext") as our
        // current
        actions.add(new MakeSubProcessVariablesAction(inVariables));

        // copy the events map from the parent process to the child, so the
        // child process can track all external events
        actions.add(new CopyEventMapAction(EventMapHelper.getMap(state.getVariables())));

        // add a variables merging command to the stack
        actions.add(new PushCommandAction(new MergeVariablesCommand(state.getVariables(), outVariables)));

        // find the called process and start its execution
        CallActivity e = (CallActivity) ProcessDefinitionUtils.findElement(pd, cmd.getElementId());
        actions.add(new FindAndCallActivityAction(e.getCalledElement()));

        return actions;
    }
}
