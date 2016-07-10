package io.takari.bpm.elements;

import java.util.List;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.FollowNewGroupAction;
import io.takari.bpm.actions.PopCommandAction;
import io.takari.bpm.actions.PushCommandAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.commands.ProcessEventMappingCommand;
import io.takari.bpm.state.ProcessInstance;

public class EventBasedGatewayHandler implements ElementHandler {

    @Override
    public List<Action> handle(ProcessInstance state, ProcessElementCommand cmd, List<Action> actions) throws ExecutionException {
        actions.add(new PopCommandAction());

        // add the process suspension command to the stack. It is expected that
        // it will be called when all outgoing sequence flows of this gateway
        // is done
        actions.add(new PushCommandAction(new ProcessEventMappingCommand(cmd.getDefinitionId())));

        // mark all following flows as "exclusive"
        actions.add(new FollowNewGroupAction(cmd.getDefinitionId(), cmd.getElementId(), true));

        return actions;
    }
}
