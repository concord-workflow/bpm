package io.takari.bpm.elements;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.*;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.PerformActionsCommand;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.state.Activations;
import io.takari.bpm.state.ProcessInstance;

import java.util.List;

public class ParallelGatewayHandler implements ElementHandler {

    @Override
    public List<Action> handle(ProcessInstance state, ProcessElementCommand cmd, List<Action> actions) throws ExecutionException {
        actions.add(new PopCommandAction());

        // include the current activation
        Activations acts = state.getActivations();
        int activated = acts.count(cmd.getDefinitionId(), cmd.getElementId()) + 1;

        IndexedProcessDefinition pd = state.getDefinition(cmd.getDefinitionId());
        List<SequenceFlow> in = ProcessDefinitionUtils.findIncomingFlows(pd, cmd.getElementId());
        int total = in.size();

        if (activated > total) {
            throw new ExecutionException("Incorrect number of activations for the element '%s' in the process '%s': expected %d, got %d",
                    cmd.getElementId(), cmd.getDefinitionId(), total, activated);
        } else if (activated == total) {
            // pop old scope
            actions.add(new PushCommandAction(new PerformActionsCommand(new PopScopeAction())));

            // because in some cases we need to evaluate flow expressions, most
            // of the heavy lifting is delegated to the reducer
            Action a = createForkAction(cmd);

            // we need to defer the action until the next iteration, so all
            // actions produced on this iteration, will be applied
            actions.add(new PushCommandAction(new PerformActionsCommand(a)));

            // new scope
            actions.add(new PushCommandAction(new PerformActionsCommand(new PushScopeAction(false))));
        } else {
            // join: keep processing
        }

        return actions;
    }

    protected Action createForkAction(ProcessElementCommand cmd) {
        return new ParallelForkAction(cmd.getDefinitionId(), cmd.getElementId());
    }
}
