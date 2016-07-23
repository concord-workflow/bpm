package io.takari.bpm.elements;

import java.util.List;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.EvaluateAndFollowFlowsAction;
import io.takari.bpm.actions.PopCommandAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.model.ExclusiveGateway;
import io.takari.bpm.state.ProcessInstance;

public class ExclusiveGatewayHandler implements ElementHandler {

    @Override
    public List<Action> handle(ProcessInstance state, ProcessElementCommand cmd, List<Action> actions) throws ExecutionException {
        actions.add(new PopCommandAction());

        IndexedProcessDefinition pd = state.getDefinition(cmd.getDefinitionId());
        ExclusiveGateway gw = (ExclusiveGateway) ProcessDefinitionUtils.findElement(pd, cmd.getElementId());
        actions.add(new EvaluateAndFollowFlowsAction(cmd.getDefinitionId(), cmd.getElementId(), gw.getDefaultFlow()));

        return actions;
    }
}
