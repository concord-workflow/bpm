package io.takari.bpm.handlers;

import io.takari.bpm.AbstractEngine;
import io.takari.bpm.DefaultExecution;
import io.takari.bpm.FlowUtils;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.commands.ProcessEventMappingCommand;
import java.util.UUID;

public class EventBasedGatewayHandler extends AbstractElementHandler {

    public EventBasedGatewayHandler(AbstractEngine engine) {
        super(engine);
    }

    @Override
    public void handle(DefaultExecution s, ProcessElementCommand c) throws ExecutionException {
        s.pop();

        // add the process suspension command to the stack. It is expected that
        // it will be called when all outgoing sequence flows of this gateway
        // is done.
        s.push(new ProcessEventMappingCommand(c.getProcessDefinitionId()));

        UUID groupId = getEngine().getUuidGenerator().generate();

        // add all elements of outgoing flows of this gateway to the stack 
        // and mark them with the 'exclusiveness' flag (this way only one flow
        // can be completed)
        FlowUtils.followFlows(getEngine(), s, c, groupId, true);
    }
}
