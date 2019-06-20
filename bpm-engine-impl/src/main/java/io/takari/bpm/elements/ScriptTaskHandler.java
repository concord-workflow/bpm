package io.takari.bpm.elements;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.ExecuteScriptAction;
import io.takari.bpm.actions.PopCommandAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.Command;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.state.ProcessInstance;

import java.util.List;
import java.util.Map;

public class ScriptTaskHandler implements ElementHandler {

    @Override
    public List<Action> handle(ProcessInstance state, ProcessElementCommand cmd, List<Action> actions) throws ExecutionException {
        actions.add(new PopCommandAction());

        IndexedProcessDefinition pd = state.getDefinition(cmd.getDefinitionId());
        Command defaultError = ProcessDefinitionUtils.findDefaultError(pd, cmd);

        // collect all boundary error events that have an errorRef
        Map<String, Command> errors = ProcessDefinitionUtils.findErrors(pd, cmd);

        actions.add(new ExecuteScriptAction(cmd.getDefinitionId(), cmd.getElementId(), defaultError, errors));
        return actions;
    }
}
