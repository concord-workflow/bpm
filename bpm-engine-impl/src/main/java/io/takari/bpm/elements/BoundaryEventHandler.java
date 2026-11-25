package io.takari.bpm.elements;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.FollowFlowsAction;
import io.takari.bpm.actions.PopCommandAction;
import io.takari.bpm.actions.RetargetFlowsAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.model.BoundaryEvent;
import io.takari.bpm.state.ProcessInstance;

import java.util.List;

public class BoundaryEventHandler implements ElementHandler {

    @Override
    public List<Action> handle(ProcessInstance state, ProcessElementCommand cmd, List<Action> actions) throws ExecutionException {
        actions.add(new PopCommandAction());

        // the token leaves the activity through the boundary event instead of its normal flows
        IndexedProcessDefinition pd = state.getDefinition(cmd.getDefinitionId());
        BoundaryEvent ev = (BoundaryEvent) ProcessDefinitionUtils.findElement(pd, cmd.getElementId());
        if (ev.getAttachedToRef() != null) {
            actions.add(new RetargetFlowsAction(cmd.getDefinitionId(), List.of(ev.getAttachedToRef()), List.of(ev.getId())));
        }

        actions.add(new FollowFlowsAction(cmd.getDefinitionId(), cmd.getElementId()));
        return actions;
    }
}
