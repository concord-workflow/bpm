package io.takari.bpm.elements;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.CreateEventAction;
import io.takari.bpm.actions.PopCommandAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.state.ProcessInstance;

import java.util.List;

public class IntermediateCatchEventHandler implements ElementHandler {

    @Override
    public List<Action> handle(ProcessInstance state, ProcessElementCommand cmd, List<Action> actions) throws ExecutionException {
        actions.add(new PopCommandAction());
        actions.add(new CreateEventAction(cmd.getDefinitionId(), cmd.getElementId()));
        return actions;
    }
}
