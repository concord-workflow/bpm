package io.takari.bpm.elements;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.*;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.PerformActionsCommand;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.state.Activations;
import io.takari.bpm.state.Activations.Activation;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.state.Scopes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParallelGatewayHandler implements ElementHandler {
    
    private static final Logger log = LoggerFactory.getLogger(ParallelGatewayHandler.class);

    @Override
    public List<Action> handle(ProcessInstance state, ProcessElementCommand cmd, List<Action> actions) throws ExecutionException {
        actions.add(new PopCommandAction());

        // include the current activation
        Activations acts = state.getActivations();
        Scopes scopes = state.getScopes();
        UUID scopeId = scopes.getCurrentId();

        IndexedProcessDefinition pd = state.getDefinition(cmd.getDefinitionId());
        List<SequenceFlow> in = ProcessDefinitionUtils.findIncomingFlows(pd, cmd.getElementId());
        
        log.debug("Handling parallel {} in {}", cmd.getElementId(), scopeId);
        
        List<Activation> awaiting = new ArrayList<>();

        boolean containsInactive = false;
        boolean containsIncomplete = false;
        for (SequenceFlow flow : in) {
            Activation act = acts.getActivation(scopes, scopeId, flow.getId());
            if (!act.isActivated()) {
                // wait for flows that haven't been triggered upstream yet
                containsInactive = true;
                awaiting.add(act);
                log.debug("  Inactive {}", act);
            } else if (act.getReceived() < act.getExpected()) {
                // wait for incoming flows that are expected to send in more activations
                containsIncomplete = true;
                awaiting.add(act);
                log.debug("  Incomplete {}", act);
            } else if (act.getReceived() > act.getExpected()) {
                throw new ExecutionException("Incorrect number of activations for the element '%s' in the process '%s': expected %d, got %d", 
                        cmd.getElementId(), cmd.getDefinitionId(), act.getExpected(), act.getReceived());
            } else {
                log.debug("  Completed {}", act);
            }
        }

        if (!containsInactive && !containsIncomplete) {
            log.debug("  All activations complete, resuming");

            // because in some cases we need to evaluate flow expressions, most
            // of the heavy lifting is delegated to the reducer
            Action a = createForkAction(cmd);

            // we need to defer the action until the next iteration, so all
            // actions produced on this iteration will be applied
            actions.add(new PushCommandAction(new PerformActionsCommand(a)));

        } else {
            // join: keep processing
        }

        return actions;
    }

    protected Action createForkAction(ProcessElementCommand cmd) {
        return new ParallelForkAction(cmd.getDefinitionId(), cmd.getElementId());
    }
}
