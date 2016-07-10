package io.takari.bpm.planner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.ActivateFlowsAction;
import io.takari.bpm.actions.FollowFlowsAction;
import io.takari.bpm.actions.PopCommandAction;
import io.takari.bpm.actions.SetVariableAction;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.HandleRaisedErrorCommand;
import io.takari.bpm.model.BoundaryEvent;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.state.BpmnErrorHelper;
import io.takari.bpm.state.ProcessInstance;

public class HandleRaisedErrorCommandHandler implements CommandHandler<HandleRaisedErrorCommand> {

    private static final Logger log = LoggerFactory.getLogger(HandleRaisedErrorCommandHandler.class);

    @Override
    public List<Action> handle(ProcessInstance state, HandleRaisedErrorCommand cmd, List<Action> actions) throws ExecutionException {
        actions.add(new PopCommandAction());

        String errorRef = BpmnErrorHelper.getRaisedError(state.getVariables());
        if (errorRef == null) {
            // no errors were raised, will continue the execution
            log.debug("handle ['{}', '{}'] -> no errors, will continue from '{}'", state.getBusinessKey(), cmd.getElementId(),
                    cmd.getElementId());
            actions.add(new FollowFlowsAction(cmd.getDefinitionId(), cmd.getElementId(), cmd.getGroupId(), cmd.isExclusive()));
            return actions;
        }

        IndexedProcessDefinition pd = state.getDefinition(cmd.getDefinitionId());

        BoundaryEvent ev = ProcessDefinitionUtils.findBoundaryErrorEvent(pd, cmd.getElementId(), errorRef);
        if (ev == null) {
            // trying to find a boundary event without the specified error
            // reference
            ev = ProcessDefinitionUtils.findBoundaryErrorEvent(pd, cmd.getElementId(), null);
        }

        if (ev == null) {
            // TODO should we allow this?
            return actions;
        }

        log.debug("handle ['{}', '{}'] -> handling boundary error '{}'", state.getBusinessKey(), cmd.getElementId(), errorRef);

        // the error is handled
        actions.add(BpmnErrorHelper.clear());

        // save errorRef for later
        actions.add(new SetVariableAction(ExecutionContext.ERROR_CODE_KEY, errorRef));

        // follow the outbound flow
        actions.add(new FollowFlowsAction(cmd.getDefinitionId(), ev.getId(), cmd.getGroupId(), cmd.isExclusive()));

        // process the inactive flows
        List<SequenceFlow> flows = ProcessDefinitionUtils.findOptionalOutgoingFlows(pd, cmd.getElementId());
        actions.add(new ActivateFlowsAction(cmd.getDefinitionId(), ProcessDefinitionUtils.toIds(flows)));

        // process the inactive boundary events
        List<BoundaryEvent> evs = new ArrayList<>(ProcessDefinitionUtils.findOptionalBoundaryEvents(pd, cmd.getElementId()));
        for (Iterator<BoundaryEvent> i = evs.iterator(); i.hasNext();) {
            BoundaryEvent e = i.next();
            if (e.getId().equals(ev.getId())) {
                i.remove();
            }
        }
        actions.add(new ActivateFlowsAction(cmd.getDefinitionId(), ProcessDefinitionUtils.toIds(evs)));

        return actions;
    }
}
