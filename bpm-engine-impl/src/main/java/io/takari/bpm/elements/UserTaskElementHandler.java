package io.takari.bpm.elements;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.PopCommandAction;
import io.takari.bpm.actions.ProcessUserTaskAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.state.ProcessInstance;

import java.util.List;

public class UserTaskElementHandler implements ElementHandler {

    @Override
    public List<Action> handle(ProcessInstance state, ProcessElementCommand cmd, List<Action> actions) throws ExecutionException {
        actions.add(new PopCommandAction());
        actions.add(new ProcessUserTaskAction(cmd.getDefinitionId(), cmd.getElementId()));
        return actions;
    }
}
