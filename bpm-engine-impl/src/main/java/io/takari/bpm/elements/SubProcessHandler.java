package io.takari.bpm.elements;

import java.util.List;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.PopCommandAction;
import io.takari.bpm.actions.PushCommandAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.Command;
import io.takari.bpm.commands.HandleRaisedErrorCommand;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.StartEvent;
import io.takari.bpm.state.ProcessInstance;

public class SubProcessHandler implements ElementHandler {

    @Override
    public List<Action> handle(ProcessInstance state, ProcessElementCommand cmd, List<Action> actions) throws ExecutionException {
        actions.add(new PopCommandAction());

        // add an error handling command to the stack
        Command errorHandlingCmd = new HandleRaisedErrorCommand(cmd.getDefinitionId(), cmd.getElementId(), cmd.getGroupId(),
                cmd.isExclusive());
        actions.add(new PushCommandAction(errorHandlingCmd));

        // find an start event of the subprocess
        IndexedProcessDefinition pd = state.getDefinition(cmd.getDefinitionId());
        ProcessDefinition sub = ProcessDefinitionUtils.findSubProcess(pd, cmd.getElementId());
        StartEvent start = ProcessDefinitionUtils.findStartEvent(sub);

        // add a start command to the stack
        Command startCmd = new ProcessElementCommand(cmd.getDefinitionId(), start.getId());
        actions.add(new PushCommandAction(startCmd));

        return actions;
    }

}
