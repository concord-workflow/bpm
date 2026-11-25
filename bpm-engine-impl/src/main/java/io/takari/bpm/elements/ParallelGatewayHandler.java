package io.takari.bpm.elements;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.*;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.PerformActionsCommand;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.state.ProcessInstance;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParallelGatewayHandler implements ElementHandler {
    
    private static final Logger log = LoggerFactory.getLogger(ParallelGatewayHandler.class);

    @Override
    public List<Action> handle(ProcessInstance state, ProcessElementCommand cmd, List<Action> actions) throws ExecutionException {
        actions.add(new PopCommandAction());

        IndexedProcessDefinition pd = state.getDefinition(cmd.getDefinitionId());

        log.debug("Handling parallel {} in {}", cmd.getElementId(), state.getScopes().getCurrentId());

        // the current activation is already included
        if (ProcessDefinitionUtils.isJoinComplete(state, pd, cmd.getElementId(), false)) {
            log.debug("  All activations complete, resuming");

            // because in some cases we need to evaluate flow expressions, most
            // of the heavy lifting is delegated to the reducer
            Action a = createForkAction(cmd);

            // we need to defer the action until the next iteration, so all
            // actions produced on this iteration will be applied
            actions.add(new PushCommandAction(new PerformActionsCommand(a)));
        } else {
            // join: wait for the rest of the tokens
            log.debug("  Waiting for more activations");
        }

        return actions;
    }

    protected Action createForkAction(ProcessElementCommand cmd) {
        return new ParallelForkAction(cmd.getDefinitionId(), cmd.getElementId());
    }
}
