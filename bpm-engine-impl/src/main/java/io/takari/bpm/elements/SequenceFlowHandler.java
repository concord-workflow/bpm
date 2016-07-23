package io.takari.bpm.elements;

import java.util.List;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.PopCommandAction;
import io.takari.bpm.actions.ProcessFlowListenersAction;
import io.takari.bpm.actions.PushCommandAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.state.ProcessInstance;

public class SequenceFlowHandler implements ElementHandler {

    @Override
    public List<Action> handle(ProcessInstance state, ProcessElementCommand cmd, List<Action> actions) throws ExecutionException {
        IndexedProcessDefinition pd = state.getDefinition(cmd.getDefinitionId());

        SequenceFlow f = (SequenceFlow) ProcessDefinitionUtils.findElement(pd, cmd.getElementId());

        actions.add(new PopCommandAction());
        actions.add(new PushCommandAction(new ProcessElementCommand(pd.getId(), f.getTo(), cmd.getGroupId(), cmd.isExclusive())));
        actions.add(new ProcessFlowListenersAction(cmd.getDefinitionId(), cmd.getElementId()));

        return actions;
    }
}
