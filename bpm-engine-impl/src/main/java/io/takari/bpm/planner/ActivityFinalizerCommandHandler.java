package io.takari.bpm.planner;

import io.takari.bpm.Configuration;
import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.*;
import io.takari.bpm.api.BpmnError;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.ActivityFinalizerCommand;
import io.takari.bpm.commands.PerformActionsCommand;
import io.takari.bpm.model.BoundaryEvent;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.state.BpmnErrorHelper;
import io.takari.bpm.state.Events;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.state.Scopes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ActivityFinalizerCommandHandler implements CommandHandler<ActivityFinalizerCommand> {

    private static final Logger log = LoggerFactory.getLogger(ActivityFinalizerCommandHandler.class);

    private final Configuration cfg;

    public ActivityFinalizerCommandHandler(Configuration cfg) {
        this.cfg = cfg;
    }

    @Override
    public List<Action> handle(ProcessInstance state, ActivityFinalizerCommand cmd, List<Action> actions) throws ExecutionException {
        actions.add(new PopCommandAction());

        Scopes scopes = state.getScopes();
        Events events = state.getEvents();
        if (!events.isEmpty(scopes, scopes.getCurrentId())) {
            // we have some events waiting in the current scope or its children
            actions.add(new PushCommandAction(new PerformActionsCommand(new PopScopeAction())));
            return actions;
        }

        BpmnError error = BpmnErrorHelper.getRaisedError(state.getVariables());
        if (error == null) {
            // no errors were raised, will continue the execution
            log.debug("handle ['{}', '{}'] -> no errors, will continue from '{}'", state.getBusinessKey(), cmd.getElementId(),
                    cmd.getElementId());
            actions.add(new FollowFlowsAction(cmd.getDefinitionId(), cmd.getElementId()));
            actions.add(new PushCommandAction(new PerformActionsCommand(new PopScopeAction())));
            return actions;
        }

        IndexedProcessDefinition pd = state.getDefinition(cmd.getDefinitionId());

        BoundaryEvent ev = ProcessDefinitionUtils.findBoundaryErrorEvent(pd, cmd.getElementId(), error.getErrorRef());
        if (ev == null) {
            // trying to find a boundary event without the specified error
            // reference
            ev = ProcessDefinitionUtils.findBoundaryErrorEvent(pd, cmd.getElementId(), null);
        }

        if (ev == null) {
            switch (cfg.getUnhandledBpmnErrorStrategy()) {
                case EXCEPTION: {
                    throw new ExecutionException("Unhandled BPMN error: " + error.getErrorRef(), error);
                }
                case PROPAGATE: {
                    log.debug("handle ['{}', '{}'] -> propagating the error: '{}'", state.getBusinessKey(), cmd.getElementId(), error.getErrorRef());
                    actions.add(new PushCommandAction(new PerformActionsCommand(new PopScopeAction())));
                    return actions;
                }
                case IGNORE: {
                    log.warn("handle ['{}', '{}'] -> unhandled BPMN error: '{}'", state.getBusinessKey(), cmd.getElementId(), error.getErrorRef());
                    actions.add(BpmnErrorHelper.clear());
                    actions.add(new PushCommandAction(new PerformActionsCommand(new PopScopeAction())));
                    return actions;
                }
            }
        }

        log.debug("handle ['{}', '{}'] -> handling boundary error '{}'", state.getBusinessKey(), cmd.getElementId(), error.getErrorRef());

        // the error is handled
        actions.add(BpmnErrorHelper.clear());

        // save the error for later
        actions.add(new SetVariableAction(ExecutionContext.LAST_ERROR_KEY, error));

        // activate the boundary error event element
        actions.add(new ActivateElementAction(cmd.getDefinitionId(), ev.getId()));

        // follow the outbound flow
        actions.add(new FollowFlowsAction(cmd.getDefinitionId(), ev.getId()));

        // process the inactive flows. Use a deferred action, so the activation will be performed after
        // the call's scope is closed
        List<SequenceFlow> flows = ProcessDefinitionUtils.findOptionalOutgoingFlows(pd, cmd.getElementId());
        actions.add(new PushCommandAction(new PerformActionsCommand(
                new ActivateFlowsAction(cmd.getDefinitionId(), ProcessDefinitionUtils.toIds(flows)))));

        // process the inactive boundary events: Use a deferred action, just as in the case above
        List<BoundaryEvent> evs = new ArrayList<>(ProcessDefinitionUtils.findOptionalBoundaryEvents(pd, cmd.getElementId()));
        for (Iterator<BoundaryEvent> i = evs.iterator(); i.hasNext(); ) {
            BoundaryEvent e = i.next();
            if (e.getId().equals(ev.getId())) {
                i.remove();
            }
        }
        actions.add(new PushCommandAction(new PerformActionsCommand(
                new ActivateFlowsAction(cmd.getDefinitionId(), ProcessDefinitionUtils.toIds(evs)))));

        // close the call's scope
        actions.add(new PushCommandAction(new PerformActionsCommand(new PopScopeAction())));

        return actions;
    }
}
