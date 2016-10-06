package io.takari.bpm.elements;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.InclusiveForkAction;
import io.takari.bpm.commands.ProcessElementCommand;

public class InclusiveGatewayHandler extends ParallelGatewayHandler {

    @Override
    protected Action createForkAction(ProcessElementCommand cmd) {
        return new InclusiveForkAction(cmd.getDefinitionId(), cmd.getElementId()/*, cmd.getScopeId()*/);
    }
}
