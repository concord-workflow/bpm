package io.takari.bpm.elements;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.MakeSubProcessVariablesAction;
import io.takari.bpm.actions.PopCommandAction;
import io.takari.bpm.actions.PushCommandAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.Command;
import io.takari.bpm.commands.HandleRaisedErrorCommand;
import io.takari.bpm.commands.MergeVariablesCommand;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.StartEvent;
import io.takari.bpm.model.SubProcess;
import io.takari.bpm.model.VariableMapping;
import io.takari.bpm.state.ProcessInstance;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SubProcessHandler implements ElementHandler {

    @Override
    public List<Action> handle(ProcessInstance state, ProcessElementCommand cmd, List<Action> actions) throws ExecutionException {
        actions.add(new PopCommandAction());

        IndexedProcessDefinition pd = state.getDefinition(cmd.getDefinitionId());
        SubProcess s = (SubProcess) ProcessDefinitionUtils.findElement(pd, cmd.getElementId());

        // add an error handling command to the stack
        Command errorHandlingCmd = new HandleRaisedErrorCommand(cmd.getDefinitionId(), cmd.getElementId(), cmd.getGroupId(),
                cmd.isExclusive());
        actions.add(new PushCommandAction(errorHandlingCmd));

        if (s.isUseSeparateContext()) {
            // set a new variables container (aka child's "ExecutionContext") as our current
            actions.add(new MakeSubProcessVariablesAction(true));

            Set<VariableMapping> outVariables = s.getOutVariables();
            if (outVariables == null) {
                outVariables = Collections.emptySet();
            }

            // restore the original variables
            actions.add(new PushCommandAction(new MergeVariablesCommand(state.getVariables(), outVariables)));
        }

        // find an start event of the subprocess
        ProcessDefinition sub = ProcessDefinitionUtils.findSubProcess(pd, cmd.getElementId());
        StartEvent start = ProcessDefinitionUtils.findStartEvent(sub);

        // add a start command to the stack
        Command startCmd = new ProcessElementCommand(cmd.getDefinitionId(), start.getId());
        actions.add(new PushCommandAction(startCmd));

        return actions;
    }

}
