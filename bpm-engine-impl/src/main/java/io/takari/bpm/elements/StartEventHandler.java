package io.takari.bpm.elements;

import java.util.List;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.FollowFlowsAction;
import io.takari.bpm.actions.PopCommandAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.state.ProcessInstance;

public class StartEventHandler implements ElementHandler {

    @Override
    public List<Action> handle(ProcessInstance state, ProcessElementCommand cmd, List<Action> actions) throws ExecutionException {
        actions.add(new PopCommandAction());
        actions.add(new FollowFlowsAction(cmd.getDefinitionId(), cmd.getElementId(), cmd.getGroupId(), cmd.isExclusive()));
        return actions;
    }
}
